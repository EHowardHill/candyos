#!/usr/bin/env bash
# CandyOS bootstrap: initialize a LineageOS tree and install the CandyOS
# local manifest. Safe to re-run.
#
# Usage: candyos-init.sh <lineage-tree>
#   <lineage-tree> is the directory the LineageOS source will live in
#   (it is created if missing). Example:
#     candyos-init.sh ~/android/lineage

set -euo pipefail
. "$(dirname "$0")/common.sh"

tree="${1:-}"
[ -n "$tree" ] || die "usage: $0 <lineage-tree>"
mkdir -p "$tree"

if [ -d "$tree/.repo" ]; then
    echo "candyos: $tree already repo-initialized — skipping repo init"
else
    (cd "$tree" && repo init -u "$LINEAGE_MANIFEST_URL" -b "$LINEAGE_BRANCH") </dev/null
fi

mkdir -p "$tree/.repo/local_manifests"
cp "$CANDYOS_ROOT/manifest/candyos.xml" "$tree/.repo/local_manifests/candyos.xml"
echo "candyos: installed manifest/candyos.xml -> $tree/.repo/local_manifests/candyos.xml"
echo "candyos: next: $0's sibling, candyos-sync.sh $tree"
