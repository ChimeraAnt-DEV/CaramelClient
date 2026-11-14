## Blueberry UI (UiTokens + shared components)
- **REMOVED.** The Compose/View mod menu, the radial wheel and their layouts were deleted; the
  in-game menu is the native Dear ImGui menu now (`sweetloader/src/pl/menu/`, bridged by
  `PreloaderMenu` + `InGameMenuController`). The class names below no longer exist; this
  section is kept as history.

- **One token file: `ui/components/UiTokens`.** It replaces the retired violet/magenta palette and
  the two separate mod-menu themes; `ModMenuTheme` and `VipTheme` are now thin adapters over it. Blue
  is the default accent (`resolveAccent` falls back to `ACCENT_500`); `PersonalizationManager`'s
  first preset is blue. `colors.xml` mirrors the tokens as `ui_*` for the XML layouts; the mod-menu
  layouts must not carry inline hex.
- **Components are pure where it matters.** `UiToggle`, `UiRow`, `UiChip`, `UiRailButton`, `UiButton`,
  `UiCard`, `UiSegmented`, `UiSlider`, `UiSearchField`, `UiWindow` keep their geometry/state machine
  pure and are covered by `UiTokensTest` (palette, WCAG contrast, toggle state machine, segmented
  hit-testing, slider value→thumb, rail-button stacking, window bounds/title-bar/content tiling,
  search-field clear bounds). `UiIcons` is the outline icon set (24dp grid, 2dp stroke, round caps).
  **Geometry that a JVM test must pin is exposed in an array form** (`UiWindow.boundsArray`/
  `titleBarArray`/`contentArray`, `UiRailButton.boundsArray`, `UiSearchField.clearBoundsArray`),
  because `android.graphics.RectF` is not on the unit-test classpath; the `RectF` overloads delegate
  to the arrays so there is one definition. `UiCard`/`UiButton` take their radius as a parameter and
  are exercised by the gallery, not by a geometry assertion.
- **The hidden UI gallery renders every component, not a sample.** `UiGalleryActivity` shows the
  palette, the type roles, the shape scale and one live view per component (toggle, row, window
  chrome, segmented, search field, slider, button, card, rail button). It is the device pass for the
  shared library — a component added without a gallery entry has no way to be eyeballed on hardware,
  and the gallery's own doc-comment claims "every shared component in every state".
- **The radial wheel is rounded annular sectors, not stroked arcs.** `RadialMenuView` builds each
  sector as one `Path` (outer arc, quadratic corner fillet, inner arc, fillet) with a **parallel-sided
  linear gap** (`RadialWheelLayout.angularHalfGapDegrees`, constant 6dp) rather than a fixed angular
  gap that widens with radius. Geometry is pure: `RadialWheelLayout` gains the fractions,
  `sectorAtLinear`, `chipCenters`; `RadialWheelLayoutTest` pins them (5-degree sweep, dead zone, chip
  clusters never overlapping the ring or each other at three screen sizes).
- **The Mod Menu keeps its top navigation bar, not a left rail.** This is the same constraint the
  earlier "side rail was reverted" note records: the app is landscape-only, so a rail only gets the
  short edge. Restyling must not convert it. The module grid is two columns wide / one compact.

### The four design scales are enforced, not merely declared (U6/U8/U9/U10)

The scales existed as tokens and were then bypassed by the layouts, so "one type scale", "one
spacing scale" and "one radius scale" described an intention rather than the app. Each now has a
token set, a migration, and a test that reads the real resource files. **Adding a numeric
`textSize`, `margin`, `padding` or `radius` to a layout fails the build.**

- **U8 type roles.** `TextAppearance.Caramel.{Display,Title,Body,Label,Caption}` in `styles.xml`,
  plus two named exceptions: `Wordmark` (the 34sp brand title) and `Micro` (the 9sp sub-caption).
  Every layout's `textSize` was a literal across 17 distinct sizes; all 759 now reference a role.
  `UiType` is the code-side equivalent. `TypeRoleTest` pins the role sizes to the `UiTokens.TEXT_*`
  constants and fails on a raw `textSize`.
  - **`UiType.FAMILY` is the CJK-capable family, and emphasis is the bold flag.** It previously named
    a variable Latin-only font, so a code-built view using a role would have rendered zh/ja/hi as
    tofu — the app ships those locales. No shipped font is both variable *and* CJK-capable, so
    `applyWeight`/`weightAxis` exist for a deliberately chosen variable family but `apply` uses bold.
    Do not "restore" the variable font without checking its glyph coverage.
- **U9 spacing.** Six steps (`space_1/2/3/4/6/8` = 4/8/12/16/24/32) on a 4dp rhythm, mirroring
  `UiTokens.SPACE_*`. 1529 margins/paddings/drawablePaddings now reference a step.
  - **Four values are deliberately off the rhythm and are named so they stay countable:**
    `space_divider` (1dp — a line, not a step), `space_hairline` (2dp — the label/value coupling),
    `space_align_inset` (52dp — the version row's icon-column alignment) and `space_dock_clearance`
    (168dp — the docked Play control's measured clearance). Snapping any of them breaks what it is
    for; `SpacingScaleTest` asserts they stay off the scale so an exception cannot quietly become a
    second scale.
  - **A layout dimension is never `sp`.** `sp` scales with the font-size setting, which is right for
    text and wrong for a gap: the one `12sp` margin in `dialog_apk_version_confirm` grew on a
    low-vision device and pushed the dialog apart. `SpacingScaleTest.noLayoutDimensionIsInSp` guards
    the whole class of bug.
- **U10 radius.** The scale is 8/12/16/28 (`radius_small/medium/large/xlarge`) with semantic aliases
  (`radius_card/window/chip/button/field/toggle`) mirroring `UiTokens.RADIUS_*`. All 19 layout
  `cardCornerRadius` values and all 95 drawable `android:radius` values now reference a token.
  - **The shapes the surface scale does not describe have their own named family**, because a 20dp
    chip radius *is its own height* and a 4dp tag is not a card: `radius_tag` (4dp), `radius_badge`
    (6dp), `radius_chip_small` (10dp), `radius_pill` (= `radius_toggle`, the stadium),
    `radius_circle` (24dp), `radius_accent_bar` (2dp), `radius_square` (0dp). `RadiusScaleTest` fails
    on any numeric radius in a layout or drawable, and asserts the small-shape tokens stay named.
  - This is still the rule AGENTS.md already records: **do not add a recursive "apply card rounding
    to every GradientDrawable" walk** — pills, tags and accent bars are meant to keep their own
    radii, which is exactly why they are tokens now rather than literals.
- **U6 motion scheme.** `UiTokens.Motion.{EXPRESSIVE,REDUCED}` and `UiTokens.motionScheme` had **zero
  consumers**: `DynamicAnim` hard-coded its springs, so "Material 3 Expressive" was a description.
  `DynamicAnim` now holds the scheme in force and derives every spring's damping and stiffness from
  it, so the two schemes are two different feels rather than one spring at two speeds.
  `DynamicAnim.applyMotionScheme(reduceMotion, platformAnimationsOff)` is the single entry point;
  `MainActivity` passes the real reduce-motion switch and the platform `ANIMATOR_DURATION_SCALE`.
  - **The platform setting wins over the in-app switch.** A user who told Android to remove
    animations must not get springs back because a stale app preference says motion is on.
    `MotionSchemeTest` pins that precedence, and pins the wiring rather than the token — a dead token
    cannot fail a test that only reads the token, which is how this went unnoticed.

- **The Closet's Hats/Back split is `ClosetCategory` (pure).** An accessory is exactly one of the two
  (`followsHead()` decides), pinned by `ClosetCategoryTest` so the screen cannot silently drop one.
- **Hidden UI gallery:** Settings > Updates, tap the version text 7 times
  (`UiGalleryActivity`, unexported). `docs/UI_CHECKLIST.md` is the device pass.
