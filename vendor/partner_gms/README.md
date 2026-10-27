# Bundled Google services: MindTheGapps + Organic Maps

CandyOS ships the real Google Mobile services built into the OS image:

* **MindTheGapps 14.0.0** (`gapps-arm/`, `gapps-arm64/`) — the GApps
  package recommended by the LineageOS wiki for LineageOS 21 /
  Android 14 (<https://wiki.lineageos.org/gapps/>): Play Store
  (Phonesky), Play Services (PrebuiltGmsCore), Google Services
  Framework, Setup Wizard, and the rest of the privileged Google
  apps, with their privapp/sysconfig XMLs. Installed privileged on
  the product partition, PRESIGNED.
* **Organic Maps** (`OrganicMaps/`, package `app.organicmaps`) — the
  open-source Maps app (docs/apps.md). Regular (unprivileged) product
  app.

## How it is wired in

LineageOS 21 has a stock hook for exactly this:
`vendor/lineage/config/partner_gms.mk` includes
`vendor/partner_gms/products/gms.mk` whenever `WITH_GMS` is true and the
directory exists. `patches/vendor-lineage/` is a one-line patch series
that sets `WITH_GMS := true`, and `candyos-sync.sh` copies this
directory into the tree at `vendor/partner_gms`. Nothing else in
LineageOS is modified.

Unlike the old microG setup there is **no signature spoofing**: the APKs
carry Google's real signatures, so nothing framework-side is involved
and `user` builds would work as well (the documented `userdebug` target
is kept for development).

## Arch conditionality

MindTheGapps ships **arm and arm64 APKs only** — there is no x86/x86_64
build. `products/gms.mk` therefore includes the generated per-arch
package list only when `TARGET_ARCH` is `arm` or `arm64`:

* `gapps-<arch>/Android.mk` is likewise guarded to its own arch, so both
  extracted trees can sit in the tree copy at once without colliding.
* **x86/x86_64 targets — the emulator QA images — intentionally build
  without GApps.** Everything CandyOS-specific remains testable there;
  the Google surfaces are only testable on arm/arm64 hardware (or an
  arm64 emulator image, if the host can run one).

## How the blobs get here

The MindTheGapps zips and the Organic Maps APK are not committed to
this repo (binary blobs, ~600 MB per arch). Run:

```
scripts/candyos-fetch-apps.sh [arm64|arm|none]
```

which downloads the pinned versions, verifies SHA-256 against the pins
recorded in the script, extracts each zip's `system/` tree into
`vendor/partner_gms/gapps-<arch>/`, and runs
`scripts/candyos-gen-gapps-mk.py` to generate:

* `gapps-<arch>/Android.mk` — one `BUILD_PREBUILT` module per APK/XML
  (product partition, `PRESIGNED`, `LOCAL_REPLACE_PREBUILT_APK_INSTALLED`
  so the Google-signed APKs install verbatim, arch-guarded);
* `gapps-<arch>/gapps-packages.mk` — the `PRODUCT_PACKAGES` list plus
  the `PRODUCT_ARTIFACT_PATH_REQUIREMENT_ALLOWED_LIST` entries,
  included by `products/gms.mk`.

Pass an arch (or `none`) to skip the ~600 MB download you do not need.
Then `candyos-sync.sh` (or the next `m`) picks the result up. Pinned
versions (2026-10):

| Blob | Version | Source |
|---|---|---|
| MindTheGapps arm64 | 14.0.0 (20250203_200051) | MindTheGapps/14.0.0-arm64 GitHub release |
| MindTheGapps arm | 14.0.0 (20250203_195950) | MindTheGapps/14.0.0-arm GitHub release |
| OrganicMaps.apk | 2026.09.29-32 | organicmaps GitHub release |

## Updating the pins

To move to newer versions: update the pins (URL + SHA-256) in
`scripts/candyos-fetch-apps.sh`, delete the old `gapps-<arch>/` trees,
re-run the fetch script, and commit the updated pins plus the
regenerated makefiles.

A new APK version can change the `<uses-library>` tags in its manifest —
the build's `manifest_check` then fails until the module declares exactly
the same set. The generator mirrors the tags automatically when it can
run an `aapt2` (`--aapt2` / `AAPT2=`, picked up from
`$CANDYOS_ANDROID_TREE/out/host/linux-x86/bin/aapt2` if it exists):
`uses-library` → `LOCAL_USES_LIBRARIES`,
`uses-library-not-required` → `LOCAL_OPTIONAL_USES_LIBRARIES`. Without
an aapt2 the modules are generated without tags and the build may
complain — re-run the generator once the tree has built one.