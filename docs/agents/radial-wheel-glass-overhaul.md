## Radial wheel glass overhaul (R1-R13)
- **REMOVED.** The Compose/View mod menu, the radial wheel and their layouts were deleted; the
  in-game menu is the native Dear ImGui menu now (`sweetloader/src/pl/menu/`, bridged by
  `PreloaderMenu` + `InGameMenuController`). The class names below no longer exist; this
  section is kept as history.

The wheel (`RadialMenuView` + `RadialMenuOverlay`) is now a glassmorphic, iOS-style radial menu.
The rules live in three pure classes and the view only applies them.

- **`RadialWheelMaterial` is the whole material decision (R2, R8, R9, R10, R12).** Three glass
  thicknesses — ULTRA_THIN 24dp/8% white, REGULAR 32dp/14%, THICK 40dp/26% — plus a SOLID fallback,
  all sitting on a 12% black scrim floor (`SCRIM_FLOOR_ALPHA`). `Env` carries every input that can
  change what is drawable (API level, whether a `PixelCopy` succeeded, reduce-transparency,
  high-contrast, thermal severity, battery saver, low memory, session active, reduce motion, blur
  fps) and `resolve`/`resolveForBackdrop` produce one `Resolved`. **Order is load-bearing**: the
  API/capability checks run first (no `RenderEffect` below API 31 → SOLID), then the accessibility
  settings (the OS always beats the in-app choice), then the thermal ladder. Do not reorder them.
- **The contrast escalation is BLACK, not white (R9).** `escalatedScrim` composites `0xFF000000`
  over the backdrop. The first cut raised the *white tint*, which over a bright frame is a no-op —
  white over white is white — and `escalatingTheWhiteTintOverABrightFrameWouldBeNoOp` pins exactly
  that. The wheel's labels are light; only darkening can help them.
- **The blur budget is the coordinator's, not the wheel's (R8).** `FpsOptimizationService
  .wheelBlurBudget()` reuses `FpsOptimizer.currentAction()` so the wheel cannot re-derive a stricter
  answer from the same sensors; `Env.budgetOverride` wins over the local reading when set.
- **`WheelGlassRenderer` is the only place the blur API lives.** On API 31+ it uses
  `Window#setBackgroundBlurRadius` + `FLAG_BLUR_BEHIND` (which blurs what is genuinely *behind* the
  window — a `RenderEffect` on the wheel's own view would blur the wheel's own content); on 28-30 it
  draws a downscale/stretch stack blur of the capture. `clearWindowBlur` runs on detach so nothing is
  left on the game's window.
- **`WheelBackdropCapture` owns the capture policy (R3, R12).** A 1/4-resolution `PixelCopy`
  (`DOWNSCALE = 4`, `MAX_DIMENSION = 512`), driven by `BackdropRefreshSchedule` at ≤10fps
  (`BLUR_FPS_MAX`, default 5). `captureDimension` is pure and pinned by `WheelBackdropCaptureTest`.
  The `Handler` is **lazily created** — an eager field would touch `Looper.getMainLooper()` and make
  the class un-constructible in a JVM test.
- **`BackdropRefreshSchedule` is the throttle (R3).** A clock-jump backwards reads as "due" (one
  wasted capture beats a frozen backdrop), the interval is capped at `1000/10`, and a rate of 0 means
  never refresh. `RadialMenuOverlay` runs it on a `Handler` loop that stops on `hide()`.
- **`RadialWheelMotion` is the feel (R1, R4-R7, R11).** Spring targets (open 0.86→1.0 @ d0.78/s380,
  select 1.06 @ d0.7/s500, settle 1.14 for 90ms), the haptic vocabulary (`hapticFor` — commit
  outranks crossing, **nothing on open**), label metrics (24dp icon, 11sp, ellipsis at 9 chars),
  the hub (40dp head, 10sp/60% white label), the cancel gesture (24dp past the outer radius → 0.4
  opacity / 0.94 scale) and the segment-target rule (`hasTouchTargets`/`maxSegmentsForRadius` enforce
  R1's 48dp). `MAX_SEGMENTS` is `WheelLayout.MAX_SECTORS` — two independent caps is how the wheels
  drift. Every haptic in the view goes through `applyHaptic(RadialWheelMotion.Haptic)`; a raw
  `performHapticFeedback` call in the wheel is the regression to watch for.
- **The annulus is 30%→46% (R1), defined once.** `RadialWheelLayout.ANNULUS_INNER/OUTER_FRACTION`
  are the source; `RING_INNER_FRACTION` is *derived* (`inner/outer`) rather than a second literal, and
  `OUTER_FRACTION` aliases the outer. `RadialWheelMotion` re-exports them. The old 0.40/0.52/0.46
  triple is gone; `RadialWheelLayoutTest.theWheelFractionsMatchTheSpec` pins the new values and that
  the hub fits inside the ring's hole.
- **Sector caps are squircles (R1/R13).** `squircleCap` uses a cubic whose control points are pulled
  toward the corner by `RadialWheelMaterial.squircleControl(squareness)` (0.5523 circular → 1.0
  square); the old `quadTo` fillet read as a rounded rectangle.
- **`R13`: the four resting sector paths are cached.** `cachedSectorPaths` is rebuilt only when the
  geometry changes (`sectorPathsValid` cleared on open/close/cancel-armed), so a steady open wheel
  re-tessellates nothing; only the hovered sector (scaled by the press animation) is rebuilt per
  frame.
- **R10 fallbacks.** Reduce motion runs a 120ms linear transition instead of the spring
  (`playOpen`/`playClose`); the SOLID tier draws a 1.5x border rather than the faint glass hairline
  (there is no blur to define the edge); high contrast outlines every label via
  `WheelGlassRenderer.labelOutlinePaint`.
- **R2 colour.** The wheel tokens are now the blue palette (`WHEEL_SCRIM` `0x73070A0F`,
  `WHEEL_SEGMENT_BASE` `0xCC0B1118`, `WHEEL_SEGMENT_HOVER` `0xE616202D`, `WHEEL_HUB_FILL`
  `0xE60B1118`); the old violet wash was the last place the retired brand survived.
- Tests: `RadialWheelMaterialTest`, `RadialWheelMotionTest`, `BackdropRefreshScheduleTest`,
  `WheelBackdropCaptureTest` (all pure, no mocks) plus the existing `RadialWheelLayoutTest`.
