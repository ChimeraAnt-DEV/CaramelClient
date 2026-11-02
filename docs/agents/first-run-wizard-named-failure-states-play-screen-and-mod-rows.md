## First-run wizard, named failure states, Play screen and mod rows (U13-U17)

- **The wizard's ordering is a pure state machine.** `launcher/core/setup/SetupWizard` maps four
  facts (game imported, version chosen, sign-in required, signed in) to the first incomplete step,
  so "resume where the user left off" falls out of the design instead of its own bookkeeping.
  `SetupWizardActivity` only renders the step and performs its action. The **progress rail is built
  from `activeSteps`**, not a fixed four: with the launcher-managed login off the rail has three
  stops and `currentIndex` must return 2 for READY, not 3 — a fixed rail that hides a step makes the
  progress look stuck. `SetupWizardTest` pins the order, the skip, and both rail shapes.
- **The wizard is shown by `SetupWizardActivity.shouldShow(activity)`**, from the splash: it requires
  *both* "not finished before" (`glowberry_setup` / `KEY_WIZARD_DONE`) and "no installed instance",
  so an existing user upgrading into this build never sees it. The wizard is skippable, and Skip
  marks it done — a user with a working install must never be trapped in it.
- **Launch refusal is a named state, not a generic error.** `core/minecraft/LaunchReadiness` (in
  `launcher.core.minecraft`) enumerates the causes in precedence order — no version, missing game
  files, 32-bit APK, stale extraction, missing signature rules, game not installed, sign-in required
  — and `blocksLaunch` is a property of the state, not a second list at the call site.
  `LaunchFailurePresenter` maps a state to its copy and its `isWarning` flag is asserted to agree
  with `blocksLaunch` for every state, so a repairable problem can never become a dead end.
- **`STALE_NATIVE_LIBS` is a warning the launcher repairs itself**, not a dialog: `MainActivity`'s
  preflight invalidates the extraction cache and launches. `SIGNATURE_RULES_MISSING` likewise warns
  without blocking, because it disables optional hooks only. Everything else blocks.
- **The Play screen's dock is one row plus the action.** `include_launch_dock.xml` puts the profile
  on the left and the **version as a chip** (`bg_dock_version_chip`, 20dp pill) on the right, above
  the full-width Play button. The chip is the version *selector*; the ids
  `select_version_button`/`text_minecraft_version` are unchanged because the popup anchor and the
  version text bind to them. Do not turn the chip back into a stacked two-line selector — it pushed
  the action down and read as a second button.
- **The diagnostics screen shows the same launch state the Play button evaluates** (U14/U16), via
  `renderReadinessRows` in `DiagnosticsActivity`: `LaunchReadiness` is evaluated once and the state
  key is printed verbatim, because that string is what a bug report should quote.
- **A mod row's state, kind and failure line come from `ModRowPresentation`** (U17), not from the
  adapter: `kindOf` reads the **entry point** (`.so` → native, `.lua` → script), because an `.AntEgg`
  can hold either runtime and the archive name does not say which. A recorded failure outranks the
  enabled flag, and a failed mod stays toggleable so the user is not trapped with a broken enabled
  mod. `ModRowPresentationTest` pins all of it.
- **The spacing scale is `dimens.xml`'s `space_1/2/3/4/6/8` (4/8/12/16/24/32dp) plus
  `min_touch_target` (48dp)**, mirroring `UiTokens`. Layouts use those rather than raw dp so the
  vertical rhythm matches the code-drawn surfaces.
