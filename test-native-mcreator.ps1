param([Parameter(Mandatory=$true)][string]$MCreatorRoot,
      [Parameter(Mandatory=$true)][string]$PluginZip,
      [string[]]$Profiles = @("forge-1.20.1", "neoforge-1.21.1"))
$ErrorActionPreference = "Stop"
$sdk = (Resolve-Path -LiteralPath $MCreatorRoot).Path
$plugin = (Resolve-Path -LiteralPath $PluginZip).Path
$root = Join-Path $PSScriptRoot ("build/native-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path "$root/plugins", "$root/core", "$root/classes", "$root/home" -Force | Out-Null
Copy-Item -Path "$sdk/plugins/*.zip" -Destination "$root/plugins"
Copy-Item -LiteralPath $plugin -Destination "$root/plugins"
Push-Location "$root/core"
try { & "$sdk/jdk/bin/jar.exe" xf "$sdk/mcreator.exe" net META-INF } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw "Core extraction failed" }
$cp = (@("$root/core", $plugin) + @(Get-ChildItem "$sdk/lib" -Filter "*.jar" | ForEach-Object FullName)) -join ";"
& "$sdk/jdk/bin/javac.exe" -proc:none -encoding UTF8 -cp $cp -d "$root/classes" "$PSScriptRoot/integration/NativeMCreatorSaveTest.java"
if ($LASTEXITCODE -ne 0) { throw "Integration test compilation failed" }
$previousHome = $env:MCREATOR_HOME
$previousPlugins = $env:MCREATOR_PLUGINS_FOLDER
try {
    $env:MCREATOR_HOME = "$root/home"
    $env:MCREATOR_PLUGINS_FOLDER = "$root/home/plugins"
    Push-Location $root
    try { & "$sdk/jdk/bin/java.exe" "--add-opens=java.desktop/java.awt=ALL-UNNAMED" "-Djava.awt.headless=true" "-Duser.home=$root/home" "-Dlog_directory=$root" "-Dcustombook.nativeManifest=$root/core/META-INF/MANIFEST.MF" -cp "$root/classes;$cp" NativeMCreatorSaveTest @Profiles }
    finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { throw "Native integration test failed. Evidence: $root" }
    Write-Host "Native integration passed. Evidence: $root"
} finally {
    $env:MCREATOR_HOME = $previousHome
    $env:MCREATOR_PLUGINS_FOLDER = $previousPlugins
}
