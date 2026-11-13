#!/usr/bin/env python3
"""Regenerate the launcher icon set from the brand art.

The launcher icon is four layers per density, and a hand-edit to one density is how the set drifts
out of step. This script is the one place the art is rasterised, so a new icon is a new source PNG
and one run.

    resources/branding/app_icon_512.png          <- the source, committed
      |
      +-- drawable-<density>/ic_launcher_foreground.png   the adaptive-icon foreground
      +-- drawable-<density>/ic_launcher_mono.png         the themed-icon silhouette
      +-- mipmap-<density>/ic_launcher.png                the legacy square icon
      +-- drawable/ic_launcher_background.xml             the background gradient
      +-- docs/public/appicon.png                         the docs site favicon and nav logo

Two things about the adaptive layers are deliberate:

* The foreground is **overscaled** past the 108dp canvas. An adaptive icon is meant to be cropped by
  whatever mask the launcher uses, and the source art has its own rounded corners; drawn at 1.0 the
  art's corners would sit inside a rounder mask and let the background show through at the edges.
  At `OVERSCALE` the art's corners fall outside every mask the platform ships, so the mask only ever
  cuts solid art.
* The background is the art's own top and bottom edge colour, sampled at generation time rather than
  picked by hand. It is only ever visible if a mask is squarer than the art's own corners, and when
  it is visible it has to look like the art continued, not like a different tile.

Usage:
    python3 scripts/gen_launcher_icons.py --source resources/branding/app_icon_512.png
    python3 scripts/gen_launcher_icons.py --check      # verify the committed set, write nothing
"""
from __future__ import annotations

import argparse
import pathlib
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
DEFAULT_SOURCE = ROOT / "resources/branding/app_icon_512.png"
# The docs site favicon and nav logo. It is the same brand mark as the launcher icon, so it is
# generated here too: left alone it kept a previous mark and the docs site wore a different logo
# from the app.
DOCS_ICON = ROOT / "docs/public/appicon.png"
DOCS_ICON_SIZE = 512

# density folder -> scale factor. `mdpi` is 1x; the icon is 48dp square and the adaptive canvas is
# 108dp, per the platform spec.
DENSITIES = [("mdpi", 1.0), ("hdpi", 1.5), ("xhdpi", 2.0), ("xxhdpi", 3.0), ("xxxhdpi", 4.0)]
LEGACY_DP = 48
ADAPTIVE_DP = 108

# How far past the 108dp canvas the foreground is drawn. 1.12 pushes the art's own rounded corners
# (20% of its width) clear of the roundest mask the platform uses, so no mask reveals the background
# inside the icon. See the module docstring.
OVERSCALE = 1.12

# The luminance below which the themed silhouette is fully transparent, and above which it is fully
# opaque. The art's gradient background sits under ~0.55 and the emblem over it, so this isolates the
# mark instead of emitting a solid tile.
MONO_LOW = 0.42
MONO_HIGH = 0.72


def source_art(path: pathlib.Path) -> Image.Image:
    if not path.is_file():
        sys.exit(f"source art not found: {path}")
    art = Image.open(path).convert("RGBA")
    if art.width != art.height:
        sys.exit(f"source art must be square, got {art.size}")
    if art.getchannel("A").getextrema()[0] != 0:
        sys.exit("source art has no transparent pixels: the corners must be transparent, or the "
                 "adaptive foreground will be a hard square")
    return art


def legacy_icon(art: Image.Image, size: int) -> Image.Image:
    """The square icon: the art as drawn, corners included."""
    return art.resize((size, size), Image.LANCZOS)


def adaptive_foreground(art: Image.Image, size: int) -> Image.Image:
    """The art drawn into a `size` canvas, overscaled and centre-cropped."""
    drawn = round(size * OVERSCALE)
    scaled = art.resize((drawn, drawn), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = (size - drawn) // 2
    canvas.paste(scaled, (offset, offset))
    return canvas


def adaptive_mono(art: Image.Image, size: int) -> Image.Image:
    """The themed-icon silhouette: white, with the art's luminance as its alpha.

    A plain white tile would theme to a featureless blob, so the emblem is kept by driving the alpha
    from the luminance ramp between `MONO_LOW` and `MONO_HIGH`.
    """
    scaled = art.resize((size, size), Image.LANCZOS).convert("RGBA")
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    src = scaled.load()
    dst = out.load()
    span = MONO_HIGH - MONO_LOW
    for y in range(size):
        for x in range(size):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            ramp = (lum - MONO_LOW) / span
            ramp = 0.0 if ramp < 0 else (1.0 if ramp > 1 else ramp)
            # The source alpha gates the silhouette so the art's own corners stay transparent.
            dst[x, y] = (255, 255, 255, int(round(255 * ramp * (a / 255.0))))
    return out


def edge_colours(art: Image.Image) -> tuple[tuple[int, int, int], tuple[int, int, int]]:
    """The art's mean top-edge and bottom-edge colour, over the opaque middle of the edge.

    Sampling the corners would read the transparent outside; sampling the middle of the edge reads
    the colour the gradient actually is where a mask could cut.
    """
    w, h = art.size
    px = art.load()
    lo, hi = int(w * 0.22), int(w * 0.78)
    band = max(2, h // 64)

    def mean(y0: int, y1: int) -> tuple[int, int, int]:
        totals = [0, 0, 0]
        n = 0
        for y in range(y0, y1):
            for x in range(lo, hi):
                r, g, b, a = px[x, y]
                if a > 200:
                    totals[0] += r
                    totals[1] += g
                    totals[2] += b
                    n += 1
        if n == 0:
            return (0, 0, 0)
        return (totals[0] // n, totals[1] // n, totals[2] // n)

    return mean(band, band * 4), mean(h - band * 4, h - band)


def hex_of(rgb: tuple[int, int, int]) -> str:
    return "#%02X%02X%02X" % rgb


def background_xml(top: tuple[int, int, int], bottom: tuple[int, int, int]) -> str:
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!--
    Adaptive-icon background. Generated by scripts/gen_launcher_icons.py from the brand art's own top
    and bottom edge colour, so a mask that cuts squarer than the art's corners reveals the art's
    gradient rather than a second tile. Regenerate rather than hand-edit: the colours are sampled.
-->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="270"
        android:startColor="{hex_of(top)}"
        android:endColor="{hex_of(bottom)}" />
</shape>
'''


def covered(icon: Image.Image) -> tuple[bool, str]:
    """Whether the central 72dp safe zone of an adaptive layer is fully opaque.

    The safe zone is the region the platform guarantees is never masked, so it must carry the art.
    """
    size = icon.width
    inset = round(size * (ADAPTIVE_DP - 72) / 2 / ADAPTIVE_DP)
    alpha = icon.getchannel("A")
    box = alpha.crop((inset, inset, size - inset, size - inset))
    low = box.getextrema()[0]
    return low == 255, f"min alpha in the safe zone is {low}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=pathlib.Path, default=DEFAULT_SOURCE)
    parser.add_argument("--check", action="store_true",
                        help="verify the committed set instead of writing it")
    args = parser.parse_args()

    art = source_art(args.source)
    written: list[str] = []
    problems: list[str] = []

    for density, scale in DENSITIES:
        legacy_size = round(LEGACY_DP * scale)
        adaptive_size = round(ADAPTIVE_DP * scale)

        layers = {
            RES / f"mipmap-{density}/ic_launcher.png": legacy_icon(art, legacy_size),
            RES / f"drawable-{density}/ic_launcher_foreground.png":
                adaptive_foreground(art, adaptive_size),
            RES / f"drawable-{density}/ic_launcher_mono.png": adaptive_mono(art, adaptive_size),
        }
        for path, image in layers.items():
            if args.check:
                if not path.is_file():
                    problems.append(f"{path.relative_to(ROOT)} is missing")
                    continue
                have = Image.open(path)
                if have.size != image.size:
                    problems.append(
                        f"{path.relative_to(ROOT)} is {have.size}, expected {image.size}")
                    continue
                if have.convert("RGBA").tobytes() != image.tobytes():
                    problems.append(f"{path.relative_to(ROOT)} is stale; regenerate")
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                image.save(path, optimize=True)
                written.append(str(path.relative_to(ROOT)))

        ok, why = covered(adaptive_foreground(art, adaptive_size))
        if not ok:
            problems.append(f"{density}: adaptive foreground does not cover the safe zone ({why})")

    top, bottom = edge_colours(art)
    xml_path = RES / "drawable/ic_launcher_background.xml"
    xml = background_xml(top, bottom)
    if args.check:
        if not xml_path.is_file() or xml_path.read_text(encoding="utf-8") != xml:
            problems.append(f"{xml_path.relative_to(ROOT)} is stale; regenerate")
    else:
        xml_path.write_text(xml, encoding="utf-8")
        written.append(str(xml_path.relative_to(ROOT)))

    docs_icon = legacy_icon(art, DOCS_ICON_SIZE)
    if args.check:
        if not DOCS_ICON.is_file():
            problems.append(f"{DOCS_ICON.relative_to(ROOT)} is missing")
        elif Image.open(DOCS_ICON).convert("RGBA").tobytes() != docs_icon.tobytes():
            problems.append(f"{DOCS_ICON.relative_to(ROOT)} is stale; regenerate")
    else:
        DOCS_ICON.parent.mkdir(parents=True, exist_ok=True)
        docs_icon.save(DOCS_ICON, optimize=True)
        written.append(str(DOCS_ICON.relative_to(ROOT)))

    if problems:
        print("launcher icon set is not sound:")
        for problem in problems:
            print(f"  {problem}")
        return 1

    if args.check:
        print("launcher icon set is sound.")
    else:
        print(f"wrote {len(written)} files; background gradient "
              f"{hex_of(top)} -> {hex_of(bottom)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
