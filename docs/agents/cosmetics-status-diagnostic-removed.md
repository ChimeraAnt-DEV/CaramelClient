## Cosmetics status diagnostic тАФ REMOVED
- `CosmeticsDiagnostics` + `CosmeticsStatusActivity` (and the Skins-screen "Cosmetics status"
  button) were deleted with the resource-pack route. The equivalent status is now the
  `NativeCosmeticsFeed` line in the Cosmetics panel. The rest of this section is kept as history.
- `run(gameDataDirs, stagingRoot, gameVersion)` is `File`-based and Android-light so it is
  unit-testable (`CosmeticsDiagnosticsTest`): it checks the pack is written under a candidate root,
  that its uuid is listed in `minecraftpe/global_resource_packs.json`, that the pack carries
  `entity/player.entity.json` + its cape render controller, and that the manifest's
  `min_engine_version` is <= the installed version. The one link only the game can answer тАФ whether
  RenderDragon draws the cape model тАФ is reported as `MANUAL`, never invented. A missing
  `min_engine_version` reads as MANUAL, not FAIL.
- The activity offers a **magenta test cape** so "did anything appear" is unambiguous, and a
  copy-report button. Do not promise cosmetics in release notes until a device check confirms
  which link fails.
- `parseVersion` compares component-wise with missing components as zero, so `1.20` == `1.20.0.0`
  and `26.51` > `1.20.0`.
