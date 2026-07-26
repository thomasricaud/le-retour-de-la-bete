"""Produit un catalogue audio candidat complet à partir du manifeste Android.

Les ressources déjà présentes dans res/raw sont recopiées sans modification.
Les voix manquantes sont synthétisées avec Edge TTS, puis normalisées en MP3
mono 44,1 kHz / 128 kbit/s. Le résultat reste dans artifacts/ jusqu'à validation
humaine à l'écoute.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import Any

import edge_tts
import imageio_ffmpeg


DEFAULT_VOICE = "fr-FR-RemyMultilingualNeural"
DEFAULT_RATE = "-12%"
DEFAULT_PITCH = "-10Hz"
VOICE_KIND = "voice"
MAX_PARALLEL_SYNTHESIS = 3
DAY_AMBIENCE_BASENAME = "commun_013_ambiance_jour_boucle"


def parse_args() -> argparse.Namespace:
    project_root = Path(__file__).resolve().parents[1]
    bundled_ffmpeg = Path(imageio_ffmpeg.get_ffmpeg_exe())
    parser = argparse.ArgumentParser(
        description="Génère les voix manquantes et assemble les 43 pistes candidates.",
    )
    parser.add_argument("--project-root", type=Path, default=project_root)
    parser.add_argument(
        "--output",
        type=Path,
        default=project_root / "artifacts" / "audio-catalogue-complet-candidat",
    )
    parser.add_argument("--voice", default=DEFAULT_VOICE)
    parser.add_argument("--rate", default=DEFAULT_RATE)
    parser.add_argument("--pitch", default=DEFAULT_PITCH)
    parser.add_argument(
        "--day-ambience",
        type=Path,
        default=project_root
        / "artifacts"
        / "audio-day-village-candidate"
        / "commun_013_ambiance_jour_boucle.mp3",
        help="Ambiance de jour candidate détendue à inclure à la place de res/raw.",
    )
    parser.add_argument(
        "--ffmpeg",
        type=Path,
        default=bundled_ffmpeg,
    )
    parser.add_argument(
        "--mp3-encoder",
        type=Path,
        default=bundled_ffmpeg,
    )
    return parser.parse_args()


def run_command(command: list[str]) -> None:
    completed = subprocess.run(
        command,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if completed.returncode != 0:
        details = completed.stderr.strip() or completed.stdout.strip()
        raise RuntimeError(f"Commande en échec ({completed.returncode}) : {details}")


def normalize_voice(
    source: Path,
    destination: Path,
    ffmpeg: Path,
    mp3_encoder: Path,
) -> None:
    filter_chain = ",".join(
        (
            "highpass=f=70",
            "lowpass=f=12500",
            "acompressor=threshold=0.18:ratio=2.5:attack=15:release=180:makeup=1.25",
            "loudnorm=I=-18:TP=-2:LRA=7",
            "apad=pad_dur=0.12",
        ),
    )
    with tempfile.TemporaryDirectory(prefix="retour-bete-voice-pcm-") as temporary:
        pcm_output = Path(temporary) / "voice.s16le"
        run_command(
            [
                str(ffmpeg),
                "-hide_banner",
                "-loglevel",
                "error",
                "-i",
                str(source),
                "-map_metadata",
                "-1",
                "-af",
                filter_chain,
                "-ac",
                "1",
                "-ar",
                "44100",
                "-c:a",
                "pcm_s16le",
                "-f",
                "s16le",
                "-y",
                str(pcm_output),
            ],
        )
        run_command(
            [
                str(mp3_encoder),
                "-loglevel",
                "error",
                "-f",
                "s16le",
                "-ar",
                "44100",
                "-ac",
                "1",
                "-i",
                str(pcm_output),
                "-map_metadata",
                "-1",
                "-c:a",
                "libmp3lame",
                "-b:a",
                "128k",
                "-y",
                str(destination),
            ],
        )
    run_command(
        [
            str(ffmpeg),
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            str(destination),
            "-f",
            "null",
            "NUL",
        ],
    )


async def synthesize_asset(
    asset: dict[str, Any],
    destination: Path,
    voice: str,
    rate: str,
    pitch: str,
    ffmpeg: Path,
    mp3_encoder: Path,
    semaphore: asyncio.Semaphore,
) -> None:
    text = asset.get("spoken_text")
    if not isinstance(text, str) or not text.strip():
        raise ValueError(f"Texte parlé absent pour {asset['basename']}")

    async with semaphore:
        with tempfile.TemporaryDirectory(prefix="retour-bete-tts-") as temporary:
            source = Path(temporary) / "edge-source.mp3"
            last_error: Exception | None = None
            for attempt in range(1, 4):
                try:
                    communicate = edge_tts.Communicate(
                        text=text,
                        voice=voice,
                        rate=rate,
                        pitch=pitch,
                    )
                    await communicate.save(str(source))
                    await asyncio.to_thread(
                        normalize_voice,
                        source,
                        destination,
                        ffmpeg,
                        mp3_encoder,
                    )
                    print(
                        f"Voix générée : {asset['basename']}",
                        flush=True,
                    )
                    return
                except Exception as error:  # noqa: BLE001 - report de production
                    last_error = error
                    if attempt < 3:
                        await asyncio.sleep(attempt * 1.5)
            raise RuntimeError(
                f"Échec après trois tentatives pour {asset['basename']}: {last_error}",
            )


def probe_audio(path: Path, ffmpeg: Path) -> dict[str, Any]:
    completed = subprocess.run(
        [
            str(ffmpeg),
            "-hide_banner",
            "-i",
            str(path),
            "-f",
            "null",
            "NUL",
        ],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"Décodage impossible pour {path.name} : {completed.stderr.strip()}",
        )

    duration_match = re.search(
        r"Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)",
        completed.stderr,
    )
    audio_match = re.search(
        r"Audio:\s*([^,\s]+).*?,\s*(\d+)\s*Hz,\s*([^,\r\n]+)",
        completed.stderr,
    )
    bitrate_match = re.search(r"(\d+)\s*kb/s", completed.stderr)
    if duration_match is None or audio_match is None:
        raise RuntimeError(
            f"Métadonnées audio illisibles pour {path.name}",
        )

    hours, minutes, seconds = duration_match.groups()
    duration_seconds = int(hours) * 3600 + int(minutes) * 60 + float(seconds)
    channel_description = audio_match.group(3).strip().lower()
    channels = 1 if channel_description.startswith("mono") else 2
    return {
        "duration_seconds": round(duration_seconds, 3),
        "codec": audio_match.group(1),
        "sample_rate": int(audio_match.group(2)),
        "channels": channels,
        "bit_rate": int(bitrate_match.group(1)) * 1_000 if bitrate_match else 0,
        "size_bytes": path.stat().st_size,
    }


async def build_catalog(args: argparse.Namespace) -> None:
    project_root = args.project_root.resolve()
    manifest_path = project_root / "app" / "src" / "main" / "assets" / "audio_manifest.json"
    raw_directory = project_root / "app" / "src" / "main" / "res" / "raw"
    output = args.output.resolve()
    tracks_directory = output / "tracks"

    for tool in (args.ffmpeg, args.mp3_encoder):
        if not tool.is_file():
            raise FileNotFoundError(f"Outil introuvable : {tool}")

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    assets: list[dict[str, Any]] = manifest["assets"]
    if manifest["asset_count"] != len(assets):
        raise ValueError("asset_count ne correspond pas au nombre d'entrées du manifeste")

    if output.exists():
        shutil.rmtree(output)
    tracks_directory.mkdir(parents=True)

    reused: list[str] = []
    generated_day_ambience: list[str] = []
    to_generate: list[dict[str, Any]] = []
    for asset in assets:
        basename = asset["basename"]
        existing = raw_directory / asset["filename"]
        if basename == DAY_AMBIENCE_BASENAME:
            if not args.day_ambience.is_file():
                raise FileNotFoundError(
                    "Ambiance de jour candidate absente. Exécuter d'abord "
                    "scripts/build_day_village_ambience.py",
                )
            shutil.copy2(
                args.day_ambience,
                tracks_directory / asset["filename"],
            )
            generated_day_ambience.append(basename)
        elif existing.is_file():
            shutil.copy2(existing, tracks_directory / existing.name)
            reused.append(basename)
        elif asset.get("kind") == VOICE_KIND:
            to_generate.append(asset)
        else:
            raise FileNotFoundError(
                f"Ressource non vocale absente et non synthétisable : {basename}",
            )

    semaphore = asyncio.Semaphore(MAX_PARALLEL_SYNTHESIS)
    tasks = [
        synthesize_asset(
            asset=asset,
            destination=tracks_directory / asset["filename"],
            voice=args.voice,
            rate=args.rate,
            pitch=args.pitch,
            ffmpeg=args.ffmpeg,
            mp3_encoder=args.mp3_encoder,
            semaphore=semaphore,
        )
        for asset in to_generate
    ]
    await asyncio.gather(*tasks)

    files = sorted(tracks_directory.glob("*.mp3"))
    expected_names = {asset["filename"] for asset in assets}
    actual_names = {path.name for path in files}
    if actual_names != expected_names:
        missing = sorted(expected_names - actual_names)
        unexpected = sorted(actual_names - expected_names)
        raise RuntimeError(f"Catalogue incomplet. Manquants={missing}, inattendus={unexpected}")

    generated_names = {asset["basename"] for asset in to_generate}
    day_ambience_names = set(generated_day_ambience)
    report_assets = []
    for asset in assets:
        path = tracks_directory / asset["filename"]
        report_assets.append(
            {
                "basename": asset["basename"],
                "origin": (
                    "generated_neural_voice"
                    if asset["basename"] in generated_names
                    else "generated_day_village_ambience"
                    if asset["basename"] in day_ambience_names
                    else "reused_validated_resource"
                ),
                "spoken_text": asset.get("spoken_text"),
                **probe_audio(path, args.ffmpeg),
            },
        )

    report = {
        "schema_version": 1,
        "status": "candidate_requires_listening_review",
        "summary": {
            "expected_assets": len(assets),
            "produced_assets": len(files),
            "reused_assets": len(reused),
            "generated_voice_assets": len(to_generate),
            "generated_day_ambiences": len(generated_day_ambience),
        },
        "voice_profile": {
            "service": "Edge TTS",
            "voice": args.voice,
            "rate": args.rate,
            "pitch": args.pitch,
            "post_processing": "mono 44.1 kHz, 128 kbit/s, EQ, compression, EBU R128 -18 LUFS",
        },
        "assets": report_assets,
    }
    (output / "production_report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    shutil.copy2(manifest_path, output / "audio_manifest.json")
    (output / "README.txt").write_text(
        "\n".join(
            (
                "CATALOGUE AUDIO COMPLET CANDIDAT",
                "",
                f"{len(files)} pistes MP3 dans le dossier tracks.",
                f"{len(reused)} ressources existantes recopiées sans modification.",
                f"{len(to_generate)} voix neuronales générées avec {args.voice}.",
                "1 ambiance diurne de village générée séparément.",
                "",
                "Statut : écoute et validation humaine requises avant intégration.",
                "Consulter production_report.json pour les durées et formats.",
                "",
            ),
        ),
        encoding="utf-8",
    )
    print(
        f"Catalogue candidat complet : {len(files)} pistes, "
        f"{len(to_generate)} voix générées, sortie={output}",
    )


def main() -> None:
    args = parse_args()
    asyncio.run(build_catalog(args))


if __name__ == "__main__":
    main()
