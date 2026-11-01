# Accessibility

What is supported today, and the known gaps. This is a statement, not a promise of a score — the
parts that are not done are listed as not done.

## Supported

- **Reduce motion.** One switch (Settings → Personalize → Reduce motion) turns off stagger, the
  hero-card pulse, press springs and the splash motion. The Android system setting
  (`ANIMATOR_DURATION_SCALE == 0`) wins over the in-app switch, so a user who told Android to
  remove animations does not get them back. `UiTokens.motionScheme` is the single rule.
- **Large font scale.** Text-bearing buttons and labels use `wrap_content` plus `android:minHeight`
  rather than a fixed height, so a label grows with the font size instead of clipping.
- **TalkBack labels on the shell.** The nav tabs, the back button, the logo and the shared
  components carry a `contentDescription`; the active tab is marked `setSelected(true)` so a screen
  reader announces it. Decorative images are marked `importantForAccessibility="no"` rather than
  left unlabelled.
- **Focus order.** The nav order is back button, tabs, then content. The in-game Mod Menu computes
  its grid focus in Java (`ModMenuNavigation`) so a controller or a keyboard can move through it.
- **Touch targets.** Interactive controls meet the 48dp minimum; the rail tab and the back button
  are widened with a `TouchDelegate` where the visual is smaller than 48dp.
- **No colour-only meaning.** A HUD threshold carries a text or shape cue as well as a colour, so a
  colour-blind user is not relying on hue alone.

## Known gaps

- **Translation coverage.** Ten locales translate a portion of the default strings; the rest fall
  back to English. Settings, the launch flow and error dialogs are prioritised.
- **Per-screen audit.** The 48dp and font-scale rules are enforced by shared components and a few
  tests, but not every screen has been walked at 200% font scale on a device.
- **In-game overlays.** The overlays are drawn on a `Canvas` over the game and are not exposed to a
  screen reader; they are a visual aid, not a navigable UI. The launcher screens are the accessible
  surface.
- **RTL.** The nav uses `paddingStart`/`paddingEnd`, but a full right-to-left pass over every layout
  has not been run.

## How to check

- Font scale: set the display font size to its largest value and walk the launcher; no button label
  may be clipped.
- Reduce motion: turn the switch on and confirm the hero-card pulse stops and press feedback is
  instant.
- TalkBack: enable it and confirm the nav announces each tab and its selected state.

Report an accessibility problem with the bug template, tagged `accessibility`.
