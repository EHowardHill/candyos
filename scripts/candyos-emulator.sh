#!/usr/bin/env bash
# CandyOS emulator: launch the built CandyOS image on the Android emulator
# (Pixel 3a geometry, the canonical QA device) for device-side verification,
# e.g. after a build. Wipes the AVD images for a clean first boot, launches
# via the lunch environment, and waits for Android to finish booting.
#
# Usage: candyos-emulator.sh [--kill | --keep-data] <lineage-tree>
#   <lineage-tree> defaults to $CANDYOS_ANDROID_TREE (see common.sh).
#   --kill (which needs no <lineage-tree>) stops a running emulator.
#   --keep-data relaunches WITHOUT wiping userdata (device keeps its
#   provisioning, settings and installed state - see the launch block).
#   Run from a normal shell / VNC desktop; the window appears on :0.
#
# Notes:
# - The emulator binary runs from a scratch copy in /tmp/opencode/
#   android-emulator because the prebuilts/android-emulator checkout is
#   intermittently unavailable on this machine. The copy is refreshed
#   automatically from <tree>/prebuilts/android-emulator/linux-x86_64/
#   when missing.
# - The lunch environment is required so the build-tree AVD images (GKI
#   kernel + system-qemu/vendor/product images) get wired in.
# - First boot takes several minutes on SwiftShader; the window freezes
#   while Android boots - watch with: adb shell getprop sys.boot_completed
# - Module-only rebuilds do not always reach the emulator - re-run a full
#   "m -j8" before relaunching (see docs/getting-started.md).

set -euo pipefail
. "$(dirname "$0")/common.sh"

TARGET=lineage_sdk_phone_x86_64-ap2a-userdebug
EMUDIR=/tmp/opencode/android-emulator
EMUWORK=/tmp/opencode/emu-work
LOG=/tmp/opencode/emulator.log

if [ "${1:-}" = "--kill" ]; then
    pkill -f "[q]emu-system-x86_64" 2>/dev/null || true
    pkill -f "[c]rashpad_handler.*emu-crash" 2>/dev/null || true
    # Wait for the graceful exit to finish: the emulator saves a
    # quickboot snapshot on the way down, and that save must complete
    # BEFORE the next launch wipes $OUT/snapshots - otherwise the fresh
    # boot resumes the stale snapshot right back (see the launch block).
    for _ in $(seq 1 40); do
        pgrep -f "[q]emu-system-x86_64" >/dev/null || break
        sleep 1
    done
    echo "candyos: emulator stopped"
    exit 0
fi

# --keep-data: relaunch WITHOUT wiping the AVD userdata - the device
# comes back with its /data intact (provisioning/OTE, settings, any
# installed state survive). Use it to iterate on a rebuilt image with
# the same device, or to recover a session; a normal launch (no flag)
# wipes for a clean first boot, as always.
KEEPDATA=
if [ "${1:-}" = "--keep-data" ]; then
    KEEPDATA=1
    shift
fi

resolve_tree "${1:-}"
OUT=$TREE/out/target/product/emu64x
ADB=$TREE/out/host/linux-x86/bin/adb

if ! pgrep -f "[q]emu-system-x86_64" >/dev/null; then
    [ -f "$OUT/system-qemu.img" ] ||
        die "no emulator images in $OUT — build first (see docs/getting-started.md)"

    # Refresh the userdata image so we always boot clean (no snapshots).
    # The emulator's persistent /data lives in $OUT (from the lunch env),
    # not in the launch dir: wipe userdata-qemu.img(.qcow2) and the FBE
    # key (encryptionkey.img.qcow2) there, or lock-screen PIN settings
    # survive every "fresh" boot. Stale files in the scratch dir go too.
    mkdir -p "$EMUWORK"
    if [ -z "$KEEPDATA" ]; then
        rm -f "$OUT/userdata-qemu.img" "$OUT/userdata-qemu.img.qcow2"
        rm -f "$OUT/cache.img.qcow2" "$OUT/encryptionkey.img.qcow2"
        rm -f "$EMUWORK"/userdata*.img* "$EMUWORK/cache.img.qcow2" \
              "$EMUWORK/encryptionkey.img.qcow2"
        cp -L "$OUT/userdata.img" "$EMUWORK/userdata.img"
    fi

    # Run the emulator from the scratch copy of prebuilts/android-emulator.
    if [ ! -x "$EMUDIR/emulator" ]; then
        [ -x "$TREE/prebuilts/android-emulator/linux-x86_64/emulator" ] ||
            die "emulator binary missing in both $EMUDIR and the tree — re-run candyos-sync.sh"
        mkdir -p "$EMUDIR"
        cp -a "$TREE/prebuilts/android-emulator/linux-x86_64/." "$EMUDIR/"
    fi

    # KitKat UI Project (P23 QA): the emulator keeps a quickboot snapshot
    # under $OUT/snapshots/ and RESUMES it on launch - the resumed guest
    # serves stale system images and stale app state, silently defeating
    # both the userdata wipe above and any image rebuild. -no-snapshot
    # alone does not stop the resume; delete the snapshot and pass
    # -no-snapshot-load too.
    rm -rf "$OUT/snapshots"
    export DISPLAY="${DISPLAY:-:0}"
    export PATH="$TREE/out/host/linux-x86/bin:$PATH"
    # envsetup.sh locates the tree by walking up from the current directory
    # (and is not nounset-safe), so source it from the tree root, then move
    # to the scratch dir for the launch.
    set +u
    cd "$TREE"
    source build/envsetup.sh >/dev/null ||
        die "sourcing build/envsetup.sh failed — is the tree fully synced?"
    lunch "$TARGET" >/dev/null ||
        die "lunch $TARGET failed — does the tree have a build for the emulator target?"
    set -u
    cd "$EMUDIR"

    nohup setsid ./emulator \
        -gpu swiftshader_indirect \
        -no-snapshot -no-snapshot-load -no-snapshot-save -no-audio \
        -skin 1080x2220 -dpi-device 440 -window-size 1280,1500 \
        > "$LOG" 2>&1 &
    echo "candyos: emulator launching (window on $DISPLAY), log: $LOG"
else
    echo "candyos: emulator already running"
fi

# Wait for the device to register with adb, then for the boot to complete
# (first boot takes several minutes on SwiftShader).
timeout 300 "$ADB" wait-for-device ||
    die "emulator did not register with adb — check $LOG"
echo -n "candyos: waiting for boot"
for _ in $(seq 1 120); do
    b="$("$ADB" -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [ "$b" = "1" ]; then
        echo
        echo "candyos: booted"
        exit 0
    fi
    echo -n "."
    sleep 10
done
die "timed out waiting for boot — check $LOG"
