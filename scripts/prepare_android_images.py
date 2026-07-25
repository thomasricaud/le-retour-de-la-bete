"""Prepare generated master artwork for Android resources.

Run with the bundled Python runtime or any Python installation containing Pillow:
    python scripts/prepare_android_images.py
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "artwork" / "source"
RES = ROOT / "app" / "src" / "main" / "res"


def contain(image: Image.Image, maximum: tuple[int, int]) -> Image.Image:
    prepared = image.convert("RGB")
    prepared.thumbnail(maximum, Image.Resampling.LANCZOS)
    return prepared


def save_webp(source_name: str, output_name: str, maximum: tuple[int, int]) -> None:
    destination = RES / "drawable-nodpi" / output_name
    destination.parent.mkdir(parents=True, exist_ok=True)
    image = contain(Image.open(SOURCE / source_name), maximum)
    image.save(destination, "WEBP", quality=86, method=6)


def save_launcher_icons() -> None:
    source = Image.open(SOURCE / "wolf_moon_emblem_master.png").convert("RGB")
    density_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in density_sizes.items():
        destination_dir = RES / folder
        destination_dir.mkdir(parents=True, exist_ok=True)
        artwork = source.resize((size, size), Image.Resampling.LANCZOS)
        artwork = ImageEnhance.Sharpness(artwork).enhance(1.15)

        standard_inset = max(2, round(size * 0.08))
        standard_size = size - standard_inset * 2
        standard_art = artwork.resize(
            (standard_size, standard_size),
            Image.Resampling.LANCZOS,
        )
        standard_mask = Image.new("L", (standard_size, standard_size), 0)
        ImageDraw.Draw(standard_mask).rounded_rectangle(
            (0, 0, standard_size - 1, standard_size - 1),
            radius=round(standard_size * 0.22),
            fill=255,
        )
        standard = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        standard.paste(
            standard_art,
            (standard_inset, standard_inset),
            standard_mask,
        )
        standard.save(destination_dir / "ic_launcher.png", "PNG", optimize=True)

        round_inset = max(1, round(size * 0.04))
        round_size = size - round_inset * 2
        round_art = artwork.resize((round_size, round_size), Image.Resampling.LANCZOS)
        round_mask = Image.new("L", (round_size, round_size), 0)
        ImageDraw.Draw(round_mask).ellipse(
            (0, 0, round_size - 1, round_size - 1),
            fill=255,
        )
        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_icon.paste(round_art, (round_inset, round_inset), round_mask)
        round_icon.save(
            destination_dir / "ic_launcher_round.png",
            "PNG",
            optimize=True,
        )


def main() -> None:
    save_webp("village_night_master.png", "bg_village_night.webp", (1600, 1200))
    save_webp("village_day_master.png", "bg_village_day.webp", (1600, 1200))
    save_webp("wolf_moon_emblem_master.png", "app_emblem.webp", (768, 768))
    save_launcher_icons()


if __name__ == "__main__":
    main()
