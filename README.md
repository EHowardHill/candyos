# CandyOS

## Quickstart

```
git clone https://github.com/EHowardHill/candyos ~/android/candyos
~/android/candyos/scripts/candyos-init.sh  ~/android/lineage
~/android/candyos/scripts/candyos-sync.sh ~/android/lineage
cd ~/android/lineage
source build/envsetup.sh
lunch lineage_sdk_phone_x86_64-ap2a-userdebug
m -j$(nproc)
```

(`envsetup.sh` finds the tree root from your current directory — source it
from inside the tree, never from elsewhere.)

Prerequisites (disk, RAM, packages, repo tool) and emulator instructions are in
[docs/getting-started.md](docs/getting-started.md).

## How it works

CandyOS modifies LineageOS in exactly two ways:

* **Patch series** (`patches/`) — changes to repos that upstream actively
  develops (frameworks/base, Settings, LatinIME) are stored as ordered
  `git am` series and replayed on top of the stock upstream checkouts after
  every sync.
* **Vendored apps** (`apps/`) — self-contained apps and KitKat-era app ports
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
