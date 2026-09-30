param(
    [Parameter(Mandatory=$true)][string]$OutputDirectory
)
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$safeRoot = $root.Replace("\", "/")
$version = (Get-Content -LiteralPath (Join-Path $root "src/main/resources/plugin.json") -Raw | ConvertFrom-Json).info.version
if ($version -ne "2.1.0") { throw "This packager is scoped to the reviewed V2.1.0 artifacts." }
$status = @(git -c "safe.directory=$safeRoot" -C $root status --porcelain)
if ($LASTEXITCODE -ne 0 -or $status.Count) { throw "Commit the reviewed sources before packaging a release." }
$destination = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $destination) { throw "Output directory already exists; no files were overwritten." }
$targets = [ordered]@{
    "2026.2" = @("fabric-26.2", "forge-1.20.1", "neoforge-1.21.1", "neoforge-1.21.8", "neoforge-26.1.2", "neoforge-26.2")
    "2024.4" = @("forge-1.20.1")
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
$kit = Join-Path $root ("build/release-kit-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $kit | Out-Null
foreach ($path in $plugins) { Copy-Item -LiteralPath $path -Destination $kit }
Copy-Item -LiteralPath $sources -Destination $kit
foreach ($name in @("CHANGELOG.md", "PATCH_NOTES.md", "GUIDE.md", "VALIDATION.md", "MCREATOR_DESCRIPTION.md", "MCREATOR_UPLOAD.md")) {
    $source = Join-Path $root "docs/2.1.0/$name"
    Copy-Item -LiteralPath $source -Destination $destination
    Copy-Item -LiteralPath $source -Destination $kit
}
# Reuse the existing enlarged MCreator icon, unchanged.
$icon = Join-Path $root "docs/2.0.0/website-icon-512x512.png"
Copy-Item -LiteralPath $icon -Destination $destination
Copy-Item -LiteralPath $icon -Destination $kit
function Write-Hashes([string]$Directory) {
    $lines = @(Get-ChildItem -LiteralPath $Directory -File | Where-Object Name -ne "SHA256SUMS.txt" | Sort-Object Name |
        ForEach-Object { (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() + "  " + $_.Name })
    [IO.File]::WriteAllLines((Join-Path $Directory "SHA256SUMS.txt"), [string[]]$lines, [Text.UTF8Encoding]::new($false))
}
Write-Hashes $kit
$kitZip = Join-Path $destination "CustomBookCreator-MCreator-upload-kit-v$version.zip"
[IO.Compression.ZipFile]::CreateFromDirectory($kit, $kitZip, [IO.Compression.CompressionLevel]::Optimal, $false)
$kitArchive = [IO.Compression.ZipFile]::OpenRead($kitZip)
try {
    foreach ($entry in $kitArchive.Entries) {
        $stream = $entry.Open()
        $sha = [Security.Cryptography.SHA256]::Create()
        try { $hash = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace("-", "") }
        finally { $stream.Dispose(); $sha.Dispose() }
        if ($hash -ne (Get-FileHash -LiteralPath (Join-Path $kit $entry.FullName) -Algorithm SHA256).Hash) {
            throw "Kit entry hash mismatch: $($entry.FullName)"
        }
    }
    Write-Output "UPLOAD_KIT_OK entries=$($kitArchive.Entries.Count)"
} finally { $kitArchive.Dispose() }
Write-Hashes $destination
Get-ChildItem -LiteralPath $destination -File | Select-Object Name, Length
