## Per-button CPS limit and hold-to-repeat (`CpsLimiter`, `ControllerProfile`)
- Two separate behaviours, deliberately not merged: **limit** drops presses above a per-button CPS
  cap (safe; stops worn-button double-fire), **repeat** injects down/up at a fixed rate while held
  (opt-in; on the attack button it is an autoclicker, so it is off by default, warns once, and is
  labelled "may be against server rules"). `CpsLimiter.isAutoclickerLike(keyCode)` decides the
  warning only -- the launcher cannot know a server's policy.
- `CpsLimiter` is **pure**: it takes `nowMs` as a parameter, never reads a clock, so the rate rules
  are unit-testable (`CpsLimiterTest`). The rolling one-second window counts from the **window
  start** (not the last press) so a burst cannot exceed the cap; the window sentinel is **-1**, not
  0, because event times legitimately start near zero at boot.
- Limits/rates live in `ControllerProfile` (sparse maps, absent = off) so they travel with the
  profile; `copy()` carries them. The **run state** (window, repeat schedule) lives in
  `ControllerInputProcessor.cpsLimiter`, never persisted.
- Wiring: `ControllerInputProcessor.onKeyDown` (called from `MinecraftActivity.dispatchKeyEvent`,
  `repeatCount == 0` only so a held hardware key does not re-arm) applies the cap and arms repeat;
  `onKeyUp` ends it. `tickCpsRepeats(SystemClock.uptimeMillis())` runs from
  `InbuiltOverlayManager.tick()` (the game's own frame tick), so repeats inject at a steady rate.
  A late tick fires once and re-aligns rather than bursting the backlog. Injected events go through
  `PreloaderInput.onKeyEvent` -- the same path a real press uses.
- `setActiveProfile` calls `resetCpsState()` so a profile change cannot carry a partial window over.
