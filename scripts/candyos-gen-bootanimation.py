#!/usr/bin/env python3
"""CandyOS boot animation generator.

Builds the frame set that the patches/vendor-lineage series installs as
vendor/lineage/bootanimation/bootanimation.tar (Phase 26,
docs/phases/TASKS.md). Run this to regenerate the tar after changing
assets/CandyOSwordswhite.svg, then commit the refreshed patch series as
usual (scripts/candyos-save.sh).

The movie is the CandyOS wordmark from assets/CandyOSwordswhite.svg -
the spinning candy disc and the "CandyOS" text - rendered from the SVG
with the Roboto 2013 family the ROM pins (bundled in assets/fonts/,
Apache-2.0, from frameworks/base/data/fonts). The disc spins clockwise
in place; the text stays static. Layout, 60 fps, 6 degrees per frame
(one revolution per second):

  part0 - 30 frames, the whole wordmark fades in from black while the
          candy starts to spin; desc.txt: "c 1 0 part0" (plays once,
          always completes)
  part1 - 60 frames, one full disc revolution, full alpha; desc.txt:
          "f 0 0 part1 30" (loops until boot completes, then fades out
          over 30 frames while the spin continues)

The disc rotation is continuous across the part boundary (174 -> 180
degrees) and the part1 wrap (534 -> 180).

The frame canvas is the wordmark content box padded to a 6:1 aspect -
the padding becomes on-screen margin (the movie quad spans the full
screen width, so without it the wordmark touches the display edges;
with it the wordmark renders centered at ~75% of the width). Android.mk
must compute IMAGEHEIGHT/IMAGESCALEHEIGHT = width / 6, and its mogrify
line must not re-quantize ("-colors 256" removed): the frames ship
pre-quantized from this script, and per-frame palette re-quantization
makes the static text edges shimmer (each frame's palette is optimized
for that frame's spinning disc). Fully transparent pixels are zeroed
here so the build-time resize antialiases the glyphs against the black
part background uniformly.

The "OS" glyphs keep their SVG gradient (white -> #dedede); cairosvg
cannot resolve its inherited stops, so the gradient axis is parsed from
the SVG and applied to the glyphs directly.

Usage: candyos-gen-bootanimation.py <output.tar>

Requires python3 with Pillow and cairosvg (cairosvg only at generation
time - the shipped tar is the artifact). Renders through a private
fontconfig setup, so no system font install is needed. The tar is
byte-for-byte reproducible (sorted entries, fixed mtime/owner,
quantized palettes), so re-running it does not churn the committed
patch.
"""

import math
import os
import re
import sys
import tarfile
import tempfile
import xml.etree.ElementTree as ET

CANDYOS_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE_SVG = os.path.join(CANDYOS_ROOT, "assets", "CandyOSwordswhite.svg")
FONTS_DIR = os.path.join(CANDYOS_ROOT, "assets", "fonts")

SVG_UNITS = (297.0, 210.0)  # viewBox of CandyOSwordswhite.svg
RENDER_SCALE = 16  # px per SVG user unit

STEP_DEGREES = 6
FADE_IN_FRAMES = 30
LOOP_FRAMES = 60
# Movie aspect, kept in sync with Android.mk (IMAGEHEIGHT = width * 1/6).
# The wordmark content gets the rest as transparent horizontal margin, so
# it renders centered at ~75% of the screen width instead of touching the
# display edges. The pair must divide common screen widths exactly
# (720/1080/1440/2160 / 6 are whole pixels) and the canvas is built to
# the exact aspect, otherwise the desc quad stretches the frames.
CANVAS_ASPECT = (6, 1)
QUANTIZE_COLORS = 256
OS_GRADIENT_END = (0xDE, 0xDE, 0xDE)  # linearGradient8's second stop

TAR_FIXED_MTIME = 1790073600  # 2026-10-21 00:00:00 UTC, pinned for reproducibility


def setup_fontconfig():
    """Point fontconfig at the bundled Roboto via a private config."""
    cache = tempfile.mkdtemp(prefix="candyos-fontconfig-")
    conf = os.path.join(cache, "fonts.conf")
    with open(conf, "w") as f:
        f.write(
            '<?xml version="1.0"?>\n'
            "<!DOCTYPE fontconfig SYSTEM \"fonts.dtd\">\n"
            "<fontconfig>\n"
            f"  <dir>{FONTS_DIR}</dir>\n"
            f"  <cachedir>{cache}</cachedir>\n"
            "</fontconfig>\n"
        )
    os.environ["FONTCONFIG_FILE"] = conf


def svg_variant(drop_ids, os_solid=False):
    """Serialize the source SVG minus the given element ids.

    display:none groups are dropped explicitly (g1) rather than relied
    on. With os_solid, the OS gradient fill becomes flat white so the
    gradient can be applied by hand afterwards.
    """
    tree = ET.parse(SOURCE_SVG)
    root = tree.getroot()

    def rm(e):
        for c in list(e):
            if c.get("id") in drop_ids:
                e.remove(c)
            else:
                rm(c)

    rm(root)
    out = ET.tostring(root, encoding="unicode")
    if os_solid:
        out = out.replace("fill:url(#linearGradient4)", "fill:#ffffff")
    return out.encode()


def render_layers():
    """Render candy / "Candy" / "OS" layers at RENDER_SCALE.

    Returns (candy, candy_word, os_word) RGBA images, all full-canvas.
    """
    import cairosvg  # after FONTCONFIG_FILE is set

    w, h = round(SVG_UNITS[0] * RENDER_SCALE), round(SVG_UNITS[1] * RENDER_SCALE)

    def png(xml):
        buf = tempfile.NamedTemporaryFile(prefix="candyos-layer-", suffix=".svg")
        buf.write(xml)
        buf.flush()
        out = tempfile.NamedTemporaryFile(prefix="candyos-layer-", suffix=".png")
        cairosvg.svg2png(url=buf.name, write_to=out.name, output_width=w, output_height=h)
        from PIL import Image
        return Image.open(out.name).convert("RGBA")

    candy = png(svg_variant({"text2", "text4"}))
    candy_word = png(svg_variant({"g3", "g1"}, os_solid=True))
    os_word = png(svg_variant({"text2", "g3", "g1"}, os_solid=True))
    return candy, candy_word, os_word


def parse_os_gradient_axis():
    """user-space y range of the OS gradient, from linearGradient4."""
    svg = open(SOURCE_SVG).read()
    m = re.search(r'<linearGradient\b[^>]*\bid="linearGradient4"[^>]*>', svg)
    attrs = dict(re.findall(r'(x1|y1|x2|y2)="([-\d.]+)"', m.group(0)))
    x1, y1, x2, y2 = (float(attrs[k]) for k in ("x1", "y1", "x2", "y2"))
    tm = re.search(r'gradientTransform="matrix\(([-\d.e]+),([-\d.e]+),([-\d.e]+),([-\d.e]+),([-\d.e]+),([-\d.e]+)\)"', m.group(0))
    if tm:
        a, b, c, d, e, f = map(float, tm.groups())
        x1, y1 = a * x1 + c * y1 + e, b * x1 + d * y1 + f
        x2, y2 = a * x2 + c * y2 + e, b * x2 + d * y2 + f
    return y1, y2


def apply_os_gradient(os_layer, y1, y2):
    """Vertical white -> #dedede fill for the OS glyphs (SVG pad mode)."""
    from PIL import Image

    top, bottom = y1 * RENDER_SCALE, y2 * RENDER_SCALE
    col = Image.new("RGBA", (1, os_layer.height))
    for y in range(os_layer.height):
        t = min(max((y - top) / (bottom - top), 0.0), 1.0)
        g = round(255 + (OS_GRADIENT_END[0] - 255) * t)
        col.putpixel((0, y), (g, g, g, 255))
    gradient = col.resize(os_layer.size)
    return Image.composite(gradient, os_layer, os_layer.getchannel("A"))


def main():
    if len(sys.argv) != 2:
        sys.exit(f"usage: {sys.argv[0]} <output.tar>")
    out = sys.argv[1]
    if os.path.exists(out):
        sys.exit(f"refusing to overwrite existing {out}")

    from PIL import Image

    setup_fontconfig()
    candy, candy_word, os_word = render_layers()

    # apply the OS gradient, then find the full-wordmark content box
    os_word = apply_os_gradient(os_word, *parse_os_gradient_axis())
    from PIL import ImageChops
    content = Image.new("L", candy.size, 0)
    for layer in (candy, candy_word, os_word):
        m = layer.getchannel("A").point(lambda v: 255 if v > 8 else 0)
        content = ImageChops.lighter(content, m)
    bbox = content.getbbox()
    cw, ch = bbox[2] - bbox[0], bbox[3] - bbox[1]

    # canvas: content box padded to the CANVAS_ASPECT movie
    if cw * CANVAS_ASPECT[1] > ch * CANVAS_ASPECT[0]:
        canvas_w, canvas_h = cw, round(cw * CANVAS_ASPECT[1] / CANVAS_ASPECT[0])
        pad_left, pad_top = 0, (canvas_h - ch) // 2
    else:
        canvas_w, canvas_h = round(ch * CANVAS_ASPECT[0] / CANVAS_ASPECT[1]), ch
        pad_left, pad_top = (canvas_w - cw) // 2, 0
    assert canvas_w >= cw and canvas_h >= ch, "canvas smaller than content"
    assert canvas_w * CANVAS_ASPECT[1] == canvas_h * CANVAS_ASPECT[0], \
        "canvas aspect is not exactly CANVAS_ASPECT - the desc quad would distort it"
    print(f"candyos: content {cw}x{ch} -> canvas {canvas_w}x{canvas_h} "
          f"({CANVAS_ASPECT[0]}:{CANVAS_ASPECT[1]})")

    # static base: canvas + "Candy" + gradient "OS"
    base = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    for layer in (candy_word, os_word):
        base.alpha_composite(layer.crop(bbox), (pad_left, pad_top))

    # spinning disc: the candy content is a circle filling its bbox
    cpts = [(x, y) for y in range(candy.height) for x in range(candy.width)
            if candy.load()[x, y][3] > 8]
    cx0, cx1 = min(p[0] for p in cpts), max(p[0] for p in cpts)
    cy0, cy1 = min(p[1] for p in cpts), max(p[1] for p in cpts)
    side = max(cx1 - cx0, cy1 - cy0) + 1
    assert abs((cx1 - cx0) - (cy1 - cy0)) <= 2, "candy is not a disc"
    rmax = max(math.hypot(x - (cx0 + cx1) / 2, y - (cy0 + cy1) / 2) for x, y in cpts)
    assert rmax <= side / 2 + 1, "candy content escapes its disc"
    disc = candy.crop((cx0, cy0, cx0 + side, cy0 + side))
    disc_pos = (pad_left + cx0 - bbox[0], pad_top + cy0 - bbox[1])

    def frame(angle_deg, alpha):
        f = base.copy()
        spun = disc.rotate(-angle_deg, resample=Image.BICUBIC,
                           center=((side - 1) / 2.0, (side - 1) / 2.0))
        f.alpha_composite(spun, disc_pos)
        if alpha < 1.0:
            f.putalpha(f.getchannel("A").point(lambda v: int(v * alpha)))
        # Zero the RGB of fully transparent pixels: palette quantization
        # leaves per-frame noise there, and the build-time mogrify -resize
        # smears it into the antialiased glyph edges (visible as shimmer).
        # Uniform black also antialiases the text correctly against the
        # black part background.
        visible = f.getchannel("A").point(lambda v: 255 if v > 0 else 0)
        f = Image.composite(f, Image.new("RGBA", f.size, (0, 0, 0, 0)), visible)
        return f.quantize(colors=QUANTIZE_COLORS, method=Image.FASTOCTREE)

    parts = {
        "part0": (
            [i * STEP_DEGREES for i in range(FADE_IN_FRAMES)],
            [(i + 1) / FADE_IN_FRAMES for i in range(FADE_IN_FRAMES)],
        ),
        "part1": (
            [180 + i * STEP_DEGREES for i in range(LOOP_FRAMES)],
            [1.0] * LOOP_FRAMES,
        ),
    }

    with tarfile.open(out, "w", format=tarfile.USTAR_FORMAT) as tar:
        names = []
        for part in sorted(parts):
            for png, (angle, alpha) in enumerate(zip(*parts[part])):
                name = f"{part}/{png:03d}.png"
                arc = os.path.join(tempfile.gettempdir(), "candyos-frames", name)
                os.makedirs(os.path.dirname(arc), exist_ok=True)
                frame(angle, alpha).save(arc, "PNG", optimize=True)
                info = tar.gettarinfo(arc, arcname=name)
                info.mtime = TAR_FIXED_MTIME
                info.uid = info.gid = 0
                info.uname = info.gname = ""
                with open(arc, "rb") as f:
                    tar.addfile(info, f)
                os.remove(arc)
                names.append(name)

    size = os.path.getsize(out)
    print(f"candyos: wrote {out} ({len(names)} frames, {size} bytes)")
    print("candyos: vendor/lineage/bootanimation/desc.txt must read:")
    print("candyos:   c 1 0 part0")
    print("candyos:   f 0 0 part1 30")


if __name__ == "__main__":
    main()
