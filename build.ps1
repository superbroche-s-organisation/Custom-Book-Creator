param(
    [Parameter(Mandatory = $false)]
    [string]$MCreatorRoot = "D:\MCreator",
    [switch]$PassThru
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$MCreatorRoot = (Resolve-Path -LiteralPath $MCreatorRoot).Path
$mcreatorExe = Join-Path $MCreatorRoot "mcreator.exe"
$jdkBin = Join-Path $MCreatorRoot "jdk\bin"
$javac = Join-Path $jdkBin "javac.exe"
$jar = Join-Path $jdkBin "jar.exe"
$libDir = Join-Path $MCreatorRoot "lib"

foreach ($requiredPath in @($mcreatorExe, $javac, $jar, $libDir)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required MCreator file or directory not found: $requiredPath"
    }
}

$pluginManifest = Get-Content -LiteralPath (Join-Path $projectRoot "src\main\resources\plugin.json") -Raw -Encoding UTF8 | ConvertFrom-Json
$pluginVersion = [string]$pluginManifest.info.version
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
        & $jar xf $mcreatorExe net META-INF
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
    $javaSources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot "src\main\java") -Filter "*.java" -File -Recurse | Sort-Object FullName | ForEach-Object { $_.FullName })
    if ($javaSources.Count -eq 0) {
        throw "No plugin Java sources were found."
    }

    & $javac --release 21 -encoding UTF-8 -cp $classPath -d $classesDir $javaSources
    if ($LASTEXITCODE -ne 0) {
        throw "Plugin Java compilation failed."
    }

    Copy-Item -Path (Join-Path $projectRoot "src\main\resources\*") -Destination $stagingDir -Recurse -Force
    Copy-Item -Path (Join-Path $classesDir "*") -Destination $stagingDir -Recurse -Force

    $distDir = Join-Path $projectRoot "dist"
    New-Item -ItemType Directory -Path $distDir -Force | Out-Null
    $pluginZip = Join-Path $distDir ("CustomBookCreator-MCreator2026.2-v" + $pluginVersion + ".zip")
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
