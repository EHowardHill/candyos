#!/usr/bin/env bash
# CandyOS sync: bring the LineageOS tree up to date, replay the CandyOS
# patch series, and refresh the vendored apps. Re-runnable; this is the ONLY
# supported way to sync a CandyOS tree (a bare "repo sync" would rebase away
# the patches and leave stale vendored apps).
#
# Usage: candyos-sync.sh <lineage-tree> [extra repo-sync args]
#   Extra args are passed straight to "repo sync -c", e.g. to sync only
#   some projects:
#     candyos-sync.sh ~/android/lineage frameworks/base

set -euo pipefail
. "$(dirname "$0")/common.sh"

resolve_tree "${1:-}"
shift || true

# 1. Guard the patched projects, then detach them so "repo sync" has nothing
#    to rebase and never sees CandyOS patches as local work.
for pair in "${CANDYOS_PATCHED[@]}"; do
    series="${pair%%[[:space:]]*}"
    path="${pair##*[[:space:]]}"
    [ -d "$TREE/$path" ] || continue
    guard_project "$TREE/$path" "$series"
    branch="$(git -C "$TREE/$path" branch --show-current)"
    if [ -n "$branch" ]; then
        git -C "$TREE/$path" checkout --detach >/dev/null 2>&1
        git -C "$TREE/$path" branch -D "$branch" >/dev/null 2>&1 || true
    fi
done

# 2. Sync the tree.
(cd "$TREE" && repo sync -c "$@")

# 3. Reset the patched projects to the upstream revision and replay the series.
for pair in "${CANDYOS_PATCHED[@]}"; do
    series="${pair%%[[:space:]]*}"
    path="${pair##*[[:space:]]}"
    [ -d "$TREE/$path" ] || die "$path is missing after sync"
    reset_project "$TREE/$path"
    apply_series "$TREE/$path" "$series"
done

# 4. Refresh the vendored app slots.
vendor_apps

echo "candyos: sync complete — tree is ready to build:"
echo "  cd $TREE && source build/envsetup.sh && lunch lineage_sdk_phone_x86_64-ap2a-userdebug && m -j\$(nproc)"
