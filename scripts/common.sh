#!/usr/bin/env bash
# Shared CandyOS definitions and helpers. Sourced by the candyos-* scripts.

CANDYOS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LINEAGE_MANIFEST_URL="https://github.com/LineageOS/android.git"
LINEAGE_BRANCH="lineage-21.0"

# Repos modified via patch series: "<patches/ subdirectory> <path in the LineageOS tree>".
# These stay as STOCK LineageOS checkouts; candyos-sync.sh replays the series on
# top of the current upstream revision after every sync.
CANDYOS_PATCHED=(
    "frameworks-base    frameworks/base"
    "settings           packages/apps/Settings"
    "latinime           packages/inputmethods/LatinIME"
    "vendor-lineage     vendor/lineage"
    "lineage-sdk        lineage-sdk"
)

# Self-contained app ports vendored wholesale: "<apps/ directory> <slot in the
# LineageOS tree>". The stock project is removed from the manifest (see
# manifest/candyos.xml) and the slot is populated from apps/ after every sync.
CANDYOS_VENDORED=(
    "CandyLauncher      packages/apps/Trebuchet"
    "Calculator         packages/apps/ExactCalculator"
    "SoundRecorder      packages/apps/Recorder"
    "Music              packages/apps/Eleven"
    "DeskClock          packages/apps/DeskClock"
    "Calendar           packages/apps/Etar"
)

# Overlay directories vendored into the tree without replacing a stock
# project: "<directory under this repo's vendor/> <path in the LineageOS
# tree>". The tree copy is regenerated from this repo after every sync.
CANDYOS_VENDOR_TREES=(
    "partner_gms        vendor/partner_gms"
    "testing            vendor/testing"
)

die() {
    echo "candyos: ERROR: $*" >&2
    exit 1
}

# Resolve the LineageOS tree path (first script argument, or
# $CANDYOS_ANDROID_TREE) into $TREE.
resolve_tree() {
    TREE="${1:-${CANDYOS_ANDROID_TREE:-}}"
    [ -n "$TREE" ] || die "usage: $0 <lineage-tree> [extra args]"
    [ -d "$TREE/.repo" ] || die "'$TREE' is not a repo-initialized LineageOS tree (run candyos-init.sh first)"
}

patch_count() {
    ls "$1"/*.patch 2>/dev/null | wc -l
}

# Refuse to touch a patched project that holds unsaved work.
guard_project() {
    local path="$1" series="$2" ahead expected
    [ -e "$path/.git/rebase-apply" ] &&
        die "$path: a 'git am' is in progress — resolve it (git am --continue) or abort it first"
    [ -n "$(git -C "$path" status --porcelain)" ] &&
        die "uncommitted changes in $path — commit or stash them first"
    if git -C "$path" rev-parse --verify "m/$LINEAGE_BRANCH" >/dev/null 2>&1; then
        ahead=$(git -C "$path" rev-list --count "m/$LINEAGE_BRANCH..HEAD")
        expected=$(patch_count "$CANDYOS_ROOT/patches/$series")
        [ "$ahead" -gt "$expected" ] &&
            die "$path has $ahead local commits but only $expected are saved in CandyOS — run candyos-save.sh first"
    fi
    return 0
}

# Put a patched project onto the plain upstream revision, discarding CandyOS
# patches (they are reapplied immediately afterwards).
reset_project() {
    local path="$1"
    git -C "$path" checkout --detach "m/$LINEAGE_BRANCH" >/dev/null 2>&1 ||
        die "$path: no m/$LINEAGE_BRANCH ref — run candyos-sync.sh once first"
    git -C "$path" reset --hard >/dev/null 2>&1
    git -C "$path" clean -fd >/dev/null 2>&1
}

# Apply a patch series with git am: 3-way first (handles upstream drift),
# then a plain retry, then a loud failure.
apply_series() {
    local path="$1" series="$2" n
    n=$(patch_count "$CANDYOS_ROOT/patches/$series")
    [ "$n" -gt 0 ] || die "no patches found in patches/$series"
    echo "candyos: $path: applying $n patch(es)"
    if git -C "$path" am -3 "$CANDYOS_ROOT/patches/$series"/*.patch; then
        return 0
    fi
    git -C "$path" am --abort >/dev/null 2>&1 || true
    if git -C "$path" am "$CANDYOS_ROOT/patches/$series"/*.patch; then
        return 0
    fi
    git -C "$path" am --abort >/dev/null 2>&1 || true
    die "$path: the patch series no longer applies — upstream has moved.
        Recover by hand:
          git -C $path am -3 $CANDYOS_ROOT/patches/$series/*.patch
          (resolve each conflict, git add ..., git am --continue)
          $CANDYOS_ROOT/scripts/candyos-save.sh <lineage-tree>   # refresh the series
        then commit the refreshed patches to the CandyOS repo."
}

# Copy every vendored app into its LineageOS slot, replacing whatever is there.
vendor_apps() {
    local name slot
    for pair in "${CANDYOS_VENDORED[@]}"; do
        name="${pair%%[[:space:]]*}"
        slot="${pair##*[[:space:]]}"
        rm -rf "$TREE/$slot"
        rsync -a "$CANDYOS_ROOT/apps/$name/" "$TREE/$slot/"
        echo "candyos: vendored apps/$name -> $slot"
    done
}

# Copy every overlay directory into its place in the LineageOS tree.
vendor_trees() {
    local name slot
    for pair in "${CANDYOS_VENDOR_TREES[@]}"; do
        name="${pair%%[[:space:]]*}"
        slot="${pair##*[[:space:]]}"
        rm -rf "$TREE/$slot"
        rsync -a "$CANDYOS_ROOT/vendor/$name/" "$TREE/$slot/"
        echo "candyos: vendored vendor/$name -> $slot"
    done
}

# Warn (loudly, but do not fail) about bundled-app APKs that are missing or
# stale in the candyos repo - candyos-fetch-apps.sh is the fix.
check_partner_gms_apks() {
    local apk missing=0
    for apk in GmsCore/GmsCore.apk FakeStore/FakeStore.apk AuroraStore/AuroraStore.apk; do
        if [ ! -s "$CANDYOS_ROOT/vendor/partner_gms/$apk" ]; then
            echo "candyos: WARNING: vendor/partner_gms/$apk is missing!" >&2
            missing=1
        fi
    done
    [ "$missing" -eq 0 ] && return 0
    echo "candyos: the microG/Aurora packages cannot build without it/them. Run:" >&2
    echo "  $CANDYOS_ROOT/scripts/candyos-fetch-apps.sh" >&2
    echo "candyos: (the APKs are pinned downloads, not committed files - see" >&2
    echo "  vendor/partner_gms/README.md)" >&2
    return 0
}
