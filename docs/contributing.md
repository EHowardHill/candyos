# Contributing to CandyOS

Set up a tree exactly like everyone else's (docs/getting-started.md), then
pick the flow matching the code you're touching. In both flows you end up
committing to the CandyOS repo and opening a normal PR — there are no fork
permissions or special branches to worry about.

## Which flow?

* App under `apps/` (Launcher, Calculator, SoundRecorder, Music, DeskClock,
  Calendar) → **Flow A**
* `frameworks/base`, `packages/apps/Settings`, `packages/inputmethods/LatinIME`
  → **Flow B** (patch series)

## Flow A — vendored apps

The copies inside the LineageOS tree are generated; **edit the copy in the
CandyOS repo** or your changes will be wiped on the next sync.

```
# edit ~/android/candyos/apps/DeskClock/...
~/android/candyos/scripts/candyos-sync.sh ~/android/lineage   # refresh the tree slot
cd ~/android/lineage
source build/envsetup.sh
lunch lineage_sdk_phone_x86_64-ap2a-userdebug && m -j$(nproc)  # build + test
~/android/candyos/scripts/candyos-emulator.sh ~/android/lineage   # boot it for QA
# then: git commit + push + PR, as with any repo
```

## Flow B — patched projects (frameworks/base, Settings, LatinIME)

You develop in the LineageOS tree, on top of the already-applied patch
series. CandyOS then re-serializes your commits into `patches/`.

1. **Work in the tree.** Edit files under the patched project as usual.
2. **Commit there** (a normal `git commit` inside e.g.
   `~/android/lineage/frameworks/base`). Follow the message convention:

   ```
   CandyOS P<phase>: <what you did>
   ```

   continuing the phase numbering in docs/phases/TASKS.md. One logical
   change per commit; no merge commits (the series must stay linear).
3. **Re-serialize:**

   ```
   ~/android/candyos/scripts/candyos-save.sh ~/android/lineage
   ```

   This regenerates `patches/<series>/` (including BASE) from everything
   currently ahead of upstream in that project — your new commit included.
4. **Build and test** (boot the image with
   `~/android/candyos/scripts/candyos-emulator.sh ~/android/lineage` —
   see docs/getting-started.md), then run
   `~/android/candyos/scripts/candyos-verify.sh ~/android/lineage` as a
   final sanity check.
5. **Commit the refreshed patches** in the CandyOS repo (add the new/changed
   `patches/` files), push to your CandyOS fork, and open a PR.

After your patch lands, re-run `candyos-sync.sh` so your tree returns to the
canonical generated state.

## Patch hygiene

* Never edit `.patch` files by hand — regenerate with `candyos-save.sh`.
* Never delete commits from the middle of a series you didn't author;
  regenerate from the tree instead.
* Keep series linear: `git am` replays them one-by-one onto moving upstream.
* If `git am -3` conflicts during a sync, fix it in the tree
  (`git am --continue`), then immediately `candyos-save.sh` and push the
  refreshed series — don't carry a conflicted tree forward.
* Update docs/phases/TASKS.md when you open or close a phase.

## What not to do

* Don't run bare `repo sync` on a CandyOS tree — use `candyos-sync.sh`.
* Don't commit inside the vendored app slots in the tree — they're generated.
* Don't leave personal branches checked out in the patched projects —
  `candyos-sync.sh` deletes whatever branch is checked out there.
