# Bundled app store: microG + Aurora Store

CandyOS ships a de-Googled Play-access stack built into the OS image:

* **microG Services** (`GmsCore/`, package `com.google.android.gms`) —
  the microG reimplementation of Google Play Services: accounts, push
  (GCM), location, licensing, billing. Installed privileged on the
  product partition.
* **microG Companion / FakeStore** (`FakeStore/`, package
  `com.android.vending`) — a stub that answers "Play Store installed?"
  checks. microG's own docs for custom ROMs recommend shipping it; it
  also carries the `com.android.vending` account/permission plumbing
  that Play-dependent apps look for.
* **Aurora Store** (`AuroraStore/`, package `com.aurora.store`) — a
  familiar storefront front-end for the Play catalog, using either an
  anonymous session or the microG Google account. Regular (unprivileged)
  product app.

## How it is wired in

LineageOS 21 already has a stock hook for exactly this:
`vendor/lineage/config/partner_gms.mk` includes
`vendor/partner_gms/products/gms.mk` whenever `WITH_GMS` is true and the
directory exists. `patches/vendor-lineage/` is a one-line patch series
that sets `WITH_GMS := true`, and `candyos-sync.sh` copies this
directory into the tree at `vendor/partner_gms`. Nothing else in
LineageOS is modified — the GMS packages come from stock
`BUILD_PREBUILT` makefiles modeled on
[android_vendor_partner_gms](https://github.com/lineageos4microg/android_vendor_partner_gms)
(the overlay used by LineageOS for microG).

## Signature spoofing (no patch needed)

LineageOS 18.1+ ships *restricted* signature spoofing support directly
in `frameworks/base` (`ComputerEngine.isMicrogSigned`): the OS reports
the Google platform signature for `com.google.android.gms` /
`com.android.vending` **if and only if** the package is signed with the
official microG release key and carries a `fake-signature` meta-data
entry. Consequences for CandyOS:

* **Use the official microG GitHub release APKs only.** F-Droid builds
  of GmsCore are signed with the F-Droid key and signature spoofing
  (and therefore Play-signature-dependent app checks) will not work with
  them.
* Signature spoofing requires a **debuggable** ROM (`userdebug`/`eng`).
  CandyOS's documented lunch target is `userdebug`, so this holds. If
  CandyOS ever ships `user` builds, restricted spoofing stops working
  and the unrestricted-spoofing framework patches (see
  lineageos4microg/docker-lineage-cicd `signature_spoofing_patches`)
  would be needed instead — deliberately out of scope for now.

Aurora Store is not signature-spoofed (it does not need it), so its
F-Droid build is fine.

## The APKs

The APKs are not committed to this repo (binary blobs, ~120 MB). Run:

```
scripts/candyos-fetch-apps.sh
```

which downloads the pinned versions and verifies their SHA-256 against
pins recorded in the script. Then `candyos-sync.sh` (or the next
`m`) picks them up. Pinned versions (2026-10):

| APK | Version | Source |
|---|---|---|
| GmsCore.apk (microG Services) | v0.3.17.252432 | microg/GmsCore GitHub release |
| FakeStore.apk (microG Companion) | v0.3.17.252432 | microg/GmsCore GitHub release |
| AuroraStore.apk (Aurora Store) | 4.8.4 | F-Droid (`com.aurora.store` v76) |

To move to newer versions: update the pins in `scripts/candyos-fetch-apps.sh`,
delete the old APKs under this directory, re-run the fetch script, and
commit the updated pins.

## Updating the microG configuration defaults

`GmsCore/microg.xml` (/product/etc/microg.xml) ships microG's default
settings for system installs (disabled check-in/GCM by default, Mozilla
location services on, vending licensing/billing on). Users can override
everything from the microG Settings app on-device.
