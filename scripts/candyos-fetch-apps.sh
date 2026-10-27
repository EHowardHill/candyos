#!/usr/bin/env bash
# CandyOS fetch: download the pinned Google-services bundles (MindTheGapps)
# and the Organic Maps release APK into vendor/partner_gms/, and generate
# the per-arch build makefiles. The blobs are NOT committed to the repo -
# this script pins the exact versions and SHA-256 checksums instead. Safe
# to re-run: existing files with a matching checksum are skipped.
#
# Usage: candyos-fetch-apps.sh [arch ...]
#   arch   limit the MindTheGapps extraction to the given arch(es): arm64,
#          arm. Default: both. "none" fetches only the plain APKs (the
#          x86/x86_64 emulator workflow builds without GApps - see
#          vendor/partner_gms/README.md).
#
#   (no tree needed - works straight from a fresh clone of this repo; a
#   built tree's aapt2 is picked up automatically for uses-library
#   mirroring when CANDYOS_ANDROID_TREE is set)

set -euo pipefail
. "$(dirname "$0")/common.sh"

# MindTheGapps 14.0.0 - the LineageOS 21 / Android 14 GApps package
# (<https://wiki.lineageos.org/gapps/>). arch|url|sha256
GAPPS=(
    "arm64|https://github.com/MindTheGapps/14.0.0-arm64/releases/download/MindTheGapps-14.0.0-arm64-20250203_200051/MindTheGapps-14.0.0-arm64-20250203_200051.zip|6e1c3616862ce5b33e2b96074f86ae846eb1351a26a980e1ed140f8a7e7a4fd6"
    "arm|https://github.com/MindTheGapps/14.0.0-arm/releases/download/MindTheGapps-14.0.0-arm-20250203_200051/MindTheGapps-14.0.0-arm-20250203_195950.zip|49ca0918cbc04f7b9d14aed56fbf2ae310f6e526703b8d9d60b32e57b8321e3b"
)

# name|dest|url|sha256
APKS=(
    "Organic Maps|vendor/partner_gms/OrganicMaps/OrganicMaps.apk|https://github.com/organicmaps/organicmaps/releases/download/2026.09.29-32-android/OrganicMaps-26092932-web-release.apk|d7dbdfc3cbfdbcce6d437f11147f2cd15384c03052d2b4594fd8c1c7777dd497"
)

sha256_of() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | cut -d' ' -f1
    else
        shasum -a 256 "$1" | cut -d' ' -f1
    fi
}

# Which arches to fetch (default: every pinned one).
wanted_archs=()
if [ $# -eq 0 ]; then
    wanted_archs=(arm64 arm)
else
    for a in "$@"; do
        [ "$a" != none ] && wanted_archs+=("$a")
    done
fi

aapt2_flag=""
tree_aapt2="${CANDYOS_ANDROID_TREE:-}/out/host/linux-x86/bin/aapt2"
if [ -x "$tree_aapt2" ]; then
    aapt2_flag="--aapt2 $tree_aapt2"
fi

fetch_and_extract_gapps() {
    local arch="$1" url="$2" expected="$3"
    local dest="$CANDYOS_ROOT/vendor/partner_gms/gapps-$arch"

    echo "candyos: MindTheGapps ($arch): downloading $url"
    local tmp
    tmp="$(mktemp "/tmp/mindthegapps-$arch.XXXXXX")"
    if ! curl -fsSL -o "$tmp" "$url"; then
        rm -f "$tmp"
        echo "candyos: ERROR: MindTheGapps ($arch): download failed" >&2
        return 1
    fi
    local actual
    actual="$(sha256_of "$tmp")"
    if [ "$actual" != "$expected" ]; then
        rm -f "$tmp"
        echo "candyos: ERROR: MindTheGapps ($arch): checksum mismatch" >&2
        echo "  expected $expected" >&2
        echo "  got      $actual" >&2
        echo "  (upstream released a new build? update the pin in $0)" >&2
        return 1
    fi

    rm -rf "$dest"
    mkdir -p "$dest"
    python3 - "$tmp" "$dest" <<'EOF'
import os, sys, zipfile
zp, dest = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(zp) as z:
    for n in z.namelist():
        n = n.lstrip("./")
        if not n.startswith("system/") or n.endswith("/"):
            continue
        out = os.path.join(dest, n)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        with z.open(n) as f, open(out, "wb") as g:
            while True:
                chunk = f.read(1 << 16)
                if not chunk:
                    break
                g.write(chunk)
EOF
    rm -f "$tmp"
    python3 "$CANDYOS_ROOT/scripts/candyos-gen-gapps-mk.py" "$dest" $aapt2_flag
    echo "candyos: MindTheGapps ($arch): ok -> $dest"
}

gapps_fail=0
for entry in "${GAPPS[@]}"; do
    IFS='|' read -r arch url expected <<<"$entry"
    for want in "${wanted_archs[@]}"; do
        if [ "$want" = "$arch" ]; then
            fetch_and_extract_gapps "$arch" "$url" "$expected" || gapps_fail=1
        fi
    done
done

fail=0
[ "$gapps_fail" -ne 0 ] && fail=1
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