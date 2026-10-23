#!/usr/bin/env bash
# CandyOS verify: check that the stored patch series still applies cleanly
# to the upstream revision currently synced in the tree. Leaves the tree in
# the normal post-sync state. Use after "repo sync" moved upstream, before a
# PR, or whenever you suspect patch rot.

set -uo pipefail
. "$(dirname "$0")/common.sh"

resolve_tree "${1:-}"
fail=0

for pair in "${CANDYOS_PATCHED[@]}"; do
    series="${pair%%[[:space:]]*}"
    path="${pair##*[[:space:]]}"
    [ -d "$TREE/$path" ] || { echo "candyos: FAIL: $path missing"; fail=1; continue; }
    if ! guard_project "$TREE/$path" "$series"; then
        fail=1
        continue
    fi
    if ! (reset_project "$TREE/$path" && apply_series "$TREE/$path" "$series"); then
        fail=1
        continue
    fi
done

if [ "$fail" -ne 0 ]; then
    echo "candyos: verification FAILED — see above"
    exit 1
fi
echo "candyos: verification passed — all series apply cleanly"
