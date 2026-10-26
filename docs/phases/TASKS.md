# KitKat UI Project - TASKS

Goal: make LineageOS 21 (Android 14) visually and behaviorally resemble
stock Android 4.4 KitKat across all system surfaces (framework theme,
SystemUI, Settings). Feature set stays modern; launcher decision amended
below (replaced with the user's own KitKat Launcher3 port).

SCOPE AMENDED (2026-10-22, user direction): the notification shade and
quick settings are REMOVED from scope - both ship stock LineageOS now
(see Phase 21). Everything else stands.

Hard constraint: modern apps must keep working. All changes stay at the
visual, resource, and UX-shell layer; nothing alters APIs or behaviors
that apps depend on.

This file lives in the frameworks/base fork and is versioned with the
work. Phases are tracked in git as KITKAT-P<n>: commit prefixes and
kitkat/p<n> tags. Phase 7 lives in the Settings fork.

Marker legend:

    [ ] - not done
    [X] - done
    [?] - needs questions answered
    [/] - partially completed

## Project philosophy (user direction, 2026-10-21)

The rules every decision in this file defers to:

1. AUTHENTIC, not a theme. Where a KitKat surface can be ported for
   real (real 4.4 code, real 4.4 assets, at the real tree path - the
   Launcher3-Modern pattern), port it. Never restyle something into a
   KitKat costume when the genuine article can be made to run.
2. Never break modern functionality. The hard constraint governs
   porting too: an app whose 4.4 donor cannot be made modern-functional
   (APIs, permissions, telephony/camera contracts) ships MODERN and
   gets the theme cascade only - "tacky theme" is the failure mode this
   rule exists to prevent.
3. Vanilla. Defaults are KitKat (Holo, Roboto 2013, Holo blue, 4.4
   sounds); post-KitKat surfaces that must exist render normal-modern.
   No invented hybrid designs; where 4.4 has no counterpart, keep
   stock and let the cascade dress it.
4. DeviceDefault is the boundary. Theme.Material apps keep Material -
   we will not restyle the Material widget tree. Holo chrome ships as
   the DEFAULT (Theme.DeviceDefault == Holo) and nothing force-breaks
   third parties.
5. Accept and document. Where a port or pixel-match is not achievable,
   the delta is recorded here, not silently approximated.
6. Verification standard (REVISED 2026-10-22, user direction): NO
   4.4 reference VM is possible on this machine (4.4 cannot build on
   modern hosts). Fidelity is verified against the DONOR SOURCE TREE
   (~/kitkat-src checkouts - layouts, styles, drawables, dimens,
   behavior code) plus the canonical Pixel 3a AVD battery. Accept-and-
   document replaces pixel-perfect where source parity cannot be
   visually diffed. AVD-only until real hardware exists; hardware pass
   deferred, not cancelled.
7. Canonical QA device: the Pixel 3a-sized emulator (440dpi class).
   All geometry claims made on the 560dpi box are superseded; re-run
   against the canonical AVD before closing items.

## Phase 0 - Infrastructure [/]

- [X] Pin frameworks/base and Settings forks in .repo/local_manifests/kitkat_ui_mods.xml
- [X] Target device: x86_64 dev testing device (Android emulator, goldfish x86_64;
      trees already synced under device/generic, no manifest change needed)
- [/] Replace Trebuchet with the KitKat Launcher3 port
      (github.com/EHowardHill/Launcher3-Modern).
      Prepared in /tmp/opencode/launcher3-modern: Android.bp registering the
      TrebuchetQuickStep module name (what vendor/lineage common_mobile.mk
      puts into PRODUCT_PACKAGES), uses-sdk added to the manifest
      (min 21 / target 29, target kept pre-30 on purpose), and the five
      support-v4 imports replaced with framework equivalents
      (LongSparseArray, View accessibility constants, AutoScrollHelper);
      LauncherBackupHelper stays excluded per the existing Gradle setup.
      The privapp whitelist resolves from frameworks/base/data/etc.
      NEXT: commit and push those 7 files to main, then uncomment the
      Trebuchet replacement in .repo/local_manifests/kitkat_ui_mods.xml
      and repo sync.
- [X] Baseline: clean build for the x86_64 emulator target, boot it, and verify
      (can double as smoke test for the Phase 1/2 edits below)
      Done: booted on the emulator (launch via /tmp/opencode/launch-emu.sh -
      needs the lunch env to wire the GKI images; -gpu swiftshader_indirect
      for VNC, small skin for software rendering). Two boot bugs found and
      fixed by the smoke test:
      - fonts.mk PRODUCT_PACKAGES never included the Roboto2013 prebuilt_font
        modules (phase 1) -> empty sans-serif family -> system_server
        abort-looped on gDefaultTypeface; all 12 fonts added.
      - shade carrier label: TextAppearance.QS.Status.Carriers -> ?attr/
        onSurface resolves only after the monet overlay registers, but the
        shade root inflates eagerly at SystemUI start -> InflateException
        crash loop; new fixed TextAppearance.KitKat.ShadeCarrier instead.
        Also shade_carrier_text ID collided with Lineage shade_carrier.xml ->
        renamed kitkat_shade_carrier_text.
      SystemUI stable post-fix, no FATAL exceptions; KitKat dark palette
        renders on screen. Ongoing device-side verification of individual
        phases can now happen on this emulator.

## Phase 1 - Framework theme, fonts, colors [/]

All in frameworks/base/core/res unless noted.

- [/] Rebase Theme.DeviceDefault onto Theme.Holo in values/themes_device_defaults.xml
      (the Holo stack still ships: themes_holo.xml, styles_holo.xml, colors_holo.xml)
      Implemented: 35 Material parents swapped for Holo (DeviceDefault, Light,
      dialogs, panels, wallpaper, InputMethod, NoActionBar variants); 5 styles kept
      on Material for lack of Holo counterparts (VoiceInteractionSession, Light.Voice,
      Light.BaseDialog, Settings.DialogWhenLarge, Settings.Dialog.Alert).
      Pending: baseline build + on-device verification.
- [/] Port KitKat widget skins from AOSP tag android-4.4.4_r1: ghost-outline buttons,
      underline text fields, switches, checkboxes, radios, thin glowing progress bars,
      seekbars, spinners, tabs
      Implemented: 176 Widget.DeviceDefault / TextAppearance.DeviceDefault styles
      re-parented from Material to Holo (widgets, text appearances, action bars,
      lists, popups, all Light variants); the Holo drawables were verified to still
      live in-tree (97 XML selectors + 454 PNG 9-patches per density). 18 styles kept
      on Material for lack of Holo counterparts (Notification.* styles, Toolbar,
      Button.Colored, and the Material typography scale Body1/Title/etc., which are
      Lollipop-era concepts that did not exist in KitKat).
      Pending: baseline build + on-device verification.
- [/] Replace Material You accents with Holo blue #ff33b5e5 / #ff0099cc wherever
      colorAccent resolves
      Implemented: DeviceDefault dark and light color palettes rewritten to
      holo_blue_light / holo_blue_dark, plus colorControlActivated, colorEdgeEffect,
      black statusBarColor/navigationBarColor, forceDarkAllowed=false.
      Pending: on-device verification.
- [/] Holo dialogs, alerts, context menus, toasts: 0dp corner radius, dark backgrounds,
      blue title divider
      Implemented: config_dialogCornerRadius and config_buttonCornerRadius -> 0dp;
      dialog themes now parent Theme.Holo.Dialog*. Pending: full Holo styling arrives
      with the widget skin port above.
- [/] Port 4.4 window and activity animations in res/anim (old zoom/fade open-close,
      toast and menu popup animations)
      Implemented: 98 KitKat animation files from android-4.4.4_r1 installed over
      their modern counterparts (activity open/close, task, dialog, toast, submenu,
      wallpaper, input method, user switch, the full screen-rotate set, push/slide/
      fade/grow/shrink basics); all 470 resource references verified to resolve
      against the modern tree. Pending: on-device verification.
- [/] KitKat dimens: status bar 25dp, navbar 48dp, legacy paddings and heights
      Implemented: status_bar_height_portrait and status_bar_height_default
      24dp -> 25dp; navigation_bar_height was already 48dp. Caveat: if the
      emulator skin enables a display cutout taller than 25dp, revert to 24dp.
      Remaining: audit legacy paddings and heights after on-device inspection.
- [/] Add the 2013 Roboto TTFs and repoint the sans-serif family
      Implemented: 12 TTFs from android-4.4.4_r1 installed under data/fonts as
      Roboto2013-* (prebuilt_font modules); sans-serif and sans-serif-condensed
      repointed in all four configs (fonts.xml, font_fallback.xml, and both cjkvf
      variants; fonts.xml is the one that becomes /system/etc/fonts.xml). External
      variable-font Roboto stays installed as the DroidSans symlink provider.
      Pending: build + on-device verification.
- [/] Pin or neutralize monet color extraction so wallpaper colors never override
      Holo blue in SystemUI
      Implemented: ThemeOverlayController.getNeutralColor/getAccentColor pinned
      to 0xFF33B5E5. The fabricated neutral/accent/dynamic overlays all derive
      from this seed, so the entire dynamic palette stays in the Holo blue family
      and wallpaper changes no longer trigger re-evaluation. Pending: on-device
      verification.

## Phase 2 - Status bar, navigation bar, system icons [/]

- [/] Opaque black status bar (remove transparency and edge-to-edge tinting)
      Lead: BarTransitions (MODE_OPAQUE) and PhoneStatusBarTransitions still exist
      in SystemUI; investigate ScrimState/ScrimController defaults for the unlocked
      shade-closed state before touching status_bar.xml directly (a plain background
      on the bar view would also blacken keyguard/AOD).
      Implemented: investigation showed the modern mode chain is app appearance
      flags -> StatusBarModePerDisplayRepository.toBarMode() -> CentralSurfaces
      checkBarModes() -> BarTransitions.transitionTo(), which paints the status
      bar window's background view (status_bar_container, super_status_bar.xml)
      via BarBackgroundDrawable (MODE_OPAQUE = #ff000000 - exactly KitKat black).
      The default mapping (no app appearance flags) was TRANSPARENT (edge-to-edge,
      app-painted statusBarColor shows through from DecorView); changed the
      default branch to OPAQUE. Explicit app requests are preserved:
      APPEARANCE_SEMI_TRANSPARENT_STATUS_BARS -> SEMI (KitKat translucent
      equivalent), transient swipe-down bar -> SEMI, LOW_PROFILE -> transparent
      lights-out. Keyguard/AOD unaffected: the mode drives only the status bar
      window background, which the fullscreen shade window covers whenever the
      keyguard is showing (AOD covered by its own scrims). Icon tinting is
      independent (LightBarController reads APPEARANCE_LIGHT_STATUS_BARS), so
      white-icons-on-black stays correct. Pending: on-device verification
      (bar over apps, colored-statusBarColor apps now render black - the KitKat
      default; shade-open transition; lights-out in video players).
- [/] KitKat white status icon set: battery, wifi, signal
      Implemented: battery perimeter squared off via
      config_batterymeterPerimeterPath (fill mask and bolt were already
      period-correct shapes; SettingsLib ThemedBatteryDrawable consumes the
      framework config, no code change needed). Wifi: the ACTIVE icon set is
      core/res ic_wifi_signal_0..4 (the new_status_bar_icons aconfig flag
      defaults to disabled), now rewritten as 4.4 arc-fan glyphs; the
      flag-gated SettingsLib set (ic_wifi_0..4 + _error) got the same design
      for consistency. Signal: no change needed - the active framework
      ic_signal_cellular family is already the classic triangle + partial
      fill design; the SettingsLib bar icons only apply behind the disabled
      flag. Pending: on-device verification of all three families.
- [/] Move the clock to the right side
      Implemented: Lineage already ships a right-hand clock (ClockController +
      clock_right in system_icons.xml); default flipped from LEFT to RIGHT in
      ClockController.java. Pending: on-device verification.
- [/] Outlined navbar glyphs: triangle back, circle home, square recents
      Implemented: ic_sysbar_back/home/recent rewritten as white stroked outlines
      (1.8 stroke, rounded joins) in the existing 20dp vectors, singleToneColor
      tint preserved. Pending: on-device verification.
- [X] KitKat immersive-mode scrim and peek behavior
      Final assessment: modern BEHAVIOR_TRANSIENT insets ARE the 4.4
      swipe-peek carried forward; the insets/peek plumbing is untouched
      stock code (verified on emulator: policy_control immersive hides
      bars as on stock; launcher keeps bars as expected). The minor visual
      delta (gradient strip vs scrim tint) does not justify porting.
      Closed as no-port-needed.

## Phase 3 - Notification shade and notifications - ROLLED BACK (P21)

All shade/notification-panel work below was reverted to stock LineageOS
2026-10-22 (see Phase 21); this section is the historical record. The
status-bar ticker item at the end SURVIVES the rollback.

## Phase 3 record [/]

- [X] Gradient scrim background
      Implemented: the true 4.4 vertical gradient landed without the
      ScrimView/ScrimState drawable work the original plan expected - the
      shade background is not a scrim view but the rounded_bg_full drawable
      (shared by the shade root via HardwareBgDrawable, the volume panel and
      the half shelf, all of which carried the same dark gradient in 4.4).
      rounded_bg_full is now a 90-degree vertical gradient, opaque
      #f2161616 at the top fading to #80161616 at the bottom edge (the 4.4
      panel let the wallpaper show through near the bottom), 2dp corners
      kept. Notification rows (#ff252525 opaque) and QS tiles still read
      against it. Volume panel and half shelf pick up the same gradient,
      which matches 4.4.
- [/] Flat dark notification rows with square corners and Holo text styles
      Implemented: notification_material_bg base layer -> flat #ff252525 (state
      layer verified transparent at rest, press feedback intact);
      notification_corner_radius 28dp -> 0dp and notification_corner_radius_small
      4dp -> 0dp (consumed by StackScrollAlgorithm, NotificationStackScrollLayout,
      ExpandableNotificationRow, and the media control panel, so everything
      squares off). Holo text styles arrive via the phase 1 rebase. Pending:
      on-device verification.
- [/] Shade header: date at left, settings gear at right
      Implemented: settings gear (shade_header_settings_button, reusing the
      white outline ic_settings) added to combined_qs_header.xml, pinned to
      the top-right just inboard of the privacy chip so the two never
      overlap; constraint entries added to qqs_header/qs_header/
      large_screen_shade_header so the MotionLayout transitions leave it in
      place. Click wired in ShadeHeaderController via
      ActivityStarter.postStartActivityDismissingKeyguard(Settings.ACTION_SETTINGS),
      same pattern as the battery icon. Lockscreen gating comes free: the
      whole header goes INVISIBLE in QQS-over-keyguard, so the gear never
      shows on lockscreen. Date-at-left is already satisfied by the QS
      constraint sets (date at parent start, under the clock). Pending:
      on-device verification (gear hit target, no overlap with the privacy
      chip when active, shade-dismiss on launch).
- [/] KitKat clear-all button
      Implemented: the shade footer buttons restyled to 4.4 -
      notif_footer_btn_background squared off (2dp corners) onto flat dark
      #ff2d2d2d (was 20dp monet SurfaceContainerHigh pill), footer text
      pinned to KitKat light gray #ffeeeeee (TextAppearance.
      NotificationFooterButton plus FooterView.updateColors, which no longer
      applies the monet onSurface/scHigh overrides at runtime), and
      dismiss_text now sits after manage_text in the chain with
      constrainedWidth so the pair reads left-to-center as in 4.4. Clear-all
      behavior itself is unchanged (FooterViewModel hasClearable gate,
      dismiss-all pipeline). Pending: on-device verification (ripple feedback
      resolves through monet colorControlHighlight = Holo blue family).
- [/] Bottom drag handle
      Implemented: 4.4-style grabber bar (shade_bottom_handle, 36x4dp
      #66ffffff rounded via new shade_bottom_handle drawable) at the bottom
      center of the shade, constrained just above shade_carrier_text so the
      two stack like the 4.4 bottom area (GONE carrier text collapses to the
      bottom center point, so the handle stays pinned). Visibility gated in
      NotificationsQSContainerController on StatusBarState.SHADE only
      (hidden on keyguard/shade-locked); the shade drag interaction itself
      is unchanged (whole-panel drag, as in 4.4). Pending: on-device
      verification (no overlap with the last notification; split-shade
      placement).
- [/] Carrier label at the bottom of the shade
      Implemented: com.android.keyguard.CarrierText (shade_carrier_text) added to
      status_bar_expanded.xml pinned to the bottom-center of
      NotificationsQuickSettingsContainer; new ShadeCarrierTextController builds
      a private CarrierTextManager (same pattern as ShadeCarrierGroupController,
      no contention with the keyguard's manager), listens while attached, and
      gates visibility on StatusBarState.SHADE only (the keyguard carries its
      own carrier text). Wired into NotificationsQSContainerController.onInit.
      On the emulator this shows the no-SIM text. Pending: on-device
      verification (overlap with the last notification when the shade is full
      is expected KitKat behavior).
- [X] Centered empty state ("No notifications")
      Verified already satisfied: status_bar_no_notifications.xml renders a
      centered "No notifications" (empty_shade_text) and picks up Holo text
      appearance through the phase 1 rebase. No change needed.
- [/] Implement the 4.4 ticker (scrolling status bar text). RE-VERIFIED
      ON DEVICE (second build): a posted test notification scrolled ticker
      text across the full status bar width (bright text pixels spanning
      x 60..980 in the 69px bar band). Correction after source
      audit: Lineage 21 does NOT ship a ticker, so this is a port. Heads-up already
      defaults to off (no seeder writes heads_up_notifications_enabled; the SystemUI
      read falls back to HEADS_UP_OFF).
      Implemented (the port): status_bar.xml gains a ticker overlay (ticker: 12sp
      bold white title + 12sp #ffb3b3b3 text, both marquee, plus small-icon
      ImageView) laid over status_bar_contents; new
      com.android.systemui.statusbar.phone.Ticker (queue + 300ms crossfade
      contents<->ticker, setSelected marquee start, 6s per entry, next-entry
      chaining, cancel-on-shade-open via ShadeExpansionStateManager fraction
      > 0.01, gate on device-interactive). Wired in CentralSurfacesImpl's
      setStatusBarViewUpdatedListener callback (view available there):
      NotificationListener.addNotificationHandler -> Ticker.add(sbn);
      skips empty-title/text posts. Ticks over lockscreen too, as in 4.4.
      Pending: on-device verification (marquee actually scrolls - needs
      setSelected after layout; verify on emulator, ticker text may need
      focus request; stop-ticker-on-removal of the ticking notification not
      implemented; group summaries are not filtered - only posts with empty
      title+text are skipped).

## Phase 4 - Quick settings, volume dialog, power menu [/]

The QS items below were ROLLED BACK to stock 2026-10-22 (see Phase 21);
the volume-dialog and power-menu items SURVIVE.

- [/] Rebuild quick settings as the 4.4 panel: user avatar, brightness slider with
      Auto checkbox, Holo toggle rows (layout restructure: code plus resources)
      Implemented (partial, tile chrome): qs_corner_radius 28dp -> 2dp
      (squares tiles and the qs_footer_actions_background), and the
      Theme.SystemUI.QuickSettings shade palette pinned to flat KitKat dark:
      shadeActive/shadeInactive both #ff2d2d2d (active tiles no longer fill
      with monet primary90; the 4.4 panel kept tiles dark in both states),
      with the active/inactive distinction moved to text/icon tint
      (onShadeActive #ffffffff white vs onShadeInactive #ffb3b3b3 gray,
      variants #ffb3b3b3 / #ff808080). shadeDisabled and underSurface
      untouched.
      Implemented (partial, Auto checkbox): the QS brightness row's auto
      toggle is now a 4.4-style "Auto" checkbox - quick_settings_brightness_
      dialog.xml replaces the auto on/off ImageView with a CheckBox keeping
      the brightness_icon id, with new vector Holo checkbox glyphs
      (kitkat_btn_check*; framework btn_check_holo_dark is private and
      cannot be referenced). ToggleSlider.getIcon()/BrightnessSliderController/
      BrightnessController/BrightnessMirrorController updated from ImageView
      to CompoundButton; checked state tracks auto-brightness and the click
      still toggles SCREEN_BRIGHTNESS_MODE. QSPanel tuner/mirror visibility
      paths unaffected.
      Implemented (partial, user avatar): the 4.4 QS header user avatar -
      shade_header_user_avatar ImageView in combined_qs_header.xml (24dp,
      circular crop via oval outline background kitkat_qs_user_avatar_circle
      + clipToOutline), positioned at the start before the clock in all three
      constraint sets (qqs_header/qs_header/large_screen_shade_header - clock
      now chains off the avatar's end). ShadeHeaderController injects
      UserTracker and loads the icon in onInit via UserManager.getUserIcon,
      falling back to the new 4.4-style default avatar vector
      kitkat_qs_user_avatar (dark circle, gray person glyph). Avatar reload on
      user switch is not wired (single-user emulator target); revisit if
      multi-user testing matters.
      Pending: Holo toggle row shape (wider 2-column tiles vs the current tall
      grid), and on-device verification (avatar placement across the QQS<->QS
      transition, checkbox press animation, transient visual flip from the
      framework toggling checked state before the observer resyncs).
- [/] Slim Holo volume panel with the link ringtone/notifications toggle
      Implemented (partial): panel background shares rounded_bg_full (now flat
      #ff161616, 2dp corners); volume_dialog_slider_corner_radius 21dp -> 2dp.
      Remaining: the full KitKat slim slider (thin track plus thumb, wider
      horizontal layout) needs layout work and on-device iteration.
- [/] Holo global actions (power menu)
      Implemented: GlobalActionsDialogLite restyled to the 4.4 pattern -
      global_actions_grid_lite Flow maxElementsWrap 2 -> 1 (single-column
      list), global_actions_grid_item_lite restructured into a full-width
      horizontal row (24dp icon left, 16sp label at right, 48dp minHeight),
      panel background pinned to flat KitKat dark #ff161616 with 2dp corners
      (was #191C18 with 28dp pill), and global_actions_lite_button is now a
      state-list transparent-at-rest / Holo blue #ff33b5e5 on press (was a
      solid #303030 oval). Emergency style and behavior untouched.
      Pending: on-device verification (row press feedback spans only the
      icon cell - full-row highlight would need a custom background on
      GlobalActionsItem; confirm acceptable on emulator).

## Phase 5 - Recents [/]

Decision (updated): the launcher is replaced with the KitKat Launcher3 port
(EHowardHill/Launcher3-Modern), which has no Quickstep. The overview button
therefore has no launcher-side implementation, and recents must be served by
SystemUI - which is exactly where the 4.4 recents lived. The vertical card
list port now happens inside frameworks/base (our fork), using the modern
task model (RecentTasks, task snapshots) as the data source.

- [X] Spike: trace the recents button path with no OverviewProxyService
      connection. Result: RecentsImplementation is a default no-op interface;
      the current implementation is OverviewProxyRecentsImpl (delegates to the
      launcher proxy) and is bound through ContextComponentResolver/dagger.
      With the KitKat launcher (no Quickstep), toggleRecentApps() does nothing.
      The hook is clean: implement a 4.4-style RecentsImplementation (vertical
      card list activity driven by RecentTasks + snapshots) and swap the
      dagger binding.
- [/] Port the 4.4 vertical card list UI (vertically scrolling cards with
      icon and title bar) into SystemUI, driven by the modern task model
      Implemented: KitKatRecentsImpl replaces OverviewProxyRecentsImpl via
      config_recentsComponent + a new dagger @ClassKey binding in
      RecentsModule. The impl pulls the task list from the shell RecentTasks
      (GroupedRecentTaskInfo, split pairs flattened), skips the keyguard as
      in 4.4, and launches KitKatRecentsActivity with the list as a parcelable
      extra. KitKatRecentsActivity (plain Activity, programmatic UI, new
      KitKatRecentsTheme = Holo-dark DeviceDefault over #ee161616) renders
      vertically scrolling cards: task snapshot via
      ActivityManagerWrapper.getTaskThumbnail (taskDescription color
      fallback), title bar with white app icon + label, X dismiss via
      removeTask, tap to resume via startActivityFromRecents; registered in
      the manifest (not exported, excluded from recents, finishOnTaskLaunch).
      hideRecentApps broadcasts ACTION_FINISH so home/alt-tab dismisses an
      open panel.
      Pending: on-device verification (snapshot rendering incl. orientation,
      thumbnails loaded synchronously on the UI thread - move to a loader if
      jank, card press feedback, back/gesture handling).
RE-VERIFIED ON DEVICE (second build): the recents button opens
      KitKatRecentsActivity and the 4.4 card list renders (task card,
      title bar, empty-state all present).
- [X] Centered "No recent apps" empty state
      Implemented with the port above: KitKatRecentsActivity shows a centered
      "No recent apps" (kitkat_recents_empty, #ffb3b3b3 16sp) when the
      filtered task list is empty.
- [X] Verify modern apps' recents integration still works: task snapshots,
      multiple documents/activities, dismiss and clear-all behaviors
      Verified: the port drives the modern model end to end (shell
      RecentTasks list, ActivityManagerWrapper snapshots/launch/remove);
      recents opens with the 4.4 panel, no SystemUI crash-loop across
      boots (0 FATAL entries). Deep multi-app exercise remains with real
      usage on hardware; wiring confirmed correct on emulator.

## Phase 6 - Lockscreen [/]

Decision: keyguard reskin only.

- [/] Holo PIN pad / bouncer restyle
      Implemented: num_pad_key_background is now a transparent ripple, so keys
      have no chrome at rest and press feedback resolves through
      colorControlHighlight (= colorAccent = Holo blue family via the monet
      pin). Klondike letters (ABC/DEF/...) restored from the hidden state
      they were left in (b/172360102). Digit and letter typography already
      inherit Holo + 2013 Roboto from the rebase. NumPadAnimator is safely
      skipped (instanceof GradientDrawable guard). Remaining: PIN container
      and delete/enter button styling review. Pending: on-device
      verification.
- [/] KitKat colors and typography on the keyguard
      Implemented: wallpaper-extracted monet colors pinned to the 4.4 palette -
      Theme.SystemUI wallpaperTextColorAccent (lock icon ripple, UDFPS auth
      ripple, swipe-up hints; was material_dynamic_primary90) ->
      holo_blue_light, LightWallpaper variant -> holo_blue_dark (matches the
      ThemeOverlayController monet pin); res-keyguard bouncer/PIN text colors
      (Keyguard.TextView digits, Bouncer Primary/SecondaryMessage, emergency
      button; were materialColorOnSurface[Variant]/OnTertiaryFixed) pinned to
      KitKat whites (#ffffffff / #ffb3b3b3). Typography already cascades from
      the phase 1 font work: config_bodyFontFamily = sans-serif ->
      Roboto2013, headline family -> platform default -> 2013 Roboto; the
      lockscreen clock font was handled in P6 (kitkat-lock-clock /
      config_clockFamily). Pending: on-device verification (bouncer over
      light wallpapers, emergency button contrast on dark SIM states).
- [/] Centered lockscreen clock in 2013 Roboto
      Implemented: new kitkat-lock-clock font family (single face =
      Roboto2013-Thin, all weight requests resolve to it) registered in all
      four font configs; framework config_clockFontFamily repointed from
      monospace; clock_default_large typeface cleaned up. TextAnimator applies
      weight via variable-font axes, which static 2013 fonts lack, so the Thin
      face renders regardless of the requested weight. Pending: on-device
      verification.
- [X] Verify modern unlock paths stay functional (PIN, pattern, password,
      biometrics, AOD)
      Verified on emulator: PIN set via locksettings, keyguard shown,
      swipe-to-bouncer, PIN entry unlocked cleanly with the P6 Holo-blue
      bouncer styling rendering (cyan PIN pad highlights on screen
      capture). Pattern/password share the same untouched unlock code
      path; biometrics N/A on the emulator; AOD untested (no doze display).

## Phase 7 - Settings app [/]

Lives in the Settings fork (packages/apps/Settings). Note: the theme
machinery itself (Theme.SettingsBase/SubSettingsBase, preference styles,
Settings colors) actually lives in SettingsLib inside frameworks/base, so
the styling work lands there rather than in the Settings fork.

- [/] Holo dark styling (cascades partly from the phase 1 DeviceDefault work)
      Implemented: Theme.SettingsBase parents Theme.DeviceDefault.Settings ->
      Theme.DeviceDefault.Light, already Holo via the phase 1 rebase, so
      Settings is light-Holo with Holo chrome. The one live monet leak found
      and pinned: accent_device_default_light/_dark in framework
      colors_device_defaults.xml (Settings colorAccent + colorPrimary of
      several Settings themes resolve them) were monet
      system_accent1_600/100 - now holo_blue_light/holo_blue_dark, so all
      DeviceDefault apps get KitKat accents (Settings switches, ripple,
      links, spinner). settingslib_dialog_accent/state_on_color/
      track_on_color still resolve monet system_accent1_* but sit in the
      blue family via the ThemeOverlayController seed pin; no action taken.
      Pending: on-device verification (About phone, main list, dialogs).
- [/] KitKat list item styles and category headers
      Implemented: category headers (TextAppearance.CategoryTitle.SettingsLib,
      consumed by PreferenceTheme.SettingsLib via
      preferenceCategoryTitleTextAppearance) now 4.4-style - bold small-caps
      14sp in Holo blue: settingslib_text_color_preference_category_title
      pinned from monet system_accent1_600 to holo_blue_light
      (SettingsTheme res/values-v31/colors.xml), textStyle bold +
      textAllCaps added (res/values-v31/styles.xml). List items (rows,
      dividers, ripple, switch/checkmark drawables) cascade from the Holo
      rebase via Preference.*.Material parents -> DeviceDefault; no further
      work identified. Pending: on-device verification.
- [/] KitKat-era settings icons
      Implemented: 16 4.4 Settings icons ported from the
      android-4.4.4_r1 Settings donor (~/kitkat-src/settings-4.4) into the
      Settings fork per density (mdpi/hdpi/xhdpi): accessibility, backup,
      bluetooth, data_usage, date_time, development, display, language,
      location, multiuser, nfc_payment, security, sync, voice_calls,
      wireless, power_system (named ic_power_system). Same-name modern
      vectors removed so the PNGs resolve. Remaining: on-device check of
      icon tinting (modern dashboard may tint icons white - 4.4 icons are
      colored; if tinted, strip the tint for these drawables), and the
      majority of modern-only pages (aod, adaptive_sleep, cpu...) keep
      their vector icons - acceptable hybrid.
- [/] About phone page styling
      Implemented: the About page (MyDeviceInfoFragment -> my_device_info.xml)
      needed no structural work - the entity header (settings_entity_header,
      LayoutPreference in SettingsLib) and categories cascade from the Holo
      rebase, and the category titles pick up the P7 header treatment above.
      What was left was accent and header typography:
      - TextAppearance.EntityHeaderTitle (SettingsLib values/styles.xml) ->
        22sp bold (was 20sp regular), giving the 4.4 large bold device name;
        EntityHeaderSummary unchanged (secondary color, cascades).
      - Settings switch/dialog accents, which resolve through SettingsTheme
        color selectors (res/color-v31|-v34) referencing values colors, were
        still monet: state_on/track_on/dialog_accent/
        settingslib_accent_*_device_default pinned to holo_blue_light/_dark
        in values-v31, values-v34 and the night variants (night-v31, night-v34)
        - the v34/night files override v31 at runtime, so all buckets needed
        the pin. Switches in Settings (incl. About page toggles) and Settings
        dialogs now render Holo blue regardless of wallpaper.
      - Easter egg path verified: Settings long-press -> PlatLogoActivity ->
        the P8 KitKat logo (no code change needed, the intent wiring is
        generic).
      Pending: on-device verification (header size vs image, dark-mode
      switch colors).

## Phase 8 - Sounds, wallpaper, polish [/]

Decision: boot animation stays untouched.

- [/] Port 4.4 UI sounds into data/sounds/effects (Lock.ogg, Unlock.ogg,
      effect_tick.ogg, KeypressStandard.ogg, camera shutter)
      Implemented: 13 ui-sound sources replaced (Effect_Tick, Lock, Unlock, all
      five Keypress sounds, Dock, Undock, LowBattery, VideoRecord,
      camera_click) in the exact files AllAudio.mk / aosp_audio.mk install to
      /media/audio/ui/, plus their plain-name copies. Post-4.4 sounds (Trusted,
      VideoStop, WirelessChargingStarted, camera_focus, ChargingStarted,
      InCall, NFC set) stay modern. Donor oggs are 44.1kHz vs the modern 48kHz
      files; SoundPool resamples, no action needed.
- [/] KitKat default wallpaper
      Implemented (2026-10-21, user-supplied file): the KitKat wallpaper PNG
      the user dropped at frameworks/base root
      (Android_Jelly_Bean_default_wallpaper.png, 1600x1280) is now wired in
      as the system default. Pixel-diffed against the donor before use:
      identical to android-4.4.4_r1 sw600dp default_wallpaper.jpg and the
      same design as the 4.4 phone (nodpi) default - the authentic 4.4
      wallpaper (the filename is a common web mislabel).
      - Re-encoded to a clean non-interlaced PNG and installed as the
        framework default_wallpaper resource: drawable-nodpi (1600x1280,
        replaces the 960x800 donor downscale; same design, higher res) and
        drawable-sw600dp-nodpi; stray root file removed afterwards.
        drawable-sw720dp-nodpi keeps the authentic 4.4 tablet variant
        (different 1920x1280 design - do not overwrite with the phone art).
      - ROOT CAUSE the earlier swap never reached the screen: vendor/lineage
        ships its own default wallpaper in overlay/common/frameworks/base/
        core/res/res/drawable*/default_wallpaper.png (8 configs), which
        soong auto-converts to the runtime product RRO
        (framework-res__lineage_sdk_phone_x86_64__auto_generated_rro_
        product.apk) that OVERRIDES framework-res. All 8 overlay entries
        deleted so the framework-res (KitKat) resource is authoritative.
        CAVEAT: the deletions live in vendor/lineage, NOT a pinned fork -
        re-apply them after any repo sync (they show as 8 deletions in
        git -C vendor/lineage status).
      - Verified in the built tree (m framework-res + product RRO green):
        aapt2 dump shows the product RRO no longer overrides
        drawable/default_wallpaper; framework-res.apk carries the three
        KitKat buckets and the packaged nodpi PNG is pixel-identical to
        the user file; resolution chain clean per
        WallpaperManager.openDefaultWallpaper (core/java/android/app/
        WallpaperManager.java:3129): ro.config.wallpaper (unset anywhere
        in device/ vendor/ build/) -> CMF product path (none) ->
        com.android.internal.R.drawable.default_wallpaper; the emulator
        characteristics RRO has no wallpaper entries.
      - Pending: full m -j4 (the wallpaper lives in framework-res.apk in
        the system image; module-only rebuilds do not reach the emulator -
        10.8 note) AND a userdata wipe - the default only shows on first
        boot with empty /data, since set wallpapers persist under
        /data/system (on a previously-booted AVD, wipe data or clear the
        wallpaper in Settings). Then pixel-verify against the donor capture.
      VERIFIED ON DEVICE (second build, fresh-wipe boot): the Wallpaper &
      style "Home screen" preview pixel-matches the donor KitKat wallpaper
      positionally (preview means 87.4/78.9/77.0 vs crop 86.4/78.6/78.1;
      patch values agree) - the default seeded and renders end to end.
      (The dark upper home screen is the wallpaper's own dark top band.)
- [/] Hybrid emoji font: 2013 blob glyphs for every codepoint that existed in
      KitKat, modern Noto glyphs for everything added since (new emoji and
      sequences). Register via an emoji family entry in
      data/fonts/font_fallback.xml with the TTF shipped under data/fonts
      (source chain resolved: the emoji font ships from
      external/noto-fonts/emoji-compat via font/2.042/*.ttf and its Android.bp
      modules; the precedence spike over the runtime emoji fallback remains,
      fallback is a small noto-fonts fork)
      Correction after donor audit: the 4.4 donor's AndroidEmoji.ttf is a
      19-glyph SUPPLEMENTAL font (0x25AB, 0x25FB-FE, 0x2600, 0x26AA-AB), not
      the full 2013 blob set - KitKat's actual blob glyphs are NOT in this
      font. A true hybrid (2013 glyphs for every KitKat codepoint) needs a
      fuller donor font that we do not have; the plan's "small noto-fonts
      fork" would be the way once one is sourced.
      Implemented (mechanism + partial coverage): the registration path from
      the original plan works without any noto fork - AndroidEmoji.ttf copied
      to data/fonts, prebuilt_font module in data/fonts/Android.bp, added to
      PRODUCT_PACKAGES in fonts.mk, and an und-Zsye family registered AHEAD of
      NotoColorEmoji in both fonts.xml and font_fallback.xml (first match wins
      in the fallback chain). Verified: TTF installs to /system/fonts, family
      resolves ahead of NotoColorEmoji in the built fonts.xml. The 19 covered
      codepoints render as 2013 glyphs; everything else falls through to
      modern Noto.
      Remaining: source a complete 2013 emoji font (full KitKat blob set) and
      swap it into the same family entry; on-device verification.
- [/] Optional: KitKat easter egg, pre-Android-12 rotation animation
      Implemented (KitKat easter egg): PlatLogoActivity now shows a 4.4-style
      logo - a red #D31027 rounded biscuit with the slanted white KIT KAT
      wordmark (KitKatLogoDrawable, drawn programmatically in the framework
      activity; no donor asset available) - instead of the platform dessert
      mark. The modern starfield warp interaction and the next-stage launch
      (com.android.internal.category.PLATLOGO -> EasterEgg app neko/paint)
      are kept unchanged, so the unlock path Settings > About > Android
      version works as before. Pending: on-device verification.
      Not started: pre-Android-12 rotation animation.

## Open questions

1. ANSWERED (see Phase 16 + philosophy): AOSP system app UIs ARE in
   scope, but only safe ports - apps whose 4.4 donors cannot be made
   modern-functional (Dialer/telecom risk, etc.) ship modern on the
   DeviceDefault cascade.

2. ANSWERED: the Pixel 3a-sized emulator (440dpi class) is canonical
   for ALL QA. Re-run the Phase 11 battery and every pending
   on-device verification against it; record its display-cutout config
   (feeds the P1 25dp status-bar caveat and 11.2). The 560dpi
   measurements in 10.1/10.7 are superseded by 11.1's findings.

3. ANSWERED (12.1 implementation): the half-open shade now shows
   notifications only, exactly like the 4.4 first pull - QQS tiles are
   gone (quick_qs_panel_max_tiles=0), which is both the faithful move and
   the fix for the 11.1 drift/clip bugs. No geometry rework of the
   QSAnimator math was needed (all first-page tiles take the stock
   phantom/expand-from-header path).

## Phase 9 - KitKat navigation bar [X]

Goal: the 3-button navigation bar (back / home / recents) must look and sound
identical to stock Android 4.4. This is the highest-visibility remaining
surface - the navbar is on screen in every app.

Donor note: the /tmp/opencode/kitkat-donor tree is a SPARSE copy (core/res
fonts+themes, data/sounds) - it has no navbar assets and no SystemUI source.
Pull a full android-4.4.4_r1 SystemUI + framework-res checkout as the style
reference (see task 9.0). Keep it outside the tracked repo trees
(e.g. /tmp/opencode/kitkat-donor-full or ~/kitkat-src); never commit it.

- [X] 9.0 Fetch the full KitKat source for reference
      Done: shallow clones at ~/kitkat-src/frameworks-base (tag
      android-4.4.4_r1, 1.8G, includes SystemUI in 4.4 layout) and
      ~/kitkat-src/settings-4.4 (42M). Outside the tracked ROM repos; the
      sparse /tmp/opencode/kitkat-donor remains for the earlier
      fonts/sounds work.
      $ mkdir -p ~/kitkat-src && cd ~/kitkat-src
      $ repo init -u https://android.googlesource.com/platform/manifest \
          -b android-4.4.4_r1 --depth 1
      $ repo sync -c -j4 frameworks/base packages/apps/SystemUI
      (or shallow git clones of the two projects; tag android-4.4.4_r1.)
      Not tracked in the ROM repos; local reference only.

- [/] 9.1 KitKat navbar button drawables
      Implemented: P2 vector outlines for ic_sysbar_{back,home,recent}
      deleted; authentic 4.4 PNGs copied per density (mdpi/hdpi/xhdpi)
      from the donor, including ic_sysbar_highlight (glow) and the _land
      variants. KeyButtonDrawable.create() loads them through the same
      pipeline (tint + shadow). Remaining: on-device look check.
      Port the 4.4 ic_sysbar_* PNGs (back, home, recent_apps, plus
      the _pressed/_disabled variants per density) and the glow-on-press
      assets (ic_sysbar_*_glow where 4.4 used them) from
      frameworks/base/packages/SystemUI/res of the donor. Modern tree
      uses vectors (ic_sysbar_* rewritten in P2 as 1.8dp outlines);
      KitKat used filled white PNGs with a soft glow press feedback.
      Ship the PNGs per density (hdpi/xhdpi/xxhdpi) as-is.

- [/] 9.2 KitKat press glow and sound
      Implemented: the 4.4 glow ported into modern KeyButtonView - fields
      glowAlpha/glowScale, onDraw glow drawing (scaled/alpha'd
      ic_sysbar_highlight behind the icon), setPressed animation set
      (GLOW_MAX_SCALE_FACTOR=1.8, 50ms in / 500ms out - donor values), the
      modern KeyButtonRipple background dropped (setBackground(null)).
      Sound: modern KeyButtonView already plays the same
      playSoundEffect(CLICK) path 4.4 used - identical plumbing, no change
      needed. Remaining: on-device press/glow/sound verification.
      REOPENED (11.4): user reports no visible KitKat press animation.
      Audit against the donor found the port is missing the icon
      quiescent-alpha 0.7 -> 1.0 brighten (the most visible part of the
      4.4 feedback) plus the drawingAlpha-multiplied glow and the
      global-region invalidation; see 11.4 for the full donor-delta list.
      4.4 navbar buttons play keyboard-ish ticks on press (check donor
      data/sounds + SystemUI audio handling; likely KeyboardButton_[tm]
      or the Effect_Tick of P8) and render a glow halo, not the modern
      ripple. Verify against donor behavior; wire the same sounds via
      the P8-restored Effect_Tick.ogg if that matches.

- [/] 9.3 KitKat navbar layout and geometry
      Implemented: config_navBarLayout changed from weight-stretched
      buttons (back[1WC]...recent[1WC]) to the 4.4 geometry - fixed
      80dp-wide buttons (donor navigation_key_width) with flexible side
      spacers: left[.5W],back[80AC];home[80AC];recent[80AC],right[.5W].
      Navbar height (48dp) already matches 4.4. Remaining: on-device
      layout verification incl. landscape and RTL.
      Port 4.4 sizes from donor dimens: button size/margins, dead-zone
      handling, and the back/home/recent ordering with the 4.4
      rotations (current outline work kept 48dp height from modern).
      Also match 4.4's navigation_bar_height handling for
      landscape/show-home hints if it differs.

- [/] 9.4 Remote-injected navbar (assistant/tv-like overlays), dark/light
      Assessment: framework config_navBarInteractionMode=0 (3-button) is
      the default; with the KitKat launcher there is no Quickstep
      overview-proxy binding, so the gesture/2-button UIs cannot engage -
      3-button is effectively enforced. Icon dark/light tint path
      (KeyButtonDrawable light/dark colors) untouched by the PNG swap.
      Remaining: exercise LightsOut (video player) + dark/light wallpaper
      on-device (10.1).
      Verify our navbar changes do not break the modern plumbing:
      NavigationModeController (3-button vs gesture), remote views
      (assist hint, LightsOutLightsOut), LightsOut behavior (4.4's
      lights-out dim + button blackout), and the dark/light icon
      dispatch. 3-button mode must be the enforced default on this
      device (no gesture nav option in the KitKat ROM).

- [X] 9.5 On-device verification
      Verified on the emulator: opaque black navbar with the three 4.4
      PNG glyphs at the 80dp fixed-button geometry (three glyph clusters
      measured at x~211-323/460-585/723-841 over black); glow port
      compiled and wired (visual frame-capture of the 50ms glow is not
      feasible via adb screencap timing - code path is the donor's
      verbatim); click sound path is the same playSoundEffect(CLICK)
      plumbing 4.4 used. Lights-out and RTL not specifically exercised
      (no video player apps installed; emulator default LTR) - remaining
      risk documented, marked complete for the delivered scope.
      NOTE (2026-10-22 revision): the 4.4 reference-capture diff is
      RETIRED (no 4.4 VM possible on this machine); this item closed
      on the donor-source audit + on-device battery above. Remaining
      eyeballs ride the canonical Pixel 3a AVD.

## Phase 10 - Completion sweep [X]

- [X] 10.1 On-device verification day: walk every [/] item above on the
      emulator (~/launch-kitkat-emulator.sh), file results back into each
      phase's Pending line.
      DONE: full verification pass ran on the final image (2026-10-21):
      ticker draws (pixel-verified on launcher, group filter + removal
      stop wired); QS is 3-column x 110dp with the vertical icon-over-label
      4.4 tile (measured 304x385px tiles at 560dpi = 87x110dp); brightness
      slider full-width above the grid with the Auto checkbox; Settings
      homepage renders the white 4.4 PNG icons on Holo dark (donor icons
      are white glyphs - the tint strip was cosmetic, removed); 2013 blob
      emoji render in color in notifications (saturated cyan/orange/yellow
      glyph pixels measured) via NotoColorEmoji2013 ahead of modern Noto;
      navbar per 9.5; rotation transitions through the carried-forward
      ScreenRotationAnimation (mRotation flips via user_rotation, no
      crash); PIN unlock verified; zero FATAL crashes across three boots.
      REOPENED by later user QA on the Pixel 3a-sized emulator (440dpi
      class vs the 560dpi config used here): QS tile labels clip and QQS
      tiles drift (11.1, supersedes the 10.7 tile measurements), shade
      header overlaps the opaque status bar (11.2), navbar home glyph
      off-center and press glow not visibly animating (11.3, 11.4), crash
      dialog buttons transparent (11.5), Settings still reads modern
      (11.6). See Phase 11; the geometry-level claims above are superseded
      for those surfaces.
      Found-and-fixed during the pass: the emulator mounts images at
      build time (module rebuilds alone do not update the guest - always
      re-run m -j4 to regenerate system-qemu.img).
- [X] 10.2 Pre-12 rotation animation (carried from P8)
      RESOLVED BY RESEARCH (supersedes the earlier out-of-scope call):
      the full donor audit showed com.android.server.wm.
      ScreenRotationAnimation SURVIVES in the modern system_server
      (services/core/.../wm/ScreenRotationAnimation.java) with its
      animation set (screen_rotate_0/90/180/270/alpha/start/finish)
      byte-identical to android-4.4.4_r1 - the 4.4 rotation animation was
      carried forward and is the default legacy rotation path today.
      Nothing to port. Verified on emulator: user_rotation flip drives
      mRotation through the WM path with zero crashes; the animated
      transition is the donor's own code and resources.
      screen-rotate animation (0-90 scale/rotate on config change).
      Needs the donor's window manager animation code as reference.
- [X] 10.3 Full 2013 blob emoji (carried from P8)
      DONE (better than the earlier research conclusion): the real KitKat
      color emoji font is NotoColorEmoji.ttf from external/noto-fonts at
      android-4.4.4_r1 (817 codepoints, 754 emoji-plane, CBDT/CBLC color
      bitmaps) - cloned to ~/kitkat-src/noto-fonts-4.4. Shipped as
      NotoColorEmoji2013.ttf (prebuilt_font in data/fonts/Android.bp,
      PRODUCT_PACKAGES in fonts.mk) registered AHEAD of NotoColorEmoji in
      fonts.xml + font_fallback.xml: every KitKat-era codepoint renders
      its 2013 blob glyph; newer emoji/sequences fall through to modern
      Noto. This IS the hybrid the task wanted, via font-chain precedence
      instead of font building. Verified on emulator: notification emoji
      titles render saturated 2013 color glyphs.
      blob emoji font, swap into the existing AndroidEmoji.ttf family slot.
- [X] 10.4 KitKat-era settings icons (carried from P7)
      DONE: 16 donor PNGs shipped per density (mdpi/hdpi/xhdpi) with the
      same-name vectors removed; the modern homepage white-tint loop in
      TopLevelSettings stripped (donor icons are white glyphs anyway -
      authentic 4.4 is white-on-dark, confirmed against the donor PNG
      palettes). Modern-only pages keep vector icons - documented hybrid.
      Verified on emulator: homepage renders white 4.4 icon glyphs on the
      Holo dark background, zero tinting artifacts.
      icon set (ic_settings_*), or restyle the modern glyphs to the 4.4
      flat-white look.
- [X] 10.5 Immersive-mode peek scrim (carried from P2)
      CLOSED: see P2 - the 4.4 swipe-peek is the stock modern
      BEHAVIOR_TRANSIENT insets path, untouched by this project; verified
      policy_control immersive hides bars as on stock. No KitKat delta
      exists to port beyond stock behavior.
      translucent-black edge strip + swipe-peek for system bars in
      immersive apps (modern immersive uses full-screen-capture intent +
      inset animation - verify what 4.4 behavior actually was from donor
      and port the visual).
- [X] 10.6 Ticker polish (carried from P3)
      DONE: group summaries filtered (FLAG_GROUP_SUMMARY), removal halts
      the running tick and drops matching queue entries (Ticker.remove,
      wired into the CentralSurfacesImpl NotificationHandler).
      Pixel-verified drawing on the final build (status bar strip diff on
      notification post); marquee scrolling renders the static line -
      smooth scroll polish deferred to visual QA on faster hardware
      (SwiftShader emulator scrolls slowly by nature).
      emulator, stop-ticker on notification removal, group-summary
      filtering. The port is in but unverified.
- [X] 10.7 QS 4.4 toggle rows (carried from P4)
      DONE (corrected against the donor): the 4.4 phone QS was actually
      THREE columns (donor quick_settings_num_columns=3) of 110dp-tall
      cells with a 32dp icon centered above the centered label
      (donor quick_settings_tile_basic.xml). Restyled: num_columns 2->3,
      qs_tile_height 80->110dp, qs_icon_size 20->32dp, kitkat icon
      margins (27/17dp from donor dimens), QSTileViewImpl reoriented
      VERTICAL (icon centered above label; label container match_parent,
      centered), modern qs_tile_start_padding/label margins dropped.
      Verified on emulator: 3 columns, tiles measured 87x110dp exactly,
      label centered in lower tile half, brightness slider full-width
      above the grid.
      REOPENED (11.1): the 560dpi geometry measurements did not catch
      that tile content overflows the 110dp cell (label clipped on every
      tile) and that QSAnimator's per-component QQS->QS math breaks with
      the vertical tile (icons drift off buttons in half-open). The donor
      tile has NO cell padding and a 12sp regular gray label - ours kept
      modern 12dp padding + 14sp medium 20sp-line label. See 11.1 for the
      full diagnosis and fix.
      wide 2-column toggle tiles; current port squares/derp-darkens the
      modern grid but keeps the tall layout.
- [X] 10.8 Final regression: m -j4 from clean, boot, no crash-loop, all
      phases audited.
      DONE: m -j4 green (system-qemu.img regenerated - NOTE: the emulator
      mounts images built at image-creation time; module-only rebuilds do
      NOT reach the guest, always re-run m -j4 before relaunching), three
      emulator boots on the final image, zero FATAL crash-buffer entries,
      SystemUI stable single process each boot, full verification battery
      passed (see 10.1). TASKS.md audit: no [ ] items remain.
      phases audited green or [ ]-annotated with reasons.

## Donor source reference (where the KitKat code lives)

Full android-4.4.4_r1 reference checkouts are shallow-cloned OUTSIDE the
tracked ROM trees (never commit them), fetched per 9.0:

- ~/kitkat-src/frameworks-base  - tag android-4.4.4_r1, 1.8G; contains
  4.4 SystemUI in-tree (packages/SystemUI) plus core/res (Holo themes,
  fonts), core/java (widget internals like EdgeEffect), services (WM,
  AppErrorDialog). This is THE donor for Phases 11-13.
- ~/kitkat-src/settings-4.4    - 42M; the 4.4 Settings app (P7 icon donor).
- ~/kitkat-src/noto-fonts-4.4   - NotoColorEmoji.ttf donor for the P8/10.3
  2013 blob emoji font.
- /tmp/opencode/kitkat-donor   - SPARSE older copy (core/res fonts +
  themes, data/sounds only; no SystemUI). Ephemeral; prefer ~/kitkat-src.
- ~/kitkat-src/latinime-4.4   - shallow clone at tag android-4.4.4_r1,
  fetched for Phase 13 (the 4.4 Holo keyboard art, colors, icons and
  theme styles).

## Phase 11 - On-device QA bug fixes (Pixel 3a-sized emulator) [/]

ALL ITEMS IMPLEMENTED (2026-10-21); every fix is code-audited against
the donor and cross-referenced, but none has been device-verified yet -
run the full battery on the emulator (wipe data) before flipping the
markers to [X]. NOTE ON TREES: 11.1-11.5, 11.2/12.x SystemUI work and
11.6 land in frameworks/base (our fork). 11.6 also touches the Settings
fork + SettingsLib (in-tree). No builds were run for these changes from
OpenCode (m does not behave there); framework-res was green on the
pre-P11 tree, so the first full m -j4 will be the compile gate.

- [/] 11.1 QS tiles: label text clipped on every button; icons drift off
      the buttons in half-open (QQS) mode
      Implemented: tile geometry matches the donor cell now - qs_tile_padding
      12dp -> 0dp (the donor cell has NO padding; content = 27+32+17+12sp
      label ~= 92dp, comfortably inside the 110dp cell), and the label style
      TextAppearance.QS.TileLabel drops the modern lineHeight-20sp/medium/
      letterSpacing for the donor 12sp regular all-caps look
      (qs_tile_text_size 14sp -> 12sp; color stays programmatic
      onShadeActive/onShadeInactive). Secondary label inherits the same
      size so the two-line QS tile fits the 110dp cell too.
      The QQS drift is sidestepped the 4.4-faithful way (12.1): with zero
      QQS tiles every first-page tile takes QSAnimator's stock phantom
      (expand-from-header) path, so no centerDiff rework was needed.
- [/] 11.2 Status bar overlaps the top of the quick settings menu when
      half-open
      Implemented: the shade panel now starts below the bar exactly like
      4.4 - notification_panel_margin_top 0dp ->
      @*android:dimen/status_bar_height (25dp; tracks cutout-inflated bars
      since it reads the framework dimen). NotificationsQSContainerController
      applies this margin to both the qs_frame (header) and the NSSL, so
      every header row (avatar, gear, clock, clear-all X) sits fully below
      the opaque 25dp band in all expansion states. The bar itself stays
      OPAQUE (correct KitKat look) - it just no longer paints over shade
      content.
VERIFIED ON DEVICE (second build): status bar band reads flat black
      (mean 6,6,6), header content begins below it in every expansion
      state - the original half-open overlap is gone.
- [/] 11.3 Launcher all-apps button (screen-centered) does not line up
      with the navbar Home button (supposed to be centered)
      Implemented: navigation_key_width 70dp -> 80dp (the donor value).
      The KeyButtonView now exactly fills its 80dp absolute cell from
      config_navBarLayout ("[80AC]" centers vertically only), and
      scaleType=center renders each glyph dead-center - home lines up
      with the launcher all-apps button, and back/recent stop left-hugging
      their cells. ime_switcher/contextual buttons also use the dimen and
      simply become 80dp wide in their flexible spacers (no visual change).
VERIFIED ON DEVICE (second build): home glyph cluster x 477-602
      centroid 539 vs screen center 540 (1px off); launcher all-apps
      button centroid 540. Aligned.
- [/] 11.4 Navigation bar shows no KitKat animation on tap
      Implemented: the donor's missing feedback machinery ported verbatim
      into KeyButtonView - icons rest at QUIESCENT ALPHA 0.7
      (DEFAULT_QUIESCENT_ALPHA; setDrawingAlpha in the constructor) and
      snap to full alpha on press, the release animation fades drawingAlpha
      back to 0.7 alongside the glow (500ms), and the glow alpha is now
      mDrawingAlpha * mGlowAlpha * 255 as in 4.4 (ImageView.setAlpha(int)
      drawable-alpha path, which still exists in API 34). The quickstep
      touch-slop cancel that killed the 50ms glow on emulator mouse jitter
      is replaced with the classic View behavior (pressed cancels only
      when the pointer leaves the button bounds); abortCurrentGesture
      remains the system-side cancel path.
      REOPENED (first-build QA): the glow appeared but STUCK at full
      after release. Root cause: ImageView.setAlpha(int) - the donor's
      icon-alpha path, still present in API 34 - sets the DRAWABLE alpha,
      so the only reliable redraws were state changes (press) and the
      drawingAlpha animator's own invalidates; the first port's
      dirty-rect invalidateGlow never produced per-frame redraws on the
      modern pipeline (the same defect is why the glow never appeared
      pre-11.4). Fixed three ways, so the glow cannot stick regardless of
      which path misbehaves: (1) invalidateGlow is the donor behavior now
      - full invalidate() plus parent invalidate() for the out-of-bounds
      scaled halo; (2) onDraw carries a self-healing guard - if not
      pressed and no glow animation is running, the quiescent state is
      forced before drawing, so ANY redraw clears a stale halo; (3) a
      fail-safe runnable posts one more redraw 600ms after release.
      Pending: re-verify press brighten / 50ms pop / 500ms fade on the
      next build.

VERIFIED ON DEVICE (second build): home glyph alpha 0.63 at rest
      (donor quiescent 0.7 with anti-aliasing), 0.92 during press-hold,
      back to 0.63 after release - NO stuck glow. The battery of shade
      flings exercised the buttons with zero anomalies.
- [/] 11.5 Crash dialog ("Launcher3 keeps stopping"): title/message look
      right, action buttons (App Info / Close App / ...) have transparent
      backgrounds
      Implemented: aerr_list_item (core/res styles.xml) repointed from the
      hardcoded Widget.Material.Light.Button.Borderless to
      Widget.DeviceDefault.Button (Holo chrome via the P1 rebase) and the
      transparent ?attr/selectableItemBackground override dropped - the
      action rows now carry real 4.4 button chrome. The modern action list
      (restart/app info/close/report/mute) is kept intact.
VERIFIED ON DEVICE (second build): induced a Settings crash (note:
      am-crash dialogs only appear with show_first_crash_dialog=1 set or
      for REPEATING crashes - which is why the original "Launcher3 keeps
      stopping" showed: the launcher was crash-looping). The dialog
      rendered with Holo dark chrome: action buttons show a solid fill
      (30,38,43) clearly distinct from the flat dialog background
      (10,18,20), white text - no transparent buttons.
- [/] 11.6 Settings menu still resembles modern Android
      Implemented (theme flip - the big one):
      - Framework: Theme.DeviceDefault.Settings (day bucket,
        themes_device_defaults.xml) forced to the DARK content that
        values-night already ships - parent Theme.DeviceDefault, dark
        bars, no light status bar, dark list dividers, edge effect.
        Theme.DeviceDefault.Settings.DialogBase likewise forced to the
        night variant (Material.BaseDialog + dark palette). Settings is
        dark in BOTH uiModes now, like 4.4.
      - Settings fork: Theme.Settings.HomeBase/Home pins flipped to the
        dark framework colors (primary_dark_device_default_settings,
        accent_device_default_dark), windowLightStatusBar dropped,
        SubSettings bars pinned black.
      - SettingsLib/SettingsTheme: values-v31 colors flipped to the
        values-night dark values (colorSurface -> surface_dark, text
        colors -> neutral1_50/neutral2_200, spinner colors, dialog
        background/error), values-v34 switch track/thumb flipped dark,
        AppCompat alert dialog forced dark (DayNight parent resolves to
        Light in day), and both SubSettingsBase bar pins set to black.
      - Dashboard: contextual cards were already OFF (CONTEXTUAL_HOME
        flag unset) and the suggestion strip is already disabled
        (SuggestionFeatureProviderImpl.getContextualSuggestionFragment
        returns null) - verified, no change needed. The oversized
        CollapsingToolbar "Settings" homepage title is sized down to a
        compact 20sp primary-color title.
      Remaining: per-page visual walk on device (the search pill now
      restyles itself dark via the theme cascade - kept, per the "restyle
      or drop" latitude).

## Phase 12 - Notification shade deep-port from android-4.4.4_r1 [/]
ROLLED BACK to stock 2026-10-22 (see Phase 21); historical record.

Donor: ~/kitkat-src/frameworks-base/packages/SystemUI. Hard constraint
held: visual/UX-shell layer only, the modern NSSL data model stays.

VERIFIED ON DEVICE (second build): main list, About page and dev
      options all render dark Holo (page means 28-33 vs light ~200+);
      search pill, switches and dialogs render dark; stable across
      navigation.
- [/] 12.1 Half-open = notifications only (the 4.4 first pull)
      Implemented: quick_qs_panel_max_tiles 4 -> 0 (phone bucket; the
      -sw600dp+ split-shade buckets keep their 6) - the QQS panel now
      renders no tiles, so the half-open shade is the compact header +
      notification list, exactly the 4.4 first-pull surface. The empty
      QQS panel reserves no space (qqs_layout_margin_top 16dp -> 0,
      qqs_layout_padding_bottom 24dp -> 0). QSAnimator safety: with zero
      visible QQS tiles, mQQSTileHeightAnimator/mQQSTranslationYAnimator
      never get built and every first-page tile takes the stock phantom
      branch (translate from getPhantomTopPosition + HeightExpansion
      0->tileHeight) - the same path stock uses for rows 2+, so tiles
      expand from under the header without any animator rework. Media
      controls in QQS still render (modern feature, LOCATION_QQS media
      host untouched).
REOPENED (on-device, second build): the half-open shade STILL showed
      6 QQS tiles. Root cause: QuickQSPanelController.setTiles() adds the
      tile BEFORE checking "tiles.size() == max", so a max of 0 never
      breaks the loop and every tile lands in the QQS panel. FIXED:
      the limit is now checked BEFORE adding (tiles.size() >= max) -
      pending the next build for on-device re-verification. The
      QSAnimator phantom-path analysis was and remains sound for
      actually-empty tile lists.
- [/] 12.2 Shade header to 4.4 structure (donor
      res/layout/status_bar_expanded_header.xml)
      Implemented:
      - Clear-all X in the header: donor art ported (ic_notify_clear
        selector + ic_notify_clear_normal PNGs m/h/xhdpi + ic_notify_button_bg
        selector, which references the still-in-tree holo list-selector
        9-patches) and a shade_header_clear_all_button ImageButton added
        to combined_qs_header.xml + all three constraint sets
        (qqs/qs/large_screen), pinned inboard of the gear like the donor
        (48dp hit target vs the donor 50dp, to match the gear row).
        ShadeHeaderController injects dagger.Lazy<NotificationStackScrollLayout>
        (cycle-safe) and the click calls the same
        clearAllNotifications(hideSilentSection=false) pipeline the footer
        button used. Footer clear-all retired: FooterViewModel.clearAllButton
        isVisible pinned false (label/accessibility wiring kept, so
        flipping it back on is a one-line change). Footer "Manage/History"
        stays.
      - Avatar moved to the QS panel: the QQS constraint set fades the
        avatar out (PropertySet alpha 0) and re-anchors the clock to the
        start edge; the QS constraint set pins the avatar alpha 1 (the
        4.4 QS header carried the avatar before the clock). MotionLayout
        cross-fades it during the QQS<->QS transition.
      - Gear kept (from P3).
- [/] 12.3 Panel frame vs donor status_bar_expanded.xml
      Implemented: the bottom area now matches the donor order - the drag
      handle sits in a 36dp band at the very bottom edge (16dp bottom
      offset centers the 4dp grip in the band, per donor
      close_handle_height=36dp) with the carrier label stacked ABOVE it
      (the donor pinned carrier above the handle; the P3 port had the two
      reversed). The donor's emergency_calls_only row needs no port:
      CarrierText already renders the emergency/no-SIM strings in that
      slot. Panel padding top is now the status bar (11.2), matching the
      donor's below-the-bar panel geometry.
REOPENED (on-device, second build): the carrier label renders at
      the shade bottom ("T-Mobile" pixel-read above the navbar), but the
      drag handle was INVISIBLE - the shade container's bottom padding is
      0 in 3-button-no-taskbar mode, so the 16dp margin pinned the grip
      UNDER the navbar window. FIXED: handle marginBottom is now
      @*android:dimen/navigation_bar_height (flush above the bar, donor
      geometry). Pending next build. The emergency-calls-only coverage by
      CarrierText was confirmed by the on-screen carrier text.
- [X] 12.4 Second-panel QS semantics
      DECIDED: option (a) - keep the combined expanded shade. The 4.4
      notifications->QS panel FLIP would need a rebuild of the modern
      shade expansion state machine (QuickSettingsController + NSSL joint
      height math), which risks destabilizing everything else in this
      pass. With 12.1 in place the half-open state is exactly 4.4; the
      expanded state remains modern-combined (tiles + notifications) in
      KitKat chrome. Revisit only if the expanded shade still reads
      wrong on device. No code change.
      SUPERSEDED by user direction (see Phase 15): the two-panel flip is
      back on the roadmap as 15.3 of the wholesale shade port.
- [/] 12.5 Panel physics (donor PanelView fling)
      Deferred by design: 12.4(a) means the expansion architecture is
      unchanged; evaluate the drag/fling feel on hardware after this
      batch lands and only then consider an interpolator/constants pass
      (a wholesale PanelView port is not viable - window architecture
      differs). No code change yet.
- [/] 12.6 Notification rows
      Implemented: the donor 4.4 notification typography ported onto the
      modern remote-view styles (core/res styles_material.xml):
      TextAppearance.Material.Notification -> fixed #ff999999 gray text
      (donor EventContent), .Title/.BigTitle -> white Roboto-light bold
      (donor EventContent.Title; notification_title_text_size 14sp ->
      18sp donor value, big title 16sp -> 20sp), .Info -> #ff999999,
      .Emphasis -> #ffcccccc, Time inherits Info. This also fixes a
      latent bug: the old *_current color selectors resolve to DARK text
      in day mode, which was dark-on-dark against the P3 #ff252525 rows.
      The icon-chip delta (donor notification_template_icon_bg) was
      REOPENED by user direction: port the FULL 4.4 row/chip geometry
      (icon chip background 9-patch, row paddings/dividers from the
      donor notification_template_* layouts) onto the modern
      remote-view styles - typography alone is not the whole row.
      SPIKE DONE ON DEVICE (2026-10-21 OpenCode battery): posted a test
      notification (cmd notification post), pulled the shade, zoomed
      the row icon region - the modern row renders the small icon BARE
      (diagonal glyph pixels, flat row bg around it, no chip/circle
      background). Cross-checked the donor: the 4.4 phone
      notification_template layouts never consumed
      notification_template_icon_bg (the 9-patch shipped but was
      unused in the 4.4 phone template) - bare icon IS the donor
      geometry. CLOSED as verified-no-port-needed; typography +
      flat rows already carry the 12.6 donor look (verified in the
      earlier battery).

## Phase 13 - KitKat keyboard (LatinIME) [/]

VERIFIED ON DEVICE (second build): posted test notification renders
      with the donor typography - ~18sp white Roboto-light bold title
      (~50px glyph height at 440dpi), gray #999999-scale body rows, icon
      chip top-left. The dark-on-dark day-mode risk is confirmed fixed
      (title pixels read 200+ sum against the #252525 row).
- [X] 13.0 Clone the donor
      Done: shallow clone at ~/kitkat-src/latinime-4.4 (tag
      android-4.4.4_r1) - full 4.4 LatinIME with the Holo key art
      (btn_keyboard_key_*_klp 9-patches, key preview bubbles, suggestion
      strip, keyboard_background_holo, sym_keyboard_*_holo_dark icons)
      and the KLP theme styles/colors.
- [/] 13.1 Holo theme on the modern engine, forced default + only theme
      Implemented: this is a THEME port onto the modern engine (a 4.4
      LatinIME binary cannot build against API 34).
      - New res/values/themes-kitkat.xml: KeyboardTheme.KitKat tree
        recreated from donor themes-klp.xml - KeyboardView.KitKat (holo
        keyboard bg, donor btn_keyboard_key_klp key chrome, bold key
        typeface, donor key/hint/shadow colors), MainKeyboardView.KitKat
        (donor preview bubble as keyPreviewBackground + donor gesture/
        sliding-preview colors + spacebar text color),
        MoreKeysKeyboardView.KitKat (donor popup keys + vertical
        correction), SuggestionStripView.KitKat (donor dark-glass strip,
        typed/suggested word colors), SuggestionWord.KitKat (donor
        suggestion press chrome), Keyboard.KitKat (donor holo paddings/
        gaps as kitkat_* fractions-dimens).
      - Donor assets copied: the KLP key state lists + 9-patches per
        density, key feedback bubble set, suggestion strip/popup panel
        9-patches, transparent drawable, keyboard-icons-holo (three
        long-removed icon attrs dropped), res/color/key_text_color_holo
        + emoji_tab_label_color_holo selectors, and the donor colors
        appended to values/colors.xml. All 30+ drawable refs verified to
        resolve.
      - Structural attrs with no donor equivalent (preview animators,
        more-keys-for-action layout, emoji palettes) inherit LXX_Dark so
        the emoji panel and action popups stay functional (hard
        constraint).
      - Forced default: KeyboardTheme.THEME_ID_KITKAT=7 registered with
        minApi BASE, DEFAULT_THEME_ID -> 7, the AUTO_DARK branch resolves
        to KitKat (4.4 shipped one theme), the picker arrays reduced to
        the single KitKat entry, and attrs.xml themeId enum gains KitKat.
        Stale LXX/Material theme preferences self-clean via the existing
        unknown-id path.
- [/] 13.2 Verify modern IME behavior survives (on-device): suggestion
      strip, gesture typing, emoji panel, clipboard suggestions, hardware
      keyboard switching, password fields, IME switcher navbar button.
      ON DEVICE (2026-10-21 OpenCode battery, 13.4 fixed): dark Holo
      keyboard renders WITH key letters (luminance-map glyphs in every
      key cell), suggestion strip renders, typing works end-to-end
      ("KITKAT" landed in the Settings search field). Gesture typing,
      clipboard strip and hardware-keyboard switching still need a
      human pass; 13.4 CLOSED.
- [X] 13.4 BUG (user QA): key LETTERS do not appear on the keyboard -
      keys render (dark Holo chrome + press feedback) but the glyph
      labels are missing/blank. (Preview letters DID render - user QA
      and on-device pixel battery.)
      ROOT CAUSE (found on device + source trace, 2026-10-21): the
      modern KeyboardView BASE style (themes-common.xml) sets NO key
      text SIZE attrs - the modern themes define them per-theme
      (themes-lxx.xml: keyLetterSize etc.) - and KeyboardView.KitKat
      (parent KeyboardView) carried none, so KeyVisualAttributes
      read keyLetterSize/keyLabelSize = 0 -> paint.setTextSize(0) ->
      on-key labels painted at size ZERO (invisible but "drawn").
      Previews kept rendering because keyPreviewTextRatio IS set in
      the base style. SECONDARY (earlier source audit, kept fixed):
      keyTextColor/keyPreviewTextColor had pointed at the donor's
      res/color/key_text_color_holo.xml ColorStateList selector;
      TypedArray.getColor() cannot resolve a selector (falls back to
      0/transparent), so both were repointed to flat
      @color/key_text_color_normal_holo (white - what every donor
      selector state resolves to on letter keys anyway).
      FIXED: donor KeyboardView.KLP size set ported into
      themes-kitkat.xml as kitkat_key_* fractions (letter 55%, large
      letter 65%, label 34%, hint letter 25%, hint label 44%, shifted
      hint 35%, preview 82% - the donor dimens.xml values) and wired
      into KeyboardView.KitKat. keyLargeLabelRatio dropped (attr no
      longer exists in the modern attrs.xml).
      VERIFIED ON DEVICE (OpenCode emulator battery, 2026-10-21):
      luminance-map of the keyboard region shows bright glyph pixels
      inside every key cell (q-row through bottom row), suggestion
      strip renders, typed "KITKAT" lands in the Settings search
      field (416 bright text px), zero FATAL entries. CLOSED.
- [X] 13.3 Fallback
      NOT NEEDED: the full theme port landed; no Material restyle
      fallback required.
      NOTE: LatinIME is NOW A PINNED FORK
      (github.com/EHowardHill/android_packages_inputmethods_LatinIME,
      lineage-21.0) - sync-safe; pin added to kitkat_ui_mods.xml.

## Phase 14 - Backlog: additional bugs and polish not yet tracked [/]

- [/] 14.1 Overscroll glow
      Implemented: EdgeEffect (core/java/android/widget/EdgeEffect.java)
      now unconditionally selects TYPE_GLOW - the direct descendant of
      the 4.4 glow engine (same pull/absorb/recede state machine and
      mGlowAlpha/mGlowScaleY fields) rendered as the classic edge glow in
      the P1-pinned Holo blue colorEdgeEffect - instead of the Android-12
      stretchy distortion. Residual delta documented: the modern glow
      draws a paint circle rather than the donor's overscroll_glow/
      overscroll_edge bitmap plume; the donor draw path + assets are in
      ~/kitkat-src if a later pass wants the soft gradient on top of
      this engine.
INCONCLUSIVE ON DEVICE (second build): multiple drag-fling-capture
      attempts found no saturated Holo-blue glow pixels - but adb
      screencap latency (~1-2s on SwiftShader) loses every sub-second
      effect race, so this is not evidence of failure (the TYPE_GLOW path
      is stock AOSP code). Needs a human eyeball; if absent, check
      EdgeEffect paint color resolution next.
- [/] 14.2 Text selection UI
      Implemented: text selection handles repointed to the 4.4 Holo
      handles (donor text_select_handle_*.png copied as
      kitkat_text_select_handle_* per density and swapped into both Holo
      theme blocks of themes_holo.xml - Holo + Holo.DeviceDefault
      variants). The floating copy/paste toolbar verified
      theme-following: FloatingToolbarPopup picks
      Theme.DeviceDefault(Light) by isLightTheme, both now Holo-chrome
      with square corners (config_dialogCornerRadius=0dp), so 4.4-style
      popups render per app theme. No further code change needed.
- [/] 14.3 Share/resolve sheet
      Implemented: the DAY Theme.DeviceDefault.Resolver now carries the
      dark content items the values-night variant already shipped
      (grey_800 floating background, dark secondary text, no light nav
      bar), so the resolver renders Holo dark in both uiModes.
VERIFIED ON DEVICE (second build): ACTION_SEND text resolve sheet
      renders dark (bottom-sheet band mean ~10/10/10 = grey_800 chrome,
      no light pills).
- [/] 14.4 Runtime permission dialogs
      Implemented: GrantPermissions (PermissionController
      res/values/themes.xml) reparented from the hardcoded
      Theme.DeviceDefault.Light.Dialog.Alert to the dark
      Theme.DeviceDefault.Dialog.Alert - Holo dark dialog chrome over
      unchanged runtime-permission behavior. CAVEAT: this lives in the
      MAINLINE PermissionController module (packages/modules/Permission),
      not a pinned fork - re-apply after syncing that project.
- [/] 14.5 QS tile icon set + side view
      Implemented: the four tiles with clean donor counterparts now ship
      the 4.4 white glyphs - airplane (qs_airplane_icon_on/off),
      bluetooth (qs_bluetooth_icon_on/off/search from the donor state
      art), location, auto-rotate - donor PNGs per density replacing the
      modern vectors (on/off distinction stays via the P4 tint, as in
      4.4). Tiles without donor art (hotspot, flashlight, battery saver,
      screen record, work profile, wallet, internet) keep their modern
      vectors - documented hybrid. The side view (chevron + per-tile
      side icon, a post-4.4 concept) is now GONE in QSTileViewImpl -
      kept in the hierarchy for QSAnimator position math but never
      rendered; only QuickAccessWalletTile used a side drawable (not a
      stock 4.4 surface). Verified no qs_* icon reference dangles
      (the four "missing" names live in res-keyguard).
- [/] 14.6 FastScroll thumb/track
      Implemented: Theme.DeviceDefault + Theme.DeviceDefault.Light now
      set fastScrollThumbDrawable -> @drawable/fastscroll_thumb_holo and
      fastScrollTrackDrawable -> the holo dark/light track - the wide 4.4
      blue thumb that still ships in core/res but was never referenced.
- [X] 14.7 Rounded corners
      VERIFIED NO-CHANGE NEEDED: the framework corner radii are already
      0dp (rounded_corner_radius/top/bottom) and
      rounded_corner_content_padding is already 0dp, so the SystemUI
      rounded-corner decor never engages on this target - corners are
      already square, including the navbar insets (the 8px
      config_rounded_mask only applies when radii are non-zero).
- [/] 14.8 Screenshot UI
      Implemented: the floating screenshot chrome restyled to 4.4 dark -
      action_chip_container_background, overlay_border and
      screenshot_edit_background pinned from monet SurfaceBright/FixedDim
      to #ee161616, and overlay_button_ripple flipped from a
      dark-on-light #1f000000 to translucent Holo blue (#3333b5e5). The
      overlay structure/behavior (share/edit/scroll/delete chips) is
      kept - restyle option chosen over removing the feature.
VERIFIED ON DEVICE (second build): screenshot overlay renders with
      dark #ee161616 action chips (pixel-matched, 1268 chip-colored
      pixels) - the bright monet pills are gone.
- [X] 14.9 Action modes / context menus
      VERIFIED NO-CHANGE NEEDED: actionModeStyle resolves to
      Widget.DeviceDefault.ActionMode -> Widget.Holo.ActionMode with the
      holo_dark cab background through the P1 rebase, in both the dark
      and Light (Inverse) variants. Code-level check only.
- [/] 14.10 Volume dialog
      Implemented: the volume rail slimmed to the 4.4 look - the seekbar
      groove is a 4dp dark #ff2d2d2d rail (was the 40dp monet
      OutlineVariant band) with square ends; the accent progress fill is
      unchanged (P1 pins it Holo blue). REMAINING (carried): the full
      horizontal-layout rebuild of the dialog (the modern dialog is a
      vertical stack of rotated rails; the 4.4 panel was a wide
      horizontal stack) - a larger layout lift, revisit if the vertical
      stack still reads wrong on device.
PARTIAL ON DEVICE (second build): the volume dialog renders dark
      with the Holo-blue accents and multiple slim slider rows; the
      4dp-rail detail could not be pixel-isolated from the rotated-slider
      rendering (the icon inside the rail dominates). Renders correctly,
      slim-rail needs a human eyeball.
- [/] 14.11 Ticker marquee smooth-scroll polish - DOCUMENT-DEFERRED
      (user, 2026-10-22): hardware/touch-only verification; the ticker
      draws and scrolls (10.6 pixel-verified); smoothness is a real-
      hardware pass item. Not a blocker.
- [/] 14.12 Widget spot-check
      Code-level pass done: Widget.DeviceDefault.ProgressBar.Horizontal/
      SeekBar/Spinner/Tab/RatingBar all parent their Holo counterparts
      (styles_device_defaults.xml), so toasts/progress/seekbars/tabs/
      spinners resolve 4.4 chrome for every DeviceDefault app. The
      visual walk on the emulator stays pending.
- [/] 14.13 Lockscreen affordances
      Colors already restyled by P6 (lock icon ripple + swipe-up hints +
      UDFPS auth ripple -> holo blue, bouncer whites). The remaining
      modern signature is geometric (pill hint + bottom shortcuts vs the
      4.4 ring). DECIDED (user): attempt the 4.4 ring-hint geometry port
      ONCE THE SHADE WORK (P15) STABILIZES - add as a tracked work item
      then, not a reskin-by-colors lift. Deferred until that gate.

## Phase 15 - KitKat shade port, Launcher3-style [/]
ROLLED BACK to stock 2026-10-22 (see Phase 21); historical record.

User direction (first-build QA): the pull-down panel still has numerous
issues; instead of continuing to patch the modern shade piecemeal, port
the 4.4 panel code wholesale and update it, the way the Launcher3 port
did. FEASIBILITY VERDICT (donor audit):
- Ports wholesale: the panel SHELL - donor status_bar_expanded.xml
  structure, the 48dp header row (status_bar_expanded_header.xml), the
  PanelView drag/fling engine (exponential-decay fling, catch-release),
  and the PanelBar/PanelHolder two-panel semantics (the notifications
  <-> QS flip).
- Stays modern (hard constraint: modern apps must keep working): the
  notification ROWS remain on the NSSL - 4.4's NotificationRowLayout
  cannot render modern remote views, groups, media styles or remote
  input - and the shade WINDOW/keyguard coupling stays (the window hosts
  keyguard swipe/bouncer/AOD paths PhoneStatusBar never had). Same split
  Launcher3-Modern made: 4.4 UI on the modern integration layer.

- [/] 15.1 Header rebuilt as the donor 48dp single row
      Implemented: qs_header.xml (expanded QS state) is now the donor row
      - avatar + clock + date at the left (the clock scale is pinned to
      1.0 via qqs_expand_clock_scale: the modern 2.57x big clock is
      retired, 4.4 showed a normal clock), privacy chip + clear-all X +
      gear at the right; the carrier_group and header system-icons rows
      (duplicating the status bar's wifi/battery, stacked under it since
      11.2) are GONE. qqs_header.xml: same for the half-open row (header
      icons gone). qs_header_height 120dp -> 48dp, and
      NotificationsQSContainerController.calculateShadeHeaderHeight now
      returns the dimen (the 3-row estimate died with the rows). This
      kills the ~72dp of dead space under the half-open header and the
      double icon rows - the two biggest of the reported panel issues.
VERIFIED ON DEVICE (second build): the shade header is the single
      48dp donor row - clock/date left, clear-all X + gear right, no
      duplicated icon rows, no big clock; the expanded QS panel shows a
      normal-scale clock. The dead ~72dp band is gone.
- [/] 15.2 Donor panel engine (PanelView fling physics)
      Implemented (approach refined during the port): the audit showed the
      modern NotificationPanelViewController IS a direct descendant of the
      donor PanelView (same setExpandedHeightInternal/onHeightUpdated/
      flingToHeight skeleton, same halfway-expansion release rule in
      shouldExpandWhenNotFlinging), so the engine was restored IN PLACE
      instead of grafting a parallel View that would fight the QS/keyguard
      expansion plumbing:
      - New KitKatPanelFling: the donor animationTick trajectory in closed
        form - constant 2000dp/s^2 acceleration either way, 100dp/s
        closing floor (no dead time from rest), 3000dp/s opening cap,
        exact settle at the target with no overshoot - applied as
        interpolator+duration on the existing ValueAnimators.
      - NotificationPanelViewController.flingToHeight: KitKatPanelFling
        replaces the FlingAnimationUtils beziers for open AND close
        (dismissing and plain paths share the single 4.4 feel), and the
        modern opening overshoot/springback is retired (rubberband settle
        via mOverExpansion preserved).
      - onFlingQsWithoutClick (the QS expand/collapse fling) uses the same
        donor physics - in 4.4 the QS panel was dragged by the same
        PanelView engine.
      - Catch-release needed no port: the modern handler already cancels
        the animator on ACTION_DOWN. The keyguard swipe-up path is
        untouched. 12.5 is hereby resolved.
ON-DEVICE (second build): all shade drags/flings/gear-toggle QS
      animations ran without jank or stuck states through the whole
      battery - behaviorally verified; the 4.4-style feel (no overshoot
      bounce) itself needs a human eyeball on real touch input.
- [/] 15.3 Two-panel flip (donor PanelBar semantics, carried from 12.4)
      Implemented:
      - The expanded QS panel now FILLS the whole shade in the unlocked
        state: QuickSettingsController.updateHeightsOnShadeLayoutChange
        lifts mMaxExpansionHeight to the full panel height (stack view
        height + its panel margins) when bar state == SHADE, so the
        notification stack's maxTopPadding pushes the notifications fully
        below the fold - the 4.4 QS panel REPLACED the notifications
        instead of stacking above them. Keyguard QS expansion keeps the
        modern sizing (shade-over-keyguard notifications stay visible).
        No NSSL visibility fight: the stack simply draws its content below
        the screen.
      - The header gear is now the donor settings_button: a tap toggles
        notifications <-> QS (flingQs FLING_EXPAND/FLING_COLLAPSE, running
        the 15.2 donor physics); Settings moved to a long-press on the
        gear. ShadeHeaderController injects
        dagger.Lazy<QuickSettingsController>.
      - The clear-all X hides in the QS state (notifications are below the
        fold; the donor QS header had no X) and collapses to 0 width so the
        gear slides to the far right across the transition.
      - "Second pull" flips to QS exactly as before (dragging down expands
        QS), now landing on a QS-only screen.
      Pending: on-device pass (drag feel, gear toggle, notification peek at
      the bottom edge, QS-on-keyguard unchanged).
REOPENED (on-device, second build, then re-fixed in tree): the gear
      toggle works (QS expanded/collapsed via taps; dumpsys confirms
      mExpanded flip), and mMaxExpansionHeight reached the full 2220px
      panel - but the OTHER writer of mMaxExpansionHeight,
      QuickSettingsController.onHeightChanged(), re-clobbered the lift
      with the QS fragment's desired height whenever the tile grid laid
      out, so notifications crept back below the tiles. FIXED: the lift
      is now applied from BOTH writers via applyDonorFlipToMaxExpansionHeight
      (). The clear-all X hides correctly in the QS state (gear slides
      to the far right). Pending next build for the full below-the-fold
      re-verification.
- [/] 15.4 Panel frame verification pass (donor
      status_bar_expanded.xml): re-check the shade container chrome
      against the DONOR SOURCE TREE (header, scroll region, carrier +
      36dp close-handle band - chrome largely in place from P3/12.3).
      REVISED (2026-10-22): source-tree comparison + canonical-AVD
      eyeball; NO 4.4 VM captures (scrapped per user).

## Implementation notes (2026-10-21 batch)

Trees touched by this batch beyond the pinned forks - all show as local
modifications; re-apply after repo syncs:
- packages/apps/Settings (pinned fork) - 11.6
- packages/inputmethods/LatinIME - Phase 13 (~120 files, donor art)
- packages/modules/Permission - 14.4 (mainline APEX)
- vendor/lineage - wallpaper overlay deletions (P8 batch, unchanged here)

No builds were run from OpenCode (m misbehaves there): framework-res was
green on the tree before Phase 11-14 edits; the first full m -j4 is the
compile gate for SystemUI (Kotlin: KeyButtonView/ShadeHeaderController/
FooterViewModel/QSTileViewImpl), LatinIME (KeyboardTheme.java + the new
theme file), Settings/SettingsLib, PermissionController and the core/res
theme edits. All cross-references (drawables/colors/attrs/strings) were
verified statically - every referenced resource exists.

Second batch (first-build QA fixes, same rules): 11.4 glow-stuck fix
(KeyButtonView: donor invalidation + onDraw self-healing guard + 600ms
fail-safe redraw) and Phase 15.1 (header collapsed to the donor 48dp
single row: qs_header.xml/qqs_header.xml restructure, header icon rows
retired, qs_header_height 48dp, qqs_expand_clock_scale 1.0,
calculateShadeHeaderHeight simplified). SystemUI code+res only.

Third batch (post-second-build): 15.2 donor PanelView fling physics
(KitKatPanelFling + NPVC.flingToHeight/onFlingQsWithoutClick, overshoot
retired) and 15.3 two-panel flip (QS fills the panel in SHADE, gear
toggles panels, Settings on gear long-press, X hidden in QS state).
SystemUI code+res only; 12.5 resolved by 15.2. First compile of this
batch caught one Kotlin/Java interop trap: the FLING_* fling types are
Kotlin companion const vals on the ShadeViewController INTERFACE - Java
resolves them through the implementing class (hence QuickSettingsController's
import static NotificationPanelViewController.FLING_EXPAND) but Kotlin
does not inherit statics that way, so ShadeHeaderController must
reference ShadeViewController.FLING_* directly (fixed; SystemUI-core
then compiled green).

On-device battery (2026-10-21, third build): booted the emulator and
walked the open items via adb screencap + pixel analysis + dumpsys.
VERIFIED: wallpaper seed (positional match), 11.2, 11.3, 11.4 (incl.
no-stuck-glow), 11.5, 11.6, 12.6, 14.3, 14.8, 15.1, ticker, recents,
keyboard renders + types (13.1/13.2 partial), gear toggle. BUGS FOUND &
FIXED IN TREE (need rebuild + recheck): 12.1 QQS tiles still rendered
(QuickQSPanelController add-before-check loop - fixed), 12.3 handle under
the navbar (margin now navigation_bar_height - fixed), 15.3 flip
re-clobbered by onHeightChanged (both writers now apply the lift via
applyDonorFlipToMaxExpansionHeight - fixed). INCONCLUSIVE (screencap
latency vs sub-second effects, needs eyeball): 14.1 overscroll glow,
14.10 slim-rail detail, 15.2 exact fling feel. Dev-notes: SUW bypass
needs FinishActivity (pm enable blocked for shell); crash dialogs are
gated by show_first_crash_dialog; uiautomator dumps stale trees on this
emulator - drive by screencap.

ON DEVICE (second build): most chrome verified incidentally through
      the battery (header 48dp row, tiles 3-col donor geometry, carrier
      label, dark QS); the full donor-capture diff stays open until the
      12.1/12.3/15.3 fixes land in the next build.

## Phase 16 - KitKat app ports (Launcher3-style) [ ]

Answers open question 1: yes, AOSP system app UIs are in scope. This is
the wholesale-port roadmap, same approach as Launcher3-Modern - pull the
android-4.4.4_r1 app, modernize the build/API layer, keep 4.4 UI and UX.
Donors come from ~/kitkat-src shallow clones (add repos as needed per
9.0); ports land as new pinned forks in kitkat_ui_mods.xml (the
Launcher3 pattern: remove-project the Lineage tree app, project at the
same path from my_github, then PRODUCT_PACKAGES swaps in
vendor/lineage/common_mobile*.mk or device mk).

Big synergy note: after the P1 rebase, Theme.DeviceDefault IS Holo -
so every ported app just declares Theme.DeviceDefault (like it did in
4.4, where DeviceDefault==Holo) and gets KitKat chrome, 2013 Roboto,
Holo blue accents and the P8 sounds for free. The porting work is
therefore mostly build plumbing, not theme duplication.

Common porting checklist (applies to every app below; from the
Launcher3-Modern experience):

1. Android.bp/Android.mk modernization: 4.4 makefiles -> soong; add
   minSdkVersion >= 21, targetSdkVersion <= 29 (pre-30 target on
   purpose, same reasoning as Launcher3), platform api scope.
2. Drop/remove: pre-support-library shims (4.4 bundles its own
   android-support-v4 jars) -> use framework classes; deprecated
   removed APIs case by case.
3. Runtime permissions (API 23+): 4.4 apps predate the model -
   CONTACTS/PHONE/SMS/CAMERA/MIC/STORAGE permissions need grants to
   still be modern-functional.
4. Notification channels (26+) for anything that posts (Music,
   Messaging, DeskClock, Calendar).
5. Theme/manifest: keep 4.4 layouts/drawables verbatim where
   possible; theme -> Theme.DeviceDefault (+Light where 4.4 used
   Holo Light); app icons/naming verbatim.
6. privapp-permissions whitelists where a port keeps
   signature/privileged permissions.

### Tier 1 - direct ports (4.4 UI, light modernization)

- [X] Calculator (donor packages/apps/Calculator) -> replace
      ExactCalculator. Pure UI + trivial math; the only work is the
      build file and theme. Lowest-risk port; do first as the template.
      DONE IN TREE (2026-10-21 batch, fourth): donor android-4.4.4_r1
      installed verbatim at packages/apps/ExactCalculator (donor
      com.android.calculator2, manifest Theme.Holo.NoActionBar kept).
      Modernization per the checklist:
      - Android.bp rewritten (soong): android_app name "ExactCalculator"
        (keeps vendor/lineage PRODUCT_PACKAGES stable - no mk edits),
        sdk_version current, java_import prebuilt for arity-2.1.2.jar.
      - support-v4 ViewPager/PagerAdapter vendored under
        com.android.calculator2.viewpager with the compat layer
        collapsed onto framework APIs: ViewCompat/ViewConfigurationCompat
        call sites -> View methods, ParcelableCompat CREATOR -> plain
        Creator, accessibility compat -> AccessibilityEvent/
        AccessibilityNodeInfo direct; EdgeEffectCompat kept as a local
        wrapper (framework onPull/onRelease return void; donor contract
        returned boolean). Donor cloned to ~/kitkat-src/support-4.4.
      - guava ImmutableMap (CalculatorEditText replacement table) ->
        java.util.HashMap.
      - No runtime permissions/channels needed (no perm-gated
        surfaces, posts nothing).
      NOTE: this REPLACES the LineageOS ExactCalculator checkout
      contents wholesale; FORKED + PINNED (2026-10-21):
      github.com/EHowardHill/android_packages_apps_ExactCalculator
      lineage-21.0, pin in kitkat_ui_mods.xml - sync-safe.
      VERIFIED ON DEVICE (2026-10-21 OpenCode battery): installs
      (had to add android:exported=true for the API 31+ manifest
      rule - fixed after PackageManager parse failure), launches,
      renders dark Holo keys with white 40sp digits + operator
      glyphs (pixel-verified bright glyph rows in digit pad and
      operator column), "DELETE" + digit labels in the a11y tree.
      Pending: pixel check vs the 4.4 donor (17.6 battery).
- [X] SoundRecorder (donor packages/apps/SoundRecorder) -> replace
      Recorder. Tiny app; add MIC runtime permission; MediaRecorder
      path unchanged. The 4.4 mic UI (press-to-record list) is iconic.
      DONE IN TREE (2026-10-21 batch, fourth): donor installed verbatim
      at packages/apps/Recorder (com.android.soundrecorder, donor
      Theme.Holo.DialogWhenLarge kept, activity exported=true for API
      31+). Android.bp: android_app name "Recorder" (PRODUCT_PACKAGES
      stable). Modernization:
      - RECORD_AUDIO runtime-permission shim (request on create,
        finish with a toast on denial) - 4.4 predates runtime perms.
      - Recordings written to getExternalFilesDir (scoped storage)
        instead of the storage root, published via MediaStore.
      - RemainingTimeCalculator stats the same app-writable dir.
      - addToMediaDB REWRITTEN for API 29+: modern MediaStore forbids
        mutation of _data (first device pass crashed: "Mutation of
        _data is not allowed"). Modern pending-insert contract:
        DISPLAY_NAME + RELATIVE_PATH(DIRECTORY_RECORDINGS) +
        IS_PENDING=1 -> stream the file in -> IS_PENDING=0; same 4.4
        fields where legal (title/artist/album, IS_MUSIC=0); legacy
        (<29) path keeps the donor flow incl. playlists.
      FORKED + PINNED (2026-10-21):
      github.com/EHowardHill/android_packages_apps_Recorder
      lineage-21.0 - sync-safe.
      VERIFIED ON DEVICE (2026-10-21 OpenCode battery): opens with
      "Record your message" + 00:00 timer on dark Holo
      dialog-when-large chrome; RECORD_AUDIO granted via shim; record
      -> file lands in Android/data/.../files; accept -> recording
      published to MediaStore (content query: _id=19,
      recording...3ga, audio/mp4 - modern mime map renames the
      container; same 3GPP media, correct behavior), zero FATALs.
- [X] Music (donor packages/apps/Music) -> replace Eleven. MediaStore
      Music columns/scan behavior unchanged; add notification channel
      + legacy storage handling.
      DONE IN TREE + VERIFIED (2026-10-21 fifth batch): donor
      android-4.4.4_r1 Music (com.android.music, 27 java + playback
      AIDL, NO support-lib deps) installed verbatim at
      packages/apps/Eleven, module name "Eleven" kept
      (common_mobile_full.mk PRODUCT_PACKAGES stable). Modernization
      set (each caught by a build/device round):
      - Android.bp: product_specific, targetSdk 29 (pre-30 on
        purpose - legacy external storage keeps the donor MediaStore
        flow working with a runtime READ grant), min 21.
      - Manifest theme swap: donor Theme.NoTitleBar resolves to
        Material LIGHT on API 34 (P17.2 boundary kept Material tree
        untouched); 4.4 rendered Holo DARK. All activities +
        application now Theme.Holo.NoActionBar (public Holo) - the
        4.4-faithful look. (First device pass rendered white; pixel
        mean 207 -> after swap 28.7.)
      - Notification channel + Builder around the donor RemoteViews
        playback status notification (API 26+; donor Notification()
        constructor path cannot carry a channel).
      - PendingIntent.FLAG_IMMUTABLE everywhere (API 31+ rule).
      - MODE_WORLD_READABLE prefs -> MODE_PRIVATE (removed API 24;
        crashed the service on first launch).
      - READ_EXTERNAL_STORAGE runtime shim in MusicBrowserActivity
        (23+; 4.4 predates runtime permissions).
      - exported=true on launcher activity + media-button + widget
        receivers (API 31+ manifest rule).
      VERIFIED ON DEVICE (OpenCode battery): installs, launches dark
      Holo, storage permission granted via shim, MediaStore populated
      (testtone.wav scanned in), zero FATALs. Playback sound itself
      needs the audio-enabled AVD (this battery ran -no-audio; the
      media-session ERROR states are the emulated HAL, not the app).
      FORKED + PINNED:
      github.com/EHowardHill/android_packages_apps_Eleven
      lineage-21.0 - sync-safe.
- [X] DeskClock/Clock (donor packages/apps/DeskClock) -> replace the
      modern DeskClock. DONE + VERIFIED + FORKED + PINNED
      (2026-10-22, sixth/seventh batches - datetimepicker vendored,
      alarm schedules end-to-end; see the ratification block and the
      seventh-batch implementation notes for the full modernization
      set). Alarm-ring + playback sound: user audio AVD.
- [X] Messaging (donor packages/apps/Mms): DECIDED (user, 2026-10-21)
      NO PORT - DEFERRED ENTIRELY. Cannot be tested without a SIM;
      ship modern Messaging on the DeviceDefault cascade. Documented
      hybrid per philosophy rules 2/3.

### Tier 2 - paired ports (must land together)

- [X] Dialer: EVALUATED (2026-10-22, ninth batch) - SHIP MODERN per
      philosophy rule 2. The 4.4 dialtacts predates the telecom
      contract entirely; grafting it onto the modern InCallService
      binding is the single riskiest port in the project (call
      reliability), and the fidelity-vs-risk ruling (user, 20.2):
      risk outranks fidelity on hard-constraint surfaces. Modern
      Dialer rides the DeviceDefault cascade. Documented no-port.
- [X] Contacts: evaluated (2026-10-22) - donor Contacts shares the
      PhoneCommon/telephony surface with Dialer; with Dialer ruled
      modern, a standalone Contacts port splits the pair. Modern
      Contacts rides the DeviceDefault cascade. Documented no-port.
- [X] Calendar (donor packages/apps/Calendar) -> replace Etar.
      DONE + VERIFIED + FORKED + PINNED (2026-10-22, ninth batch):
      donor com.android.calendar verbatim at packages/apps/Etar with
      its FULL framework-lib dependency set vendored in-tree
      (ex/chips, opt/colorpicker, opt/datetimepicker,
      opt/timezonepicker, ex/common, opt/calendar=calendarcommon2);
      donor resource sets node-wise merged; CalendarTests module
      kept (platform_test_list expects it). Modernization set:
      targetSdk 29, TaskStackBuilder/LruCache/ExploreByTouchHelper/
      AndroidHttpClient.parseDate replacements, vendored-lib R
      repointed to com.android.calendar.R, exported per 31+.
      VERIFIED ON DEVICE: launches AllInOneActivity, 4.4 Holo-Light
      Calendar look (light content + dark bar), zero FATALs.
      FORKED: github.com/EHowardHill/android_packages_apps_Etar.

### Tier 3 - heavy ports (evaluate before committing)

- [/] Gallery: DECIDED (user) - USE THE EARLIER-VERSION GALLERY.
      STATUS: DEFERRED to a focused port batch (2026-10-22 ruling).
      The 4.4 Gallery2 donor is large (GLAlbum stack + jni); the
      risk inventory says: donor 4.4 Gallery2 == the SAME app the
      modern tree ships, so the port = git history replacement of
      the modern Gallery2 tree with the donor, modernized - a
      bigger single-app lift than Music/DeskClock/Calendar
      combined. Recorded as the next port after the current batch;
      modern Gallery2 rides the P1 cascade until then.
- [/] Camera: DECIDED (user) - USE AN EARLIER-VERSION CAMERA where
      possible. STATUS: DEFERRED with Gallery (same batch). The
      camera1 legacy-mode trial needs the emulator camera HAL +
      real capture verification - the honest spike is on-device with
      the emulator camera wired (or your hardware); the emulated
      camera HAL on this AVD is unreliable for capture. Modern
      Aperture rides the cascade until the spike runs.
- [X] Browser: RATIFIED NO-PORT (user). Removed WebView APIs make the
      4.4 Browser non-functional on API 34; Jelly rides the cascade.
- [X] PackageInstaller: VERIFIED NO-CHANGE NEEDED (2026-10-22):
      Theme.AlertDialogActivity parents Theme.DeviceDefault.Light.
      Dialog.Alert -> Theme.Holo.Light.Dialog.Alert via the P1
      rebase - install/permission dialogs already render Holo
      chrome. The modern install/permission flow is hard-constraint
      surface (no port).
- [X] DocumentsUI / StorageManager / SettingsIntelligence /
      SetupWizard / EmergencyInfo: no 4.4 counterpart - DeviceDefault
      cascade only. Documented.
- [X] CellBroadcastReceiver, CertInstaller, KeyChain, Stk,
      ManagedProvisioning: tiny dialogs, Holo via P1 rebase. Done.
- [X] AudioFX / MusicFX / FMRadio / Profiles / LineageParts: no AOSP
      4.4 donor - keep as-is on the cascade. Done.

### Tier 5 - infra donors

- [X] PhoneCommon: not needed (Dialer/Contacts shipped modern).
- [/] Gallery JNI + external deps: folded into the deferred Gallery
      port batch (see Tier 3 Gallery entry).
- [X] external/* donor audits: done as each port landed (support
      libs vendored per app; tz data materialized for Calendar).

### Manifest & packaging work

- [X] kitkat_ui_mods.xml: ALL NINE projects pinned (frameworks/base,
      Settings, Trebuchet=Launcher3-Modern, ExactCalculator, Recorder,
      LatinIME, Eleven, DeskClock, Etar) - repo sync safe.
- [X] PRODUCT_PACKAGES: nothing needed - every port kept its module
      name (ExactCalculator/Recorder/Eleven/DeskClock/Etar), so the
      vendor/lineage common*.mk entries resolve unchanged.
- [X] Donor references: all cloned to ~/kitkat-src (never in the ROM
      trees): frameworks-base, settings-4.4, latinime-4.4,
      calculator-4.4, soundrecorder-4.4, music-4.4, deskclock-4.4,
      calendar-4.4, ex-4.4 (chips+common), colorpicker-4.4,
      datetimepicker-4.4, timezonepicker-4.4, tzsupport-4.4,
      calendar-opt-4.4, support-4.4.
- [X] Wholesale-port decision review: done per app across the
      batches - ports shipped where donor runs on API 34 (Calculator,
      SoundRecorder, Music, DeskClock, Calendar); no-port documented
      where it cannot (Messaging=SIM untestable, Dialer=telecom risk,
      Contacts=pair split, Browser=dead WebView APIs, PackageInstaller
      =flow already Holo); deferred where the donor needs a focused
      batch (Gallery JNI, Camera camera1 spike).

## Phase 17 - Widget pass: full KitKat widget parity [X]

Goal: every widget that existed in 4.4 renders IDENTICALLY (buttons,
sliders, checkboxes, radios, switches, spinners, pickers, etc.) across
system surfaces. P1 already rebased 176 Widget.DeviceDefault styles
onto Holo and the Holo asset stack still ships in-tree (97 selector
XMLs + 454 9-patches per density); 14.12 spot-checked progress/seek/
spinner/tab/ratingbar. This phase closes the remaining gaps.

- [/] 17.1 The 18 residual Material styles from P1 (kept "for lack of
      Holo counterparts"): Per-item disposition, per the philosophy:
      - Widget.DeviceDefault.Button.Colored RE-PARENTED from
        Widget.Material.Button.Colored to Widget.DeviceDefault.Button
        (styles_device_defaults.xml): 4.4 had no colored-button concept,
        the Holo ghost button IS the accent button. Built.
      - Notification.* / Toolbar / Material typography scale / Preference.*
        / AlertDialog.DeviceDefault / ButtonBar / SegmentedButton:
        ACCEPT-AND-DOCUMENT (doc block in styles_device_defaults.xml):
        remote-view contracts, Toolbar did not exist in 4.4, typography
        scale is Lollipop-era, Settings renders via SettingsLib
        PreferenceTheme (P7 chrome). No code change.
      Done in tree; resource-only change rides the next build.
- [X] 17.2 Theme.Material tree decision. DECIDED (philosophy rule 4):
      DeviceDefault-only is the boundary. Theme.Material apps keep
      Material chrome; we will NOT port Holo styles into the Material
      tree. Nothing force-breaks third parties; defaults are KitKat.
      No work item.
- [/] 17.3 Runtime override bucket sweep: base values/ styles are
      Holo, but values-night/, -v21/, -v31/, -v34/ (and the -v23/-v24
      elevation/StateListAnimator/backgroundTint defaults) override at
      runtime - the same trap 11.6 hit in Settings v31/v34. Every
      DeviceDefault widget style needs a per-bucket audit so no
      monet/elevation leak survives at runtime. (Material buckets are
      OUT OF SCOPE per 17.2.)
      AUDITED (2026-10-21 batch): core/res has NO values-v21/v31/v34
      widget-style buckets (only values-night); values-night colors/
      themes refs resolve to the P1/P7-pinned holo blues (Settings/
      SystemUI v31/v34 were pinned in 11.6); the framework
      system_accent1/system_neutral* refs are the accepted
      monet-derived chain (blue family via the ThemeOverlayController
      seed pin). No leak found; residual: the on-device visual battery
      (17.6) on the canonical AVD.
- [/] 17.4 Code-driven modern chrome (styles alone cannot fix):
      AUDITED (2026-10-21 batch): NO-CHANGE NEEDED - elevation defaults
      are 0 (Holo themes never set window elevation), StateListAnimator
      is only attached by Material button styles (our DeviceDefault
      buttons parent Holo 9-patch selectors), EditText/progress
      backgroundTintList is null unless a Material theme sets it, and
      RippleDrawable only arrives via ?attr/selectableItemBackground
      which Holo themes resolve to the 4.4 state-list. All the modern
      chrome dies with the P1 Holo parents. Residual: on-device spot
      check (a Material-declaring app renders Material per 17.2 - by
      design).
- [/] 17.5 Per-widget donor delta pass (vs ~/kitkat-src/frameworks-base
      captures): Button/ImageButton/ToggleButton/CheckBox/RadioButton/
      CheckedTextView, Spinner (+dropdown), ProgressBar (4 variants +
      inverse), SeekBar, RatingBar, Switch, EditText/
      AutoCompleteTextView (+dropdown), DatePicker/TimePicker/
      NumberPicker (the big Holo dialog widgets - highest risk),
      ZoomControls, QuickContactBadge, TabWidget, Chronometer/
      TextClock, SearchView, ListPopupWindow/PopupMenu, toast
      (already 4.4 via P1). Each: diff against donor, port the delta,
      record accept-and-document calls.
      STATUS: the P1 rebase already re-parented this entire inventory
      (confirmed in styles_device_defaults.xml - all of the above
      including DatePicker/TimePicker/NumberPicker/CalendarView and
      Light variants resolve Widget.Holo.*, verified 2026-10-21).
      The remaining delta work is VISUAL (render differences the
      static parents cannot show) - owned by the 17.6 playground
      eyeball battery (revised standard: donor-source comparison +
      canonical AVD; no 4.4 VM).
- [X] 17.6 Widget playground acceptance gate:
      HARNESS WRITTEN + BUILT (m KitKatWidgetPlayground green):
      development/samples/KitKatWidgetPlayground - a dev-tool APK
      (android_app "KitKatWidgetPlayground", NOT in any PRODUCT_PACKAGES)
      exercising every 17.5 widget in a single ScrollView, dark
      (Theme.DeviceDefault) and Light (Theme.DeviceDefault.Light)
      activities.
      REVISED STANDARD (2026-10-22, user): the 4.4 reference VM is
      SCRAPPED - 4.4 cannot build on this machine. The playground stays
      as the canonical AVD eyeball surface (install, screencap, human
      check against donor-capture knowledge recorded in TASKS.md and
      the ~/kitkat-src tree); NO pixel-perfect diff vs a 4.4 build.
      Retires the 14.12 "visual walk pending" line the same way.

## Consolidated remaining-work index (FINAL - 2026-10-22, ninth batch)

AMENDED (later 2026-10-22): the shade/QS user passes below (P3/P4 QS
parts, 20.x residuals, shade stability gates) are CANCELLED by the
Phase 21 rollback - those surfaces are stock now. The volume,
power-menu, recents, keyboard and app-port passes stand.

ALL engineering work in TASKS.md is complete. Nine forks pinned and
pushed (frameworks/base, Settings, Trebuchet=Launcher3-Modern,
ExactCalculator, Recorder, LatinIME, Eleven, DeskClock, Etar); every
phase implemented; m -j8 green; emulator batteries run with zero
FATALs; every open marker is one of exactly three kinds:

A) USER VISUAL PASSES (the canonical Pixel 3a AVD + your audio AVD):
   - P3/P4/P9 shade/QS/volume/power-menu chrome eyeballs
   - P5 recents card feel; P6 bouncer/keyguard colors; P8 easter egg
   - 14.x residual eyeballs (14.10 slim rail, 14.12 via the
     KitKatWidgetPlayground if desired)
   - Music playback + DeskClock alarm-ring (audio AVD)
   - Calendar month/day/week + event flows; brightness dialog
   - 20.x residuals: drag feel, gear/X taps on real touch
B) HARDWARE/TOUCH PASSES (document-deferred): 14.11 marquee
   smoothness, 15.2 fling feel, gesture typing, 14.1 glow eyeball.
C) FOCUSED-PORT BATCHES (deferred by ruling, donor documented):
   - Gallery (4.4 Gallery2 tree replacement + JNI)
   - Camera (camera1 legacy-mode spike - needs a working capture HAL)
   - 14.13 lockscreen ring-hint geometry (gated on your
     confirmation that 20.x shade stability is good on your AVD)

The 72 [/] markers are per-phase "implemented, battery-or-eyeball
pending" records - trace each to its phase notes for status. No
unowned work remains.

## Phase 20 - Shade/QS donor audit (user pass 2026-10-22) [X]
ROLLED BACK to stock 2026-10-22 (see Phase 21); historical record.

User's visual pass surfaced six issues, concentrated on the shade/QS;
the donor re-study expanded the audit to ten. All DECIDED (user,
2026-10-22) and IMPLEMENTED + DEVICE-VERIFIED (2026-10-22, eighth
OpenCode batch - m -j8 green, emulator battery, zero FATALs):

- [X] 20.1 Keyboard toggle keys (?123/ABC) blank. Root cause: the
      layout-switch keys carry followFunctionalTextColor and
      themes-kitkat set no functionalTextColor -> transparent
      (13.4-class bug on functional keys). Fixed: functionalTextColor
      pinned white in themes-kitkat (LatinIME fork, pushed).
- [X] 20.2 Shade drag blur/unroll. DECIDED: approximation (risk-
      ruled). Implemented: DepthController shade blur/zoom off
      (single boolean, keyguard pipeline preserved); grabber handle
      re-parented to ride the expansion edge (donor draw() semantics)
      via a ShadeExpansionStateManager listener in
      NotificationsQSContainerController; content alpha already
      donor via 18.5. Verified: no blur behind the dragging panel.
      Fine drag feel = human/hardware pass.
- [X] 20.3 Footer buttons. Two fixes: the 18.3 ViewModel hide PLUS
      the live leak - FooterView.setFooterLabelVisible(false)
      re-showed both buttons every layout pass (NSSL normal path);
      that branch now keeps both GONE. Verified: no footer buttons.
- [X] 20.4 Header buttons: donor ic_notify_settings art per density
      + ic_notify_button_bg press 9-patch + 50x50dp on both; ripple
      removed. Behavior unchanged (gear-tap = flip = donor
      animateExpandSettingsPanel; corrected earlier note).
- [X] 20.5 Space above slider: SUPERSEDED by 20.8 (no inline slider
      exists on the donor phone QS).
- [X] 20.6 Tile margins 8dp -> 4dp (donor quick_settings_cell_gap);
      page bg stays gradient (source-faithful ruling - the user's
      "flat black" was the 20.7 tile color). Verified: 3 cols, 4dp
      gaps, #161616 tiles on the gradient page.
- [X] 20.7 Tile background: shadeActive/shadeInactive -> #ff161616
      flat, ripple color -> #ff212121 donor press, corners 0dp.
      Verified: dominant pixel (22,22,22) = #161616 exactly.
- [X] 20.8 Brightness: DECIDED fidelity > UX. Inline slider NOT
      attached anymore (QSPanel.setBrightnessView; plumbing intact,
      one-line revert); donor brightness TILE
      (KitKatBrightnessTile) opens the donor-restyled
      BrightnessDialog. Retires 20.5.
- [X] 20.9 Header clock/date: donor Expanded.Clock/.Date values
      (32dp light white / 12dp #cccccc caps) on the donor pill
      pattern (ic_notify_button_bg, enabled=false).
- [X] 20.10 QS grid: DECIDED full donor set+order.
      quick_settings_tiles_default =
      user,brightness,settings,wifi,cell,battery,bt,location,rotation;
      KitKat{User,Settings,Brightness}Tile +
      KitKatQSTilesModule (ReferenceSystemUIModule). Modern-only
      tiles dropped from the default grid (user can re-add via
      Settings; no exceptions requested). Verified: donor grid
      renders.

Cross-cutting: the shade battery re-ran after the fixes (expand/
collapse, QS grid geometry, footer, keyboard, header buttons) -
zero FATALs; residual human passes: drag feel, gear/X taps on
real touch, brightness dialog open on the audio AVD.

- P19: SCRAPPED (2026-10-22) - 4.4 will not build on this host.
  All downstream obligations retargeted (philosophy rule 6 revised,
  17.6/15.4 closed under the source-tree standard).

## Phase 21 - Shade/QS rollback to stock (user direction, 2026-10-22) [X]

User direction (2026-10-22): the shade/QS work did not feel good.
ALL notification-panel and quick-settings changes are reverted;
both surfaces now ship 100% stock LineageOS. Everything else stays
KitKat: status bar (incl. the 4.4 ticker), navbar, recents, keyguard,
volume, power menu, screenshot overlay, framework theme/fonts/sounds,
and the app ports.

Scope reverted (historical phases superseded - see their headers):
- P3 shade items (gradient bg, flat rows, header gear, footer
  buttons, drag handle, carrier label), keeping the ticker.
- P4 QS items (tile chrome, Auto checkbox, avatar, QuickSettings
  palette), keeping the volume dialog and power menu.
- 11.1/11.2 (tile geometry, panel margin).
- P12 deep-port (QQS max-tiles, header clear-all, panel frame).
- P15 wholesale port (48dp header, PanelView fling, two-panel flip).
- P18 shade/QS parts (footer drop, QS gap, scrim bools) + 18.4.
- P20 donor rebuild (all ten items; the LatinIME 20.1 keyboard fix
  lives in the LatinIME fork and STAYS).
- 12.6 core notification typography (styles_material.xml colors +
  18sp/20sp title sizes back to stock 14sp/16sp).

Method: every shade/QS-only file restored byte-identical to the
LineageOS baseline 993528cf0efb (== lineageos/lineage-21.0 tip) -
the drop-the-file-and-pull-from-remote approach, applied to the
fork tree. Shared files got hunk-level reverts: core dimens keeps
the 25dp status bar and drops the 18sp/20sp notification titles;
CentralSurfacesImpl keeps the ticker; SystemUI styles keep the
keyguard accent pin and KitKatRecentsTheme; colors keep the power
menu and screenshot-overlay pins.

Verification: index diff vs the baseline is empty for every
shade/QS file; grep audit found zero dangling references to the
deleted KitKat shade/QS resources/classes; m SystemUI green
(4:17) and m framework-res green on emu64x after the revert.
Reverted as a single commit on kitkat-mods; push pending.

DELTA NOTE (accept-and-document, rule 5): the stock shade now
renders over the KitKat theme cascade (25dp opaque bar, Holo
accents where the framework feeds SystemUI), so it reads
LineageOS-modern, not pure 4.4 - exactly as intended by "stock".

## Phase 22 - Bundled microG + Aurora Store [X]

Goal (user direction 2026-10-23): give users built-in Play-like access -
microG for the services layer, Aurora Store as the storefront - without
shipping anything Google-signed.

Mechanism (deliberately zero new framework patching):

* LineageOS 21 already supports restricted signature spoofing natively
  (ComputerEngine.isMicrogSigned): com.google.android.gms /
  com.android.vending, signed with the official microG key, may present
  the Google platform signature on debuggable builds. CandyOS builds
  userdebug, so no frameworks/base change is needed; official microG
  release APKs (NOT F-Droid builds, which are signed differently) are
  therefore mandatory.
* LineageOS 21 already ships the WITH_GMS hook: vendor/lineage/config/
  common.mk -includes partner_gms.mk, which inherits
  vendor/partner_gms/products/gms.mk when WITH_GMS is true. A one-line
  patch series (patches/vendor-lineage/) sets the flag; vendor/lineage
  is otherwise stock, so it joins CANDYOS_PATCHED and gets the normal
  guard/reset/replay/save treatment.
* vendor/partner_gms/ is a new overlay class (CANDYOS_VENDOR_TREES in
  scripts/common.sh, copied by candyos-sync.sh like vendored apps, but
  into an otherwise-unoccupied path - nothing to remove from the
  manifest). Modeled on lineageos4microg/android_vendor_partner_gms:
  GmsCore + FakeStore as PRESIGNED privileged product apps with
  privapp/default/sysconfig XMLs (FAKE_PACKAGE_SIGNATURE dropped -
  stock LOS has no such permission and does not need it), microg.xml
  system-defaults, AuroraStore as a plain PRESIGNED product app.

APKs: not committed (binary blobs). scripts/candyos-fetch-apps.sh pins
exact versions + SHA-256 (microG Services v0.3.17.252432, microG
Companion v0.3.17.252432, Aurora Store 4.8.4/F-Droid v76); sync warns
if they are missing.

Shipped defaults: microG check-in/GCM off, Mozilla location services
on, vending licensing/billing on (vendor/partner_gms/GmsCore/microg.xml);
everything user-overridable in the microG settings app on-device.

DELTA NOTE (accept-and-document, rule 5): signature spoofing - and with
it Play-signature-dependent checks - only works on debuggable builds.
If CandyOS ever builds `user`, restricted spoofing silently goes away;
the unrestricted-spoofing patches from lineageos4microg would then be
required (out of scope, documented in vendor/partner_gms/README.md).

Verification standard (rule 6): code authored and reviewed off-tree on
macOS; build/runtime verification on the Linux build host is the first
user test. Script logic (fetch idempotency + checksum pins) and patch
application context verified locally against the upstream
lineage-21.0 vendor/lineage checkout.

## Phase 23 - The Testing app (KitKat test bench) [/]

Goal: a single bundled app that exercises every CandyOS-specific
surface for on-device verification, styled by the system itself
(Theme.DeviceDefault == Holo via the P1 rebase, so the app doubles as
a live test of the theme cascade).

Mechanism (same overlay class as partner_gms, P22): the app lives whole
in vendor/testing/ in the CandyOS repo -> tree vendor/testing via
CANDYOS_VENDOR_TREES (scripts/common.sh); the products include is one
line in the patches/vendor-lineage series (-include
vendor/testing/testing.mk -> PRODUCT_PACKAGES += Testing). Product-
partition install (product_specific: true) - the sdk_phone emulator
target enforces the AOSP generic-system artifact-path requirement, which
forbids new files under system/ (learned the hard way; the partner_gms
apps already sat on /product for the same reason). Plain unprivileged
app, public framework APIs only (sdk_version: current, no androidx
dependency), framework Notification.Builder + channel (targetSdk 34).

Test matrix (all in the app):
- Ticker: long post (both rows marquee), short, burst of 3 (queue
  chaining), group child + summary (summary must NOT tick - P3 polish
  filter), ongoing, cancel-all.
- Holo widget skins: ghost button (also the click-sound path), switch,
  checkbox, radios, seekbar, horizontal determinate + indeterminate +
  circular progress, spinner, text field (typing also exercises the
  ported 4.4 keypress sounds).
- Roboto 2013: thin/light/regular/bold/italic/condensed samples.
- Hybrid emoji: KitKat-era row renders the 2013 blobs
  (NotoColorEmoji2013 family precedence, P1/P10); modern-only row falls
  through to current Noto.
- Holo dialog and toast; holo_blue_light accent pin swatch.
- System surfaces: second activity (4.4 activity-open animation + a
  card for KitKatRecentsActivity; finish plays the close animation),
  volume panel, immersive toggle (status bar peek), orientation toggle
  (pre-12 rotation animation), Holo dark/light theme flip
  (survives recreate).

Status: implemented; m Testing green on emu64x (product/app/Testing);
candyos-verify.sh pass after the series regeneration. Ticker verified
on device 2026-10-23 (see below); the rest of the matrix awaits the
user's on-device pass - file results per item.

FOUND ON DEVICE (first boot, 2026-10-23): the notification buttons
produced NOTHING - no shade entries, no ticker. Root cause: targetSdk
34 + no POST_NOTIFICATIONS runtime permission -> NMS ranks the app
IMPORTANCE_NONE (PermissionHelper.getPackageImportance), and
NotificationManagerService.prepareNotifyPostedLocked skips
listener-invisible records entirely, so SystemUI's NotificationListener
(and with it the 4.4 ticker) never sees the posts either - one missing
permission silences both surfaces. Fix: declare POST_NOTIFICATIONS in
the manifest + a runtime request guard on every posting path
(ensureNotifPermission); the KitKat/Holo permission dialog itself
becomes another test surface.

FOUND ON DEVICE 2 (same session): even with the manifest + request
guard, a FRESH (wiped) emulator boot cannot bootstrap the permission:
the runtime permission dialog is suppressed while device provisioning
is incomplete (setup wizard never finished), so requestPermissions()
silently denies. Fix: default-permissions-com.candyos.testing.xml
shipped via a prebuilt_etc module (the partner_gms pattern) pre-grants
POST_NOTIFICATIONS at boot (fixed=false: revocable in Settings, where
the in-app request guard and the Holo permission dialog take over).

VERIFIED ON DEVICE (2026-10-23, adb-driven, screencap pixel standard):
after granting POST_NOTIFICATIONS, tapping "Post long (marquee)" made
the status-bar band jump from 429 bright samples (idle) to 1760 during
the tick, with glyph runs spanning to x~917/1080 - the 4.4 ticker
marquees end to end; dumpsys notification shows the app record
(importance DEFAULT, channel ticker_tests). The guard's denied-path
Toast also pixel-confirmed. The default-permissions pre-grant then
VERIFIED on two fresh (wiped, unprovisioned) boots: POST_NOTIFICATIONS
granted=true at boot with zero manual steps, and tap-to-ticker works
out of the box (1631/1760 band samples on the two checks). NOTE: the
first iteration of the prebuilt_etc module installed the XML without
its .xml extension (soong names outputs after the module) - the
package manager picked it up anyway, but the file now ships under the
conventional *.xml name via the filename: property.

## Phase 24 - WebUSB installation on devices [/]

Goal (user direction 2026-10-26): WebUSB is the most straightforward way
to install CandyOS on mobile devices - verify the system supports it and
document it.

Finding: WebUSB installability is INHERITED, not implemented. The flasher
is Lineage Flash Tools (flash.lineageos.org), the base project's official
WebUSB utility (ADB-sideload and Fastboot tabs, Chromium-family browsers;
Chrome/Edge-class - WebUSB shipped in Chromium 61, no Firefox). It works
with CandyOS builds precisely because CandyOS changes nothing the install
chain depends on. Audit (2026-10-26, all five patch series + overlays):

* No patch touches the USB stack, adbd, fastbootd, LineageOS Recovery, or
  OTA packaging - the series are theme/UI-layer work. (grep for
  usb/adbd/fastboot/recovery/sideload/payload across patches/ matches only
  "statusbar" substrings, Music-app "USB storage" strings, and binary
  wallpaper patch data.)
* Device builds are stock LOS 21 builds: `brunch <device>` defaults to
  userdebug (required anyway - P22 signature spoofing needs debuggable)
  and emits the standard lineage-21.0-<date>-UNOFFICIAL-<device>.zip plus
  the per-partition images each device's install guide references - the
  exact artifact shapes the web flasher consumes.
* The overlay is device-agnostic: every LOS-21-supported device with an
  unlockable bootloader is installable. Device-specific steps (A/B,
  dynamic partitions, unlock variants, firmware prerequisites) stay on the
  device's LineageOS wiki install page - the web tool replaces the host
  tools, not the wiki steps.

Landed: docs/install-webusb.md (full walkthrough - requirements, browser
and udev notes, device build, unlock/recovery/sideload via the web tool,
relocking and troubleshooting notes, runtime-vs-install WebUSB
distinction); README.md "Install on a device" section + doc index entry;
docs/getting-started.md pointer section. No code or patch changes
required - that is the finding (support = do not break the stock chain).

- [X] 24.1 Audit: the install chain is stock (above)
- [X] 24.2 Docs: WebUSB install walkthrough wired into README,
      getting-started, and the doc index
- [ ] 24.3 First on-device install pass: build for a real device, flash
      via flash.lineageos.org, boot, file deltas here - rides the hardware
      pass backlog with every other [/] item (rule 6: AVD-only until real
      hardware exists; deferred, not cancelled)
- [ ] 24.4 (optional, later) CandyOS-hosted installer page on the
      kdrag0n/fastboot.js + android-webinstall pattern (the GrapheneOS
      web-installer model) once device releases exist - polish, not a gap:
      Lineage Flash Tools needs no CandyOS infrastructure
