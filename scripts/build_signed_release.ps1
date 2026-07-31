param(
    [switch]$SkipChecks,
    [string]$VersionName,
    [int]$VersionCode
)

$ErrorActionPreference = "Stop"
$hasVersionName = -not [string]::IsNullOrWhiteSpace($VersionName)
$hasVersionCode = $VersionCode -gt 0
if ($hasVersionName -ne $hasVersionCode) {
    throw "VersionName et VersionCode doivent être fournis ensemble."
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$signingDirectory = Join-Path $projectRoot ".signing"
$credentialsPath = Join-Path $signingDirectory "credentials.dpapi.xml"
$keystorePath = Join-Path $signingDirectory "retour-bete-release.jks"

if (-not (Test-Path -LiteralPath $credentialsPath)) {
    throw "Identifiants absents : $credentialsPath"
}
if (-not (Test-Path -LiteralPath $keystorePath)) {
    throw "Clé de signature absente : $keystorePath"
}

function ConvertFrom-LocalSecureString {
    param([Security.SecureString]$Value)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$credentials = Import-Clixml -LiteralPath $credentialsPath
$env:ANDROID_KEYSTORE_PATH = $keystorePath
$env:ANDROID_KEYSTORE_PASSWORD =
    ConvertFrom-LocalSecureString $credentials.StorePassword
$env:ANDROID_KEY_ALIAS = $credentials.KeyAlias
$env:ANDROID_KEY_PASSWORD =
    ConvertFrom-LocalSecureString $credentials.KeyPassword

$jdk17 = "C:\Users\Thomas\AppData\Local\Programs\jdk-17"
if (Test-Path -LiteralPath $jdk17) {
    $env:JAVA_HOME = $jdk17
}

try {
    Push-Location $projectRoot
    try {
        $tasks = if ($SkipChecks) {
            @("assembleRelease")
        } else {
            @("testDebugUnitTest", "lintDebug", "assembleRelease")
        }
        $gradleArgs = @()
        $gradleArgs += $tasks
        if ($hasVersionName) {
            $gradleArgs += "-PappVersionName=$VersionName"
            $gradleArgs += "-PappVersionCode=$VersionCode"
        }
        $gradleArgs += "--offline"
        $gradleArgs += "--no-daemon"
        & ".\gradlew.bat" $gradleArgs
        if ($LASTEXITCODE -ne 0) {
            throw "La construction Gradle a échoué."
        }

        $apkPath = Join-Path $projectRoot `
            "app\build\outputs\apk\release\app-release.apk"
        $apksigner = Join-Path `
            "C:\Users\Thomas\AppData\Local\Android\Sdk" `
            "build-tools\35.0.0\apksigner.bat"
        $signatureOutput = & $apksigner verify --verbose --print-certs $apkPath
        $signatureExitCode = $LASTEXITCODE
        $signatureOutput
        if ($signatureExitCode -ne 0) {
            throw "La vérification de signature de l'APK a échoué."
        }

        $expectedFingerprintPath = Join-Path $projectRoot `
            "distribution\release-signing-certificate.sha256"
        $expectedFingerprint =
            (Get-Content -Raw -LiteralPath $expectedFingerprintPath).Trim()
        $certificateMatch = $signatureOutput | Select-String -Pattern `
            '^Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})$'
        if (-not $certificateMatch) {
            throw "Empreinte du certificat de signature introuvable."
        }
        $actualFingerprint = $certificateMatch.Matches[0].Groups[1].Value
        if ($actualFingerprint -ne $expectedFingerprint) {
            throw (
                "Le certificat de signature ne correspond pas aux versions " +
                "déjà installées. Publication interdite."
            )
        }
        Get-Item -LiteralPath $apkPath
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item Env:ANDROID_KEYSTORE_PATH -ErrorAction SilentlyContinue
    Remove-Item Env:ANDROID_KEYSTORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:ANDROID_KEY_ALIAS -ErrorAction SilentlyContinue
    Remove-Item Env:ANDROID_KEY_PASSWORD -ErrorAction SilentlyContinue
}
