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
        & $apksigner verify --verbose $apkPath
        if ($LASTEXITCODE -ne 0) {
            throw "La vérification de signature de l'APK a échoué."
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
