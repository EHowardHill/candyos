# CandyOS — TASKS

REVISED 2026-10-28 (user direction): this file is now a working tracker
for OPEN WORK ONLY. It previously carried the full per-phase historical
record of P0–P27 (~2400 lines) relocated unchanged from the dev forks,
with markers gone stale — phases implemented then rolled back still
marked [/], closed passes still listed, live-tree caveats recorded
nowhere a sync would surface them. Nothing is lost by this revision:

* this file's own git history (everything up to commit 8fadfe8) is the
  per-item P0–P27 record;
* the forks' tagged history (kitkat/p<n> phase tags on the
  EHowardHill/* read-only mirrors, notably android_frameworks_base
  kitkat-mods) holds the pre-rollback commits;
* docs/architecture.md maps what ships today.

Phase numbers are cumulative and never reused — old numbers stay valid
as references (patch-series subjects, code comments, the CandyOS P<n>
commit convention). Open work starts at P28.

## Goal

Make LineageOS 21 (Android 14) look and behave like stock Android 4.4
KitKat across all system surfaces, with the feature set staying modern,
delivered as patch series + vendored apps + vendor overlays
(docs/architecture.md).

Hard constraint (unchanged): modern apps must keep working. All changes
stay at the visual, resource, and UX-shell layer; nothing alters APIs
or behaviors apps depend on.

## Rules

1. AUTHENTIC, not a theme: port real 4.4 code and assets at the real
   tree path where a donor can run on API 34; never costume-restyle.
2. Never break modern functionality: a surface whose 4.4 donor cannot
   be made modern-functional ships modern on the theme cascade —
   "tacky theme" is the failure mode this rule exists to prevent.
3. Vanilla: defaults are KitKat (Holo, Roboto 2013, Holo blue, 4.4
   sounds). Where 4.4 has no counterpart, keep stock and let the
   cascade dress it. No invented hybrid designs.
4. DeviceDefault is the boundary: Theme.Material apps keep Material;
   Holo ships as Theme.DeviceDefault == Holo. Nothing force-breaks
   third parties.
5. Accept and document: where a port or pixel-match is not achievable,
   record the delta here — never silently approximate.
6. Verification standard: against the DONOR SOURCE TREE (~/kitkat-src)
   plus the canonical Pixel 3a-sized AVD (440dpi class) and the
   audio-enabled AVD for sound. NO 4.4 reference VM (P19: 4.4 cannot
   build on this host). On-device standard: adb screencap pixel
   battery; human eyeball for feel and sub-second effects. AVD-only
   until real hardware exists.
7. Markers: [ ] open, [X] done, [?] blocked on a question, [/] partial.

## Donor sources (~/kitkat-src — local reference only, never committed)

* frameworks-base (android-4.4.4_r1) — core/res themes + fonts, the
  4.4 SystemUI in its original in-tree layout, services. THE donor for
  shade/keyguard work.
* latinime-4.4, settings-4.4, noto-fonts-4.4 (NotoColorEmoji2013).
* Per-app donors: calculator / soundrecorder / music / deskclock /
  calendar-4.4, ex-4.4 (chips + common), colorpicker /
  datetimepicker / timezonepicker / tzsupport / calendar-opt-4.4,
  support-4.4.
* /tmp/opencode/kitkat-donor — sparse old copy (fonts + sounds only);
  ephemeral, prefer ~/kitkat-src.

## Shipped state (2026-10-28 snapshot)

KitKat-shipped: framework Holo rebase, 2013 Roboto, hybrid emoji, 4.4
window animations / dimens / sounds, 4.4 wallpaper + easter egg
(P1/P8/P17); opaque status bar, 4.4 icons, right-hand clock, ticker
(P2/P3/P23); 4.4 navbar glyphs + press glow (P9/P11); KitKat recents
(P5); keyguard colors, 2013 Roboto clock, Holo PIN pad (P6); volume +
power menu restyle (P4/P14); KitKat keyboard forced default (P13);
Settings dark Holo + 4.4 icons (P7/P11.6); app ports Calculator /
SoundRecorder / Music / DeskClock / Calendar (P16); Notes + Reminders
(P24); CandyExperience onboarding + About + Testing menu (P25); boot
animation wordmark (P26); MindTheGapps + Organic Maps (P27); WebUSB
installability inherited (P24 audit).

Stock by direction (P21 rollback, now superseded): notification shade +
quick settings — see Phase 28.

## Phase 28 — KitKat notification shade + QS re-land [ ]

User direction (2026-10-28): the KitKat theme is the new default system
UI style, so the shade + QS come back into scope. This SUPERSEDES the
P21 rollback and the 2026-10-22 scope amendment.

This is a RE-LAND, not a rewrite. The complete implementation — deep
port (P12), wholesale port (P15), donor rebuild (P20), with the 11.x
geometry fixes — was device-verified before the P21 revert and survives
verbatim in the frameworks/base fork history (kitkat-mods, tags
kitkat/p12..p20, commits before the single P21 revert commit). The
per-item record is the old TASKS.md (this file's git history):
12.1–12.6, 15.1–15.4, 20.1–20.10, 11.1/11.2.

P21 happened because the shade "did not feel good" on the user's pass —
so this phase carries the fix-the-feel burden, not just the revert:

- [/] 28.1 Recover the shade/QS commit set from fork history onto the
      candyos frameworks/base series (30 patches today; the rollback is
      0029): 48dp donor header, QQS = notifications-only, two-panel
      flip + gear toggle, donor QS grid + tiles + brightness dialog,
      KitKatPanelFling physics, ticker interplay unchanged (the ticker
      survived P21 and ships today). DONE 2026-10-28: the series was
      rebuilt as a clean 13-patch chain (the P21 rollback patch and the
      old fork-tree tracker commits folded out — see 31.3), replayed on
      the synced upstream revision, and diff-verified against the
      pre-P21 tree state: the shade/QS set + the P23 ticker fix, byte
      for byte. candyos-verify.sh green. PENDING: the 28.5 build shows it
      on the emulator.
- [X] 28.2 Carry the three fixed-in-tree-but-never-rebuilt fixes of
      the last batch: QQS max-tiles check-before-add, drag handle
      above the navbar, applyDonorFlipToMaxExpansionHeight on both
      height writers. All three verified present in the re-landed
      tree (QuickQSPanelController.setTiles, status_bar_expanded.xml
      bottom margin, both mMaxExpansionHeight writers) — they were in
      the pre-P21 commits, so the re-land carries them by
      construction; the 28.5 build re-verifies them on device.
- [ ] 28.3 The P21 feel pass, on the canonical AVD AND the user's
      hands before any marker flips: drag/fling feel, gear/X taps,
      QQS<->QS transitions, brightness dialog open.
- [X] 28.4 Regenerate the series (candyos-save.sh), candyos-verify.sh
      green, architecture.md change-map row updated. 13 patches,
      BASE 993528cf0efb (= current upstream); all six series (incl.
      the two NEW ones from Phase 31) verify clean.
- [ ] 28.5 Full build + emulator battery + USER ACCEPTANCE GATE — the
      P21 lesson: this phase does not close without the user pass.
      Build + emulator run by the user (28.3 and 28.5 markers flip on
      that pass).
- [/] 28.6 QS footer actions flat-donor strip — user 28.3 emulator-pass
      critique: the bottom band (settings/power/user row,
      qs_footer_actions) was stock AOSP's M3 "underSurface" layer —
      material_dynamic_neutral0 pure black, rounded top corners, 4dp
      elevation shadow, alpha delayed to the last 10% of the QS
      expansion (FooterActionsViewModel) — which the P24 flat
      #ff161616 page turned into a black band popping in over the
      gray page (read as a bug; no P12/P15/P20-era patch ever touched
      the row). DONE 2026-10-29 (frameworks/base 0015):
      qs_footer_actions_background flat #ff161616 with square top
      corners (donor band: status_bar_expanded_header /
      notification_header_bg, flat opaque #FF000000 full-bleed from
      the first frame) + footer_actions outlineProvider=none (no
      elevation shadow; the 4dp elevation stays matched to the QS
      scroll view for z-order parity). The stock last-10% fade is
      kept on purpose: band color == panel color makes the surface
      transition invisible, only the glyphs ride the fade; the
      keyguard QS gets the same flat band, matching its #ff161616
      tiles. PENDING: the 28.5 emulator pass.

## Phase 29 — Lock screen [ ]

The keyguard already ships KitKat colors and typography (P6: Holo
bouncer + PIN pad, 2013 Roboto thin clock, 4.4 Lock/Unlock sounds,
monet pinned). Open work:

- [ ] 29.1 Ring-hint geometry port (the 14.13 carry): the modern
      swipe-up pill hint + bottom shortcuts vs the 4.4 unlock-ring
      pattern, donor ~/kitkat-src/frameworks-base keyguard. Was gated
      on shade stability (P20-era ruling); the gate is Phase 28 now —
      do this once 28.5 closes.
- [ ] 29.2 P6 leftovers: PIN container + delete/enter button styling
      review; bouncer over light wallpapers; emergency-button contrast
      on dark SIM states.

## Phase 30 — Settings visual pass [ ]

Settings ships dark Holo with 4.4 icons (P7 + 11.6); the code work is
done. Remaining:

- [ ] 30.1 On-device visual walk: About phone, main list, dialogs,
      dark-mode switch colors, search pill (restyles itself dark via
      the cascade), the modern-only pages' vector-icon hybrid
      (accepted per rule 5).

## Phase 31 — Single-repo migration loose ends [ ]

Live-tree edits that pre-date the candyos patch-series model and are
NOT captured by any series — every sync silently reverts them. Verify
against a freshly synced tree, then either capture as a patch series or
accept-and-document the loss:

- [X] 31.1 vendor/lineage default-wallpaper overlay deletions (the 8
      entries under overlay/common/frameworks/base/core/res — the P8
      root cause; the word "wallpaper" appears nowhere in
      patches/vendor-lineage). Without them, the LOS product RRO
      re-overrides the KitKat framework-res wallpaper on every sync.
      CAPTURED 2026-10-28: committed in the vendor/lineage project and
      regenerated into patches/vendor-lineage (0004, the P28 patch);
      no XML/mk references the drawables. Fixed for every future sync.
- [X] 31.2 packages/modules/Permission GrantPermissions restyle (P14.4:
      Theme.DeviceDefault.Dialog.Alert parent) — not a patched project;
      the runtime-permission dialog reverts to Material light on sync.
      CAPTURED 2026-10-28: the live-tree edit was still present (never
      sync-clobbered since); committed it, added the NEW
      patches/permission series (1 patch) and a CANDYOS_PATCHED entry
      (packages/modules/Permission) to scripts/common.sh so sync/save/
      verify/manage it like the others. Fixed for every future sync.
- [X] 31.3 The frameworks-base series carries the old fork-tree
      TASKS.md bookkeeping (roughly half the 30 patches touch only the
      tracker). docs/phases/TASKS.md is canonical now; fold the
      tracker-only commits out when regenerating the series for P28.
      Cosmetic, but it declutters review. DONE 2026-10-28 with the P28
      series rebuild: the 15 tracker-only patches (TASKS.md /
      kitkat_notes.txt) are gone, the 13 surviving patches are
      code+assets only, the TASKS.md hunks were filtered out of the
      three mixed patches, and the tree no longer contains TASKS.md or
      kitkat_notes.txt at all.

## Phase 32 — Bundled wallpapers [ ]

User direction (2026-10-28): `assets/wallpapers/` is the wallpaper set —
`default.jpg` becomes the system default, every image ships on the
system, and the folder updates often, so a sync/build must always pick
up the current contents. (Phase number reserved: 31 was the last used.)

- [X] 32.1 Default wallpaper: the generated framework overlay
      `vendor/candyos/overlay` overrides framework-res
      `default_wallpaper` (drawable-nodpi + sw600dp + sw720dp buckets)
      with `default.jpg`, registered as the LAST
      `PRODUCT_PACKAGE_OVERLAY` by patches/vendor-lineage/0005 (wins over
      the LineageOS overlays; the LOS RRO's own wallpaper entries were
      already deleted in 31.1). The KitKat 4.4 wallpaper from the
      frameworks/base P8 patch stays as the in-tree fallback.
- [X] 32.2 All images on the system + pickable: CandyOS Experience gains
      a Wallpapers screen (menu entry, Holo grid of thumbnails,
      system + lock apply via WallpaperManager; SET_WALLPAPER +
      platform-signed SET_WALLPAPER_HINTS). The images + the
      string-array that drives the screen are GENERATED app res — folder
      edits never touch app code.
- [X] 32.3 The "every build" plumbing: scripts/candyos-gen-wallpapers.py
      regenerates BOTH generated locations (repo overlay + repo app res)
      from assets/wallpapers/ and pushes them into a standing tree when
      given the tree argument; candyos-sync.sh runs it before the
      vendored copies on every sync (so a sync IS a wallpaper refresh),
      and `vendor/candyos` rides CANDYOS_VENDOR_TREES. Edit the folder,
      re-run sync (or the script alone + rebuild), commit, done.
- [ ] 32.4 On-device QA (rides the user's next build): default wallpaper
      boots (home + lock), the Wallpapers grid renders all 9, each pick
      applies system + lock, default entry applies what the overlay
      ships. NOTE: whether the overlay lands static or as an enforced
      RRO depends on the emulator target's ENFORCE_RRO setting — verify
      the override actually wins in the built image.
- [ ] 32.5 USER ACCEPTANCE (rides 28.5's build + pass).

## Phase 33 — KitKat launcher icons [ ]

User direction (2026-10-29): ALL built-in LineageOS apps use KitKat-styled
icons; `reference/icons/` (the committed 43-PNG KitKat reference set) is
the reference. Source of truth + generator: `reference/icons/`,
`reference/donor-icons/`, `reference/originals/` and
`scripts/candyos-gen-icons.py` (repo app res + the eleven patched projects;
see docs/architecture.md "Launcher icons (P33)").

Already KitKat by construction (no change): the vendored 4.4 ports
(Calculator, Calendar, DeskClock, Music, Sound Recorder) — donor icons,
verified against the reference set. CandyOS Experience keeps its candy
mark (already period-styled; user direction).

- [X] 33.1 Donor-byte icons where a 4.4 donor exists: Settings
      (settings-4.4 ic_launcher_settings, per-density drawables replacing
      the adaptive icon) and the LatinIME keyboard drawer entry
      (latinime-4.4 ic_launcher_keyboard, mdpi..xxhdpi). New patches
      0003/0004 on the settings/latinime series.
- [X] 33.2 Reference-set icons for the stock drawer apps: Contacts
      (People), Dialer (Phone), Messaging, Jelly (Browser), Glimpse
      (Gallery), DocumentsUI (Downloads — Files' 4.4 ancestor), Aperture
      (Camera). Nine NEW one-patch series (patches/contacts dialer
      messaging jelly glimpse documentsui aperture audiofx stk),
      registered in CANDYOS_PATCHED; candyos-save.sh + candyos-verify.sh
      green (all 15 series apply clean).
- [X] 33.3 No-counterpart apps: AudioFX (synthesized Holo-dark EQ-fader
      tile) and Stk (synthesized gray SIM-card tile) — KitKat-family
      geometry (rounded corners, darker border ring, soft shadow), drawn
      by candyos-gen-icons.py.
- [X] 33.4 CandyOS originals re-tiled (Notes, Reminders): brand art kept,
      re-tiled onto the reference family geometry by candyos-gen-icons.py
      (repo apps/ + tree slots; no patch series — vendored apps).
      AMENDED 2026-10-29 (user direction): Notes switched to the
      user-supplied reference/icons/Notes.png (used as-is; non-square
      references auto-cropped to content and square-padded), Reminders
      stays re-tiled. Same pass: candyos-gen-wallpapers.py now takes the
      system-default wallpaper as any single default.<ext> (Phase 32's
      plumbing convention; the strict default.jpg name requirement is
      gone).
- [ ] 33.5 On-device QA (rides the next build): every drawer entry shows
      its KitKat icon at all densities, Settings + keyboard included;
      presigned exceptions documented (CandyLauncher "thelauncher",
      the microG/Aurora APKs, Organic Maps — cannot be restyled without
      breaking signatures; list current as of P34).
- [ ] 33.6 USER ACCEPTANCE (rides the 28.5 build + pass).

## Backlog — deferred by ruling, not scheduled

* Gallery (P16 Tier 3): the 4.4 Gallery2 donor == the app the modern
  tree already ships, so the port is a git-history replacement + JNI
  modernization — a focused batch bigger than Music + DeskClock +
  Calendar combined. Modern Gallery2 rides the cascade until then.
* Camera (P16 Tier 3): camera1 legacy-mode spike; needs a working
  capture HAL (the emulator camera HAL is unreliable for capture) —
  real hardware or a dedicated AVD. Modern Aperture rides the cascade.
* 24.4 (optional polish): a CandyOS-hosted WebUSB installer page on
  the fastboot.js + android-webinstall pattern — not a gap; Lineage
  Flash Tools already installs CandyOS today.

## QA passes riding the next builds

- [ ] P25.3: CandyExperience fresh-wipe on-device QA — Setup flow,
      Wi-Fi pick round-trip, apps disable/restore, nav mode switches,
      About / Menu / Testing menus.
- [ ] P26 build pass: full ROM build + emulator QA of the boot
      animation (26.1–26.4 verified against the build recipe; the AVD
      eyeball rides this).
- [ ] P27.3: full arm64 device build with the bundled Google-services
      tree — manifest_check / artifact-path pass, then Play Store
      login + app install on hardware. (MindTheGapps era; the build part
      rides P34.6 now — see the Phase 34 supersede note.)
- [ ] P24.3: first real-device WebUSB install pass (rides the P27.3
      build).

## Hardware / touch passes (rule 6: AVD-only until hardware)

- [ ] ticker marquee smoothness (14.11), gesture typing (13.2),
      overscroll glow eyeball (14.1), volume slim-rail detail (14.10),
      power-menu row press highlight (P4), recents card feel (P5),
      Music playback + DeskClock alarm-ring (audio AVD), Calendar
      month/day/week + event flows (P16), bouncer-over-wallpaper
      eyeball (= 29.2).
## Phase 34 — microG + Aurora Store replace MindTheGapps [ ]

User direction (2026-10-29): drop the real Google services (MindTheGapps)
and ship the opt-in Play compatibility stack instead — microG Services +
microG Companion + Aurora Store — with **whether it runs at all** being
the user's call: a dedicated OTE step decides at first run, and the
CandyOS Experience menu ("Google services") can flip it anytime after.

- [X] 34.1 Remove MindTheGapps: fetch script pins, gapps-arm64/gapps-arm
      extraction, gen-gapps-mk.py retired, .gitignore/common.sh cleaned,
      docs rewritten (README, architecture, apps, getting-started,
      install-webusb).
- [X] 34.2 Signature spoofing, take two (the documented reason microG was
      originally dropped): frameworks/base P34 patch
      (patches/frameworks-base/0016) declares the hidden
      FAKE_PACKAGE_SIGNATURE (signature|privileged) and spoofs the fixed
      Google certificate for exactly com.google.android.gms +
      com.android.vending when they hold it — CalyxOS approach,
      metadata-independent (the LOS built-in userdebug hook is dead with
      current microG: resource-typed fake-signature meta-data is dropped
      by package parsing), works on user builds too. Whitelist grants
      scoped in vendor/partner_gms/microg (GmsCore: +INSTALL_LOCATION_
      PROVIDER, CHANGE_DEVICE_IDLE_TEMP_WHITELIST, UPDATE_APP_OPS_STATS +
      power-save sysconfig — the LineageOS-for-microG grant set;
      Companion: spoofing only).
- [X] 34.3 vendor/partner_gms/microg: GmsCore + Companion product
      priv-apps, AuroraStore product app, PRESIGNED verbatim installs,
      uses-library sets mirrored from the pinned APKs; products/gms.mk
      unconditional (universal APKs — the emulator QA images now carry
      the whole stack; MindTheGapps was arm/arm64-only).
- [X] 34.4 CandyOS Experience: dedicated OTE "Google services" step
      (after Default apps), fresh-boot defaults pinned disabled by the
      wizard, PlayConfig shared by the step and the new
      PlayServicesActivity ("Google services" menu entry) with
      instant-apply toggles; Aurora moved out of the generic
      optional-apps list into the dedicated step.
- [X] 34.5 Updater integration: microG Services + Companion tracked from
      the shared microg/GmsCore release repo (new TrackedApp.assetFilter
      regex per entry — a release carries -hw/.asc/org.microg.gms assets
      that must not be picked; ETag cache key now per package so two
      entries may share one repo). Aurora NOT tracked (GitLab-only
      releases); its own self-updater covers it, and the pinned APK is
      the upstream-signed build so in-app updates install over the
      preload.
- [ ] 34.6 QA (rides the next build): fresh-wipe OTE pass — play step
      shows both rows unchecked, opt-in enables exactly the checked
      packages, finish leaves microG off when unchecked; menu screen
      round-trip both ways; spoof check — `adb shell dumpsys package
      com.google.android.gms` shows the Google certificate
      (F0:FD:6C:5B:41:0F:25:CB:25:C3:B5:33:46:C8:97:2F:AE:30:F8:EE:74:11:
      DF:91:04:80:AD:6B:2D:60:DB:83), not the NOGAPPS one
      (9B:D0:67:27:...); push notification end-to-end after battery-
      optimization exemption; microG update pass once a newer GmsCore
      release lands (assetFilter picks the right APKs).
- [ ] 34.7 USER ACCEPTANCE (rides 34.6 + the P28.5 build).

Notes / deltas accepted by this phase (rule 5):
* Compatibility narrows vs real GApps: Play Integrity / some banking /
  anti-tamper apps and paid-app licensing stay broken — inherent to
  microG + a stub Play Store, not fixable at the CandyOS layer.
* P27.3 (MindTheGapps arm64 device build + Play login pass) is superseded:
  the equivalent hardware pass is now 34.6 (microG spoof + Aurora
  download on hardware).
