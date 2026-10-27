# Installing CandyOS on a device (WebUSB)

CandyOS installs on physical devices the way LineageOS does, and the most
straightforward path is **WebUSB**: a Chromium-family browser talks to the
device over USB directly, so nothing has to be installed on the host.

The flasher is [Lineage Flash Tools](https://flash.lineageos.org), the LineageOS
project's official WebUSB utility.

Lineage Flash Tools exposes four tabs (ADB for recovery sideload, Fastboot
for bootloader/fastbootd, Samsung Odin, Amlogic burn); a CandyOS install
uses the ADB and Fastboot tabs. Full tool documentation, including
per-platform setup, lives on the LineageOS wiki:
[Using Lineage Flash Tools](https://wiki.lineageos.org/web_flash_tools).

## Requirements

* **A device supported by LineageOS 21**, with an unlockable bootloader.
  CandyOS publishes no device list (yet) — pick from
  <https://wiki.lineageos.org/devices/> and follow that device's LineageOS
  installation guide for everything device-specific: which images to
  flash, A/B and dynamic-partition handling, firmware prerequisites, and
  the exact unlock variant. The web tool replaces the *host tools*, not
  the wiki steps.
* **A browser with WebUSB** — Chrome, Edge, or another Chromium-family
  browser (WebUSB shipped in Chromium 61; Firefox does not implement it —
  see <https://caniuse.com/webusb>). On Windows you may need the
  [OEM USB driver](https://developer.android.com/studio/run/win-usb); on
  Linux, udev rules for adb/fastboot devices
  (`android-sdk-platform-tools-common` on Debian/Ubuntu, `android-udev`
  on Arch), unless your distribution ships systemd v261 or newer.
* **A clean USB path.** Plug the device directly into the computer; skip
  hubs, extension cables, and adapters. Flashing is throughput-heavy and
  marginal cables are the most common failure mode (see the wiki's
  transfer-error notes).
* **Backups.** Unlocking the bootloader wipes all data, and the install
  expects a data wipe on top — this is a normal custom-ROM install.

## Build CandyOS for the device

CandyOS does not distribute device images yet; this flow assumes you
built the package (or received it from whoever maintains CandyOS for your
device). From an already synced tree
([docs/getting-started.md](getting-started.md)):

```
cd ~/android/lineage
source build/envsetup.sh
breakfast <device>
brunch <device>
```

* `brunch` = `breakfast` + `mka bacon` and defaults to a `userdebug`
  build. With MindTheGapps the Google apps carry real signatures, so
  unlike the old microG setup nothing depends on the build being
  debuggable — but keep `userdebug` for development anyway
  (see [vendor/partner_gms/README.md](../vendor/partner_gms/README.md)).
* Artifacts land in `out/target/product/<device>/`:
  * `lineage-21.0-<date>-UNOFFICIAL-<device>.zip` — the flashable package,
    named and formatted exactly like a LineageOS build because it is the
    same build system;
  * the images the device's install guide calls for — `recovery.img`
    (or `boot.img` on A/B devices), `super_empty.img` on retrofitted
    dynamic-partition devices, and so on.
* CandyOS QA is emulator-only so far (TASKS.md rule 6) — a device build is
  the first hardware test of the CandyOS surfaces. File findings in the
  phase tracker.

## Flash from the browser

Open <https://flash.lineageos.org> and follow your device's LineageOS
installation guide, substituting the web tool for every `adb`/`fastboot`
command. In shape:

1. **Prepare the device.** Enable Developer options and OEM unlocking as
   the device guide specifies. (USB debugging is only needed if you want
   to drive reboots from the booted OS through the ADB tab; sideload and
   fastboot modes do not require it.)
2. **Unlock the bootloader.** Boot to bootloader mode, click **Connect**
   on the **Fastboot** tab, and pick the device in the browser's prompt.
   Use **Run command** → `flashing unlock` — or the variant your device's
   guide specifies — and confirm on-screen, exactly as with local
   fastboot.
3. **Install recovery.** Per the device guide, either **Flash image**
   (e.g. the CandyOS `recovery.img` onto the `recovery` partition) or
   **Boot image** to boot it without flashing, with **Wipe super**
   (the device's `super_empty.img`) where the guide calls for it. Then
   reboot into recovery.
4. **Wipe.** Do the guide's format step from recovery (*Factory reset* /
   *Format data*) — always required when arriving from stock or another
   OS.
5. **Sideload CandyOS.** In LineageOS Recovery choose
   *Apply update > Apply from ADB*, then switch to the **ADB** tab, click
   **Connect** again (the device re-enumerates after every reboot — that
   is normal WebUSB behavior), **Choose ZIP** → the CandyOS
   `lineage-21.0-<date>-UNOFFICIAL-<device>.zip`, then **Sideload**.
   If you also have local adb installed, run `adb kill-server` first so
   the daemon does not claim the device from the browser.
6. **Reboot.** When sideloading finishes, reboot to system from the
   recovery menu. First boot takes a few minutes.

## Notes

* **Reconnecting.** Every reboot changes the device's USB identity;
  re-select it with **Connect** on the relevant tab. If the tool reports
  transfer errors, change the cable/port before anything else.
* **Relocking.** Do not relock the bootloader unless the device's
  LineageOS wiki explicitly supports it with custom OSes — relocking
  with a custom OS bricks most devices (the wiki covers the few that
  support it). CandyOS ships `userdebug` and is not relock-intended.
* **Command-line fallback.** The identical steps work with local
  [platform-tools](https://wiki.lineageos.org/adb_fastboot_guide) if a
  browser is not an option; the wiki's per-device guides are written that
  way.
* **A CandyOS-hosted installer** (one page, CandyOS images, GrapheneOS
  web-installer style, on the
  [fastboot.js](https://github.com/kdrag0n/fastboot.js) /
  [android-webinstall](https://github.com/kdrag0n/android-webinstall)
  pattern) is a possible later enhancement once CandyOS publishes device
  releases. Lineage Flash Tools requires no CandyOS infrastructure, so
  today that is polish, not a gap.
* **WebUSB on CandyOS itself.** WebUSB in the installed OS is a
  browser-app capability — Chromium-family browsers support it and can
  be installed through the bundled Play Store; the framework's USB host
  stack is unmodified stock LineageOS. The install flow above is
  independent of this: it runs entirely in the host browser.
