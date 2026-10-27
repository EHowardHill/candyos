# CandyOS architecture

## The three mechanisms

**1. Patch series** — for repos upstream actively develops (frameworks/base,
Settings, LatinIME, vendor/lineage). The repos stay as *stock* LineageOS
checkouts in the manifest; after every sync, `candyos-sync.sh` resets each
one to the plain upstream revision and replays an ordered `git format-patch`
series with `git am -3`. Patches are plain files in this repo, so they
review, diff, and merge like any other change.

**2. Vendored apps** — for self-contained KitKat-era app ports that share no
history with the app they replace. These live whole under `apps/` and are
copied into their slots after every sync. The stock project is removed from
the manifest (`manifest/candyos.xml`) so `repo sync` never resurrects it.

**3. Vendor overlays** — for content that is added to the tree without
replacing any stock project. Directories under `vendor/` are copied
verbatim into the tree after every sync (same reset-and-copy discipline as
the vendored apps, but into otherwise-unoccupied paths). Currently:
`vendor/partner_gms` (microG Services, microG Companion/FakeStore, Aurora
Store plus their privapp-permissions/sysconfig XMLs), which plugs into the
*stock* LineageOS `WITH_GMS` hook in `vendor/lineage/config/common.mk`
(the flag itself is set by the `patches/vendor-lineage/` series). The
partner_gms APKs are pinned downloads managed by `scripts/candyos-fetch-apps.sh`,
not committed blobs; see `vendor/partner_gms/README.md` for the details,
including why no signature-spoofing framework patch is needed on
LineageOS 21. (The former `vendor/testing` overlay was sunset 2026-10-26 —
its test bench moved into the CandyOS Experience app's Testing menu; see
`docs/phases/TASKS.md`, Phases 23 and 25.)

## Change map

| CandyOS piece | LineageOS location | Mechanism | Notes |
|---|---|---|---|
| `patches/frameworks-base/` | `frameworks/base` | 29-patch series | Holo theme, 2013 Roboto + hybrid emoji fonts, SystemUI (status bar, navbar, volume, power menu, recents, keyguard), 4.4 UI sounds, monet pinning. The notification shade and quick settings stay **stock** (rolled back in P21). |
| `patches/settings/` | `packages/apps/Settings` | 2-patch series | Holo reskin: layouts, icons, styling. |
| `patches/latinime/` | `packages/inputmethods/LatinIME` | 3-patch series | Forces the KitKat Holo keyboard theme, key-text-size attrs, flat key colors, donor label geometry. |
| `patches/vendor-lineage/` | `vendor/lineage` | 1-patch series | P22: sets `WITH_GMS := true` so `vendor/partner_gms` is inherited. Everything else in vendor/lineage stays stock. (P23, which bundled the Testing app, was sunset with the app.) |
| `patches/lineage-sdk/` | `lineage-sdk` | 1-patch series | Brands the `lineageos.platform` strings (system label, version, updates, API level) as CandyOS. |
| `apps/CandyLauncher/` | `packages/apps/Trebuchet` | vendored | Prebuilt CandyLauncher APK (Flutter launcher, `com.example.thelauncher`); imported as `TrebuchetQuickStep`, the module name `vendor/lineage` expects. Non-privileged install: no privapp whitelist ships for it. |
| `apps/Calculator/` | `packages/apps/ExactCalculator` | vendored | 4.4.4_r1 Calculator with modernized soong build, module name kept. |
| `apps/SoundRecorder/` | `packages/apps/Recorder` | vendored | 4.4.4 SoundRecorder port, scoped storage, module name kept. |
| `apps/Music/` | `packages/apps/Eleven` | vendored | 4.4.4 Music (`com.android.music`), module name kept. |
| `apps/DeskClock/` | `packages/apps/DeskClock` | vendored | 4.4.4 DeskClock, datetimepicker vendored in-tree, module name kept. |
| `apps/Calendar/` | `packages/apps/Etar` | vendored | 4.4.4 Calendar with picker libs vendored in-tree, module name kept. |
| `apps/CandyExperience/` | `packages/apps/SetupWizard` | vendored | CandyOS Experience (Kotlin): the first-run onboarding (Wi-Fi, optional default apps, navigation style) and the menu app that stays in the drawer — About the system, plus the Testing bench (ported from the sunset `vendor/testing` overlay): 4.4 ticker posts, Holo widget gallery, Roboto 2013 + hybrid-emoji samples, Holo dialog/toast, volume panel, immersive, rotation, second activity for the 4.4 transitions and KitKatRecentsActivity. Privileged system_ext app on the platform certificate (WRITE_SECURE_SETTINGS, CHANGE_COMPONENT_ENABLED_STATE) with its own privapp whitelist and default-permissions pre-grant; module name `LineageSetupWizard` kept so `vendor/lineage` product config resolves unchanged. |
| `vendor/partner_gms/` | `vendor/partner_gms` | vendor overlay | microG Services + microG Companion (privileged product apps) and Aurora Store, with privapp/sysconfig XMLs; picked up by the stock `WITH_GMS` hook (P22). |

Keeping the stock module names is deliberate: `vendor/lineage` product config
references them (`PRODUCT_PACKAGES`), so the build wires the vendored apps in
with zero product-config changes.

## Patch series details

* Each `patches/<series>/` directory holds an ordered `NNNN-*.patch` chain
  generated by `git format-patch` — the numbering is the apply order.
* `patches/<series>/BASE` records the upstream commit the series was last
  generated against. It is informational: `git am -3` is self-contained and
  does not need the base, but BASE tells you how far upstream has drifted
  since the last refresh.
* Applying uses `git am -3` first (three-way merge against upstream drift),
  with a plain `git am` as fallback.
* The mbox `From <sha>` header of each patch is zeroed by `candyos-save.sh`:
  `git am` mints new commit shas on every replay, so patch identity is the
  content, not the sha.
* Legacy commit messages in the series use the old project's prefixes
  (`KITKAT-P<n>`, `KitKat UI P<n>:`). New commits use `CandyOS P<n>:`
  (see docs/contributing.md).

## History and provenance

The patch series were extracted — unchanged — from the original dev forks,
which remain on GitHub as read-only historical mirrors (with the original
`kitkat/p<n>` phase tags):

* `EHowardHill/android_frameworks_base` (branch `kitkat-mods` history)
* `EHowardHill/android_packages_apps_Settings`
* `EHowardHill/android_packages_inputmethods_LatinIME`
* the app-port forks under `EHowardHill/` for the six vendored apps

The forks are not needed to build or develop CandyOS; the canonical content
lives in this repo.

## Tree state after `candyos-sync.sh`

* Patched projects: detached HEAD at `m/lineage-21.0` (the synced upstream
  revision) plus the CandyOS patch commits on top. Never keep personal
  branches checked out in these projects — `candyos-sync.sh` resets them.
* Vendored slots: plain directories (no `.git`), exact copies of `apps/`.
* Overlay paths (e.g. `vendor/partner_gms`): plain directories (no `.git`),
  exact copies of `vendor/`.
* Everything else: untouched stock LineageOS.
