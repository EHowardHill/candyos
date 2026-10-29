#!/usr/bin/env python3
"""CandyOS icons: generate every built-in app's launcher icon in the KitKat
style from the reference set (P33).

reference/icons/ (the committed reference PNGs) is the source of truth for
the stock LineageOS apps that have a KitKat counterpart; the authentic 4.4
donor density files under reference/donor-icons/ are copied byte-for-byte
for Settings and the keyboard (rule 1: authentic, not a theme); the
CandyOS-original Notes/Reminders art lives under reference/originals/ and
is re-tiled onto KitKat geometry (rounded corners + darker border + soft
shadow, matching the reference tile family); AudioFX and SIM Toolkit have
no KitKat counterpart and get period-style glyphs drawn here.

Regenerates (never hand-edit):

  apps/{Notes,Reminders}/res/mipmap-<dpi>/ic_launcher.png
      The two vendored CandyOS-originals, re-tiled KitKat-style.

With a LineageOS tree argument the in-tree copies are ALSO refreshed:

  packages/apps/Contacts        reference People.png
  packages/apps/Dialer          reference Phone.png
  packages/apps/Messaging       reference Messaging.png
  packages/apps/Jelly           reference Browser.png
  packages/apps/Glimpse         reference Gallery.png
  packages/apps/DocumentsUI     reference Downloads.png
  packages/apps/Aperture        reference Camera.png
  packages/apps/AudioFX         synthesized EQ tile
  packages/apps/Stk             synthesized SIM tile
  packages/apps/Settings        donor settings-4.4 ic_launcher_settings
  packages/inputmethods/LatinIME donor latinime-4.4 ic_launcher_keyboard

  plus the adaptive-icon XML + background/foreground/monochrome drawables
  are removed (legacy full-bleed PNGs are the 4.4 behavior) and the two
  vendored app slots get the re-tiled PNGs. Those tree projects are patched
  projects - commit the result per docs/contributing.md Flow B and re-run
  candyos-save.sh; the vendored slots need no commit (sync re-copies them).

Usage: candyos-gen-icons.py [<lineage-tree>]
"""

import os
import shutil
import sys

from PIL import Image, ImageDraw, ImageFilter

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(SCRIPT_DIR)
REF = os.path.join(REPO, "reference", "icons")
DONOR = os.path.join(REPO, "reference", "donor-icons")
ORIGINALS = os.path.join(REPO, "reference", "originals")

MASTER = 512          # synthesized / re-tiled master canvas size
MARGIN = 13           # tile inset on the master (matches the reference set)
RADIUS = 64           # corner radius (12.5%, the Settings/tile family)
SHADOW_BLUR = 9
SHADOW_ALPHA = 90     # ~35% black under the tile edge
SHADOW_DY = 3
BORDER = 4            # darker ring width on the master

# density bucket -> px
DENSITY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

# series -> (<project path in the tree>, <res root under the project>,
#            <resource dir>, <file name>, <master source>)
#   master source: ref:<Reference.png> | donor:<settings|latinime> |
#                  synth:<audiofx|stk>
TREE_APPS = [
    ("contacts",    "packages/apps/Contacts",        "res",                  "mipmap",   "ic_launcher.png",        "ref:People.png"),
    ("dialer",      "packages/apps/Dialer",           "java/com/android/dialer/app/res", "mipmap", "ic_launcher.png",  "ref:Phone.png"),
    ("messaging",   "packages/apps/Messaging",        "res",                  "mipmap",   "ic_launcher.png",        "ref:Messaging.png"),
    ("jelly",       "packages/apps/Jelly",            "app/src/main/res",     "mipmap",   "ic_launcher.png",        "ref:Browser.png"),
    ("glimpse",     "packages/apps/Glimpse",           "app/src/main/res",     "mipmap",   "ic_launcher.png",        "ref:Gallery.png"),
    ("documentsui", "packages/apps/DocumentsUI",      "res",                  "mipmap",   "ic_launcher.png",        "ref:Downloads.png"),
    ("aperture",    "packages/apps/Aperture",         "app/src/main/res",     "mipmap",   "ic_launcher.png",        "ref:Camera.png"),
    ("audiofx",     "packages/apps/AudioFX",           "res",                  "mipmap",   "ic_launcher.png",        "synth:audiofx"),
    ("stk",         "packages/apps/Stk",               "res",                  "mipmap",   "ic_launcher.png",        "synth:stk"),
    ("settings",    "packages/apps/Settings",          "res",                  "drawable", "ic_launcher_settings.png", "donor:settings"),
    ("latinime",    "packages/inputmethods/LatinIME",  "java/res",            "mipmap",   "ic_launcher.png",        "donor:latinime"),
]

# adaptive-icon machinery each swap removes (relative to the res root).
TREE_REMOVE = {
    "contacts":    ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "dialer":      ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "messaging":   ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "jelly":       ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "glimpse":     ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "documentsui": ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "aperture":    ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "audiofx":     ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "stk":         ["mipmap-anydpi/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "settings":    ["drawable/ic_launcher_settings.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml"],
    "latinime":    ["mipmap-anydpi-v26/ic_launcher.xml", "drawable/ic_launcher_background.xml",
                    "drawable/ic_launcher_foreground.xml"],
}

# CandyOS-originals re-tiled from reference/originals (never CandyExperience:
# its mark is already period-styled by design).
RETILED = [
    ("Notes", os.path.join(REPO, "apps", "Notes", "res")),
    ("Reminders", os.path.join(REPO, "apps", "Reminders", "res")),
]
TREE_RETILED_RES = {
    "Notes": "packages/apps/Notes/res",
    "Reminders": "packages/apps/Reminders/res",
}


def die(msg):
    print(f"candyos: ERROR: {msg}", file=sys.stderr)
    sys.exit(1)


def rounded_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    return m


def compose_tile(art, shadow=True):
    """Place full-bleed tile art on a MASTER canvas, KitKat-style: inset a
    little, round the corners, ring a darker border, drop a soft shadow."""
    tile = MASTER - 2 * MARGIN
    art = art.resize((tile, tile), Image.LANCZOS)
    mask = rounded_mask(tile, RADIUS)

    # darker border ring, sampled from the art's own edge color
    ring = Image.new("RGBA", (tile, tile), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring)
    rd.rounded_rectangle([0, 0, tile - 1, tile - 1], radius=RADIUS,
                         outline=(0, 0, 0, 255), width=BORDER)
    ring = Image.composite(ring, Image.new("RGBA", (tile, tile), (0, 0, 0, 0)), mask)
    border = Image.new("RGBA", (tile, tile), (0, 0, 0, 70))
    art = Image.alpha_composite(art, Image.composite(border, Image.new("RGBA", (tile, tile), (0, 0, 0, 0)), ring))

    canvas = Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0))
    if shadow:
        sh = Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0))
        sd = ImageDraw.Draw(sh)
        sd.rounded_rectangle(
            [MARGIN, MARGIN + SHADOW_DY, MARGIN + tile - 1, MARGIN + tile - 1 + SHADOW_DY],
            radius=RADIUS, fill=(0, 0, 0, SHADOW_ALPHA))
        sh = sh.filter(ImageFilter.GaussianBlur(SHADOW_BLUR))
        canvas = Image.alpha_composite(canvas, sh)
    canvas.paste(art, (MARGIN, MARGIN), mask)
    return canvas


def master_retile(path):
    return compose_tile(Image.open(path).convert("RGBA"))


def vertical_gradient(size, top, bottom):
    g = Image.new("RGBA", (1, size))
    px = g.load()
    for y in range(size):
        t = y / (size - 1)
        px[0, y] = (int(top[0] + (bottom[0] - top[0]) * t),
                    int(top[1] + (bottom[1] - top[1]) * t),
                    int(top[2] + (bottom[2] - top[2]) * t), 255)
    return g.resize((size, size), Image.BILINEAR)


def synth_audiofx():
    """Dark Holo-blue tile with three EQ faders (the CM11 DSP-Manager slot,
    KitKat-family flat)."""
    tile = MASTER - 2 * MARGIN
    art = vertical_gradient(tile, (66, 82, 95), (38, 49, 58))
    glyph = Image.new("RGBA", (tile, tile), (0, 0, 0, 0))
    d = ImageDraw.Draw(glyph)
    track_w, knob_r = tile // 9, tile // 11
    xs = [tile * 7 // 20 - track_w // 2, tile // 2 - track_w // 2, tile * 13 // 20 - track_w // 2]
    ys = [int(tile * f) for f in (0.34, 0.62, 0.45)]
    for x in xs:
        d.rounded_rectangle([x, tile * 18 // 100, x + track_w, tile * 82 // 100],
                            radius=track_w // 2, fill=(255, 255, 255, 36))
    for x, y in zip(xs, ys):
        d.ellipse([x + track_w // 2 - knob_r, y - knob_r,
                   x + track_w // 2 + knob_r, y + knob_r], fill=(240, 240, 240, 255))
    return compose_tile(Image.alpha_composite(art, glyph))


def synth_stk():
    """Light gray tile with a SIM card (the 4.4 Stk glyph)."""
    tile = MASTER - 2 * MARGIN
    art = vertical_gradient(tile, (224, 224, 224), (176, 176, 176))
    d = ImageDraw.Draw(art)
    w, h = tile * 52 // 100, tile * 62 // 100
    x0, y0 = (tile - w) // 2, (tile - h) // 2
    cut = w * 22 // 100
    # card with the classic clipped top-left corner
    pts = [(x0 + cut, y0), (x0 + w, y0), (x0 + w, y0 + h), (x0, y0 + h), (x0, y0 + cut)]
    d.polygon(pts, fill=(250, 250, 250, 255), outline=(120, 120, 120, 255))
    # gold contact pads inside the card, along the clipped edge
    pad = w * 13 // 100
    for r in range(2):
        for c in range(3):
            px0 = x0 + cut + c * pad * 105 // 100
            py0 = y0 + cut // 2 + r * pad * 105 // 100
            d.rectangle([px0, py0, px0 + pad * 72 // 100, py0 + pad * 72 // 100],
                        fill=(201, 160, 60, 255))
    return compose_tile(art)


def master_for(source):
    kind, arg = source.split(":", 1)
    if kind == "ref":
        path = os.path.join(REF, arg)
        if not os.path.isfile(path):
            die(f"reference/icons/{arg} is missing")
        return ("copy512", path)
    if kind == "donor":
        return ("donor", arg)
    if kind == "synth":
        return ("synth", arg)
    die(f"unknown master source '{source}'")


def push_donor(name, out_base, res_dir, res_name):
    src = os.path.join(DONOR, name)
    for dpi in sorted(os.listdir(src)):
        if not dpi.startswith("mipmap-"):
            continue
        files = os.listdir(os.path.join(src, dpi))
        if len(files) != 1:
            die(f"reference/donor-icons/{name}/{dpi}: expected exactly one file")
        os.makedirs(os.path.join(out_base, f"{res_dir}-{dpi.split('-')[1]}"), exist_ok=True)
        shutil.copyfile(os.path.join(src, dpi, files[0]),
                        os.path.join(out_base, f"{res_dir}-{dpi.split('-')[1]}", res_name))


def main():
    tree = None
    if len(sys.argv) > 1:
        tree = sys.argv[1]
        if not os.path.isdir(os.path.join(tree, ".repo")):
            die(f"'{tree}' is not a repo-initialized LineageOS tree")

    # 1. CandyOS-originals, re-tiled (repo side).
    for name, res in RETILED:
        master = master_retile(os.path.join(ORIGINALS, f"{name}.png"))
        for dpi, size in DENSITY.items():
            d = os.path.join(res, f"mipmap-{dpi}")
            os.makedirs(d, exist_ok=True)
            master.resize((size, size), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"))
        print(f"candyos: apps/{name}: re-tiled KitKat icon ({len(DENSITY)} densities)")

    if not tree:
        print("candyos: icons regenerated (pass a lineage tree to refresh "
              "the in-tree apps too; then commit + candyos-save.sh)")
        return

    # 2. Stock in-tree apps (patched projects - the caller commits).
    synth = {"audiofx": synth_audiofx, "stk": synth_stk}
    for series, project, res_root, res_dir, res_name, source in TREE_APPS:
        base = os.path.join(tree, project, res_root)
        if not os.path.isdir(base):
            die(f"{base} does not exist - is this a synced CandyOS tree?")
        kind, arg = master_for(source)
        if kind == "copy512":
            img = Image.open(arg).convert("RGBA")
            for dpi, size in DENSITY.items():
                d = os.path.join(base, f"{res_dir}-{dpi}")
                os.makedirs(d, exist_ok=True)
                img.resize((size, size), Image.LANCZOS).save(os.path.join(d, res_name))
        elif kind == "donor":
            push_donor(arg, base, res_dir, res_name)
        else:
            img = synth[arg]()
            for dpi, size in DENSITY.items():
                d = os.path.join(base, f"{res_dir}-{dpi}")
                os.makedirs(d, exist_ok=True)
                img.resize((size, size), Image.LANCZOS).save(os.path.join(d, res_name))
        removed = []
        for rel in TREE_REMOVE[series]:
            p = os.path.join(base, rel)
            if os.path.exists(p):
                os.remove(p)
                removed.append(rel)
        print(f"candyos: {project}: KitKat icon written ({res_dir}-<dpi>/{res_name}"
              f"{', adaptive set removed' if removed else ''})")

    # 3. Vendored slots: same re-tiled PNGs the repo carries.
    for name, res in RETILED:
        t_res = os.path.join(tree, TREE_RETILED_RES[name])
        for dpi in DENSITY:
            src = os.path.join(os.path.join(REPO, 'apps', name, 'res'), f"mipmap-{dpi}", "ic_launcher.png")
            d = os.path.join(t_res, f"mipmap-{dpi}")
            os.makedirs(d, exist_ok=True)
            shutil.copyfile(src, os.path.join(d, "ic_launcher.png"))
        print(f"candyos: {TREE_RETILED_RES[name]}: re-tiled icon pushed")

    print("candyos: tree updated - commit each listed project "
          "(docs/contributing.md Flow B), then candyos-save.sh")


if __name__ == "__main__":
    main()
