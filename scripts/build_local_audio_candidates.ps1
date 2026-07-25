param(
    [string]$SourceRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$OutputDirectory = (
        Join-Path (Split-Path -Parent $PSScriptRoot) "artifacts\audio-montage-candidate"
    ),
    [string]$FfmpegPath = "",
    [string]$Mp3EncoderPath = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$bundledFfmpeg = "C:\Program Files\CEWE\Logiciel de creation CEWE\ffmpeg.exe"
$legacyLameFfmpeg = "E:\Thomas\Documents\MkvToMp4_0.224\Tools\ffmpeg\x64\ffmpeg.exe"
$ffmpegCommand = Get-Command ffmpeg -ErrorAction SilentlyContinue
$processingCandidates = @(
    $FfmpegPath
    if ($null -ne $ffmpegCommand) { $ffmpegCommand.Source }
    $bundledFfmpeg
    $legacyLameFfmpeg
) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
$processingFfmpeg = $processingCandidates |
    Where-Object { Test-Path -LiteralPath $_ } |
    Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($processingFfmpeg)) {
    throw "ffmpeg est introuvable. Installez-le ou adaptez `$bundledFfmpeg dans ce script."
}

$encoderCandidates = @(
    $Mp3EncoderPath
    $processingFfmpeg
    $legacyLameFfmpeg
) |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    Select-Object -Unique
$lameFfmpeg = $null
foreach ($candidate in $encoderCandidates) {
    if (-not (Test-Path -LiteralPath $candidate)) {
        continue
    }
    $ErrorActionPreference = "Continue"
    $encoderList = & $candidate -encoders 2>$null
    $ErrorActionPreference = "Stop"
    if ($encoderList -match "libmp3lame") {
        $lameFfmpeg = $candidate
        break
    }
}
if ([string]::IsNullOrWhiteSpace($lameFfmpeg)) {
    throw "Aucun build ffmpeg avec l'encodeur libmp3lame n'a été trouvé."
}

$night = Join-Path $SourceRoot "Fichier nuit.mp3.mpeg"
$lg = Join-Path $SourceRoot "LG.mp3.mpeg"
foreach ($source in @($night, $lg)) {
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Source audio introuvable : $source"
    }
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

function Export-VoiceCandidate {
    param(
        [Parameter(Mandatory)]
        [string]$InputFile,
        [Parameter(Mandatory)]
        [double]$StartSeconds,
        [Parameter(Mandatory)]
        [double]$DurationSeconds,
        [Parameter(Mandatory)]
        [string]$Basename,
        [double]$GainDb = -4.0
    )

    $destination = Join-Path $OutputDirectory "$Basename.mp3"
    & $lameFfmpeg `
        -loglevel error `
        -ss $StartSeconds `
        -t $DurationSeconds `
        -i $InputFile `
        -map_metadata -1 `
        -af "pan=mono|c0=0.5*c0+0.5*c1,volume=${GainDb}dB" `
        -ar 44100 `
        -c:a libmp3lame `
        -b:a 128k `
        -y `
        $destination
    if ($LASTEXITCODE -ne 0) {
        throw "Échec de production : $destination"
    }
}

# Fenêtres conservatrices : elles incluent une marge autour de la parole/du dernier bip.
Export-VoiceCandidate `
    -InputFile $lg `
    -StartSeconds 2.8 `
    -DurationSeconds 6.7 `
    -Basename "commun_001_nuit_depart_30"

Export-VoiceCandidate `
    -InputFile $lg `
    -StartSeconds 180.2 `
    -DurationSeconds 11.5 `
    -Basename "commun_002_bips_11"

Export-VoiceCandidate `
    -InputFile $night `
    -StartSeconds 64.0 `
    -DurationSeconds 3.35 `
    -Basename "confirme_201_premiere_nuit_reveil_sang"

Export-VoiceCandidate `
    -InputFile $lg `
    -StartSeconds 191.8 `
    -DurationSeconds 3.6 `
    -Basename "commun_005_cocorico"

Export-VoiceCandidate `
    -InputFile $night `
    -StartSeconds 256.7 `
    -DurationSeconds 3.6 `
    -Basename "commun_006_reveil_village" `
    -GainDb 0

# Boucle candidate de 26 s. Les 3 dernières secondes fondent la fin de la plage
# instrumentale (151,5-154,5 s) dans son début (125,5-128,5 s), de sorte que la fin
# rejoint le même point musical que le début.
$ambienceDestination = Join-Path $OutputDirectory "commun_012_ambiance_nuit_boucle.mp3"
$ambienceWave = Join-Path $OutputDirectory ".commun_012_ambiance_nuit_boucle.wav"
$ambienceFilter = @(
    "[0:a]asplit=2[baseSrc][headSrc]"
    "[baseSrc]atrim=start=3:end=29,asetpts=PTS-STARTPTS,afade=t=out:st=23:d=3[base]"
    "[headSrc]atrim=start=0:end=3,asetpts=PTS-STARTPTS,afade=t=in:st=0:d=3,adelay=23000|23000[head]"
    "[base][head]amix=inputs=2:duration=longest:dropout_transition=0," +
        "volume=-4dB,afade=t=in:st=0:d=0.05,afade=t=out:st=25.95:d=0.05[out]"
) -join ";"

& $processingFfmpeg `
    -hide_banner `
    -loglevel error `
    -ss 125.5 `
    -t 29 `
    -i $lg `
    -map_metadata -1 `
    -filter_complex $ambienceFilter `
    -map "[out]" `
    -ar 44100 `
    -c:a pcm_s16le `
    -y `
    $ambienceWave
if ($LASTEXITCODE -ne 0) {
    throw "Échec de production intermédiaire : $ambienceWave"
}

& $lameFfmpeg `
    -loglevel error `
    -i $ambienceWave `
    -map_metadata -1 `
    -c:a libmp3lame `
    -b:a 192k `
    -y `
    $ambienceDestination
if ($LASTEXITCODE -ne 0) {
    throw "Échec de production : $ambienceDestination"
}
Remove-Item -LiteralPath $ambienceWave

$manifestPath = Join-Path $SourceRoot "app\src\main\assets\audio_manifest.json"
$manifest = Get-Content -Raw -Encoding UTF8 -LiteralPath $manifestPath | ConvertFrom-Json
$candidateBasenames = @(
    "commun_001_nuit_depart_30"
    "commun_002_bips_11"
    "commun_005_cocorico"
    "commun_006_reveil_village"
    "commun_012_ambiance_nuit_boucle"
    "confirme_201_premiere_nuit_reveil_sang"
)
$notProduced = @(
    $manifest.assets |
        Where-Object { $_.basename -notin $candidateBasenames } |
        ForEach-Object { $_.basename }
)

$audit = [ordered]@{
    schema_version = 1
    status = "candidate_requires_listening_review"
    generated_at = (Get-Date).ToString("o")
    summary = [ordered]@{
        expected_assets = $manifest.asset_count
        produced_candidates = $candidateBasenames.Count
        high_confidence_candidates = 3
        listening_review_candidates = 3
        not_produced = $notProduced.Count
    }
    sources = @(
        "Fichier nuit.mp3.mpeg"
        "LG.mp3.mpeg"
    )
    candidates = @(
        [ordered]@{
            basename = "commun_001_nuit_depart_30"
            confidence = "high"
            source = "LG.mp3.mpeg"
            source_window_seconds = @(2.8, 9.5)
            note = "Texte 30 secondes détecté ; fond musical indissociable."
        }
        [ordered]@{
            basename = "commun_002_bips_11"
            confidence = "high"
            source = "LG.mp3.mpeg"
            source_window_seconds = @(180.2, 191.7)
            note = "Onze impulsions espacées d'environ une seconde ; fond musical indissociable."
        }
        [ordered]@{
            basename = "confirme_201_premiere_nuit_reveil_sang"
            confidence = "high"
            source = "Fichier nuit.mp3.mpeg"
            source_window_seconds = @(64.0, 67.35)
            note = "Début exact d'une annonce plus longue, coupé avant la suite ; fond musical indissociable."
        }
        [ordered]@{
            basename = "commun_005_cocorico"
            confidence = "medium"
            source = "LG.mp3.mpeg"
            source_window_seconds = @(191.8, 195.4)
            note = "Premier des deux appels de coq détectés ; validation à l'écoute requise."
        }
        [ordered]@{
            basename = "commun_006_reveil_village"
            confidence = "medium"
            source = "Fichier nuit.mp3.mpeg"
            source_window_seconds = @(256.7, 260.3)
            note = "Phrase détectée dans le dernier bloc isolé ; validation à l'écoute requise."
        }
        [ordered]@{
            basename = "commun_012_ambiance_nuit_boucle"
            confidence = "medium"
            source = "LG.mp3.mpeg"
            source_window_seconds = @(125.5, 154.5)
            note = "Boucle reconstruite avec fondu circulaire de 3 secondes ; écoute du raccord requise."
        }
    )
    rejected_findings = @(
        [ordered]@{
            basename = "commun_001_nuit_depart_45"
            reason = "Aucune mention de quarante-cinq secondes dans les trois sources."
        }
        [ordered]@{
            basename = "commun_003_endormissement"
            reason = "Seul le fragment « Tous les villageois s'endorment » est détecté ; la suite contractuelle manque."
        }
        [ordered]@{
            basename = "debutant_101_premiere_nuit_reveil_sang"
            reason = "La phrase « Le loup garou de sang se réveille » existe, mais sans le préfixe obligatoire « Première nuit »."
        }
        [ordered]@{
            basename = "commun_004_tous_se_rendorment"
            reason = "Aucune reconnaissance exacte dans les blocs de fin."
        }
    )
    not_produced_basenames = $notProduced
}

$audit |
    ConvertTo-Json -Depth 6 |
    Set-Content -LiteralPath (Join-Path $OutputDirectory "candidate_audit.json") -Encoding UTF8

Write-Output "Candidats produits dans : $OutputDirectory"
