<p align="center">
  <img src="apps/assets/finalassetyellow.png" alt="CandyOS">
</p>

# CandyOS

## Quickstart

```
git clone https://github.com/EHowardHill/candyos ~/android/candyos
~/android/candyos/scripts/candyos-fetch-apps.sh
~/android/candyos/scripts/candyos-init.sh  ~/android/lineage
~/android/candyos/scripts/candyos-sync.sh ~/android/lineage
cd ~/android/lineage
source build/envsetup.sh
lunch lineage_sdk_phone_x86_64-ap2a-userdebug
m -j8
~/android/candyos/scripts/candyos-emulator.sh ~/android/lineage
```

(`envsetup.sh` finds the tree root from your current directory — source it
from inside the tree, never from elsewhere.)

Prerequisites (disk, RAM, packages, repo tool) and emulator instructions are in
[docs/getting-started.md](docs/getting-started.md).

## Install on a device

CandyOS installs on physical devices over **WebUSB**: flash from a
Chromium-family browser with
[Lineage Flash Tools](https://flash.lineageos.org) — no adb/fastboot on
the host. Build for any LineageOS 21-supported device
(`breakfast <device> && brunch <device>`) and follow
[docs/install-webusb.md](docs/install-webusb.md), which also records why
the WebUSB path is supported (the overlay never touches the USB, recovery,
or packaging chain).

## How it works

CandyOS modifies LineageOS in exactly three ways:

* **Patch series** (`patches/`) — changes to repos that upstream actively
  develops (frameworks/base, Settings, LatinIME, vendor/lineage,
  lineage-sdk, packages/modules/Permission, and the nine launcher-icon apps
  Contacts / Dialer / Messaging / Jelly / Glimpse / DocumentsUI /
  Aperture / AudioFX / Stk) are stored
  as ordered `git am` series and replayed on top of the stock upstream
  checkouts after every sync.
* **Vendored apps** (`apps/`) — self-contained apps and KitKat-era app ports
  (Launcher, Calculator, SoundRecorder, Music, DeskClock, Calendar, plus the
  **CandyOS Experience** app: first-run onboarding, the About and Testing
  menus that live on in the drawer, and the test bench ported from the
  sunset `vendor/testing` overlay) are kept
  whole in this repo and copied into the corresponding LineageOS app slots,
  which are removed from the repo manifest so stock versions never return.
* **Vendor overlays** (`vendor/`) — `vendor/partner_gms` (microG Services +
  microG Companion + Aurora Store — the **opt-in** Play compatibility
  stack — plus Organic Maps) is copied into the tree and included through
  the stock LineageOS `WITH_GMS` hook. The packages are preloaded disabled
  and turned on per user by the CandyOS Experience app (first-run wizard
  step + menu, P34), with signature spoofing scoped to a single
  whitelisted permission (framework patch
  `patches/frameworks-base/0016`). The blobs are pinned downloads fetched
  by `scripts/candyos-fetch-apps.sh`. (The `vendor/testing` overlay was
  sunset 2026-10-26 — its test bench lives on in the CandyOS Experience
  app's Testing menu.)

`manifest/candyos.xml` holds the manifest surgery, `scripts/` holds the
init/sync/save/verify/emulator workflow plus the arm64 Mac share bundle,
and `docs/` explains all of it.

## Documentation

* [docs/getting-started.md](docs/getting-started.md) — full bootstrap, build,
  emulator, and share-a-bundle walkthrough
* [docs/install-webusb.md](docs/install-webusb.md) — installing CandyOS on a
  supported phone from the browser (WebUSB): requirements, device build,
  flashing with Lineage Flash Tools
* [docs/architecture.md](docs/architecture.md) — mechanisms, slot/module map,
  patch policy
* [docs/updater.md](docs/updater.md) — the bundled-app GitHub release updater
  in CandyOS Experience: daily check, silent install, how to track a new
  bundled app
* [docs/contributing.md](docs/contributing.md) — how to make and submit changes
* [docs/phases/TASKS.md](docs/phases/TASKS.md) — the working tracker for open
  work (P0–P27 closed, their record lives in that file's git history and the
  forks; P28+ live on)
* [vendor/partner_gms/README.md](vendor/partner_gms/README.md) — the bundled
  MindTheGapps + Organic Maps setup, the arch conditionality, and how to
  bump the pinned versions

## Credits

CandyOS is maintained by:

- **Undefined** — Lead Designer
  ([GitHub](https://github.com/UndefinedYouTube) ·
  [YouTube](https://www.youtube.com/@Undefined.100))
- **Cinemint** — Project Engineer
  ([GitHub](https://github.com/EHowardHill/) ·
  [YouTube](https://www.youtube.com/@cinemint-tech))
- **Lex** — Launcher Engineer
  ([GitHub](https://github.com/LecSUSOff))

Based on [LineageOS](https://www.lineageos.org) and AOSP. KitKat-era app
sources are derived from `android-4.4.4_r1`. Licensed under the Apache License
2.0 — see [LICENSE](LICENSE).
