# BerryAPI cosmetic C ABI (preloader mirror + launcher surface)

BerryAPI owns the cosmetics service. The preloader does not reimplement it; it mirrors the C surface
and drives it from Java, the same way the replay and governor surfaces are bound.

## The ABI

`berry/cosmetic_c.h` exports `BERRY_GetCosmeticApi()` (a process-lifetime `BerryCosmeticApi` of
function pointers) plus `BERRY_CosmeticStructSize()` / `BERRY_CosmeticAbiVersion()` /
`BERRY_CosmeticBoneReaderStructSize()`. The preloader `dlopen`s the library, so it cannot include the
header; it mirrors the struct in `include/pl/runtime/BerryCosmeticMirror.hpp` and checks the reported
`size`/`abi_version` **before** touching a pointer. The mirror pins `sizeof` for every struct
(`BerryCosmeticAssetC` 520, stats 56, bone transform 44, bone reader 32, `BerryCosmeticApi` 160) and
every status/slot enum value at compile time, so a field added, removed or reordered is a build
error, not a mis-typed call. The cosmetic ABI is version **1**, independent of the global
`BERRY_ABI_VERSION`.

`scripts/verify_berryapi_abi.py` is the build gate: it fails if the bundled
`app/src/main/jniLibs/arm64-v8a/libBerryAPI.so` is missing any of the 18 required `BERRY_*` symbols
or if any of the 8 pinned ABI constants drift. The cosmetic group
(`BERRY_GetCosmeticApi`, `BERRY_CosmeticStructSize`, `BERRY_CosmeticAbiVersion`,
`BERRY_CosmeticBoneReaderStructSize`) and its three constants are checked there.

## The launcher surface

- `org.chimeramc.client.preloader.BerryApi` carries the `cosmetic*` methods and the `nativeCosmetic*`
  declarations, plus the `SLOT_*` and `COSMETIC_*` constants (mirroring `BerryCosmeticSlotC` and
  `BerryCosmeticStatus`). The catalog and presets cross as strings, so no numeric width crosses the
  boundary; every entry point answers the fail-closed default (`false`/`0`/`""`/empty/`-1`) when the
  library or its cosmetic ABI is absent.
- `org.chimeramc.client.core.cosmetics.BerryCosmetics` maps the launcher's equipped set into
  BerryAPI's loadout: it clears the local loadout and equips each chosen id. `equip` takes an asset
  id only — BerryAPI's registry resolves the slot — so a launcher id BerryAPI's packs do not register
  is refused by name rather than equipped into a guessed slot.
- `NativeCosmeticsRuntime.syncEquipped` calls `BerryCosmetics.syncEquipped` alongside the existing
  native publish, so the two stores cannot drift. The Diagnostics screen reports the surface on its
  own row (`BerryCosmetics.summary()`), because the backend can be present while the cosmetic struct
  is absent or refused.

## The player-model hook (the bone half)

The surface draws **nothing** without bones. `set_player_model_hook(target, reader)` installs a
detour on the game's player-render target; the preloader owns the per-build target (a signature-scan
result), so the preloader installs the hook itself and the JNI entries only report its state
(`nativeCosmeticHasPlayerModelHook`) — Java never holds a raw game pointer. Until that hook installs,
the catalog, loadout and presets are live but nothing renders.

## Tests

- Native host: `tests/cosmetics/BerryCosmeticMirrorTest.cpp` (mirror, ABI check, marshalling) via
  `bash app/src/main/cpp/preloader/tests/cosmetics/run_tests.sh`.
- Launcher: `BerryCosmeticApiTest` (enum values, fail-closed defaults, record/counter decoding) and
  `BerryCosmeticsTest` (the equip-id filter and the no-op sync).
- `JniBindingParityTest` proves every Java `nativeCosmetic*` declaration has a matching export.

Do not add a parallel cosmetics surface in the preloader: the engine-level render hook is the
preloader's, everything script- or data-facing is BerryAPI's.
