$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$credentialsPath = Join-Path `
    $projectRoot `
    ".signing\credentials.dpapi.xml"

if (-not (Test-Path -LiteralPath $credentialsPath)) {
    throw "Identifiants absents : $credentialsPath"
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

[pscustomobject]@{
    KeystorePassword =
        ConvertFrom-LocalSecureString $credentials.StorePassword
    KeyAlias = $credentials.KeyAlias
    KeyPassword =
        ConvertFrom-LocalSecureString $credentials.KeyPassword
}
