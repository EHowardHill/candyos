#!/usr/bin/env bash
#
# Usage: candyos-build-arm64-mac.sh [--skip-build] <lineage-tree>
#   <lineage-tree> defaults to $CANDYOS_ANDROID_TREE (see common.sh).
#   --skip-build repackages the existing emu64a build without re-running
#   the build (for bundle tweaks after one full run).
#
# Steps:
# 1. Verifies the arm64 WebView prebuilt is the real APK, not an unfetched
#    git-lfs pointer (a pointer file kills the build hours in - see
#    docs/getting-started.md Troubleshooting).
# 2. Builds lineage_sdk_phone_arm64-ap2a-userdebug (m -j8); images land in
#    out/target/product/emu64a/.
# 3. Downloads Google's darwin-arm64 emulator package (the tree prebuilts
#    ship only linux-x86_64 and darwin-x86_64), cached in
#    out/candyos-mac-bundle/cache/.
# 4. Stages CandyOS-macOS-Apple-Silicon/ with the emulator images under SDK
#    system-image naming, the emulator, "Run CandyOS.command" /
#    "Reset CandyOS.command" launchers, and a README for the recipient.
# 5. Zips it to out/candyos-mac-bundle/CandyOS-macOS-Apple-Silicon.zip.
#
# Notes:
# - The arm64 image cannot boot on this x86_64 machine (the emulator
#   refuses cross-arch guests), so the bundle's first boot happens on the
#   recipient's Mac; the build is otherwise the same synced tree, patch
#   series and apps as the QA'd emu64x image.
# - out/ is wiped by "m clean" - move the finished zip somewhere safe.

set -euo pipefail
. "$(dirname "$0")/common.sh"

TARGET=lineage_sdk_phone_arm64-ap2a-userdebug
PRODUCT=emu64a
BUNDLE=CandyOS-macOS-Apple-Silicon

SKIPBUILD=
if [ "${1:-}" = "--skip-build" ]; then
    SKIPBUILD=1
    shift
fi

resolve_tree "${1:-}"

OUT=$TREE/out/target/product/$PRODUCT
WORK=$TREE/out/candyos-mac-bundle
STAGE=$WORK/$BUNDLE
IMAGES=$STAGE/system-image
WEBVIEW=$TREE/external/chromium-webview/prebuilt/arm64/webview.apk

for tool in curl unzip zip; do
    command -v "$tool" >/dev/null 2>&1 ||
        die "$tool is required (e.g. apt install $tool)"
done

# 1. Arm64 WebView prebuilt: an unfetched git-lfs pointer is 134 bytes and
#    surfaces as a late "not a valid zip file" build failure.
if [ "$(stat -c%s "$WEBVIEW" 2>/dev/null || echo 0)" -lt 1000 ]; then
    command -v git-lfs >/dev/null 2>&1 ||
        die "git-lfs is missing - see docs/getting-started.md prerequisites"
    echo "candyos: arm64 WebView is an LFS pointer - pulling the real APK (once)"
    git -C "$TREE/external/chromium-webview/prebuilt/arm64" lfs pull
    [ "$(stat -c%s "$WEBVIEW")" -ge 1000000 ] ||
        die "arm64 WebView still tiny after 'git lfs pull'"
fi

# 2. Build the arm64 emulator image.
if [ -z "$SKIPBUILD" ]; then
    echo "candyos: building $TARGET (first arm64 build: a few hours)"
    set +u
    cd "$TREE"
    source build/envsetup.sh >/dev/null
    lunch "$TARGET" >/dev/null
    m -j8
    set -u
else
    echo "candyos: --skip-build: packaging the existing $PRODUCT build"
fi
[ -f "$OUT/system-qemu.img" ] ||
    die "no emulator images in $OUT - build first (see docs/getting-started.md)"

# 3. Google's darwin-arm64 emulator package (from the SDK repository
#    listing; the version there is the current stable).
mkdir -p "$WORK/cache"
EMU_PKG=
for xml in repository2-3.xml repository2-2.xml repository2-1.xml; do
    EMU_PKG=$(curl -fsSL "https://dl.google.com/android/repository/$xml" 2>/dev/null |
        grep -oE 'emulator-darwin_aarch64-[0-9]+\.zip' | sort -Vu | tail -1) || true
    if [ -n "${EMU_PKG:-}" ]; then break; fi
done
[ -n "${EMU_PKG:-}" ] ||
    die "could not find the darwin-arm64 emulator in Google's SDK repository"
CACHE=$WORK/cache/$EMU_PKG
if [ -f "$CACHE" ]; then
    echo "candyos: using cached $CACHE"
else
    echo "candyos: downloading $EMU_PKG"
    curl -fsSL --retry 3 -o "$CACHE.part" "https://dl.google.com/android/repository/$EMU_PKG"
    mv "$CACHE.part" "$CACHE"
fi
unzip -tq "$CACHE" >/dev/null ||
    die "the emulator package failed its integrity check"

# 4. Stage the bundle.
rm -rf "$STAGE"
mkdir -p "$STAGE/emulator" "$IMAGES"
unzip -q -o "$CACHE" -d "$STAGE/emulator"
if [ ! -x "$STAGE/emulator/emulator" ]; then
    inner=$(find "$STAGE/emulator" -mindepth 2 -maxdepth 2 -type f -name emulator -print -quit)
    [ -n "$inner" ] || die "unexpected emulator package layout - no emulator binary"
    mv "$(dirname "$inner")"/* "$STAGE/emulator/"
    rmdir "$(dirname "$inner")"
fi

# The AVD loader expects SDK system-image naming: the tree's *-qemu.img
# files are the same content Google ships as system.img etc. in its
# system-image packages. The kernel is staged under both names the loader
# looks for (64-bit guests probe kernel-ranchu-64, then kernel-ranchu).
copy_img() {
    [ -f "$1" ] || die "missing build output: $1"
    cp -L "$1" "$2"
}
echo "candyos: staging images"
copy_img "$OUT/system-qemu.img"       "$IMAGES/system.img"
copy_img "$OUT/vendor-qemu.img"       "$IMAGES/vendor.img"
copy_img "$OUT/product-qemu.img"      "$IMAGES/product.img"
copy_img "$OUT/system_ext-qemu.img"   "$IMAGES/system_ext.img"
copy_img "$OUT/system_dlkm-qemu.img"  "$IMAGES/system_dlkm.img"
copy_img "$OUT/ramdisk-qemu.img"     "$IMAGES/ramdisk.img"
copy_img "$OUT/userdata.img"          "$IMAGES/userdata.img"
copy_img "$OUT/encryptionkey.img"     "$IMAGES/encryptionkey.img"
copy_img "$OUT/kernel-ranchu"         "$IMAGES/kernel-ranchu"
cp -L "$IMAGES/kernel-ranchu" "$IMAGES/kernel-ranchu-64"
copy_img "$OUT/advancedFeatures.ini"         "$IMAGES/advancedFeatures.ini"
copy_img "$OUT/VerifiedBootParams.textproto" "$IMAGES/VerifiedBootParams.textproto"
if [ -f "$OUT/system/build.prop" ]; then
    cp -L "$OUT/system/build.prop" "$IMAGES/build.prop"
fi

cat > "$STAGE/Run CandyOS.command" <<'EOF'
#!/bin/bash
# CandyOS launcher - double-click me.
echo "Starting CandyOS..."
if [ "$(uname -m)" != "arm64" ]; then
    echo "This package is for Apple Silicon Macs (M1/M2/M3/M4)."
    echo "Ask your CandyOS friend for the right version for this Mac."
    read -p "Press Enter to close..."
    exit 1
fi
cd "$(dirname "$0")" || exit 1
if [ ! -x "emulator/emulator" ]; then
    echo "CandyOS is missing pieces - the unzip may not have finished."
    echo "Try unzipping again, then run me once more."
    read -p "Press Enter to close..."
    exit 1
fi

# AVD setup: rewritten on every launch, so the package can be moved
# freely; the phone's data lives in ~/.android/avd/CandyOS.avd and is
# kept between runs (Reset CandyOS.command wipes it).
AVD="$HOME/.android/avd"
IMAGES="$(cd system-image && pwd)"
mkdir -p "$AVD/CandyOS.avd"
{
    echo "avd.ini.encoding=UTF-8"
    echo "AvdId=CandyOS"
    echo "tag.display=CandyOS"
    echo "tag.id=default"
    echo "abi.type=arm64-v8a"
    echo "hw.cpu.arch=arm64"
    echo "hw.cpu.ncore=4"
    echo "hw.device.name=pixel_3a"
    echo "hw.ramSize=4096"
    echo "vm.heapSize=512"
    echo "hw.lcd.width=1080"
    echo "hw.lcd.height=2220"
    echo "hw.lcd.density=440"
    echo "skin.name=1080x2220"
    echo "skin.dynamic=yes"
    echo "showDeviceFrame=no"
    echo "hw.keyboard=yes"
    echo "hw.gpu.enabled=yes"
    echo "hw.gpu.mode=host"
    echo "disk.dataPartition.size=6G"
    echo "image.sysdir.1=$IMAGES/"
} > "$AVD/CandyOS.avd/config.ini"
{
    echo "avd.ini.encoding=UTF-8"
    echo "path=$AVD/CandyOS.avd"
    echo "path.rel=avd/CandyOS.avd"
} > "$AVD/CandyOS.ini"

echo "First boot takes a few minutes; the window may look frozen - normal."
echo "Keep this Terminal window open while CandyOS runs (minimizing is fine)."
exec "./emulator/emulator" -avd CandyOS \
    -no-snapshot -no-snapshot-load -no-snapshot-save \
    -skin 1080x2220 -dpi-device 440 -gpu host
EOF
chmod +x "$STAGE/Run CandyOS.command"

cat > "$STAGE/Reset CandyOS.command" <<'EOF'
#!/bin/bash
# Resets CandyOS to a clean first boot (erases all settings and apps).
if pgrep -f qemu-system >/dev/null 2>&1; then
    echo "CandyOS is still running - close its window first, then try again."
    read -p "Press Enter to close..."
    exit 1
fi
AVD="$HOME/.android/avd/CandyOS.avd"
if [ -d "$AVD" ]; then
    rm -f "$AVD"/userdata-qemu.img "$AVD"/*.qcow2 "$AVD"/encryptionkey.img
    rm -rf "$AVD/snapshots"
    echo "CandyOS is reset - the next launch starts from its very first boot."
else
    echo "Nothing to reset - CandyOS has not been run yet."
fi
read -p "Press Enter to close..."
EOF
chmod +x "$STAGE/Reset CandyOS.command"

cat > "$STAGE/READ ME FIRST.txt" <<'EOF'
CandyOS for Apple Silicon Macs
==============================

Your friend built a whole Android phone system ("CandyOS"), and this
package runs it in a window on your Mac. Nothing is installed - the
whole system lives in this folder. Delete the folder and it is gone.

What you need
-------------
- An Apple Silicon Mac (M1, M2, M3, or M4) with macOS 11 or newer
- About 15 GB of free disk space
- 8 GB of memory (RAM) or more

How to run it
-------------
1. Double-click "Run CandyOS.command".
   macOS may stop you the first time because your friend built this and
   it is not from the App Store. If that happens, right-click the file
   and choose "Open", then "Open" again. On newer macOS versions, open
   System Settings -> Privacy & Security, scroll down, and click
   "Open Anyway".
2. A phone-shaped window appears and boots. The FIRST boot takes a few
   minutes and the window may look frozen - that is normal.
3. Your mouse is the finger. Your keyboard types into the phone.
   Everything you do is saved automatically for next time.

Quitting and starting over
--------------------------
- Close the CandyOS window to quit (the Terminal window can close too).
- To wipe the phone back to its very first boot, double-click
  "Reset CandyOS.command".

Trouble?
--------
- If nothing appears, just double-click "Run CandyOS.command" again.
- Still stuck? Take a screenshot and send it to the friend who sent
  you this package.
EOF

# 5. Zip it.
echo "candyos: zipping (a few minutes)"
rm -f "$WORK/$BUNDLE.zip"
( cd "$WORK" && zip -q -r -X "$BUNDLE.zip" "$BUNDLE" )

echo "candyos: done: $WORK/$BUNDLE.zip ($(du -h "$WORK/$BUNDLE.zip" | cut -f1))"
echo "candyos: upload it to Google Drive (or Dropbox) and send her the link"
echo "candyos: she: unzip -> Run CandyOS.command (right-click -> Open the first time)"
echo "candyos: move the zip out of out/ - a clean build wipes it"
