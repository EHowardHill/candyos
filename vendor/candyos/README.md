# vendor/candyos — the CandyOS vendor overlay

**Everything under `overlay/` here is GENERATED** by
`scripts/candyos-gen-wallpapers.py` from `assets/wallpapers/` — never edit it
directly; edit the wallpapers folder and re-run the script (a plain
`candyos-sync.sh` re-runs it too).

The overlay directory is registered as a build-time
`PRODUCT_PACKAGE_OVERLAYS` entry (appended after the LineageOS overlays in
`vendor/lineage/config/common.mk` by `patches/vendor-lineage/`, so it has the
higher priority) and ships:

* `frameworks/base/core/res/res/drawable{,-sw600dp,-sw720dp}-nodpi/default_wallpaper.*`
  — **the system default wallpaper**, from `assets/wallpapers/default.jpg`.
  It overrides the framework-res default (the KitKat 4.4 wallpaper the
  frameworks/base P8 patch installs as fallback) in all three density
  buckets, the same mechanism the LineageOS product overlay used before P28
  deleted its eight wallpaper entries from it.

Every image in `assets/wallpapers/` (the default included) is additionally
bundled into the CandyOS Experience app (`apps/CandyExperience/res/`,
generated the same way) and listed by its **Wallpapers** screen, which
applies the picked image as the system and lock-screen wallpaper.

To change the set: drop/remove/rename files in `assets/wallpapers/` (the
default is the file named exactly `default.jpg`), then
`scripts/candyos-gen-wallpapers.py [tree]` — or just `candyos-sync.sh`, which
regenerates and re-vendors it on every sync — and commit the result.