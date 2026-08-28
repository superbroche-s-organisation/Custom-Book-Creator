param(
    [Parameter(Mandatory = $false)]
    [string]$MCreatorRoot = "D:\MCreator"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
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

$temporaryRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("custom-book-creator-build-" + [guid]::NewGuid())
$coreClasses = Join-Path $temporaryRoot "mcreator-core"
$classesDir = Join-Path $temporaryRoot "classes"
$stagingDir = Join-Path $temporaryRoot "plugin"
$coreJar = Join-Path $temporaryRoot "mcreator-core.jar"

try {
    New-Item -ItemType Directory -Path $coreClasses, $classesDir, $stagingDir -Force | Out-Null

    Push-Location $coreClasses
    try {
        & $jar xf $mcreatorExe
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to extract the MCreator classes."
        }
        & $jar cf $coreJar net META-INF
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to prepare the MCreator classpath."
        }
    }
    finally {
        Pop-Location
    }

    $libraries = Get-ChildItem -LiteralPath $libDir -Filter "*.jar" -File | ForEach-Object { $_.FullName }
    $classPath = (@($coreJar) + $libraries) -join ";"
    $javaSources = Get-ChildItem -LiteralPath (Join-Path $projectRoot "src\main\java") -Filter "*.java" -File -Recurse | ForEach-Object { $_.FullName }

    & $javac --release 21 -encoding UTF-8 -cp $classPath -d $classesDir $javaSources
    if ($LASTEXITCODE -ne 0) {
        throw "Plugin Java compilation failed."
    }

    Copy-Item -Path (Join-Path $projectRoot "src\main\resources\*") -Destination $stagingDir -Recurse -Force
    Copy-Item -Path (Join-Path $classesDir "*") -Destination $stagingDir -Recurse -Force

    $distDir = Join-Path $projectRoot "dist"
    New-Item -ItemType Directory -Path $distDir -Force | Out-Null
    $pluginZip = Join-Path $distDir "CustomBookCreator-MCreator2026.2-v1.1.zip"
    if (Test-Path -LiteralPath $pluginZip) {
        Remove-Item -LiteralPath $pluginZip -Force
    }
    & $jar --create --file $pluginZip --no-manifest -C $stagingDir .
    if ($LASTEXITCODE -ne 0) {
        throw "Plugin archive creation failed."
    }
    Write-Host "Plugin created: $pluginZip"
}
finally {
    if (Test-Path -LiteralPath $temporaryRoot) {
        $resolvedTemporary = (Resolve-Path -LiteralPath $temporaryRoot).Path
        $systemTemporary = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
        if ($resolvedTemporary.StartsWith($systemTemporary, [System.StringComparison]::OrdinalIgnoreCase)) {
            Remove-Item -LiteralPath $resolvedTemporary -Recurse -Force
        }
    }
}
