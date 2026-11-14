## One design system: colour, type, spacing, radius, motion (U7–U11)
- **REMOVED.** The Compose/View mod menu, the radial wheel and their layouts were deleted; the
  in-game menu is the native Dear ImGui menu now (`sweetloader/src/pl/menu/`, bridged by
  `PreloaderMenu` + `InGameMenuController`). The class names below no longer exist; this
  section is kept as history.
- **`UiTokens` is the single source of truth and every scale is closed.** `RADIUS_SMALL/MEDIUM/
  LARGE/XLARGE` = 8/12/16/28dp and the named surface radii (`RADIUS_WINDOW/CARD/CHIP/BUTTON`) are
  aliases of those four, never literals. `SPACE_1/2/3/4/6/8` = 4/8/12/16/24/32dp.
  `TEXT_DISPLAY/TITLE_ROLE/BODY/LABEL/CAPTION` are the five type roles. `UiTokensTest` pins all
  four scales, so adding a bespoke value fails the build rather than drifting.
  The XML side is the same scale: `dimens.xml` declares `radius_small/medium/large/xlarge` at those
  four values and the card tokens (`chimera_card_radius` 16dp, `chimera_card_radius_small` 12dp)
  **alias** them. They used to be a fifth and sixth radius (14dp/10dp) that every shared card
  drawable read, so an XML card and a code-drawn `UiCard` rounded different corners;
  `UiTokensTest.theXmlRadiusTokensAreTheSameScaleAsTheCode` reads the real resource file to keep
  them aliased.
- **One accent, resolved from the theme (U7).** `PersonalizationManager.getAccentColor()` now reads
  the theme's `colorPrimary` through `resolveThemeColor` — so Material You's wallpaper palette wins
  when dynamic colour is on and the brand colour is the fallback. The stored accent pref, the two
  preset swatch arrays (`PRESET_COLORS`/`MORE_COLORS`), `setAccentColor`/`clearAccentColor`/
  `hasCustomAccent` and the whole Settings colour-picker card were deleted. **Do not add a second
  accent source**: a stored swatch and a theme attribute fight over every tinted view, which is the
  bug this removed. `BRAND_ACCENT` is the no-theme fallback.
- **One accessibility switch (U11).** `PersonalizationManager.isReduceMotion()`/`setReduceMotion()`
  derive from and write both `show_animations` and `enable_glow_effects`, so the old three toggles
  (Show Animations / Enable Glow / Compact Mode) are one `switch_reduce_motion`. The OS "remove
  animations" setting still wins: `UiTokens.motionScheme(reduceMotion, platformAnimationsOff)`
  returns `Motion.REDUCED` (120ms linear, no overshoot) or `Motion.EXPRESSIVE` (spring), and every
  animated surface reads its duration/easing from that scheme instead of a hard-coded spring.
- **App-level compact mode is gone.** `PersonalizationManager.isCompactMode`/`setCompactMode`,
  `KEY_COMPACT_MODE`, `MainActivity.applyCompactMode` and the two fragment padding branches were
  deleted; those paddings are now `UiTokens.SPACE_4`. The in-game Mod Menu's own compact mode
  (`ModMenuOverlay.setCompactMode`, a width/span decision) is a different thing and stays.
