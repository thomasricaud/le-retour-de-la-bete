"""Génère une ambiance de village diurne, détendue et bouclable.

Le paysage sonore est créé sans reprendre la matière musicale nocturne et sans
aucune voix : oiseaux, fontaine, souffle extérieur, pas et activité artisanale
lointaine. Le résultat est un candidat à valider à l'écoute.
"""

from __future__ import annotations

import argparse
import asyncio
import math
import random
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

import imageio_ffmpeg

SAMPLE_RATE = 44_100
SOURCE_DURATION_SECONDS = 42.0
LOOP_DURATION_SECONDS = 36.0


def parse_args() -> argparse.Namespace:
    project_root = Path(__file__).resolve().parents[1]
    bundled_ffmpeg = Path(imageio_ffmpeg.get_ffmpeg_exe())
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output",
        type=Path,
        default=project_root
        / "artifacts"
        / "audio-day-village-candidate"
        / "commun_013_ambiance_jour_boucle.mp3",
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


def render_environment_wave(destination: Path) -> None:
    random_generator = random.Random(7_426)
    total_frames = int(SOURCE_DURATION_SECONDS * SAMPLE_RATE)
    bird_events = (
        (2.0, 0.42, 2_100.0, 3_100.0, -0.65),
        (7.7, 0.34, 2_450.0, 3_500.0, 0.55),
        (13.6, 0.48, 1_900.0, 2_900.0, -0.2),
        (20.4, 0.38, 2_300.0, 3_300.0, 0.72),
        (27.8, 0.46, 2_050.0, 3_050.0, -0.55),
        (34.1, 0.35, 2_500.0, 3_650.0, 0.35),
        (39.8, 0.42, 2_100.0, 3_200.0, -0.7),
    )
    footstep_events = (5.3, 11.1, 17.8, 24.3, 31.2, 38.0)
    wooden_knock_events = (3.8, 4.15, 14.2, 14.55, 23.7, 32.4, 32.75)
    cart_creak_events = (
        (8.8, 1.4, -0.45),
        (19.5, 1.1, 0.6),
        (29.0, 1.5, -0.1),
    )

    breeze_left = 0.0
    breeze_right = 0.0
    fountain_left = 0.0
    fountain_right = 0.0
    frames = bytearray()
    for frame_index in range(total_frames):
        time_seconds = frame_index / SAMPLE_RATE
        breeze_left = breeze_left * 0.995 + random_generator.uniform(-1.0, 1.0) * 0.005
        breeze_right = breeze_right * 0.995 + random_generator.uniform(-1.0, 1.0) * 0.005
        fountain_left = (
            fountain_left * 0.72 + random_generator.uniform(-1.0, 1.0) * 0.28
        )
        fountain_right = (
            fountain_right * 0.72 + random_generator.uniform(-1.0, 1.0) * 0.28
        )
        left = breeze_left * 0.032 + fountain_left * 0.006
        right = breeze_right * 0.032 + fountain_right * 0.006

        for start, duration, start_frequency, end_frequency, pan in bird_events:
            local_time = time_seconds - start
            if 0.0 <= local_time < duration:
                progress = local_time / duration
                envelope = math.sin(math.pi * progress) ** 2
                frequency = start_frequency + (end_frequency - start_frequency) * progress
                phase = 2.0 * math.pi * (
                    start_frequency * local_time
                    + 0.5 * (end_frequency - start_frequency) * local_time * progress
                )
                chirp = (
                    math.sin(phase) + 0.32 * math.sin(phase * 2.03)
                ) * envelope * 0.055
                left += chirp * math.sqrt((1.0 - pan) / 2.0)
                right += chirp * math.sqrt((1.0 + pan) / 2.0)

        for start in footstep_events:
            local_time = time_seconds - start
            if 0.0 <= local_time < 0.16:
                envelope = math.exp(-local_time * 24.0)
                step = random_generator.uniform(-1.0, 1.0) * envelope * 0.018
                left += step
                right += step * 0.85

        for start in wooden_knock_events:
            local_time = time_seconds - start
            if 0.0 <= local_time < 0.22:
                envelope = math.exp(-local_time * 19.0)
                knock = (
                    math.sin(2.0 * math.pi * 230.0 * local_time)
                    + 0.45 * math.sin(2.0 * math.pi * 510.0 * local_time)
                ) * envelope * 0.026
                left += knock * 0.8
                right += knock

        for start, duration, pan in cart_creak_events:
            local_time = time_seconds - start
            if 0.0 <= local_time < duration:
                progress = local_time / duration
                envelope = math.sin(math.pi * progress) ** 2
                frequency = 390.0 + 85.0 * math.sin(progress * math.pi)
                creak = (
                    math.sin(2.0 * math.pi * frequency * local_time)
                    + 0.3 * math.sin(2.0 * math.pi * frequency * 1.7 * local_time)
                ) * envelope * 0.011
                left += creak * math.sqrt((1.0 - pan) / 2.0)
                right += creak * math.sqrt((1.0 + pan) / 2.0)

        left_sample = max(-1.0, min(1.0, left))
        right_sample = max(-1.0, min(1.0, right))
        frames.extend(
            struct.pack(
                "<hh",
                int(left_sample * 32_767),
                int(right_sample * 32_767),
            ),
        )

    with wave.open(str(destination), "wb") as wave_file:
        wave_file.setnchannels(2)
        wave_file.setsampwidth(2)
        wave_file.setframerate(SAMPLE_RATE)
        wave_file.writeframes(frames)


def run_command(command: list[str]) -> None:
    result = subprocess.run(
        command,
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        details = result.stderr.strip() or result.stdout.strip()
        raise RuntimeError(f"Commande audio en échec : {details}")


def mix_soundscape(
    environment: Path,
    output: Path,
    ffmpeg: Path,
    mp3_encoder: Path,
) -> None:
    command = [
        str(ffmpeg),
        "-hide_banner",
        "-loglevel",
        "error",
        "-i",
        str(environment),
    ]
    filters = [
        f"[0:a]atrim=0:{LOOP_DURATION_SECONDS},"
        "highpass=f=45,lowpass=f=12500,"
        "afade=t=in:st=0:d=0.2,"
        f"afade=t=out:st={LOOP_DURATION_SECONDS - 0.2}:d=0.2,"
        "alimiter=limit=0.92,loudnorm=I=-19:TP=-3:LRA=8[out]",
    ]

    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="retour-bete-day-pcm-") as temporary:
        pcm_output = Path(temporary) / "day-village-loop.s16le"
        command.extend(
            (
                "-filter_complex",
                ";".join(filters),
                "-map",
                "[out]",
                "-ar",
                str(SAMPLE_RATE),
                "-c:a",
                "pcm_s16le",
                "-f",
                "s16le",
                "-y",
                str(pcm_output),
            ),
        )
        run_command(command)
        if pcm_output.stat().st_size < SAMPLE_RATE * 2 * 2:
            raise RuntimeError("Le mix PCM est vide ou trop court.")
        run_command(
            [
                str(mp3_encoder),
                "-loglevel",
                "error",
                "-f",
                "s16le",
                "-ar",
                str(SAMPLE_RATE),
                "-ac",
                "2",
                "-i",
                str(pcm_output),
                "-map_metadata",
                "-1",
                "-c:a",
                "libmp3lame",
                "-b:a",
                "192k",
                "-y",
                str(output),
            ],
        )
    run_command(
        [
            str(ffmpeg),
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            str(output),
            "-f",
            "null",
            "NUL",
        ],
    )


async def build(args: argparse.Namespace) -> None:
    for tool in (args.ffmpeg, args.mp3_encoder):
        if not tool.is_file():
            raise FileNotFoundError(f"Outil introuvable : {tool}")

    with tempfile.TemporaryDirectory(prefix="retour-bete-day-ambience-") as temporary:
        working_directory = Path(temporary)
        environment = working_directory / "environment.wav"
        await asyncio.to_thread(render_environment_wave, environment)
        await asyncio.to_thread(
            mix_soundscape,
            environment,
            args.output.resolve(),
            args.ffmpeg,
            args.mp3_encoder,
        )
    print(f"Ambiance de jour candidate générée : {args.output.resolve()}")


def main() -> None:
    asyncio.run(build(parse_args()))


if __name__ == "__main__":
    main()
