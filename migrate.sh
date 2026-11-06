#!/usr/bin/env bash
# CandyOS migration: move the project (repo config, docs, and an existing
# LineageOS tree) from one LineageOS major version to the next.
#
# Currently: LineageOS 21 (lineage-21.0, Android 14, release ap2a)
#        --> LineageOS 24 (lineage-24.0, Android 16, release ap4a)
#
# What it does, in order:
#   1. Refuses to run with uncommitted changes in the candyos repo
#      (the sed edits below touch tracked files — commit or stash first).
#   2. Rewrites the branch/release identifiers across the candyos repo:
#        scripts/common.sh     LINEAGE_BRANCH lineage-21.0 -> lineage-24.0
#        scripts/*, README.md, docs/*, manifest/candyos.xml:
#          lineage-21.0    -> lineage-24.0
#          ap2a            -> ap4a      (lunch/sdk image release letter)
#          "LineageOS 21"  -> "LineageOS 24"
#   3. Re-initializes the LineageOS tree on the new branch (repo init -b
#      lineage-24.0) and reinstalls the candyos local manifest.
#   4. Runs scripts/candyos-sync.sh: syncs the new tree, replays the
#      patch series, and re-vendors the apps and overlays.
#
# EXPECT FAILURES IN STEP 4. Jumping three major versions means the
# upstream patch series in patches/ will almost certainly NOT apply
# cleanly (frameworks/base, Settings, LatinIME, etc. have moved).
# When a series fails, the script stops and tells you how to recover:
# resolve the conflicts by hand, then run candyos-save.sh to regenerate
# the series for the new base, and commit the result.
#
# Usage: migrate.sh [lineage-tree]
#   [lineage-tree] defaults to ~/android/lineage (the quickstart layout).
#
# The sed rewrites (step 2) are one-shot: re-running this script after a
# successful migration is a no-op (nothing matches lineage-21.0 anymore).

set -euo pipefail

# ---------------------------------------------------------------------------
# Migration definition — edit these when bumping to a future version.
# ---------------------------------------------------------------------------
OLD_BRANCH="lineage-21.0"
NEW_BRANCH="lineage-24.0"
OLD_RELEASE="ap2a"          # lunch release letter (lineage...-<rel>-userdebug)
NEW_RELEASE="ap4a"          # verify against `lunch` output after the sync
OLD_MARKETING="LineageOS 21"
NEW_MARKETING="LineageOS 24"

MANIFEST_URL="https://github.com/LineageOS/android.git"

# ---------------------------------------------------------------------------
CANDYOS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$CANDYOS_ROOT/scripts/common.sh"

die() { echo "migrate: ERROR: $*" >&2; exit 1; }

TREE="${1:-$HOME/android/lineage}"

# 1. Guard: don't sed tracked files on top of uncommitted work.
[ -z "$(git -C "$CANDYOS_ROOT" status --porcelain)" ] ||
    die "uncommitted changes in the candyos repo — commit or stash first"
[ -n "$(git -C "$CANDYOS_ROOT" rev-parse --verify HEAD 2>/dev/null)" ] ||
    die "candyos repo has no commits yet — commit before migrating"

# 2. Rewrite the version identifiers across the candyos repo
#    (source files, scripts, docs, manifest — never .git or the tree).
echo "migrate: rewriting $OLD_BRANCH/$OLD_RELEASE -> $NEW_BRANCH/$NEW_RELEASE in the candyos repo"
FILES="$(git -C "$CANDYOS_ROOT" ls-files | grep -Ev '\.(png|jpg|jpeg|gif|webp|mp4|zip|apk|jar|keystore|jks)$' || true)"
for f in $FILES; do
    f="$CANDYOS_ROOT/$f"
    [ -f "$f" ] || continue
    if grep -q -e "$OLD_BRANCH" -e "$OLD_RELEASE" -e "$OLD_MARKETING" "$f" 2>/dev/null; then
        sed -i -e "s/$OLD_BRANCH/$NEW_BRANCH/g" \
               -e "s/$OLD_RELEASE/$NEW_RELEASE/g" \
               -e "s/$OLD_MARKETING/$NEW_MARKETING/g" "$f"
        echo "  updated: ${f#$CANDYOS_ROOT/}"
    fi
done
echo "migrate: review the diff (git diff), then commit before syncing"

# 3. Re-init the LineageOS tree on the new branch. The local manifest
#    survives repo init, but reinstall it to be safe (it is unchanged by
#    the sed pass — the manifest only removes app projects, which keep
#    their names across versions).
[ -d "$TREE/.repo" ] || die "'$TREE' is not a repo-initialized tree — run scripts/candyos-init.sh $TREE first"
echo "migrate: re-initializing $TREE on $NEW_BRANCH (this moves every project)"
(cd "$TREE" && repo init -u "$MANIFEST_URL" -b "$NEW_BRANCH") </dev/null
mkdir -p "$TREE/.repo/local_manifests"
cp "$CANDYOS_ROOT/manifest/candyos.xml" "$TREE/.repo/local_manifests/candyos.xml"

# 4. Sync the new tree, replay the patches, re-vendor the apps.
#    candyos-sync.sh detaches and guards the patched projects first, so
#    no CandyOS work is lost even if the tree still has patches applied.
echo "migrate: syncing $NEW_BRANCH and replaying the CandyOS patch series"
if "$CANDYOS_ROOT/scripts/candyos-sync.sh" "$TREE"; then
    echo "migrate: done — verify the lunch target, then build:"
    echo "  cd $TREE && source build/envsetup.sh && lunch lineage_sdk_phone_x86_64-$NEW_RELEASE-userdebug && m -j8"
    exit 0
fi

# A patch series failed to apply — the expected case across three majors.
cat >&2 <<EOF

migrate: sync stopped: a patch series did not apply on $NEW_BRANCH.
This is expected — the patches were written against $OLD_BRANCH.

Recover per series (the error above names the path):

  git -C $TREE/<path> am -3 $CANDYOS_ROOT/patches/<series>/*.patch
  # resolve each conflict:
  #   git -C $TREE/<path> checkout --theirs <file> is NOT enough — read the
  #   rejected .rej hunks, re-implement them on the new source, then:
  #   git -C $TREE/<path> add -A && git -C $TREE/<path> am --continue
  $CANDYOS_ROOT/scripts/candyos-save.sh $TREE   # regenerate the series

Then finish the rest by re-running the normal sync once all series are
saved on the new base:

  $CANDYOS_ROOT/scripts/candyos-sync.sh $TREE

Finally, commit the regenerated patches/ to the candyos repo. Upstream
APIs changed across three majors — expect build breakage (vendor/lineage
hooks, setup wizard flows, partner_gms hooks) beyond the patches
themselves; fix forward and save again.
EOF
exit 1