#!/usr/bin/env bash
#
# Usage: candyos-build-avd-zip.sh [--skip-build] [--arm64] [<lineage-tree>]
#   <lineage-tree> defaults to $CANDYOS_ANDROID_TREE (see common.sh).
#   --skip-build packages the existing build without re-running "m".
#   --arm64 packages the arm64 image (for Android Studio on Apple
#     Silicon); the default x86_64 image runs at full speed on typical
#     Intel/AMD/Windows hosts.
#
# Steps:
# 1. Verifies the WebView prebuilt is the real APK, not an unfetched
#    git-lfs pointer (a pointer file kills the build hours in - see
#    docs/getting-started.md Troubleshooting).
# 2. Builds lineage_sdk_phone_<arch>-ap2a-userdebug (m -j8); images land
#    in out/target/product/<product>/.
# 3. Stages the AVD image under SDK system-image naming plus ready-to-copy
#    AVD config templates and a README - DATA ONLY. No scripts or programs
#    ship in the zip: the recipient's AVD setup is fully manual (create a
#    folder, copy/edit two small text files by hand, following the README).
#    The recipient needs only Android Studio (its SDK already ships the
#    emulator), NOT this repo or the tree.
# 4. Zips it to <candyos-repo>/CandyOS-AndroidStudio-<arch>.zip.
#
# Notes:
# - The x86_64 image is the same emu64x build candyos-emulator.sh QAs.
# - The zip lands in the candyos repo directory, NOT out/ - "m clean"
#   wipes out/, and the zip is gitignored (see .gitignore).
# - The AVD is registered by config files the recipient places by hand
#   (the same mechanism as the macOS bundle and as Studio itself):
#   ~/.android/avd/CandyOS.ini points at ~/.android/avd/CandyOS.avd/,
#   whose config.ini points image.sysdir.1 back at the unzipped
#   system-image folder. No avdmanager / SDK writes are needed; the
#   system files stay in the unzipped folder (delete the folder and
#   CandyOS is gone), and Android Studio's Device Manager picks the
#   AVD up on its next scan.

set -euo pipefail
. "$(dirname "$0")/common.sh"

command -v zip >/dev/null 2>&1 ||
    die "zip is required (e.g. apt install zip)"

SKIPBUILD=
ARCH=x86_64
while [ $# -gt 0 ]; do
    case "$1" in
        --skip-build) SKIPBUILD=1 ;;
        --arm64) ARCH=arm64 ;;
        *) break ;;
    esac
    shift
done

case "$ARCH" in
    x86_64)
        TARGET=lineage_sdk_phone_x86_64-ap2a-userdebug
        PRODUCT=emu64x
        ABI=x86_64
        CPU=x86_64
        ;;
    arm64)
        TARGET=lineage_sdk_phone_arm64-ap2a-userdebug
        PRODUCT=emu64a
        ABI=arm64-v8a
        CPU=arm64
        ;;
    *) die "unknown arch: $ARCH" ;;
esac

resolve_tree "${1:-}"

OUT=$TREE/out/target/product/$PRODUCT
WORK=$TREE/out/candyos-avd-zip
BUNDLE=CandyOS-AndroidStudio-$ARCH
STAGE=$WORK/$BUNDLE
IMAGES=$STAGE/system-image
WEBVIEW=$TREE/external/chromium-webview/prebuilt/$ARCH/webview.apk
ZIP=$CANDYOS_ROOT/$BUNDLE.zip

# 1. WebView prebuilt: an unfetched git-lfs pointer is 134 bytes and
# surfaces as a late "not a valid zip file" build failure.
if [ -e "$TREE/external/chromium-webview" ]; then
    if [ "$(stat -c%s "$WEBVIEW" 2>/dev/null || echo 0)" -lt 1000 ]; then
        command -v git-lfs >/dev/null 2>&1 ||
            die "git-lfs is missing - see docs/getting-started.md prerequisites"
        echo "candyos: $ARCH WebView is an LFS pointer - pulling the real APK (once)"
        git -C "$TREE/external/chromium-webview/prebuilt/$ARCH" lfs pull
        [ "$(stat -c%s "$WEBVIEW")" -ge 1000000 ] ||
            die "$ARCH WebView still tiny after 'git lfs pull'"
    fi
fi

# 2. Build the emulator image.
if [ -z "$SKIPBUILD" ]; then
    echo "candyos: building $TARGET (first build: a few hours)"
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

# Android version, for the README and image metadata.
SDK=$(sed -n 's/^ro.build.version.sdk=//p'  "$OUT/system/build.prop" 2>/dev/null | head -n1 || true)
RELEASE=$(sed -n 's/^ro.build.version.release=//p' "$OUT/system/build.prop" 2>/dev/null | head -n1 || true)
SDK=${SDK:-34}
RELEASE=${RELEASE:-14}
case "$ARCH" in
    x86_64) ARCHNOTE=" - runs at full speed on typical
  Windows / Intel / AMD PCs; on Apple Silicon Macs it works but runs
  through CPU emulation, so ask for the arm64 build if you are on a Mac" ;;
    arm64)  ARCHNOTE=" - built for Apple
  Silicon Macs (M1/M2/M3/M4), where it runs at full speed; typical PCs
  need the x86_64 build instead" ;;
esac

# 3. Stage the bundle.
echo "candyos: staging images"
rm -rf "$STAGE"
mkdir -p "$IMAGES"

# The AVD loader expects SDK system-image naming: the tree's *-qemu.img
# files are the same content Google ships as system.img etc. in its
# system-image packages. The kernel is staged under both names the
# loader looks for (64-bit guests probe kernel-ranchu-64, then
# kernel-ranchu).
copy_img() {
    [ -f "$1" ] || die "missing build output: $1"
    cp -L "$1" "$2"
}
copy_img "$OUT/system-qemu.img"       "$IMAGES/system.img"
copy_img "$OUT/vendor-qemu.img"       "$IMAGES/vendor.img"
copy_img "$OUT/product-qemu.img"      "$IMAGES/product.img"
copy_img "$OUT/system_ext-qemu.img"   "$IMAGES/system_ext.img"
copy_img "$OUT/system_dlkm-qemu.img"  "$IMAGES/system_dlkm.img"
copy_img "$OUT/ramdisk-qemu.img"      "$IMAGES/ramdisk.img"
copy_img "$OUT/userdata.img"          "$IMAGES/userdata.img"
copy_img "$OUT/encryptionkey.img"     "$IMAGES/encryptionkey.img"
copy_img "$OUT/kernel-ranchu"         "$IMAGES/kernel-ranchu"
cp -L "$IMAGES/kernel-ranchu" "$IMAGES/kernel-ranchu-64"
copy_img "$OUT/advancedFeatures.ini"         "$IMAGES/advancedFeatures.ini"
copy_img "$OUT/VerifiedBootParams.textproto" "$IMAGES/VerifiedBootParams.textproto"
if [ -f "$OUT/system/build.prop" ]; then
    cp -L "$OUT/system/build.prop" "$IMAGES/build.prop"
fi
cat > "$IMAGES/source.properties" <<EOF
Pkg.Desc=CandyOS system image (Android $RELEASE, API $SDK)
Pkg.Revision=1.0.0
AndroidVersion.ApiLevel=$SDK
EOF

# AVD config templates - DATA ONLY. The recipient copies these by hand
# (following the README) into the two files the AVD loader looks for:
#   ~/.android/avd/CandyOS.avd/config.ini  (from avd-config.txt)
#   ~/.android/avd/CandyOS.ini              (from avd-ini.txt)
# and edits the two marked path lines to where they unzipped the bundle.
cat > "$STAGE/avd-config.txt" <<EOF
avd.ini.encoding=UTF-8
AvdId=CandyOS
tag.display=CandyOS
tag.id=default
abi.type=$ABI
hw.cpu.arch=$CPU
hw.cpu.ncore=4
hw.device.name=pixel_3a
hw.device.manufacturer=Google
hw.ramSize=4096
vm.heapSize=512
hw.lcd.width=1080
hw.lcd.height=2220
hw.lcd.density=440
skin.name=1080x2220
skin.dynamic=yes
showDeviceFrame=no
hw.keyboard=yes
hw.gpu.enabled=yes
hw.gpu.mode=host
disk.dataPartition.size=6G
image.sysdir.1=EDIT-ME: full path of this bundle's system-image folder
EOF

cat > "$STAGE/avd-ini.txt" <<EOF
avd.ini.encoding=UTF-8
path=EDIT-ME: full path of the CandyOS.avd folder inside .android/avd
path.rel=avd/CandyOS.avd
EOF

cat > "$STAGE/READ ME FIRST.txt" <<EOF
CandyOS for Android Studio
===========================

What you need
-------------
- Android Studio (developer.android.com/studio), with its standard
  SDK components - the Android Emulator is included in a default
  install
- About 15 GB of free disk space (once unzipped)
- 8 GB of memory (RAM) or more
- This build is Android $RELEASE (API $SDK) for $ABI CPUs$ARCHNOTE

Setup (once, about 10 minutes)
------------------------------
Throughout, YOURNAME means your user name, and you can unzip this
package anywhere you like (the examples use the Desktop; a short path
like that is safest on Windows). Forward slashes (/) work in these
files on ALL systems - do not use backslashes.

1. Create the AVD folder inside your home folder. In your file
   manager, or in a terminal:

   - Windows:  open a file Explorer, click the address bar, type
              %USERPROFILE%\\.android\\avd   and press Enter. Create
              a NEW folder there named:   CandyOS.avd
              (if .android\\avd does not exist yet, open Android
              Studio once, start creating any dummy virtual device,
              then delete it - the folders get created there)
   - Mac:     in Terminal:   mkdir -p ~/.android/avd/CandyOS.avd
   - Linux:   in a shell:    mkdir -p ~/.android/avd/CandyOS.avd

2. Open "avd-config.txt" (in this folder) in a text editor
   (Notepad / TextEdit / gedit). Near the bottom, edit the line

       image.sysdir.1=EDIT-ME: ...

   to hold the FULL path of this bundle's system-image folder, ending
   with a slash. For example, if you unzipped to the Desktop:

   - Windows:  image.sysdir.1=C:/Users/YOURNAME/Desktop/$BUNDLE/system-image/
   - Mac:     image.sysdir.1=/Users/YOURNAME/Desktop/$BUNDLE/system-image/
   - Linux:   image.sysdir.1=/home/YOURNAME/Desktop/$BUNDLE/system-image/

   Then use File -> Save As and save it inside the CandyOS.avd folder
   you created in step 1, named exactly:

       config.ini

   (Notepad: choose "Save as type: All Files" so it does not add .txt;
    in Notepad's save dialog, type the name in double quotes:
    "config.ini". TextEdit: use Format -> Make Plain Text first.)

3. Open "avd-ini.txt" (in this folder). Edit the line

       path=EDIT-ME: ...

   to hold the FULL path of that same CandyOS.avd folder, WITHOUT a
   trailing slash:

   - Windows:  path=C:/Users/YOURNAME/.android/avd/CandyOS.avd
   - Mac:     path=/Users/YOURNAME/.android/avd/CandyOS.avd
   - Linux:   path=/home/YOURNAME/.android/avd/CandyOS.avd

   Save it in the .android/avd folder (NOT inside CandyOS.avd - one
   level up, next to it), named exactly:

       CandyOS.ini

   (same "All Files" / double-quotes trick in step 2 on Windows)

4. Done! Continue with "How to run it" below.

How to run it
-------------
1. Open Android Studio -> Device Manager (the phone icon).
2. "CandyOS" appears in the device list -> press its Play button.
3. A phone-shaped window appears and boots. The FIRST boot takes a
   few minutes and the window may look frozen - that is normal.
4. Your mouse is the finger. Your keyboard types into the phone.
   Everything you do is saved automatically for next time.

Quitting, resetting, uninstalling
---------------------------------
- Close the phone window to quit.
- To wipe the phone back to its very first boot: quit first, then in
  Device Manager open CandyOS's three-dots menu -> "Wipe Data".
- To uninstall completely: delete this folder, and delete the
  "CandyOS.avd" folder and the "CandyOS.ini" file inside the
  ".android/avd" folder in your home directory.

Trouble?
--------
- CandyOS missing from Device Manager: check the two saved files have
  the exact names (config.ini inside CandyOS.avd; CandyOS.ini next to
  it) and the path lines are correct, then reopen Device Manager.
- "system image missing" or similar: the image.sysdir.1 line in
  config.ini must be the full path of the system-image folder, ending
  in a slash, with forward slashes only.
- Still stuck? Take a screenshot of what you see and send it to the
  friend who sent you this package.
EOF

# 4. Zip it (into the candyos repo dir, not out/ - "m clean" wipes out/).
echo "candyos: zipping (a few minutes)"
rm -f "$ZIP"
( cd "$WORK" && zip -q -r -X "$ZIP" "$BUNDLE" )

echo "candyos: done: $ZIP ($(du -h "$ZIP" | cut -f1))"
echo "candyos: send it to your recipient (Drive/Dropbox - it is too big for"
echo "candyos: email); she needs only Android Studio:"
echo "candyos:   unzip -> follow \"READ ME FIRST.txt\" (data-only setup) -> Device Manager -> CandyOS -> Play"
echo "candyos: the zip is gitignored; the staging copy in out/ is disposable"