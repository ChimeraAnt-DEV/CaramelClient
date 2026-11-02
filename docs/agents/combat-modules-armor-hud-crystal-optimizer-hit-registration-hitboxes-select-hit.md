## Combat modules: Armor HUD, Crystal Optimizer, Hit Registration, Hitboxes, Select Hit
Five modules in the Mod Menu's PvP tab, all under `core.mods.inbuilt.overlay`. Each is built as
**decision/logic + a game-data seam**, because this repo cannot read live game state (no
`libminecraftpe.so`, no entity structures тАФ the shipped signature rules cover only five
menu/HUD addresses). The seam is the honest boundary; nothing fabricates a reading across it.

- **`ArmorHudMod` + `ArmorHudOverlay`.** Durability/enchant readout for self and, optionally,
  the current target. `ArmorHudMod.DataSource` is the seam: `read()` returns a `Snapshot`, and
  `Snapshot.absent()` means *no data*, which is deliberately distinct from *wearing nothing*.
  The overlay draws a dimmed `-` per cell when absent. Never render absent as full durability -
  that reads as a healthy armor set when nothing is known. A throwing or null-returning
  `DataSource` degrades to absent rather than crashing the overlay. `Piece.fraction()` returns 1
  for a zero max so an unknown piece never renders as empty.
- **`CrystalPlacementSolver` + `CrystalOptimizerMod` + `CrystalOptimizerOverlay`.** The solver is
  pure geometry and fully unit-tested: it ranks obsidian/bedrock candidates by `blastDamage`, a
  quadratic falloff to zero at `MAX_EFFECTIVE_DISTANCE`, expressed in **half-hearts**
  (`MAX_DAMAGE_HALF_HEARTS = 24`, above a full 20 health bar so a point-blank placement is
  correctly rejected as lethal to self). Candidates are rejected when out of `maxRange`, when
  self-damage exceeds target-damage, or when the blast would drop the player below `minSelfHp`.
  `CrystalOptimizerMod.WorldSource` is the seam; `evaluate()` returns null without one.
  - **Manual assist is the default and is listed first in the settings dialog.** It highlights
    the spot and never acts. Automatic placement is the explicit opt-in, because a client that
    places and breaks blocks for the player is what servers restrict or ban тАФ
    `crystal_optimizer_fairness_note` says so in-app.
- **`HitRegistrationMod`.** Shapes the look input the device sends; it cannot make a miss hit,
  because Bedrock resolves hits server-side. `hitreg_scope_note` states that in-app. Shaping is
  sensitivity, EMA smoothing, and sub-frame prediction. **Micro deltas (below `MICRO_DELTA`)
  bypass smoothing and prediction entirely** тАФ predicting a slow track would overshoot the target
  the player is settling onto, which makes aiming worse. Prediction only applies to a continuing
  burst (`hasLast` and within `BURST_GAP_MS`), so the first frame of a flick is never
  extrapolated from a stale one. Sensitivity is applied *before* the micro-delta test, so a micro
  delta is still scaled; the tests pin that ordering.
  - It is **mutually exclusive with `AimSettingsMod`'s shaping** at the send step: each carries
    its own smoothing, so running both compounds the damping.
    `MinecraftActivity.pojavSendLookDelta` picks Hit Registration when active, else Aim Settings.
    Aim Settings still draws its crosshair.
  - Wired into the controller hot path at `ControllerInputProcessor.shapePair` for the **right
    stick only** (the look stick), reusing the caller's buffer so the path still allocates
    nothing. `SystemClock` is not mocked in unit tests, so `TimeSource` is injectable
    (`setTimeSource`); `HitRegistrationModTest` installs a `FakeClock`.
- The `armor_hud` / `crystal_optimizer` / `hit_registration` / `hit_timing` / `hitbox` ids live
  in `ModIds`, with all five in `PVP_MODULES` so they land in the PvP tab.
  `ModIds.requiresGameData(id)` marks the three that need a native feed (armor, crystal, hitbox).
  All are registered in `InbuiltModuleProvider` (entries, configs, setters, and an explicit
  `createCombatConfigSchema` category layout) and in `InbuiltOverlayManager`
  (show/hide/tick/visibility/reset-position), and they set `customConfig = true`.
- Tests: `CrystalPlacementSolverTest`, `ArmorHudModTest`, `HitRegistrationModTest`,
  `HitTimingSolverTest`, `HitboxProjectorTest` (no mocks).
- **`createCombatConfigSchema` descriptions and scope notes.** `configNode` attaches the
  `_desc` string for a config key via `configDescriptionRes(key)`, and each combat module appends
  an `info` node carrying its scope note (`hitreg_scope_note`, `hit_timing_scope_note`,
  `hitbox_scope_note`, `crystal_optimizer_fairness_note`, `armor_hud_no_data`). Those notes
  already existed but had **no consumer in code or layout** тАФ a module named after an outcome
  ("Hit Registration") that stops short of it reads as broken unless the dialog says why.
  `configDescriptionRes` is an explicit switch, not a name lookup, so a renamed key cannot
  silently bind to an unrelated string. The note node goes in the default category, so it is the
  first thing read.
