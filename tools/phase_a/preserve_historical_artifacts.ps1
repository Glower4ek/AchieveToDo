Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$preservationRoot = Join-Path $repoRoot 'reference/phase_a_preservation'
$rawDir = Join-Path $preservationRoot 'files/raw'
$finalDir = Join-Path $preservationRoot 'files/final'
$reportPath = Join-Path $preservationRoot 'artifact-report.json'

$artifacts = @(
    @{
        id = 'bacap'
        title = "BlazeandCave's Advancements Pack (BACAP)"
        page_url = 'https://modrinth.com/datapack/blazeandcaves-advancements-pack'
        download_url = 'https://cdn.modrinth.com/data/VoVJ47kN/versions/i8N5hYLH/BlazeandCave%27s%20Advancements%20Pack%201.18.1.zip'
        wrapper_sha1 = $null
        final_sha1 = '45b8bb0076bbf5b92fde7dc9590c6686937abbc0'
        expected_file_name = 'bacap.zip'
    }
    @{
        id = 'bacap_hardcore'
        title = 'BACAP (Hardcore version)'
        page_url = 'https://modrinth.com/datapack/blazeandcaves-advancements-pack-hardcore-version'
        download_url = 'https://cdn.modrinth.com/data/QEv1xmKi/versions/uRKM9Bou/BlazeandCave%27s%20Advancements%20Pack%20Hardcore.zip'
        wrapper_sha1 = $null
        final_sha1 = 'ec5203496a822e6145562cd81e781ca0eea2c968'
        expected_file_name = 'bacap_hardcore.zip'
    }
    @{
        id = 'bacap_terralith'
        title = 'BACAP (Terralith version)'
        page_url = 'https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/'
        download_url = 'https://www.mediafire.com/file/ljb8qwofxk4dq9i/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Terralith_1.18.zip/file'
        wrapper_sha1 = '2699070cf5040ab519c223178ee64ee9eafe3691'
        final_sha1 = '3d8cc170c1bf2a00460a8d7e779acbe9d5034dea'
        expected_file_name = 'bacap_terralith.zip'
    }
    @{
        id = 'bacap_amplified_nether'
        title = 'BACAP (Amplified Nether version)'
        page_url = 'https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/'
        download_url = 'https://www.mediafire.com/file/ak5sjemiz60mzrc/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Amplified_Nether_1.18.zip/file'
        wrapper_sha1 = '981ff801e3cf7eace1ddc2fff8b6165c12ea52b0'
        final_sha1 = '9956d0a7d26e0b7d166711fa4b6bf856d2993a44'
        expected_file_name = 'bacap_amplified_nether.zip'
    }
    @{
        id = 'bacap_nullscape'
        title = 'BACAP (Nullscape version)'
        page_url = 'https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/'
        download_url = 'https://www.mediafire.com/file/hsj4koctw778e43/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Nullscape_1.18.zip/file'
        wrapper_sha1 = '029c29644a9e94dd8c4111dc3ab2165e79fe4d66'
        final_sha1 = '6a50de576558b6b9079a60ffdff73cd9e622eac1'
        expected_file_name = 'bacap_nullscape.zip'
    }
    @{
        id = 'terralith'
        title = 'Terralith'
        page_url = 'https://www.planetminecraft.com/data-pack/terralith-overworld-evolved-100-biomes-caves-and-more/'
        download_url = 'https://cdn.modrinth.com/data/8oi3bsk5/versions/PcYlKx8w/Terralith_1.21_v2.5.7.zip'
        wrapper_sha1 = $null
        final_sha1 = '9645d4c557e8419154cc5775c5fe3ba9e82bfb8f'
        expected_file_name = 'terralith.zip'
    }
    @{
        id = 'amplified_nether'
        title = 'Amplified Nether'
        page_url = 'https://www.planetminecraft.com/data-pack/amplified-nether-1-18/'
        download_url = 'https://cdn.modrinth.com/data/wXiGiyGX/versions/jfHNaJaE/Amplified_Nether_1.21_v1.2.7.zip'
        wrapper_sha1 = $null
        final_sha1 = '473ddf1042ae7d4c19eff01944d0b9da0f11c831'
        expected_file_name = 'amplified_nether.zip'
    }
    @{
        id = 'nullscape'
        title = 'Nullscape'
        page_url = 'https://www.planetminecraft.com/data-pack/nullscape/'
        download_url = 'https://cdn.modrinth.com/data/LPjGiSO4/versions/J4B2BaWk/Nullscape_1.21_v1.2.10.zip'
        wrapper_sha1 = $null
        final_sha1 = '0a55bff36ab26b13963213ed1482d1b8b84ab568'
        expected_file_name = 'nullscape.zip'
    }
)

function Get-Hashes {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    return @{
        sha1 = (Get-FileHash -Algorithm SHA1 -LiteralPath $Path).Hash.ToLowerInvariant()
        sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
    }
}

function Resolve-DownloadUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    if ($Url -notmatch '^https://www\.mediafire\.com/file/') {
        return $Url
    }

    $response = Invoke-WebRequest -Uri $Url
    $content = $response.Content
    $patterns = @(
        'aria-label="Download file"\s+href="([^"]+)"',
        'id="downloadButton"[^>]*href="([^"]+)"',
        '(https://download[^"'' ]+)',
        '(https://www\.mediafire\.com/\?[^"'' ]+)'
    )

    foreach ($pattern in $patterns) {
        $match = [regex]::Match($content, $pattern)
        if ($match.Success) {
            $resolved = if ($match.Groups.Count -gt 1) { $match.Groups[1].Value } else { $match.Value }
            return $resolved.Replace('&amp;', '&')
        }
    }

    throw "Unable to resolve direct download URL from MediaFire page '$Url'."
}

function Expand-FirstFileFromZip {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourcePath,
        [Parameter(Mandatory = $true)]
        [string]$DestinationPath
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($SourcePath)
    try {
        $entry = $archive.Entries | Where-Object { -not [string]::IsNullOrEmpty($_.Name) } | Select-Object -First 1
        if ($null -eq $entry) {
            throw "Archive '$SourcePath' does not contain a file entry."
        }
        $entryStream = $entry.Open()
        try {
            $outputStream = [System.IO.File]::Open($DestinationPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
            try {
                $entryStream.CopyTo($outputStream)
            } finally {
                $outputStream.Dispose()
            }
        } finally {
            $entryStream.Dispose()
        }
        return $entry.FullName
    } finally {
        $archive.Dispose()
    }
}

New-Item -ItemType Directory -Force -Path $rawDir, $finalDir | Out-Null

$report = [System.Collections.Generic.List[object]]::new()

foreach ($artifact in $artifacts) {
    $rawPath = Join-Path $rawDir ($artifact.id + '-download.bin')
    $finalPath = Join-Path $finalDir $artifact.expected_file_name
    $resolvedDownloadUrl = Resolve-DownloadUrl -Url $artifact.download_url

    Invoke-WebRequest -Uri $resolvedDownloadUrl -OutFile $rawPath

    $rawHashes = Get-Hashes -Path $rawPath
    $rawMode = 'direct'
    $wrapperExtractedEntry = $null

    if ($artifact.wrapper_sha1) {
        if ($rawHashes.sha1 -eq $artifact.wrapper_sha1) {
            $rawMode = 'wrapper'
            $wrapperExtractedEntry = Expand-FirstFileFromZip -SourcePath $rawPath -DestinationPath $finalPath
        } elseif ($rawHashes.sha1 -eq $artifact.final_sha1) {
            $rawMode = 'direct-final'
            Copy-Item -LiteralPath $rawPath -Destination $finalPath -Force
        } else {
            throw "Artifact '$($artifact.id)' hash mismatch. Expected wrapper SHA-1 '$($artifact.wrapper_sha1)' or final SHA-1 '$($artifact.final_sha1)', got '$($rawHashes.sha1)'."
        }
    } else {
        if ($rawHashes.sha1 -ne $artifact.final_sha1) {
            throw "Artifact '$($artifact.id)' hash mismatch. Expected final SHA-1 '$($artifact.final_sha1)', got '$($rawHashes.sha1)'."
        }
        Copy-Item -LiteralPath $rawPath -Destination $finalPath -Force
    }

    $finalHashes = Get-Hashes -Path $finalPath
    if ($finalHashes.sha1 -ne $artifact.final_sha1) {
        throw "Artifact '$($artifact.id)' final SHA-1 mismatch. Expected '$($artifact.final_sha1)', got '$($finalHashes.sha1)'."
    }

    $report.Add([ordered]@{
        id = $artifact.id
        title = $artifact.title
        page_url = $artifact.page_url
        source_download_url = $artifact.download_url
        resolved_download_url = $resolvedDownloadUrl
        raw_path = $rawPath
        raw_sha1 = $rawHashes.sha1
        raw_sha256 = $rawHashes.sha256
        wrapper_sha1_expected = $artifact.wrapper_sha1
        final_path = $finalPath
        final_sha1_expected = $artifact.final_sha1
        final_sha1_actual = $finalHashes.sha1
        final_sha256_actual = $finalHashes.sha256
        acquisition_mode = $rawMode
        extracted_entry = $wrapperExtractedEntry
        verified = $true
    }) | Out-Null
}

$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $reportPath -Encoding UTF8
Write-Output "Preserved $($report.Count) historical artifacts."
Write-Output $reportPath
