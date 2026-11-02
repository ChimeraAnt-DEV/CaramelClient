## PvP Suite (V1.1) тАФ four overlay-only modules in the General tab
Four client-side visual modules, all under `core.mods.inbuilt.overlay`, all filed in the Mod Menu's
**General** tab (the tab formerly called "Modules"). They are **not** PvP-classified and no new
section or tab exists for them тАФ `ModIds.isPvpModule` deliberately excludes them, so the PvP run
stays a single contiguous section. `PvpSuiteModulesTest` pins the placement, the usability and the
single-section invariant.

- **The target seam is the proximity-voice peer feed.** `PeerPositions.read()` is the one place the
  suite asks for other players' positions (`VoiceChatModule.audiblePeers()`); a missing module, a
  throwing feed or a non-finite coordinate all degrade to an empty list. Vanilla players, mobs and
  items are invisible to it тАФ the same limit the Hitboxes peer boxes document тАФ and every module
  fails closed (draws nothing) rather than guessing.
- **Reach Indicator** (`ReachIndicator` + `ReachIndicatorMod` + `ReachIndicatorOverlay`). Distance
  to the peer under the crosshair, from the same crosshair test the Hitboxes module uses so the
  number and the box agree. A reading is `null` for every no-data case, so the overlay cannot render
  a stale or zero distance. Placement is a below-crosshair/above-hotbar choice.
- **Trajectory Prediction** (`TrajectorySolver` + `TrajectoryPredictionMod` + overlay). The arc is
  pure arithmetic тАФ per-projectile speed, gravity and drag, with an arrow's draw charge adding
  range. `TrajectoryPredictionMod.HeldItemSource` is the seam; with no provider the arc is empty and
  the overlay says "waiting for game data". It predicts from your aim, never a projectile in flight
  (that needs the native entity feed).
- **Hit Prediction** (`VelocitySmoother` + `HitPredictor` + `HitPredictionMod` + overlay). A ghost
  box at a peer's predicted next position. Velocity is the mean over the last five samples, not the
  last frame, so one jittery sample cannot fling the marker; a peer above
  `HitPredictor.UNRELIABLE_SPEED` gets no marker rather than a misleading one.
- **Custom Kill Effects** (`KillCreditRegistry` + `KillEffectsMod` + overlay). A client-side particle
  burst on a credited kill. Credit is inferred: a hit you landed within `CREDIT_WINDOW_MS` (2s)
  followed by that player's death. Three styles (burst/column/ring), computed from elapsed time so a
  dropped frame cannot make the effect jump. `KillEffectsMod.KillSource` is the death seam; with no
  provider the overlay shows "waiting for game data".
- **Overlays read the camera directly, not through the voice seam.** They use
  `LocalPlayerFeed.localCamera(...)`, because they work without the voice module and so cannot depend
  on a seam the voice feature installs. All four are full-screen, `FLAG_NOT_TOUCHABLE` and
  world-positioned (nothing to drag), like the Hitboxes overlay.
- Run state advances on the game's own frame tick: `InbuiltOverlayManager.tick()` calls
  `HitPredictionMod.tick(now)` and `KillEffectsMod.tick(now)` alongside the CPS repeats; both are
  no-ops when their module is off.
- Config: each module gets a short `createPvpSuiteConfigSchema` dialog (its own display options plus
  "show everywhere") and an `info` node carrying its scope note (`reach_indicator_scope_note`,
  `trajectory_prediction_scope_note`, `hit_prediction_scope_note`, `kill_effects_scope_note`). The
  RADIO choice nodes get their `options` generated from the config's `min_value` label list, or the
  dialog would render an empty selector.
- Tests (no mocks): `TrajectorySolverTest`, `ReachIndicatorTest`, `VelocitySmootherTest`,
  `HitPredictorTest`, `KillCreditRegistryTest`, `PvpSuiteModulesTest`.
