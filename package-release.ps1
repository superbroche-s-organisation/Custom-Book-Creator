param(
    [Parameter(Mandatory=$true)][string]$OutputDirectory
)
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$safeRoot = $root.Replace("\", "/")
$version = (Get-Content -LiteralPath (Join-Path $root "src/main/resources/plugin.json") -Raw | ConvertFrom-Json).info.version
if ($version -notmatch '^\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?$') { throw "Set a valid release version before packaging." }
$status = @(git -c "safe.directory=$safeRoot" -C $root status --porcelain)
if ($LASTEXITCODE -ne 0 -or $status.Count) { throw "Commit the reviewed sources before packaging a release." }
$destination = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $destination) { throw "Output directory already exists; no files were overwritten." }
$targets = [ordered]@{
    "2026.2" = @("fabric-26.2", "forge-1.20.1", "neoforge-1.21.1", "neoforge-1.21.8", "neoforge-26.1.2", "neoforge-26.2")
    "2024.4" = @("forge-1.20.1", "neoforge-1.21.1")
    "2022.2" = @("forge-1.17.1")
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Read-Entry($Archive, [string]$Name) {
    $entry = $Archive.GetEntry($Name)
    if ($null -eq $entry) { throw "Missing archive entry: $Name" }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}
$plugins = @()
foreach ($target in $targets.Keys) {
    $name = "CustomBookCreator-MCreator$target-v$version.zip"
    $path = Join-Path $root "dist/$name"
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $manifest = (Read-Entry $archive "plugin.json") | ConvertFrom-Json
        if ($manifest.info.version -ne $version -or $manifest.info.author -ne "Superbroche" -or
            $manifest.id -ne "custom_book_creator" -or -not $manifest.javaplugin) { throw "Invalid plugin manifest: $name" }
        $catalog = @((Read-Entry $archive "custombook-compatibility.json") | ConvertFrom-Json)
        $actual = @($archive.Entries | Where-Object FullName -Match '^[^/]+/custombook\.definition\.yaml$' |
            ForEach-Object { $_.FullName.Split("/")[0] } | Sort-Object)
        if (($actual -join ",") -ne (($targets[$target] | Sort-Object) -join ",") -or
            (($catalog.id | Sort-Object) -join ",") -ne ($actual -join ",")) { throw "Profile/catalog mismatch: $name" }
        if (@($catalog | Where-Object { $_.mcreator -ne $target -or $_.status -ne "api-compiled" -or $_.gameplayTested }).Count) {
            throw "Incorrect validation metadata: $name"
        }
        if (@($archive.Entries | Where-Object FullName -Match '(^|/)\.\.(/|$)|^net/mcreator/.*\.class$|^(sources|build|dist)/').Count) {
            throw "Unexpected plugin payload: $name"
        }
        $class = $archive.GetEntry("fr/superbroche/mcreator/custombook/ui/modgui/CustomBookGUI.class")
        $stream = $class.Open()
        try {
            $header = New-Object byte[] 8
            if ($stream.Read($header, 0, 8) -ne 8) { throw "Truncated class" }
            $major = 256 * $header[6] + $header[7]
            if ($major -ne $(if ($target -eq "2022.2") {61} else {65})) { throw "Wrong Java target: $name" }
        } finally { $stream.Dispose() }
        Write-Output "PLUGIN_ARCHIVE_OK $name profiles=$($actual.Count)"
    } finally { $archive.Dispose() }
    $plugins += $path
}
New-Item -ItemType Directory -Path $destination | Out-Null
foreach ($path in $plugins) { Copy-Item -LiteralPath $path -Destination $destination }
$sourcesName = "CustomBookCreator-v$version-sources.zip"
$sources = Join-Path $destination $sourcesName
& git -c "safe.directory=$safeRoot" -C $root archive --format=zip --prefix="CustomBookCreator-$version/" --output=$sources HEAD
if ($LASTEXITCODE -ne 0) { throw "Source archive failed" }
$sourceArchive = [IO.Compression.ZipFile]::OpenRead($sources)
try {
    $prefix = "CustomBookCreator-$version/"
    $tracked = @(git -c "safe.directory=$safeRoot" -C $root ls-tree -r --name-only HEAD)
    if ($LASTEXITCODE -ne 0) { throw "Cannot verify source inventory" }
    $sourceFiles = @($sourceArchive.Entries | Where-Object { -not $_.FullName.EndsWith("/") })
    if ($tracked.Count -ne $sourceFiles.Count) { throw "Source inventory differs from commit" }
    foreach ($file in $tracked) {
        if (-not $sourceArchive.GetEntry($prefix + $file)) { throw "Missing source: $file" }
    }
    $manifest = (Read-Entry $sourceArchive ($prefix + "src/main/resources/plugin.json")) | ConvertFrom-Json
    if ($manifest.info.version -ne $version) { throw "Source archive has wrong version" }
    Write-Output "SOURCE_ARCHIVE_OK files=$($sourceFiles.Count)"
} finally { $sourceArchive.Dispose() }
$expectedAssets = @($plugins | ForEach-Object { Split-Path -Leaf $_ }) + @($sourcesName)
$actualAssets = @(Get-ChildItem -LiteralPath $destination -File | ForEach-Object Name)
if ($actualAssets.Count -ne 4 -or
    (($actualAssets | Sort-Object) -join ',') -ne (($expectedAssets | Sort-Object) -join ',')) {
    throw "A GitHub release must contain exactly the three plugin ZIPs and the sources ZIP."
}
Write-Output "RELEASE_ASSETS_OK count=4"
Get-ChildItem -LiteralPath $destination -File | Select-Object Name, Length
