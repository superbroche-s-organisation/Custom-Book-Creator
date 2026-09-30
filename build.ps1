param(
    [Parameter(Mandatory = $false)]
    [string]$MCreatorRoot = "D:\MCreator",
    [ValidateSet("2026.2", "2024.4", "2022.2")]
    [string]$TargetMCreator = "2026.2",
    # Development only: unvalidated profiles never enter a normal release ZIP.
    [switch]$IncludeExperimental,
    [switch]$PassThru
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$MCreatorRoot = (Resolve-Path -LiteralPath $MCreatorRoot).Path
$mcreatorExe = Join-Path $MCreatorRoot "mcreator.exe"
$coreArchive = if ($TargetMCreator -eq "2022.2") { Join-Path $MCreatorRoot "lib/mcreator.jar" } else { $mcreatorExe }
$javaRelease = if ($TargetMCreator -eq "2022.2") { 17 } else { 21 }
$jdkBin = Join-Path $MCreatorRoot "jdk\bin"
$javac = Join-Path $jdkBin "javac.exe"
$jar = Join-Path $jdkBin "jar.exe"
$libDir = Join-Path $MCreatorRoot "lib"

foreach ($requiredPath in @($coreArchive, $javac, $jar, $libDir)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required MCreator file or directory not found: $requiredPath"
    }
}

$pluginManifest = Get-Content -LiteralPath (Join-Path $projectRoot "src\main\resources\plugin.json") -Raw -Encoding UTF8 | ConvertFrom-Json
$pluginVersion = [string]$pluginManifest.info.version
if ($IncludeExperimental) {
    $pluginVersion += "-experimental";
    $pluginManifest.info.version = $pluginVersion
}
if ($pluginVersion -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$') {
    throw "plugin.json contains an empty or unsafe plugin version: $pluginVersion"
}

$systemTemporary = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd([char[]]'\/')
$temporaryName = "custom-book-creator-build-" + [guid]::NewGuid().ToString("N")
$temporaryRoot = Join-Path $systemTemporary $temporaryName
$coreClasses = Join-Path $temporaryRoot "mcreator-core"
$classesDir = Join-Path $temporaryRoot "classes"
$stagingDir = Join-Path $temporaryRoot "plugin"
$temporaryZip = Join-Path $temporaryRoot "plugin.zip"

try {
    New-Item -ItemType Directory -Path $coreClasses, $classesDir, $stagingDir -Force | Out-Null

    Push-Location $coreClasses
    try {
        & $jar xf $coreArchive net META-INF
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to extract the MCreator classes."
        }
    }
    finally {
        Pop-Location
    }

    $libraries = @(Get-ChildItem -LiteralPath $libDir -Filter "*.jar" -File | Sort-Object Name | ForEach-Object { $_.FullName })
    # Use extracted classes directly. This avoids javac opening a writable ZIP
    # filesystem for a temporary core JAR on Windows/JDK 25.
    $classPath = (@($coreClasses) + $libraries) -join [System.IO.Path]::PathSeparator
    $sourceRoot = Join-Path $projectRoot "src\main\java"
    if ($TargetMCreator -ne "2026.2") {
        $adaptedSources = Join-Path $temporaryRoot "sources"
        Copy-Item -LiteralPath $sourceRoot -Destination $adaptedSources -Recurse
        $adapter = if ($TargetMCreator -eq "2022.2") { "prepare-mcreator2022.ps1" } else { "prepare-mcreator2024.ps1" }
        & (Join-Path $projectRoot $adapter) -SourceRoot $adaptedSources
        $sourceRoot = $adaptedSources
    }
    $javaSources = @(Get-ChildItem -LiteralPath $sourceRoot -Filter "*.java" -File -Recurse | Sort-Object FullName | ForEach-Object { $_.FullName })
    if ($javaSources.Count -eq 0) {
        throw "No plugin Java sources were found."
    }

    & $javac --release $javaRelease -proc:none -encoding UTF-8 -cp $classPath -d $classesDir $javaSources
    if ($LASTEXITCODE -ne 0) {
        throw "Plugin Java compilation failed."
    }

    Copy-Item -Path (Join-Path $projectRoot "src\main\resources\*") -Destination $stagingDir -Recurse -Force
    & (Join-Path $projectRoot "prepare-compatibility.ps1") -ResourcesRoot $stagingDir
    & (Join-Path $projectRoot "prepare-modern-compatibility.ps1") -ResourcesRoot $stagingDir
    & (Join-Path $projectRoot "prepare-writing.ps1") -ResourcesRoot $stagingDir
    if ($TargetMCreator -eq "2022.2") {
        & (Join-Path $projectRoot "prepare-legacy-compatibility.ps1") -ResourcesRoot $stagingDir
    }
    $releaseProfiles = switch ($TargetMCreator) {
        "2022.2" { @("forge-1.17.1") }
        "2024.4" { @("forge-1.20.1") }
        default { @("forge-1.20.1", "neoforge-1.21.1", "neoforge-1.21.8", "neoforge-26.1.2", "neoforge-26.2", "fabric-26.2") }
    }
    if (-not $IncludeExperimental) {
        foreach ($profile in Get-ChildItem -LiteralPath $stagingDir -Directory) {
            if ($profile.Name -notin $releaseProfiles -and (Test-Path -LiteralPath (Join-Path $profile.FullName "custombook.definition.yaml"))) {
                if ((Split-Path -Parent $profile.FullName) -ne $stagingDir) { throw "Unexpected staging path" }
                Remove-Item -LiteralPath $profile.FullName -Recurse -Force
            }
        }
    }
    Copy-Item -Path (Join-Path $classesDir "*") -Destination $stagingDir -Recurse -Force
    if ($TargetMCreator -ne "2026.2") {
        $keptProfile = if ($TargetMCreator -eq "2022.2") { "forge-1.17.1" } else { "forge-1.20.1" }
        if ($TargetMCreator -eq "2022.2") {
            $pluginManifest.PSObject.Properties.Remove("supportedversions")
            $pluginManifest | Add-Member minversion 202200200000
            $pluginManifest | Add-Member maxversion 202200300000
        } else {
            $pluginManifest.supportedversions = @(2024004)
        }
        $pluginManifest.info.description = "Custom Book Creator for MCreator $TargetMCreator / $keptProfile."
        $pluginManifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $stagingDir "plugin.json") -Encoding utf8
        # Only strip generated staging directories, never project sources or installed generators.
        foreach ($profile in Get-ChildItem -LiteralPath $stagingDir -Directory) {
            if ($profile.Name -ne $keptProfile -and (Test-Path -LiteralPath (Join-Path $profile.FullName "custombook.definition.yaml"))) {
                if ((Split-Path -Parent $profile.FullName) -ne $stagingDir) { throw "Unexpected staging path" }
                Remove-Item -LiteralPath $profile.FullName -Recurse -Force
            }
        }
    }
    $catalog = @(foreach ($profile in Get-ChildItem -LiteralPath $stagingDir -Directory | Sort-Object Name) {
        if (-not (Test-Path -LiteralPath (Join-Path $profile.FullName "custombook.definition.yaml"))) { continue }
        $parts = $profile.Name.Split("-", 2)
        [ordered]@{ id=$profile.Name; mcreator=$TargetMCreator; minecraft=$parts[1]; loader=$parts[0];
            status=$(if ($profile.Name -in $releaseProfiles) {"api-compiled"} else {"experimental"});
            gameplayTested=$false; requiresInstalledGenerator=$true }
    })
    [IO.File]::WriteAllText((Join-Path $stagingDir "custombook-compatibility.json"),
        (ConvertTo-Json -InputObject $catalog -Depth 4), [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $stagingDir "plugin.json"),
        ($pluginManifest | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))

    $distDir = Join-Path $projectRoot "dist"
    New-Item -ItemType Directory -Path $distDir -Force | Out-Null
    $pluginZip = Join-Path $distDir ("CustomBookCreator-MCreator" + $TargetMCreator + "-v" + $pluginVersion + ".zip")
    & $jar --create --file $temporaryZip --no-manifest -C $stagingDir .
    if ($LASTEXITCODE -ne 0) {
        throw "Plugin archive creation failed."
    }
    # Do not replace the previous successful build until archive creation ends.
    Copy-Item -LiteralPath $temporaryZip -Destination $pluginZip -Force
    Write-Host "Plugin created: $pluginZip"
    if ($PassThru) {
        Write-Output $pluginZip
    }
}
finally {
    if (Test-Path -LiteralPath $temporaryRoot) {
        $resolvedTemporary = (Resolve-Path -LiteralPath $temporaryRoot).Path
        $temporaryItem = Get-Item -LiteralPath $resolvedTemporary -Force
        $isExpectedDirectory = $resolvedTemporary.Equals($temporaryRoot, [System.StringComparison]::OrdinalIgnoreCase) -and
            (Split-Path -Parent $resolvedTemporary).Equals($systemTemporary, [System.StringComparison]::OrdinalIgnoreCase) -and
            (Split-Path -Leaf $resolvedTemporary) -eq $temporaryName -and
            -not ($temporaryItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint)
        if (-not $isExpectedDirectory) {
            throw "Refusing to clean an unexpected temporary path: $resolvedTemporary"
        }
        Remove-Item -LiteralPath $resolvedTemporary -Recurse -Force
    }
}
