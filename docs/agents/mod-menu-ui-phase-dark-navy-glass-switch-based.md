## Mod Menu UI phase (M1-M10) — dark navy, glass, switch-based

The Mod Menu is the in-game surface, so it has its **own** palette rather than following the
launcher's. `ModMenuPalette` is that token set (`#070A0F` base, `#0B1118`/`#101823`/`#16202D`
surfaces, `#E8EEF6`/`#93A3B8`/`#5C6B7E` text, `#3E6FF0` accent) and it is deliberately
**Android-free** — the channel maths, the alpha mask and the WCAG contrast ratio are done by hand
rather than through `android.graphics.Color`, because that class is a stub off-device and every
rule here has to be unit-testable without one. `ModMenuPaletteTest` pins the contrast floors
(primary-on-surface2 15.3:1, accent-on-surface1 4.3:1, error/success/warn on surface2). No pure
black and no pure white: `#070A0F` + `#E8EEF6` is what reads as "smooth" rather than OLED-harsh.

- **The switch-vs-action split is a type, not a flag (M2, M3).** A module has an enable state and is
  drawn with a pill switch; an action fires once and is drawn with a chevron. `ModMenuRowKind.of(hasState)`
  is the rule (`ModMenuRowKindTest` pins that no row is ever both), `ModMenuAction` is the one-shot
  entry and `ModMenuActionCatalog` holds the two shipped actions (`reset_hud`, `reload_modules`).
  `ModMenuActionTest` asserts every action is an `ACTION` row and its id is unique. A switch that
  fires an action is the single most confusing control in a mod menu — never render one.
- **`ModMenuSwitch` is the pure geometry and `ModMenuSwitchView` only paints it.** Travel, the
  clamped overshoot (a spring must not push the thumb out of its track), the accent/muted track, the
  error track for a failed row, the on/off damping pair (turning on is springier than turning off)
  and the 48dp touch-target rule all live in the pure class; `ModMenuSwitchTest` pins them. The
  visual is 52x32dp and the view reserves a 48dp target around it.
- **`ModMenuSettings` (M6) is a value object with the system clamp inside it.** `effectiveGlass` and
  `effectiveMotion` take the platform's reduce-transparency / reduce-motion flags and let them win
  over the in-menu choice — a user-facing override that beats the OS accessibility setting is a bug,
  not a feature. `blurs()` also returns false at a 0fps blur budget. `ModMenuSettingsTest` pins all
  of it, plus the slider clamps and the six accent swatches (distinct, opaque).
- **`ModMenuMotion` (U11) has two schemes.** Full motion is the spring pair; reduced motion is still
  a *transition* (a short linear fade), never an instant pop. The reduced easing is a plain
  `reducedInterpolation(float)` rather than an `Interpolator`, for the same off-device reason as the
  palette; `ModMenuSwitchView` wraps it in a `LinearInterpolator` when it needs the real type.
- **`ModMenuRowState` (M10) is the failure surfacing, and its check order is load-bearing.**
  `of(registered, available, crashed, enabled)` returns FAILED when the module never registered,
  CRASHED before UNSUPPORTED, then LOADED/DISABLED. Only LOADED and DISABLED are interactive; every
  broken state is dimmed and carries a reason. `ModMenuAdapter.reasonFor` words an unsupported row
  from `ModAvailability` (a server-decided module is not "waiting for game data") and the overlay's
  `showRowReasonSheet` offers a re-enable only for CRASHED. The states are derived in
  `ModMenuOverlay.applyRowStates` from `ModLoadDiagnostics` and `ModCrashAttribution`, so the menu
  cannot call a module healthy while the crash guard has disabled it.
- **Drag-to-reorder is per section (M9), and the rule is pure.** `ModMenuOrdering.apply` reorders one
  section's keys; a key the saved order does not mention is appended rather than promoted to the
  front, and an unknown key is dropped, so a user's arrangement survives an update that adds or
  removes a module. `ModMenuOrderingTest` pins that, the duplicate-key case and the out-of-range move.
  `ModMenuAdapter.moveModule` refuses a cross-section move and `canReorderBetween` is the
  `ItemTouchHelper` guard, because the list renders one header per contiguous group run.
  `applySavedOrder` **rebuilds the group headers** — dropping them there silently flattens the whole
  non-compact layout.
- **`ModMenuTheme` now reads the navy palette and the menu's own settings.** `new ModMenuTheme(ctx,
  settings)` takes the accent from the swatch the player chose; the no-settings constructor still
  resolves the launcher accent. Card elevation is 0 by default (a lifted card over a live game reads
  as fog) and the glow toggle can still raise it.
- Layout: `item_mod_menu_card.xml` and `item_mod_menu_compact.xml` both carry the same
  `ModMenuSwitchView` (so the grid and the compact list cannot disagree about how a module is
  toggled), the status dot, the one-line reason and the action chevron. The Settings tab's appearance
  block (`group_mod_setting_*`, `seekbar_mod_setting_*`, `value_mod_setting_*`) is wired by
  `setupModMenuAppearanceSettings`; the segmented rows are plain TextViews, not Spinners, because a
  Spinner's popup is a second window that outlives the host view and this menu already had a
  stale-popup bug from one. Every control writes through `InbuiltModManager.setModMenuSettings`
  (one pref per field, so a new field is a new key rather than a blob needing a migration) and
  re-applies immediately.
- The Mod Menu's top nav is still the top bar over the `ModMenuPage` registry (M7) and per-module
  enable state is still persisted through `InbuiltModManager.resolveInbuiltModEnabled` (M8); this
  phase restyles and re-controls the surface, it does not move the navigation.
