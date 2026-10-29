# Bundled Google services: microG + Aurora Store + Organic Maps

CandyOS ships a fully **optional** Google Play compatibility stack instead
of the real Google Mobile services:

* **microG Services** (`microg/com.google.android.gms.apk`, package
  `com.google.android.gms`) — the FLOSS reimplementation of Google Play
  services (Apache-2.0): sign-in, GCM push, Maps SDK, location, DroidGuard
  and UnifiedNlp are all integrated in the one APK now. Preloaded
  **privileged** on the product partition so the whitelisted permissions
  below are grantable.
* **microG Companion** (`microg/com.android.vending.apk`, package
  `com.android.vending`) — the Play Store *stub* apps check for (license
  checks, "is Play installed"). Also privileged, also spoofed.
* **Aurora Store** (`microg/com.aurora.store.apk`, package
  `com.aurora.store`) — the FLOSS Play *client* (GPL-3.0): searches and
  downloads apps from the real Play Store with an anonymous or personal
  account. Regular (unprivileged) product app.
* **Organic Maps** (`OrganicMaps/`, package `app.organicmaps`) — the
  open-source Maps app (docs/apps.md). Regular product app.

None of them run out of the box: they are preloaded **disabled**, and the
OTE wizard's Google-services step (plus the CandyOS Experience menu,
"Google services") enables them per user on opt-in — and can disable them
again at any time. That management UI lives in
`apps/CandyExperience/src/org/candyos/experience/` (`PlayConfig` +
`PlayServicesActivity`).

## Signature spoofing

Apps that use Google services verify that `com.google.android.gms` /
`com.android.vending` carry Google's certificate, so the microG packages
must appear to be signed with it. This used to be the reason CandyOS moved
away from microG to MindTheGapps ("no signature spoofing") — P34 brings it
back, scoped down to near-nothing:

* `patches/frameworks-base/0016` (P34) declares a hidden
  `android.permission.FAKE_PACKAGE_SIGNATURE`
  (`signature|privileged`) and applies the fixed Google platform
  certificate to exactly `com.google.android.gms` and
  `com.android.vending` **when they hold that permission** — nothing
  reads app meta-data, and no other package can ever get the grant:
  it lives only in this repo's privapp whitelists:
  * `microg/privapp-permissions-com.google.android.gms.xml` —
    FAKE_PACKAGE_SIGNATURE, INSTALL_LOCATION_PROVIDER (UnifiedNlp as the
    OS network location provider), CHANGE_DEVICE_IDLE_TEMP_WHITELIST and
    UPDATE_APP_OPS_STATS (GCM push maintenance) — the grant set the
    LineageOS-for-microG project ships.
  * `microg/privapp-permissions-com.android.vending.xml` —
    FAKE_PACKAGE_SIGNATURE only.
  * `microg/sysconfig-com.google.android.gms.xml` — power-save / data-save
    exemptions so GCM keeps its connection.
* LineageOS 21 also has its own built-in spoofing hook
  (`isMicrogSigned`/`generateFakeSignature` in ComputerEngine,
  userdebug-only), but it reads the app's `fake-signature` meta-data,
  which current microG builds store as a *string resource* — package
  parsing drops resource-typed meta-data values, so that hook finds
  nothing with modern microG. The P34 patch is metadata-independent and,
  unlike the LineageOS hook, works on user builds as well. The two do not
  conflict (same fixed certificate).

## How it is wired in

LineageOS 21's stock hook, unchanged: `vendor/lineage/config/partner_gms.mk`
includes `vendor/partner_gms/products/gms.mk` whenever `WITH_GMS` is true
(the flag is set by `patches/vendor-lineage/`). `products/gms.mk` now just
adds `OrganicMaps`, `GmsCore`, `Companion` and `AuroraStore` to
`PRODUCT_PACKAGES` — the modules and the per-package XMLs are committed
in `microg/Android.mk`, and unlike MindTheGapps there are no per-arch
makefiles: the APKs are universal, so **the x86/x86_64 emulator QA images
bundle the whole stack too** (the OTE step is testable there).

## How the blobs get here

The APKs are not committed to this repo (binary blobs, ~125 MB total). Run:

```
scripts/candyos-fetch-apps.sh
```

which downloads the pinned versions, verifies SHA-256 against the pins
recorded in the script, and drops them at the fixed paths the makefiles
expect. Pinned versions (2026-10):

| Blob | Version | Source |
|---|---|---|
| microG Services | 0.3.17.252432 (252432034) | microg/GmsCore GitHub release |
| microG Companion | 0.3.17.40226 (84022634) | microg/GmsCore GitHub release (Companion ships in lockstep with GmsCore releases) |
| Aurora Store | 4.8.4 (76) | AuroraOSS GitLab release, mirrored by IzzyOnDroid (upstream-signed build) |
| OrganicMaps.apk | 2026.09.29-32 | organicmaps GitHub release |

Aurora's flavor matters: the pinned APK is **Aurora's own build** (Rahul
Patel's certificate, via the IzzyOnDroid mirror of the GitLab release), not
the F-Droid-rebuilt one — that keeps Aurora's in-app self-updater able to
install its own later releases over the preload.

## Updates

* microG Services + Companion are tracked by the CandyOS Experience
  release updater (both APKs come from the same `microg/GmsCore` GitHub
  releases; see docs/updater.md for the multi-asset filtering this
  needed).
* Aurora publishes **no GitHub releases** (GitLab only), so it is the one
  bundled app the updater cannot track. Its built-in self-updater shows
  updates in the app itself (installs prompt for confirmation — Aurora
  is not its own installer of record when preloaded).
* Both mechanisms install over the preload cleanly (same certificate).

## Updating the pins

To move to newer versions: update the pins (URL + SHA-256) in
`scripts/candyos-fetch-apps.sh`, delete the old APKs, re-run the fetch
script, and commit the updated pins. For microG, both APK pins move
together (one GitHub release provides both assets).

If a new APK version changes its `<uses-library>` tags, the build's
`manifest_check` fails until `microg/Android.mk` declares exactly the same
set — mirror them from `aapt2 dump badging <apk>` (the modules carry the
current sets inline).
