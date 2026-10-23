# CandyOS

**LineageOS 21 that looks and feels like Android 4.4 KitKat.**

CandyOS is a single-repo overlay on top of a stock LineageOS checkout: every
CandyOS change lives in this repository, and the LineageOS source itself is
never forked, vendored, or re-hosted. Everything needed to understand, build,
and extend the ROM is here.

Current status: boots and runs on the x86_64 emulator target
(`lineage_sdk_phone_x86_64-ap2a-userdebug`, Pixel 3a geometry). UI conversion
phases 1–21 are complete — see [docs/phases/TASKS.md](docs/phases/TASKS.md).

## Quickstart

```
git clone https://github.com/EHowardHill/candyos ~/android/candyos
~/android/candyos/scripts/candyos-init.sh  ~/android/lineage
~/android/candyos/scripts/candyos-sync.sh ~/android/lineage
source ~/android/lineage/build/envsetup.sh
lunch lineage_sdk_phone_x86_64-ap2a-userdebug
m -j$(nproc)
```

Prerequisites (disk, RAM, packages, repo tool) and emulator instructions are in
[docs/getting-started.md](docs/getting-started.md).

## How it works

CandyOS modifies LineageOS in exactly two ways:

* **Patch series** (`patches/`) — changes to repos that upstream actively
  develops (frameworks/base, Settings, LatinIME) are stored as ordered
  `git am` series and replayed on top of the stock upstream checkouts after
  every sync.
* **Vendored apps** (`apps/`) — self-contained KitKat-era app ports
  (Launcher, Calculator, SoundRecorder, Music, DeskClock, Calendar) are kept
  whole in this repo and copied into the corresponding LineageOS app slots,
  which are removed from the repo manifest so stock versions never return.

`manifest/candyos.xml` holds the manifest surgery, `scripts/` holds the
init/sync/save/verify workflow, and `docs/` explains all of it.

## Documentation

* [docs/getting-started.md](docs/getting-started.md) — full bootstrap, build,
  and emulator walkthrough
* [docs/architecture.md](docs/architecture.md) — mechanisms, slot/module map,
  patch policy
* [docs/contributing.md](docs/contributing.md) — how to make and submit changes
* [docs/phases/TASKS.md](docs/phases/TASKS.md) — the phase tracker (P1–P21 done,
  open phases live on)

## Credits

Based on [LineageOS](https://www.lineageos.org) and AOSP. KitKat-era app
sources are derived from `android-4.4.4_r1`. Licensed under the Apache License
2.0 — see [LICENSE](LICENSE).
