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

function Get-DeliveryProfile {
    param([Parameter(Mandatory)][string]$Basename)

    if ($Basename -notmatch "debutant(?<Number>\d+)$") {
        throw "Numéro de guidage absent : $Basename"
    }
    $profiles = @{
        1 = @{ Rate = "-4%"; Pitch = "+1st" }
        2 = @{ Rate = "-5%"; Pitch = "-1st" }
        3 = @{ Rate = "-7%"; Pitch = "+0st" }
        4 = @{ Rate = "-5%"; Pitch = "-1st" }
        5 = @{ Rate = "-3%"; Pitch = "+0st" }
        6 = @{ Rate = "-6%"; Pitch = "-1st" }
        7 = @{ Rate = "-8%"; Pitch = "-2st" }
        8 = @{ Rate = "-4%"; Pitch = "+1st" }
        9 = @{ Rate = "-6%"; Pitch = "-1st" }
        10 = @{ Rate = "-7%"; Pitch = "-2st" }
        11 = @{ Rate = "-5%"; Pitch = "-1st" }
    }
    return $profiles[[int]$Matches.Number]
}

function ConvertTo-GuidanceSsml {
    param(
        [Parameter(Mandatory)][string]$Text,
        [Parameter(Mandatory)][string]$Basename
    )

    $profile = Get-DeliveryProfile -Basename $Basename
    $content = [System.Security.SecurityElement]::Escape($Text)
    foreach ($phrase in @(
        "Le retour de la Bête",
        "loup-garou de sang",
        "Venez à moi, ma meute, mes adorateurs !",
        "APPEL",
        "votre objectif",
        "SUIVANT",
        "VOIR"
    )) {
        $escapedPhrase = [System.Security.SecurityElement]::Escape($phrase)
        $content = $content.Replace(
            $escapedPhrase,
            "<emphasis level=`"moderate`">$escapedPhrase</emphasis>"
        )
    }
    $content = $content -replace "…", "…<break time=`"320ms`"/>"
    $content = $content -replace "([.!?]) ", '$1<break time="180ms"/> '
    return @"
<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="fr-FR"><prosody rate="$($profile.Rate)" pitch="$($profile.Pitch)">$content</prosody></speak>
"@
}

function Split-GuidanceText {
    param(
        [Parameter(Mandatory)][string]$Text,
        [int]$MaximumLength = 700
    )

    $sentences = [regex]::Split($Text.Trim(), "(?<=[.!?…])\s+")
    $chunks = [System.Collections.Generic.List[string]]::new()
    $current = ""
    foreach ($sentence in $sentences) {
        $candidate = if ([string]::IsNullOrWhiteSpace($current)) {
            $sentence
        } else {
            "$current $sentence"
        }
        if ($candidate.Length -le $MaximumLength) {
            $current = $candidate
        } else {
            if (-not [string]::IsNullOrWhiteSpace($current)) {
                $chunks.Add($current)
            }
            $current = $sentence
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($current)) {
        $chunks.Add($current)
    }
    return $chunks.ToArray()
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

    $normalizedWavePath = Join-Path $temporaryDirectory "$($asset.basename)-normalise.wav"
    $destination = Join-Path $OutputDirectory $asset.filename
    $synthesizer = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer
    $wavePaths = [System.Collections.Generic.List[string]]::new()
    try {
        $synthesizer.Voice = $voice
        $chunks = @(Split-GuidanceText -Text $asset.spoken_text)
        for ($chunkIndex = 0; $chunkIndex -lt $chunks.Count; $chunkIndex++) {
            $wavePath = Join-Path `
                $temporaryDirectory `
                "$($asset.basename)-$($chunkIndex + 1).wav"
            $wavePaths.Add($wavePath)
            $ssml = ConvertTo-GuidanceSsml `
                -Text $chunks[$chunkIndex] `
                -Basename $asset.basename
            $operation = $synthesizer.SynthesizeSsmlToStreamAsync($ssml)
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
        }
    } finally {
        $synthesizer.Dispose()
    }

    $ffmpegArguments = @("-hide_banner", "-loglevel", "error")
    foreach ($wavePath in $wavePaths) {
        $ffmpegArguments += @("-i", $wavePath)
    }
    if ($wavePaths.Count -eq 1) {
        $ffmpegArguments += @(
            "-af",
            "highpass=f=70,lowpass=f=12500,acompressor=threshold=0.18:ratio=2.5:attack=15:release=180:makeup=1.25,loudnorm=I=-18:TP=-2:LRA=7,apad=pad_dur=0.12",
            "-map_metadata", "-1",
            "-ac", "1",
            "-ar", "44100",
            "-c:a", "pcm_s16le",
            "-y",
            $normalizedWavePath
        )
    } else {
        $concatInputs = (0..($wavePaths.Count - 1) | ForEach-Object { "[$($_):a]" }) -join ""
        $filterComplex = "$concatInputs" +
            "concat=n=$($wavePaths.Count):v=0:a=1[voice];" +
            "[voice]highpass=f=70,lowpass=f=12500," +
            "acompressor=threshold=0.18:ratio=2.5:attack=15:release=180:makeup=1.25," +
            "loudnorm=I=-18:TP=-2:LRA=7,apad=pad_dur=0.12[out]"
        $ffmpegArguments += @(
            "-filter_complex",
            $filterComplex,
            "-map", "[out]",
            "-map_metadata", "-1",
            "-ac", "1",
            "-ar", "44100",
            "-c:a", "pcm_s16le",
            "-y",
            $normalizedWavePath
        )
    }
    & $FfmpegPath @ffmpegArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Échec de normalisation : $($asset.filename)"
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
    foreach ($wavePath in $wavePaths) {
        Remove-Item -LiteralPath $wavePath
    }
    Remove-Item -LiteralPath $normalizedWavePath
    Write-Output "Voix générée : $($asset.filename) ($($voice.DisplayName))"
}

Write-Output "22 voix françaises Windows générées dans $OutputDirectory."
