param(
    [Parameter(Mandatory=$true)][string]$MCreatorRoot,
    [string]$GeneratedSourcesDirectory = "build/v21-validation/generated/forge-1.17.1"
)
$ErrorActionPreference = "Stop"
$project = $PSScriptRoot
$legacyRoot = (Resolve-Path -LiteralPath $MCreatorRoot).Path
$zip = & (Join-Path $project "build.ps1") -MCreatorRoot $legacyRoot -TargetMCreator "2022.2" -PassThru
$testRoot = Join-Path $project ("build/legacy2022-tests/" + [guid]::NewGuid().ToString("N"))
$plugin = Join-Path $testRoot "plugin"
$classes = Join-Path $testRoot "classes"
New-Item -ItemType Directory -Path $plugin, $classes -Force | Out-Null
Expand-Archive -LiteralPath $zip -DestinationPath $plugin
$classpath = $plugin + ";" + (Join-Path $legacyRoot "lib/*")
$javac = Join-Path $legacyRoot "jdk/bin/javac.exe"
$java = Join-Path $legacyRoot "jdk/bin/java.exe"
& $javac --release 17 -encoding UTF-8 -cp $classpath -d $classes (Join-Path $project "compatibility/tests/Legacy2022PackageTest.java")
if ($LASTEXITCODE -ne 0) { throw "Legacy test compilation failed" }
$output = if ([IO.Path]::IsPathRooted($GeneratedSourcesDirectory)) { $GeneratedSourcesDirectory } else { Join-Path $project $GeneratedSourcesDirectory }
& $java -ea "-Djava.awt.headless=true" "-Dlog_directory=$testRoot" "-Dcustombook.pluginRoot=$plugin" -cp ($classes + ";" + $classpath) Legacy2022PackageTest $output
if ($LASTEXITCODE -ne 0) { throw "Legacy package tests failed" }
Write-Output "Legacy package tested: $zip"
Write-Output "Generated sources retained: $output"
