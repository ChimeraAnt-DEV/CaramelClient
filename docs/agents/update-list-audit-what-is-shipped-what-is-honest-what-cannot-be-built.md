## Update-list audit (CaramelClient) — what is shipped, what is honest, what cannot be built

The B/G/U/R/M/F/C update list was audited item by item. Recording the conclusions so a later pass
does not re-derive them, and so the two items that cannot be built as written are not "fixed" into
a regression.

### Implemented and verified (logic + tests, device unverified unless noted)
- **B2** in-app copyright is Apache-2.0 across all 11 locales. **B10** "Bedrock Optifine Mode" is
  `optifine_mode_title` = "Game Performance Mode"; no Java-brand wording remains. **B5** the only
  package format is `.antegg` (`.chimerapack`/`.levipack` have zero hits). **C3** the `.antegg`
  manifest carries a `permissions` list (`AntEggManifest`, parsed and emitted by the template).
- **U1** the four destinations are Play/Library/Mods/Settings. **U5** `NavLayoutMode` (bottom nav →
  nav rail → expanded rail) with `nav_bar.xml` + `nav_rail.xml`. **U13** `SetupWizardActivity`.
  **U14** `LaunchFailurePresenter` maps `LaunchReadiness.State` to named copy. **U16**
  `DiagnosticsActivity` (now also reports Min SDK and the launch ABI). **U17** mod rows carry a
  one-line reason and per-row actions (`ModMenuAdapter`).
- **R1/R3/R12/R13** `RadialWheelLayout` (segment cap, 48dp targets), `WheelBackdropCapture`
  (capture-then-blur), `RadialWheelMaterial` (API-level fallback + SOLID tier). **R2** three glass
  tiers. **R5** haptics (`KEYBOARD_TAP`/`CLOCK_TICK`). **R8** thermal ladder via
  `RadialWheelMaterial.budgetFor`/`degrade`. **R9** `MIN_CONTRAST = 4.5f`. **R10** fallbacks.
- **M1** dark-navy tokens in `colors.xml` (`mod_menu_bg_base` `#070A0F` …). **M3** module (switch) vs
  action (button) are separate builders and render differently. **M5** `ModMenuSwitchView` (52x32
  track, 24dp thumb). **M6** settings tab. **M7** sections + search. **M8** `resolveInbuiltModEnabled`
  / `setInbuiltModEnabled` / `ModLoadoutStore.apply`. **M9** `ModMenuOrdering.apply` (pure, persisted).
  **M10** failure badges + reason sheet.
- **G1** `ExtractionCachePolicy` (native extraction version → wipe stale libs). **G2** "Repair
  installation" in Diagnostics. **G3** `libc++_shared.so` provided by the launcher, `ElfHeaderValidator`
  (ABI gate) + `ElfLibraryReader`/`NativeLibraryLoadOrder` (multi-`.so` load order) wired into
  `ModNativeLoader` and `AntEggLoader.loadNative`.
  **G4** `.antegg` dependency check (`AntEggLoader`). **G5** signature DB with fail-closed refusal.
  **G6** submodule-init check. **G8** `ModCrashAttribution` auto-disable after N crashes. **G9**
  `CosmeticsDiagnosticsReport` + dialog. **G10** upstream known-issues note (`cosmetics_known_issues_note`,
  MCPE-238211/243527/241755/242499/229837). **G13** scheduled world backups. **G14** server list.
  **G15** migration interruption messaging. **U12** the blue firework touch layer is deleted.

### How many catalogue modules are still disabled, and why
`PvpModuleCatalog` holds 200 entries: 38 `link()` (a view of a module the launcher already ships)
and 162 `add()` (a roadmap entry). The badge rule is `ModIds.requiresGameData`, which for a
catalogue entry reads `PvpModuleScopes.isReachable`; the honest split today is:

- **50 reachable** and toggleable — 29 `DEVICE_INPUT`, 12 `LOCAL_FEED`, 7 `GAME_OPTIONS`, 3
  `PEER_FEED`. All 50 are `add()` roadmap entries that a route already drives (the 38 `link()`
  entries are *skipped* from the PvP list — they are shown as the real module elsewhere — so they
  are neither counted as reachable nor as disabled here).
- **112 disabled / badged** — 52 `ENGINE_HOOK` and 60 `SERVER_DECIDED`. The counts are pinned by
  `PvpModuleScopesTest.theBadgeCountsMatchTheAuditNote`, so this paragraph and the menu cannot
  drift apart.

A disabled entry is not "broken": it is `add(..., implemented = false)`, so the badge is the
truthful state. The 60 `SERVER_DECIDED` ones are client-side helpers of an outcome the *server*
decides (velocity, reach, auto-block, and the many "server-restricted" notes); no client code can
guarantee them, so no amount of BerryAPI turns them into a promise.

### What BerryAPI needs to enable the rest of the 52 `ENGINE_HOOK` modules
The 52 are one of two shapes, and each needs a different thing.

1. **Entity/world reads (ESP, entity counts, mob/item hitboxes, waypoint-to-entity, block/light
   reads).** These need a **complete world seam**, not just the local-player slot. `BerrySeamC`
   (BerryAPI `seam.h`, ABI 8, `sizeof` 112) already exposes the full surface — `get_entity`,
   `all_entities`, `entities_in_radius`, `entities_in_aabb`, `raycast_entity`, `raycast_block`,
   `get_block`, `get_time`, `get_biome`, `get_light`, `get_chunk_state`, `get_local_player`. The
   preloader's `BindWorldSeam()` fills **only `get_local_player`** (from the `ClientInstance`
   vtable slot the render feed already reads); every other slot is left null, which BerryAPI reads
   as its fail-closed "no data" default. So the binding is done — **what is missing is the host
   side of those callbacks.** Answering `all_entities`/`get_block`/`raycast_*` needs an anchor into
   the game's entity list and level object that the stripped `libminecraftpe.so` does not currently
   give the preloader (the same wall the native entity/camera feed note documents). Until that
   anchor exists, filling those slots would be inventing a reading, so they stay null and the
   dependent modules stay badged.
2. **Local-player and input reads.** Already reachable (`LOCAL_FEED`/`DEVICE_INPUT`), which is why
   they are in the 50.

So BerryAPI needs, in order:
- **The game-version signature pack for the build the user runs.** BerryAPI's OTA packs
  (`sigpack.h`) resolve hooks and struct layouts by build fingerprint; without a pack for the active
  `libminecraftpe.so` build, the hooks do not arm and the world/entity callbacks have nothing to
  read through. The launcher ships the pack side (`resources/preloader/preloader_signature_rules_source.json`,
  revision 7, 1.26.33.x … 1.26.50.x); a build outside those ranges arms nothing.
- **`BERRY_InjectWithConfig` with `enable_client = 1` and the local player id.** `BERRY_Inject()`
  uses `DefaultConfigC()`, which already sets `enable_client`/`enable_render`/`auto_attach_modules`
  to 1, so the bundled client is built; `local_player_id` stays 0 (unknown) until a seam resolves
  the local player, which only affects the voice proximity check, not the world reads.
- **An entity/level anchor in the preloader** (the item above) to actually answer the non-null seam
  slots. This is the single largest blocker for the entity-dependent part of the 52.
- **BerryAPI cosmetics (`Berry::Render`) stay a separate path**: they need the `PlayerModelHook`
  (a player-render detour) bound through BerryAPI's own hook backend, which is BerryAPI's job once
  it is attached — the launcher only binds the seam and starts the runtime (see the BerryAPI
  integration note).

### Cannot be built as written (do not "fix" these)
- **G7 — crash isolation via a separate `:modhost` process.** A native mod's whole purpose is to hook
  the *game's* address space; `System.loadLibrary("preloader")` maps the preloader into the game
  process, and the mod-menu/HUD/render hooks are in-process calls. A `:modhost` process could not
  install a hook in `libminecraftpe.so`, so it would isolate the crash by removing the feature. The
  honest response is per-mod crash attribution (G8) plus the `:crash` process the manifest already
  uses for the crash reporter — both present. Do not add a `:modhost` process and claim isolation.
- **B16 — "signed release builds + checksums" in CI is done** (the release job signs via
  `kevin-david/zipalign-sign-android-release` and publishes `compute_checksums.py` sidecars). The
  *debug* APK is still what the debug artifact upload carries, which is correct for a debug artifact;
  the release artifact is signed. Re-check this if the release job is ever reordered.

### Orphaned strings are not automatically debt
A full scan (`R.string.*`, `@string/*` in layouts, plus the `getIdentifier`/`getString` dynamic
paths) finds ~218 `strings.xml` entries with no consumer. They are **not** a uniform defect:
- The families the update list *removed* (accent, compact-mode, glass-card, firework, blur-intensity,
  animations-enabled, glow-effects, `vip_*`) have **zero** orphan hits — the removals were clean.
- The remainder are deliberate: strings for features whose UI lives elsewhere (the account-skin
  diagnostics rows read by `CosmeticsPanel`), `.antegg`/manifest authoring text, and a handful of
  historical labels. `mod_config_badge_with_count`, `mod_config_restart_hint`,
  `mod_config_offhand_swap_key` and `mod_config_category_prediction` are leftovers of that kind.
- **Do not bulk-delete by scan.** `GamePackageManager.getGameStringResource` and
  `ExternalButtonOverlay` resolve resources by *name at runtime* (`getIdentifier`), and the account
  skin path builds ids dynamically; a name-matching scan cannot see those. Deleting an "orphan"
  there is a silent runtime break. Delete only a family whose consumer you have confirmed is gone,
  the way the accent/`vip_*` removals were done.
