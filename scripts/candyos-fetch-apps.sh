#!/usr/bin/env bash
# CandyOS fetch: download the pinned Google-services bundles (microG
# Services + microG Companion + Aurora Store) and the Organic Maps release
# APK into vendor/partner_gms/. The blobs are NOT committed to the repo -
# this script pins the exact versions and SHA-256 checksums instead. Safe
# to re-run: existing files with a matching checksum are skipped.
#
# Usage: candyos-fetch-apps.sh
#
#   (no tree needed - works straight from a fresh clone of this repo; the
#   APKs are universal, so there are no per-arch variants to pick between)

set -euo pipefail
. "$(dirname "$0")/common.sh"

# label|dest|url|sha256
APKS=(
    "Organic Maps|vendor/partner_gms/OrganicMaps/OrganicMaps.apk|https://github.com/organicmaps/organicmaps/releases/download/2026.09.29-32-android/OrganicMaps-26092932-web-release.apk|d7dbdfc3cbfdbcce6d437f11147f2cd15384c03052d2b4594fd8c1c7777dd497"
    "microG Services (GmsCore)|vendor/partner_gms/microg/com.google.android.gms.apk|https://github.com/microg/GmsCore/releases/download/v0.3.17.252432/com.google.android.gms-252432034.apk|e5e6cba078bdec812b5450599350e5c1a12c8b7438da5bf77123af170f8c1a35"
    "microG Companion (FakeStore)|vendor/partner_gms/microg/com.android.vending.apk|https://github.com/microg/GmsCore/releases/download/v0.3.17.252432/com.android.vending-84022634.apk|9b8616011cc8f026bcbbe094df850794abb9d6d9f23214d7a8f8c4d1033ea012"
    "Aurora Store|vendor/partner_gms/microg/com.aurora.store.apk|https://apt.izzysoft.de/fdroid/repo/com.aurora.store_76.apk|8a1ed9aa09631290da91cb793e0517b0f20dc70239ac94ae6682cd94f91a4bad"
)

sha256_of() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | cut -d' ' -f1
    else
        shasum -a 256 "$1" | cut -d' ' -f1
    fi
}

fail=0
for entry in "${APKS[@]}"; do
    IFS='|' read -r label dest url expected <<<"$entry"
    dest="$CANDYOS_ROOT/$dest"

    if [ -f "$dest" ] && [ "$(sha256_of "$dest")" = "$expected" ]; then
        echo "candyos: $label: already present and pinned - skipping"
        continue
    fi

    echo "candyos: $label: downloading $url"
    mkdir -p "$(dirname "$dest")"
    tmp="$(mktemp "$dest.XXXX")"
    if ! curl -fsSL -o "$tmp" "$url"; then
        rm -f "$tmp"
        echo "candyos: ERROR: $label: download failed" >&2
        fail=1
        continue
    fi
    actual="$(sha256_of "$tmp")"
    if [ "$actual" != "$expected" ]; then
        rm -f "$tmp"
        echo "candyos: ERROR: $label: checksum mismatch" >&2
        echo "  expected $expected" >&2
        echo "  got      $actual" >&2
        echo "  (upstream released a new build? update the pin in $0)" >&2
        fail=1
        continue
    fi
    mv "$tmp" "$dest"
    echo "candyos: $label: ok -> $dest"
done

if [ "$fail" -ne 0 ]; then
    die "one or more downloads failed - see above"
fi

echo "candyos: all bundled blobs are ready (next candyos-sync.sh will copy them into the tree)"
