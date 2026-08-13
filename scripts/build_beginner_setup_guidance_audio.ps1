param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$OutputDirectory = "",
    [string]$FfmpegPath = "C:\Program Files\CEWE\Logiciel de creation CEWE\ffmpeg.exe",
    [string]$Mp3EncoderPath = "E:\Thomas\Documents\MkvToMp4_0.224\Tools\ffmpeg\x64\ffmpeg.exe"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$manifestPath = Join-Path $ProjectRoot "app\src\main\assets\audio_manifest.json"
$temporaryDirectory = Join-Path $ProjectRoot "artifacts\guidage-debutant-wav-temp"
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $ProjectRoot "artifacts\guidage-debutant-candidat"
}

if (-not (Test-Path -LiteralPath $FfmpegPath -PathType Leaf)) {
    throw "ffmpeg introuvable : $FfmpegPath"
}
if (-not (Test-Path -LiteralPath $Mp3EncoderPath -PathType Leaf)) {
    throw "Encodeur MP3 introuvable : $Mp3EncoderPath"
}

$manifest = Get-Content -Raw -Encoding UTF8 -LiteralPath $manifestPath |
    ConvertFrom-Json
$assets = @(
    $manifest.assets |
        Where-Object { $_.category -eq "guidage_debutant" }
)
if ($assets.Count -ne 22) {
    throw "22 ressources de guidage attendues, $($assets.Count) trouvées."
}

$existing = @(
    $assets |
        Where-Object {
            Test-Path -LiteralPath (Join-Path $OutputDirectory $_.filename)
        }
)
if ($existing.Count -gt 0) {
    throw "Ressources déjà présentes : $($existing.filename -join ', ')"
}

New-Item -ItemType Directory -Force -Path $temporaryDirectory | Out-Null
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

Add-Type -AssemblyName System.Runtime.WindowsRuntime
[Windows.Media.SpeechSynthesis.SpeechSynthesizer, Windows.Media.SpeechSynthesis, ContentType=WindowsRuntime] |
    Out-Null
[Windows.Media.SpeechSynthesis.SpeechSynthesisStream, Windows.Media.SpeechSynthesis, ContentType=WindowsRuntime] |
    Out-Null

$asTaskMethod = [System.WindowsRuntimeSystemExtensions].GetMethods() |
    Where-Object {
        $_.Name -eq "AsTask" -and
        $_.IsGenericMethod -and
        $_.GetParameters().Count -eq 1
    } |
    Select-Object -First 1

function Get-LocalVoice {
    param([Parameter(Mandatory)][string]$DisplayName)

    $voice = [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices |
        Where-Object { $_.DisplayName -eq $DisplayName } |
        Select-Object -First 1
    if ($null -eq $voice -or $voice.Language -ne "fr-FR") {
        throw "Voix française Windows absente : $DisplayName"
    }
    return $voice
}

$maleVoice = Get-LocalVoice -DisplayName "Microsoft Paul"
$femaleVoice = Get-LocalVoice -DisplayName "Microsoft Julie"

foreach ($asset in $assets) {
    $voice = if ($asset.basename.StartsWith("guidage_homme_")) {
        $maleVoice
    } elseif ($asset.basename.StartsWith("guidage_femme_")) {
        $femaleVoice
    } else {
        throw "Voix impossible à déterminer pour $($asset.basename)."
    }

    $wavePath = Join-Path $temporaryDirectory "$($asset.basename).wav"
    $normalizedWavePath = Join-Path $temporaryDirectory "$($asset.basename)-normalise.wav"
    $destination = Join-Path $OutputDirectory $asset.filename
    $synthesizer = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer
    try {
        $synthesizer.Voice = $voice
        $operation = $synthesizer.SynthesizeTextToStreamAsync($asset.spoken_text)
        $task = $asTaskMethod.MakeGenericMethod(
            [Windows.Media.SpeechSynthesis.SpeechSynthesisStream]
        ).Invoke($null, @($operation))
        $task.Wait()
        $speechStream = $task.Result
        try {
            $input = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead(
                $speechStream
            )
            $output = [System.IO.File]::Create($wavePath)
            try {
                $input.CopyTo($output)
            } finally {
                $output.Dispose()
                $input.Dispose()
            }
        } finally {
            $speechStream.Dispose()
        }
    } finally {
        $synthesizer.Dispose()
    }

    & $FfmpegPath `
        -hide_banner `
        -loglevel error `
        -i $wavePath `
        -map_metadata -1 `
        -af "highpass=f=70,lowpass=f=12500,acompressor=threshold=0.18:ratio=2.5:attack=15:release=180:makeup=1.25,loudnorm=I=-18:TP=-2:LRA=7,apad=pad_dur=0.12" `
        -ac 1 `
        -ar 44100 `
        -c:a pcm_s16le `
        -y `
        $normalizedWavePath
    if ($LASTEXITCODE -ne 0) {
        throw "Échec de normalisation : $wavePath"
    }

    & $Mp3EncoderPath `
        -loglevel error `
        -i $normalizedWavePath `
        -map_metadata -1 `
        -c:a libmp3lame `
        -b:a 128k `
        -y `
        $destination
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $destination)) {
        throw "Échec de production : $destination"
    }
    Remove-Item -LiteralPath $wavePath
    Remove-Item -LiteralPath $normalizedWavePath
    Write-Output "Voix générée : $($asset.filename) ($($voice.DisplayName))"
}

Write-Output "22 voix françaises Windows générées dans $OutputDirectory."
