#!/usr/bin/env bash
# CandyOS save: regenerate the patch series in this repo from the commits
# currently sitting in the patched projects of your LineageOS tree.
# Run this AFTER committing new work in frameworks/base, Settings, or
# LatinIME (see docs/contributing.md), then commit the refreshed patches
# to the CandyOS repo like any other file.
#
# Usage: candyos-save.sh <lineage-tree>

set -euo pipefail
. "$(dirname "$0")/common.sh"

resolve_tree "${1:-}"

for pair in "${CANDYOS_PATCHED[@]}"; do
    series="${pair%%[[:space:]]*}"
    path="${pair##*[[:space:]]}"
    [ -d "$TREE/$path" ] || die "$TREE/$path does not exist — is this a CandyOS tree?"

    [ -n "$(git -C "$TREE/$path" status --porcelain)" ] &&
        die "uncommitted changes in $path — commit your work first"
    base="$(git -C "$TREE/$path" merge-base HEAD "m/$LINEAGE_BRANCH")" ||
        die "$path: no m/$LINEAGE_BRANCH ref — run candyos-sync.sh once first"
    n="$(git -C "$TREE/$path" rev-list --count "$base..HEAD")"
    [ "$n" -gt 0 ] || die "$path: nothing ahead of upstream — refusing to empty patches/$series"

    out="$CANDYOS_ROOT/patches/$series"
    rm -f "$out"/*.patch
    git -C "$TREE/$path" format-patch "$base..HEAD" -o "$out" >/dev/null
    echo "$base" > "$out/BASE"
    echo "candyos: patches/$series regenerated ($n patch(es), BASE $base)"
done

echo "candyos: now commit and push the refreshed patches in $CANDYOS_ROOT"
