# Getting started with CandyOS

## Prerequisites

* A Linux machine (Ubuntu 20.04+ recommended) with **~200 GB free disk** and
  **16 GB RAM minimum**.
* The LineageOS build packages — see
  <https://wiki.lineageos.org/dev/dependencies/> for your distro.
* The [repo tool](https://source.android.com/setup/download#repo):
  ```
  mkdir -p ~/.local/bin
  curl https://storage.googleapis.com/git-repo-downloads/repo > ~/.local/bin/repo
  chmod a+x ~/.local/bin/repo
  ```
  and make sure `~/.local/bin` is on your `PATH`.
* git configured (`user.name` / `user.email`).
* **git-lfs**, installed AND activated (`sudo apt install git-lfs` then
  `git lfs install`) **before** the first sync — LineageOS stores the
  WebView prebuilt APKs as LFS objects. Without it the sync checks out
  134-byte pointer stubs and the build dies late with
  "webview.apk: not a valid zip file" (see Troubleshooting).

## Bootstrap the tree

CandyOS is an overlay. You clone it next to the LineageOS tree, and its
scripts manage the tree for you:

```
git clone https://github.com/EHowardHill/candyos ~/android/candyos
~/android/candyos/scripts/candyos-fetch-apps.sh
~/android/candyos/scripts/candyos-init.sh  ~/android/lineage
~/android/candyos/scripts/candyos-sync.sh ~/android/lineage
```

* `candyos-fetch-apps.sh` downloads the pinned microG and Aurora Store
  release APKs into `vendor/partner_gms/` (they are not committed to the
  repo). Run it once after cloning, and again whenever the pins change.
* `candyos-init.sh` runs `repo init` against the LineageOS manifest (branch
  `lineage-21.0`) and installs `manifest/candyos.xml` as a local manifest.
  If the tree was already initialized, it only refreshes the manifest.
* `candyos-sync.sh` runs `repo sync`, then replays the CandyOS patch series
  onto frameworks/base, Settings, LatinIME, and vendor/lineage, then fills
  the six vendored app slots from `apps/` and copies the `vendor/`
  overlays (bundled microG + Aurora Store) into the tree. **Always sync
  with this script** — a bare `repo sync` would rebase away the patches
  and leave stale vendored apps behind.

The first sync downloads the whole LineageOS tree and takes a while (a
fastmirror/close server helps). Subsequent syncs are incremental.

## Build

`envsetup.sh` finds the tree root from your current directory, so always
`cd` into the tree before sourcing it:

```
cd ~/android/lineage
source build/envsetup.sh
lunch lineage_sdk_phone_x86_64-ap2a-userdebug
m -j8
```

The build output lands in `out/target/product/emu64x/`. Expect a few hours on
a typical dev machine; incremental rebuilds are much faster.

## Run it on the emulator

The bundled launcher, `scripts/candyos-emulator.sh`, does the whole loop —
clean boot, launch, and wait:

```
~/android/candyos/scripts/candyos-emulator.sh ~/android/lineage
```

(The `<lineage-tree>` argument can be dropped when `$CANDYOS_ANDROID_TREE`
is exported — all candyos-* scripts accept that default.)

What it does:

* Wipes the AVD `userdata`/`cache`/FBE-key images in
  `out/target/product/emu64x/` first, so every launch is a clean first
  boot — lock-screen PINs and other `/data` state from a previous run
  never survive a relaunch (snapshots are disabled too).
* Launches the emulator with the lunch environment wired in, on the
  canonical QA device geometry (Pixel 3a-sized: 1080x2220 skin @ 440dpi,
  SwiftShader rendering, window on `DISPLAY=:0`).
* Waits until Android reports `sys.boot_completed` — up to 5 minutes for
  the device to register with adb, then 20 minutes for the boot itself.
  First boot takes several minutes on SwiftShader; the window freezes
  while Android boots, which is normal.

To stop the emulator (no `<lineage-tree>` needed):

```
~/android/candyos/scripts/candyos-emulator.sh --kill
```

Details and fallbacks:

* The launch log is `/tmp/opencode/emulator.log`.
* The emulator binary runs from a scratch copy in
  `/tmp/opencode/android-emulator` because the tree's
  `prebuilts/android-emulator` checkout is intermittently unavailable on
  this machine; the script refreshes the copy from the tree when it is
  missing.
* Launching by hand works too — the emulator ships with the tree:

  ```
  cd ~/android/lineage
  source build/envsetup.sh
  lunch lineage_sdk_phone_x86_64-ap2a-userdebug
  emulator -gpu swiftshader_indirect -no-snapshot -no-audio \
           -skin 1080x2220 -dpi-device 440
  ```

Watch progress with:

```
adb shell getprop sys.boot_completed   # 1 when booted
```

Notes from QA (see docs/phases/TASKS.md): module-only rebuilds do not always
reach a booted emulator image — re-run a full `m -j8` before relaunching
to regenerate `system-qemu.img`, and wipe `userdata-qemu.img*` for a clean
profile (the launcher script does the wipe for you).

## Keeping the tree current

```
~/android/candyos/scripts/candyos-sync.sh ~/android/lineage
```

That's the whole update story: it re-syncs, re-applies the patch series,
re-vendores the apps and overlays, and warns if the bundled microG/Aurora
APKs are missing (fix: re-run `candyos-fetch-apps.sh`). If LineageOS
upstream changed code the patches touch, `git am -3` usually resolves it;
when it can't, the script stops with exact recovery instructions (resolve
by hand, then run `candyos-save.sh` to refresh the stored series and
push it).

To check that the series still applies without a full sync round-trip:

```
~/android/candyos/scripts/candyos-verify.sh ~/android/lineage
```

## Troubleshooting

* **`candyos-sync.sh` refuses to run — "uncommitted changes in ..."** — you
  have local work in a patched project. Commit it and run
  `candyos-save.sh`, or stash it.
* **Patch series fails to apply** — upstream moved. The script prints the
  exact `git am` + `candyos-save.sh` recovery sequence; see
  docs/contributing.md for the full flow.
* **Vendored app edits vanish after syncing** — by design: the tree slots are
  generated from `apps/`. Edit the copy in the CandyOS repo instead (see
  docs/contributing.md).
* **`candyos: WARNING: vendor/partner_gms/... is missing!`** — the bundled
  microG/Aurora APKs have not been fetched. Run
  `~/android/candyos/scripts/candyos-fetch-apps.sh` and re-sync.
* **`webview.apk: zip2zip: not a valid zip file`** (late in a full build) —
  the WebView prebuilt is a 134-byte git-lfs pointer instead of the real
  ~342 MB APK: the blob was never fetched. Fix it with:
  ```
  git -C ~/android/lineage/external/chromium-webview/prebuilt/x86_64 lfs pull
  ```
  (the other `prebuilt/<arch>/` dirs hold pointers too — pull them the
  same way if you ever build for those architectures). Once pulled, the
  blob is cached in the local LFS store and survives future syncs.
