## Material 3 Expressive primitives (U6): variable-font roles and shape morphing
- **`UiType` is the one type system (U8 enforced).** The five `Role`s map to the five
  `UiTokens.TEXT_*` sizes and weights, and `apply(view, role)` sets family + size + weight together —
  a screen asks for a role, never a raw sp. The family is the **variable** `dmsans`
  (`UiType.FAMILY`), so a weight is a real axis value: `UiType.weightAxis(w)` builds the platform's
  `"'wght' 600"` string (pure and pinned by `UiTypeTest`), `applyWeight` sets it, and the coarse
  `BOLD`/`NORMAL` flag stays only as a fallback for a device that ignores the axis. `dmsans` **is**
  variable (`wght` 100–1000, plus `opsz`), which is why it is the family; check a font's `fvar` table
  for a `wght` axis before using it as a role family — `misans`/`poppins`/`lato` are static and a
  weight axis on one of those does nothing.
- **`UiShape` is the Expressive shape scale plus its morph.** The stops are a closed set
  (`EXTRA_SMALL 4` … `FULL`), a superset of the token radii, exposed as `Role`s (INDICATOR/CHIP/
  FIELD/CARD/WINDOW/PILL). `morph(from, to, t)` is a pure interpolation with one rule worth knowing:
  a `FULL` end is a sentinel, not a length, so a morph to or from a pill keeps the *other* end's
  radius (the corner is already a stadium; size is what reads as "became a pill"). `clampToBox`
  stops a large radius eating a small box. All pure, pinned by `UiShapeTest`.
- **The rail uses both.** The selected rail row is an accent-wash surface whose corners animate
  between CHIP and CARD (`BaseActivity.applyRailTabShape`), and the active destination's label is
  heavier through the weight axis, not just a different colour. The fill is
  `UiTokens.withAlpha(accent, …)` — the accent still comes from the theme (U7); do not add a second
  accent source for the morph.


- **The nav bar is the original horizontal top bar.** `BaseActivity.wrapWithNavBar` inflates `layout/nav_bar.xml` into a **vertical** wrapper (bar above content, content weighted 1). `nav_bar.xml` is a `LinearLayout` root holding a single 48dp `FrameLayout` row тАФ brand/back on the start, six tabs in a `HorizontalScrollView` in the centre, news/account controls on the end тАФ above a 1dp `nav_divider`. Do NOT add a second bar or a fragment-based tab host.
- **A vertical side rail was tried and reverted. Do not reintroduce it.** The rail put the tabs in a column beside the content; because the app is landscape-only (`sensorLandscape` on ~29 activities) the column gets only ~360dp of height, and the fixed chrome (brand header, dividers, stacked footer) left room for barely two of six rows. Since `nav_item_launch` was first, Launch was the only reachable tab, which reads as "the other tabs are gone". A top bar has the whole screen *width* for its tabs, and `HorizontalScrollView` already handles six labelled entries, so the height pressure never arises. Restoring the bar also means restoring the wrapper orientation (`VERTICAL`), the content params (`MATCH_PARENT, 0, 1f`) and the enter animation axis (`springTranslationYTo`) тАФ flipping only some of these leaves the content mis-measured.
- **Each tab is one `TextView` with an icon, not icon+label pair.** The original bar puts the icon on the `TextView` itself via `android:drawableStart` and tints it with `TextViewCompat.setCompoundDrawableTintList`. Only `nav_tab_*` exists тАФ there are no `nav_item_*`, `nav_label_*` or `nav_indicator_*` ids; an earlier rail had four parallel arrays and removing the rail means those ids are gone from the layout, so any leftover `findViewById(R.id.nav_item_*)` is a compile error. `R.id.nav_tab_*` is the click target.
- Navigation is **activity-based**: each tab starts an Activity (`switchNavTab` тЖТ `startActivity` + `nav_tab_in/out` fade-through), and `setActiveNavTab(R.id.nav_tab_*)` re-tints the tab text and its compound icon. `LauncherTab` is the single source of truth mapping tab тЖТ activity, and its declaration order *is* the rail order and the bumper cycle order.
- `BaseActivity.dispatchKeyEvent` cycles tabs on L1/R1, L2/R2 and D-pad left/right (wrapping). Two guards matter:
  - **`shouldHandleNavKeys()`** (default true) тАФ override to `false` on any screen that needs raw button presses. `CustomizeActivity` does, so `ControllerSettingsFragment`'s illustration can highlight the pressed button.
  - **`LauncherTab.shouldHandleKey(code, navBarPresent, gameSessionActive)`** тАФ tab switching is suppressed while `LowLatencyNetworkManager.isGameSessionActive()`. Note `MinecraftActivity` extends the *game's* `com.mojang.minecraftpe.MainActivity`, NOT the launcher's, so gameplay never had the nav bar or this handler тАФ but the session guard is kept as defence in depth.
- `BaseActivity.NAV_TAB_IDS` is the single id array shared by `setupBaseNavBar()` and `setActiveNavTab()`; adding a tab means updating the layout, that array, the click handler, and `LauncherTab`. `LauncherTabTest` covers ordering, activity mapping, wraparound, the key map and both guards.
- **There are six tabs.** `LAUNCH, VERSIONS, INSTALLATIONS, MODS, CUSTOMIZE, SETTINGS`. About is *not* a tab тАФ it is reached from Settings (`SettingsActivity` `openAbout`). Controller and Skins are *not* tabs either; both live inside `CustomizeActivity` as `ControllerSettingsFragment` / `SkinsSettingsFragment` swapped behind `customize_tab_controller` / `customize_tab_skins`. Do not re-promote About/Controller/Skins to top-level tabs; add a new destination inside an existing tab instead.
- **VOICE is not a launcher nav tab; it is a Mod Menu section.** `VoiceChatActivity` exists and is reachable (the Mod Menu's Voice entry and the mod's own screen), but the launcher's top bar is navigation, and a communication feature does not belong in that row тАФ `LauncherTab` documents this and `nav_bar.xml` has six tabs. The in-game home is the Mod Menu's Voice section (`nav_voice`), which is the first entry ahead of General
- **Screens that tint the rail themselves must touch all three pieces.** `SettingsActivity` re-applies a freshly chosen accent to the Settings entry directly (indicator + icon + label); the old text-view+compound-drawable path no longer applies. `MainActivity`/`InstancesActivity` still swallow a click on their own row via `nav_item_*`.
- `ControllerSettingsFragment` is the live home of the controller illustration and profile editor. It exposes `wantsRawKeyEvents()`, `handleHardwareKey(keyCode, down)` and `handleHardwareMotion(event)`, which `CustomizeActivity.dispatchKeyEvent` / `dispatchGenericMotionEvent` forward. The old standalone `ControllerActivity` was deleted тАФ do not reintroduce it.
- `focus_ring` was prototyped and removed тАФ no focus system consumes it; do not re-add a token without a consumer. Nav rows get touch feedback from `DynamicAnim.applyPressScale(row)`.
- **Dead code to ignore:** `VersionManager.importGameFile` only accepts `.apk`/`.xapk` and is unused тАФ `ApkImportManager` is the live import gate. `unsupported_version_msg` ("older than 1.21.80") has no consumer in code or layout; the real preflight is the ABI one.
