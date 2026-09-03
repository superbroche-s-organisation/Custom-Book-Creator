param(
    [Parameter(Mandatory = $false)]
    [string]$MCreatorRoot = "D:\MCreator",
    # When supplied, TemplateRenderTest keeps its generated Java sources here
    # for compilation against the corresponding Minecraft/NeoForge libraries.
    [Parameter(Mandatory = $false)]
    [string]$GeneratedSourcesDirectory
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$MCreatorRoot = (Resolve-Path -LiteralPath $MCreatorRoot).Path
$jdkBin = Join-Path $MCreatorRoot "jdk\bin"
$java = Join-Path $jdkBin "java.exe"
$javac = Join-Path $jdkBin "javac.exe"
$jar = Join-Path $jdkBin "jar.exe"
$mcreatorExe = Join-Path $MCreatorRoot "mcreator.exe"
$libDir = Join-Path $MCreatorRoot "lib"

foreach ($requiredPath in @($java, $javac, $jar, $mcreatorExe, $libDir)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required MCreator file or directory not found: $requiredPath"
    }
}

$testSources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot "tests") -Filter "*.java" -File -Recurse | Sort-Object FullName)
if ($testSources.Count -eq 0) {
    throw "No Java tests were found."
}

# Discover public entry points, while also compiling support classes without a
# main method. A test's public class name must match its Java file name.
$testClasses = @(
    foreach ($source in $testSources) {
        $sourceText = Get-Content -LiteralPath $source.FullName -Raw -Encoding UTF8
        if ($sourceText -notmatch '\bpublic\s+static\s+void\s+main\s*\(\s*(?:java\.lang\.)?String\s*(?:\[\]|\.\.\.)\s+\w+\s*\)') {
            continue
        }
        $className = $source.BaseName
        # A package declaration can only precede imports/types. Do not mistake
        # embedded Java fixture text blocks for the package of the test itself.
        if ($sourceText -match '\A(?:\s|//[^\r\n]*(?:\r?\n|\z)|/\*[\s\S]*?\*/)*package\s+([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*)\s*;') {
            $className = $Matches[1] + "." + $className
        }
        [pscustomobject]@{ Name = $className; SourceName = $source.BaseName }
    }
)
if ($testClasses.Count -eq 0) {
    throw "No public static void main(String[] args) test entry points were found."
}

$generatedSourcesPath = $null
if (-not [string]::IsNullOrWhiteSpace($GeneratedSourcesDirectory)) {
    if (-not ($testClasses.SourceName -contains "TemplateRenderTest")) {
        throw "GeneratedSourcesDirectory requires tests/TemplateRenderTest.java."
    }
    $generatedSourcesPath = if ([System.IO.Path]::IsPathRooted($GeneratedSourcesDirectory)) {
        [System.IO.Path]::GetFullPath($GeneratedSourcesDirectory)
    }
    else {
        [System.IO.Path]::GetFullPath((Join-Path $projectRoot $GeneratedSourcesDirectory))
    }
}

$systemTemporary = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd([char[]]'\/')
$temporaryName = "custom-book-creator-tests-" + [guid]::NewGuid().ToString("N")
$temporaryRoot = Join-Path $systemTemporary $temporaryName
$coreClasses = Join-Path $temporaryRoot "mcreator-core"
$pluginClasses = Join-Path $temporaryRoot "plugin"
$compiledTests = Join-Path $temporaryRoot "tests"

try {
    Write-Host "Building the plugin before running tests..."
    $pluginZip = & (Join-Path $projectRoot "build.ps1") -MCreatorRoot $MCreatorRoot -PassThru
    if (-not ($pluginZip -is [string]) -or -not (Test-Path -LiteralPath $pluginZip -PathType Leaf)) {
        throw "The build did not return an installable plugin archive."
    }

    New-Item -ItemType Directory -Path $coreClasses, $pluginClasses, $compiledTests | Out-Null
    Push-Location $coreClasses
    try {
        & $jar xf $mcreatorExe net META-INF
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to extract the MCreator classes for tests."
        }
    }
    finally {
        Pop-Location
    }

    # Tests load the freshly packaged bytecode, not an older class directory.
    Push-Location $pluginClasses
    try {
        & $jar xf $pluginZip
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to extract the built plugin for tests."
        }
    }
    finally {
        Pop-Location
    }

    $libraries = @(Get-ChildItem -LiteralPath $libDir -Filter "*.jar" -File | Sort-Object Name | ForEach-Object { $_.FullName })
    $compileClassPath = (@($pluginClasses, $coreClasses) + $libraries) -join [System.IO.Path]::PathSeparator
    $runtimeClassPath = (@($compiledTests, $pluginClasses, $coreClasses) + $libraries) -join [System.IO.Path]::PathSeparator
    $sourcePaths = @($testSources | ForEach-Object { $_.FullName })

    Write-Host ("Compiling {0} Java test/support files..." -f $sourcePaths.Count)
    & $javac --release 21 -encoding UTF-8 -cp $compileClassPath -d $compiledTests $sourcePaths
    if ($LASTEXITCODE -ne 0) {
        throw "Java test compilation failed."
    }

    $failedTests = [System.Collections.Generic.List[string]]::new()
    Push-Location $projectRoot
    try {
        foreach ($testClass in $testClasses) {
            Write-Host ("Running {0}..." -f $testClass.Name)
            $testArguments = @()
            if ($testClass.SourceName -eq "TemplateRenderTest" -and $null -ne $generatedSourcesPath) {
                $testArguments = @($generatedSourcesPath)
            }
            # Each test gets a separate JVM, so static MCreator registrations and
            # translation fixtures cannot leak between tests.
            & $java -ea "-Djava.awt.headless=true" -cp $runtimeClassPath $testClass.Name @testArguments
            if ($LASTEXITCODE -ne 0) {
                $failedTests.Add($testClass.Name)
                Write-Host ("FAILED: {0} (exit {1})" -f $testClass.Name, $LASTEXITCODE) -ForegroundColor Red
            }
        }
    }
    finally {
        Pop-Location
    }

    if ($failedTests.Count -gt 0) {
        throw ("{0} of {1} test programs failed: {2}" -f $failedTests.Count, $testClasses.Count, ($failedTests -join ", "))
    }
    Write-Host ("All {0} test programs passed." -f $testClasses.Count) -ForegroundColor Green
    Write-Host "Tested plugin: $pluginZip"
    if ($null -ne $generatedSourcesPath) {
        Write-Host "Generated Java sources retained for external compilation: $generatedSourcesPath"
        Write-Host "External Minecraft/NeoForge compilation is a separate validation step."
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
