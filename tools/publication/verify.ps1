param(
    [ValidateSet('Bootstrap', 'Baseline', 'Accepted', 'Build', 'All')]
    [string]$Phase = 'All',
    [string]$GradleUserHome,
    [string]$JavaHome = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Set-Location -LiteralPath $projectRoot
if (!$GradleUserHome) { $GradleUserHome = Join-Path $projectRoot '.gradle-user-home' }
$env:JAVA_HOME = $JavaHome
$env:GRADLE_USER_HOME = [System.IO.Path]::GetFullPath($GradleUserHome)
$env:TEMP = Join-Path $projectRoot 'build/tmp/codex_java_uds_probe'
$env:TMP = $env:TEMP
$resultRoot = Join-Path $projectRoot 'build/tmp/publication_validation'
New-Item -ItemType Directory -Force -Path $env:TEMP, $resultRoot | Out-Null

function Assert-Hash([string]$Path, [string]$Expected) {
    $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $Expected) { throw "Input hash mismatch: $Path ($actual)" }
}

# These checks run from the snapshot itself, before any Gradle cache exists.
Assert-Hash 'reference/phase_a_preservation/files/final/bacap.zip' '8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70'
Assert-Hash 'reference/localization/fixtures/bacap_1.21.zip' 'c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2'
Assert-Hash 'reference/publication/fixtures/historical_0_1_5_achievetodo.mixins.json' '56fcb41d75273382681cbeede45882a876de68babcd6580ae63d5c20c0ac211f'
$resources = Get-Content -Raw -LiteralPath 'reference/publication/fixtures/generated_gameplay_resources_sha256.json' | ConvertFrom-Json -AsHashtable
if ($resources.Count -ne 303) { throw 'Expected exactly 303 accepted generated gameplay resources' }
foreach ($entry in $resources.GetEnumerator()) { Assert-Hash $entry.Key $entry.Value }
$pins = Get-Content -Raw -LiteralPath 'gradle.properties'
foreach ($pin in @('modVersion=0.1.5.4', 'minJavaVersion=25', 'minMinecraftVersion=26.2', 'minLoaderVersion=0.19.5', 'apiVersion=0.161.0', 'loomVersion=1.18.2')) {
    if (!$pins.Contains($pin)) { throw "Missing accepted pin: $pin" }
}
if (!(Get-Content -Raw -LiteralPath 'gradle/wrapper/gradle-wrapper.properties').Contains('gradle-9.8.0-bin.zip')) { throw 'Wrong Gradle wrapper version' }
Write-Output 'PUBLICATION_SOURCE_INPUTS_COMPLETE generatedResources=303'

function Invoke-Gate([string]$Name, [string[]]$Arguments, [int]$ExpectedExit) {
    $Arguments | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $resultRoot "$Name.argv.json") -Encoding utf8
    & './gradlew.bat' @Arguments 2>&1 | Tee-Object -FilePath (Join-Path $resultRoot "$Name.log")
    $code = $LASTEXITCODE
    Set-Content -LiteralPath (Join-Path $resultRoot "$Name.exit") -Value $code -Encoding ascii
    if ($code -ne $ExpectedExit) { throw "STOP: $Name exit $code; expected $ExpectedExit" }
}

function Read-TestResults([string]$Directory) {
    $files = @(Get-ChildItem -LiteralPath $Directory -Filter 'TEST-*.xml' -File)
    if (!$files.Count) { throw "Missing JUnit XML: $Directory" }
    $totals = @{ tests = 0; failures = 0; errors = 0; skipped = 0 }
    $failures = @()
    foreach ($file in $files) {
        [xml]$xml = Get-Content -Raw -LiteralPath $file.FullName
        $suite = $xml.testsuite
        foreach ($field in @('tests', 'failures', 'errors', 'skipped')) { $totals[$field] += [int]$suite.GetAttribute($field) }
        foreach ($case in $suite.SelectNodes('testcase[failure]')) {
            $failure = $case.SelectSingleNode('failure')
            $failures += @{ suite = $suite.GetAttribute('name'); test = $case.GetAttribute('name'); type = $failure.GetAttribute('type'); message = $failure.GetAttribute('message'); trace = $failure.InnerText.Replace("`r`n", "`n").Trim() }
        }
    }
    return @{ totals = $totals; failures = $failures; suites = $files.Count }
}

$phases = if ($Phase -eq 'All') { @('Bootstrap', 'Baseline', 'Accepted', 'Build') } else { @($Phase) }
foreach ($step in $phases) {
    if ($step -eq 'Bootstrap') {
        Invoke-Gate 'bootstrap' @('--no-daemon', 'compileJava', 'compileTestJava', 'compileGametestJava') 0
    } elseif ($step -eq 'Baseline') {
        Invoke-Gate 'baseline' @('--no-daemon', 'compileJava', 'compileTestJava', 'compileGametestJava', 'test', '--rerun-tasks') 1
        $actual = Read-TestResults 'build/test-results/test'
        $historical = Read-TestResults 'reference/phase_a_planning/pre26_2_post_cleanup_build_0_1_5_4_retry1/evidence/baseline-junit'
        if ($actual.totals.tests -ne 391 -or $actual.totals.failures -ne 4 -or $actual.totals.errors -ne 0 -or $actual.totals.skipped -ne 0) { throw 'STOP: unfiltered baseline outcome changed' }
        foreach ($failure in $actual.failures) {
            $match = @($historical.failures | Where-Object { $_.suite -eq $failure.suite -and $_.test -eq $failure.test })
            if ($match.Count -ne 1) { throw "STOP: unexpected failure $($failure.suite).$($failure.test)" }
            foreach ($field in @('type', 'message', 'trace')) {
                if ($match[0][$field] -cne $failure[$field]) { throw "STOP: historical failure $field changed for $($failure.test)" }
            }
        }
        $actual | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $resultRoot 'baseline-summary.json') -Encoding utf8
        Write-Output 'PUBLICATION_BASELINE_VERIFIED tests=391 immutableFailures=4 errors=0 skipped=0'
    } elseif ($step -eq 'Accepted') {
        $manifest = Get-Content -Raw -LiteralPath 'reference/phase_a_planning/pre26_2_smoke_bugfix_0_1_5_4/bugfix_manifest.json' | ConvertFrom-Json
        $suites = @($manifest.tests.accepted.suites)
        if ($suites.Count -ne 24) { throw 'Accepted suite selection must contain exactly 24 suites' }
        $arguments = @('--no-daemon', '--init-script', 'tools/publication/accepted.init.gradle', '--init-script', 'tools/publication/report-routing.init.gradle', 'compileJava', 'compileTestJava', 'compileGametestJava', 'test', '--rerun-tasks')
        foreach ($suite in $suites) { $arguments += @('--tests', [string]$suite.name) }
        Invoke-Gate 'accepted' $arguments 0
        $actual = Read-TestResults 'build/tmp/publication_validation/accepted-junit'
        if ($actual.totals.tests -ne 103 -or $actual.totals.failures -ne 0 -or $actual.totals.errors -ne 0 -or $actual.totals.skipped -ne 0) { throw 'STOP: accepted suite outcome changed' }
        $actual | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $resultRoot 'accepted-summary.json') -Encoding utf8
        Write-Output 'PUBLICATION_ACCEPTED_VERIFIED tests=103 failures=0 errors=0 skipped=0'
    } elseif ($step -eq 'Build') {
        # The accepted JAR contains two deterministic HashCache records. Recreate
        # them from committed gameplay bytes; never commit cache output itself.
        $cacheRoot = Join-Path $projectRoot 'src/main/generated/.cache'
        New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null
        $cacheSpecs = @(
            @{ provider = 'AchieveToDo/Advancements'; directory = 'data/achievetodo/advancement/abilities'; pattern = '*.json'; sha256 = 'c4459e1f7645ca5bca1158ec3aa8a4e1522809485ba05f94e742fe7c09706085' },
            @{ provider = 'AchieveToDo/AbilityUnlockMessagesGenerator'; directory = 'data/achievetodo/function/abilities'; pattern = '*.mcfunction'; sha256 = 'b99fb23e7393773edff38f3f1df88990f3b165cbee8c7200116938676b268841' }
        )
        foreach ($spec in $cacheSpecs) {
            $sha1 = [System.Security.Cryptography.SHA1]::Create()
            try {
                $name = [Convert]::ToHexString($sha1.ComputeHash([Text.Encoding]::UTF8.GetBytes($spec.provider))).ToLowerInvariant()
                $text = "// 26.2`t-999999999-01-01T00:00:00`t$($spec.provider)`r`n"
                [string[]]$names = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot "src/main/generated/$($spec.directory)") -Filter $spec.pattern -File | ForEach-Object Name)
                [Array]::Sort($names, [StringComparer]::Ordinal)
                foreach ($resourceName in $names) {
                    $relative = "$($spec.directory)/$resourceName"
                    $hash = [Convert]::ToHexString($sha1.ComputeHash([IO.File]::ReadAllBytes((Join-Path $projectRoot "src/main/generated/$relative")))).ToLowerInvariant()
                    $text += "$hash $relative`r`n"
                }
                $destination = Join-Path $cacheRoot $name
                [IO.File]::WriteAllText($destination, $text, [Text.UTF8Encoding]::new($false))
                Assert-Hash $destination $spec.sha256
            } finally { $sha1.Dispose() }
        }
        Invoke-Gate 'build' @('--no-daemon', 'build', '-x', 'test', '-x', 'runGameTest') 0
        $jar = Get-Item -LiteralPath 'build/libs/achievetodo-mc26.2+0.1.5.4.jar'
        Assert-Hash $jar.FullName '0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b'
        if ($jar.Length -ne 1546595) { throw 'STOP: accepted JAR size changed' }
        @{ path = 'build/libs/achievetodo-mc26.2+0.1.5.4.jar'; size = $jar.Length; sha256 = (Get-FileHash -LiteralPath $jar.FullName).Hash.ToLowerInvariant(); comparison = 'BYTE_IDENTICAL' } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $resultRoot 'jar-summary.json') -Encoding utf8
        Write-Output 'PUBLICATION_JAR_VERIFIED size=1546595 sha256=0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b'
    }
}
