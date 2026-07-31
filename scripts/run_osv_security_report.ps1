param(
    [Parameter(Mandatory = $true)]
    [string]$SbomPath,
    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory,
    [string]$ProjectVersion = "source",
    [string]$CommitSha = $env:GITHUB_SHA,
    [string]$ApkSha256,
    [string]$CertificateSha256,
    [string]$ScannerPath
)

$ErrorActionPreference = "Stop"
$scannerVersion = "2.4.0"
$scannerChecksums = @{
    "osv-scanner_linux_amd64" =
        "15314940c10d26af9c6649f150b8a47c1262e8fc7e17b1d1029b0e479e8ed8a0"
    "osv-scanner_windows_amd64.exe" =
        "0cdd113610126d5dfd5e12ad0e0b4f3e879291ff19bb43b0c52ed2f2c2df1a37"
}

$resolvedSbom = (Resolve-Path -LiteralPath $SbomPath).Path
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$resolvedOutput = (Resolve-Path -LiteralPath $OutputDirectory).Path

if ([Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
        [Runtime.InteropServices.OSPlatform]::Windows
    )) {
    $scannerAsset = "osv-scanner_windows_amd64.exe"
} elseif ([Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
        [Runtime.InteropServices.OSPlatform]::Linux
    )) {
    $scannerAsset = "osv-scanner_linux_amd64"
} else {
    throw "OSV-Scanner n'est épinglé que pour Windows x64 et Linux x64."
}

$temporaryDirectory = $null
if ([string]::IsNullOrWhiteSpace($ScannerPath)) {
    $temporaryDirectory = Join-Path ([IO.Path]::GetTempPath()) `
        ("osv-scanner-" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null
    $ScannerPath = Join-Path $temporaryDirectory $scannerAsset
    $downloadUrl =
        "https://github.com/google/osv-scanner/releases/download/" +
        "v$scannerVersion/$scannerAsset"
    Invoke-WebRequest -Uri $downloadUrl -OutFile $ScannerPath
} else {
    $ScannerPath = (Resolve-Path -LiteralPath $ScannerPath).Path
}

try {
    $expectedScannerHash = $scannerChecksums[$scannerAsset]
    $actualScannerHash =
        (Get-FileHash -Algorithm SHA256 -LiteralPath $ScannerPath).Hash.ToLowerInvariant()
    if ($actualScannerHash -ne $expectedScannerHash) {
        throw "L'empreinte du binaire OSV-Scanner $scannerVersion est invalide."
    }

    if ($scannerAsset -notlike "*.exe") {
        & chmod +x $ScannerPath
        if ($LASTEXITCODE -ne 0) {
            throw "Impossible de rendre OSV-Scanner exécutable."
        }
    }

    $osvJsonPath = Join-Path $resolvedOutput "osv-results.json"
    $sarifPath = Join-Path $resolvedOutput "osv-results.sarif"
    $summaryJsonPath = Join-Path $resolvedOutput "security-report.json"
    $summaryMarkdownPath = Join-Path $resolvedOutput "security-report.md"

    if (Test-Path Variable:PSNativeCommandUseErrorActionPreference) {
        $PSNativeCommandUseErrorActionPreference = $false
    }

    & $ScannerPath scan source --lockfile $resolvedSbom `
        --format json --all-packages --output-file $osvJsonPath
    $jsonExitCode = $LASTEXITCODE
    if ($jsonExitCode -gt 1) {
        throw "OSV-Scanner a échoué pendant la production du rapport JSON."
    }

    & $ScannerPath scan source --lockfile $resolvedSbom `
        --format sarif --output-file $sarifPath
    $sarifExitCode = $LASTEXITCODE
    if ($sarifExitCode -gt 1) {
        throw "OSV-Scanner a échoué pendant la production du rapport SARIF."
    }

    $osvReport = Get-Content -Raw -LiteralPath $osvJsonPath | ConvertFrom-Json
    foreach ($result in @($osvReport.results)) {
        $result.source.path = Split-Path -Leaf $resolvedSbom
    }
    $osvReport | ConvertTo-Json -Depth 100 |
        Set-Content -LiteralPath $osvJsonPath -Encoding utf8

    $packages = @(
        $osvReport.results |
            ForEach-Object { $_.packages } |
            Where-Object { $null -ne $_ }
    )
    $vulnerabilities = @(
        $packages |
            ForEach-Object { $_.vulnerabilities } |
            Where-Object { $null -ne $_ }
    )
    $vulnerabilityIds = @(
        $vulnerabilities |
            ForEach-Object { $_.id } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            Sort-Object -Unique
    )
    $sbom = Get-Content -Raw -LiteralPath $resolvedSbom | ConvertFrom-Json
    $componentCount = @($sbom.components).Count
    $generatedAt = [DateTimeOffset]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
    $status = if ($vulnerabilityIds.Count -eq 0) { "passed" } else { "failed" }

    $summary = [ordered]@{
        schema_version = 1
        status = $status
        generated_at = $generatedAt
        project_version = $ProjectVersion
        commit = $CommitSha
        scanner = [ordered]@{
            name = "OSV-Scanner"
            version = $scannerVersion
            sha256 = $actualScannerHash
        }
        sbom = [ordered]@{
            format = $sbom.bomFormat
            specification_version = $sbom.specVersion
            component_count = $componentCount
        }
        result = [ordered]@{
            packages_analyzed = $packages.Count
            known_vulnerability_count = $vulnerabilityIds.Count
            vulnerability_ids = $vulnerabilityIds
        }
        apk = [ordered]@{
            sha256 = $ApkSha256
            signing_certificate_sha256 = $CertificateSha256
        }
    }
    $summary | ConvertTo-Json -Depth 10 |
        Set-Content -LiteralPath $summaryJsonPath -Encoding utf8

    $resultSentence = if ($vulnerabilityIds.Count -eq 0) {
        "**Résultat : aucune vulnérabilité connue détectée.**"
    } else {
        "**Résultat : $($vulnerabilityIds.Count) vulnérabilité(s) connue(s) détectée(s).**"
    }
    $lines = @(
        "# Rapport public de sécurité",
        "",
        $resultSentence,
        "",
        "- Version analysée : ``$ProjectVersion``",
        "- Commit : ``$CommitSha``",
        "- Analyse générée le : $generatedAt",
        "- Outil : OSV-Scanner $scannerVersion (binaire vérifié par SHA-256)",
        "- SBOM : CycloneDX $($sbom.specVersion), $componentCount composants",
        "- Paquets analysés : $($packages.Count)",
        "- Vulnérabilités connues détectées : $($vulnerabilityIds.Count)"
    )
    if (-not [string]::IsNullOrWhiteSpace($ApkSha256)) {
        $lines += "- APK SHA-256 : ``$ApkSha256``"
    }
    if (-not [string]::IsNullOrWhiteSpace($CertificateSha256)) {
        $lines += "- Certificat de signature SHA-256 : ``$CertificateSha256``"
    }
    if ($vulnerabilityIds.Count -gt 0) {
        $lines += ""
        $lines += "## Identifiants détectés"
        $lines += ""
        foreach ($vulnerabilityId in $vulnerabilityIds) {
            $lines += "- [$vulnerabilityId](https://osv.dev/vulnerability/$vulnerabilityId)"
        }
    }
    $lines += ""
    $lines += "Cette analyse compare les composants déclarés dans le SBOM à la base OSV au moment indiqué. Elle réduit le risque lié aux dépendances connues, sans garantir l'absence de toute vulnérabilité."
    $lines | Set-Content -LiteralPath $summaryMarkdownPath -Encoding utf8

    Write-Host "$($packages.Count) paquets analysés ; $($vulnerabilityIds.Count) vulnérabilité(s) connue(s)."
    if ($vulnerabilityIds.Count -gt 0 -or $jsonExitCode -ne 0 -or $sarifExitCode -ne 0) {
        throw "La publication est bloquée par l'analyse OSV."
    }
} finally {
    if ($null -ne $temporaryDirectory -and
        (Test-Path -LiteralPath $temporaryDirectory)) {
        Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
    }
}
