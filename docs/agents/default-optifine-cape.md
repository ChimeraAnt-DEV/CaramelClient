## Default Optifine cape (`CosmeticCatalog` + `CosmeticStore`)
- Optifine Mode + no equipped cape implies the classic all-black Optifine cape with an "OF" monogram.
  `CosmeticCatalog.resolveEquippedCape(id, optifineMode)` is the single rule; an explicit selection
  always wins. `CosmeticStore.getEquippedCapeForDisplay()` applies it (reads `FeatureSettings`), and
  the cosmetics panel and `MinecraftRuntimePreparer` both use it so preview and launch-time pack
  agree. Two entries: `optifine_red`/`optifine_blue`.
- `CapePattern.OPTIFINE` is a monogram, **not** a generated weave: the generation loop skips it, so it
  appears on exactly two capes. The glyph rule is `CapePatterns.optifineMonogramAt` (shared by the
  texture painter and the preview), and the loop that draws it is applied *after* the cloth colour so
  the letters stay solid. `OptifineCapeTest` pins the default rule, the black cloth and the monogram.
