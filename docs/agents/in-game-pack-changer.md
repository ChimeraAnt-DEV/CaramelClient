## In-game pack changer (core.content.InGamePackChanger + PackChangerPanel)
- Per-instance opt-in toggle in Instance Settings (`GameVersion.inGamePackChangerEnabled`,
  persisted in `VersionProfileMetadata`). When on, the in-game Mod Menu's **Packs** section lists
  the instance's resource packs with a per-pack pill toggle.
- It rewrites `minecraftpe/global_resource_packs.json`, the file the running game reads, and
  writes to **every candidate game-data root** (`LauncherStorage.getCandidateGameDataDirs`) -- the
  same defence the cape installer uses, because the game picks its storage from isolation and
  internal/external, and a write to only the wrong guess is silently ignored.
- **The pack-list `version` must be a three-number array (`[1,0,0]`), never a string.** The game
  matches the entry against the manifest's own array version, so `"version": "1.0.0"` is not
  comparable and the pack is dropped silently -- it applies to the in-game changer, skin packs and
  cape packs alike because they all share this list. Every writer (`InGamePackChanger`,
  `SkinPackActivator`) routes through a `versionArray` helper, and existing string entries are
  normalised on the next rewrite. `InGamePackChangerTest.theVersionIsWrittenAsAnArrayNotAString`
  and `CapeInGameInstallerTest.theGlobalEntryVersionIsAnArraySoTheGameMatchesThePack` pin it.
- **A live in-place reload is not possible on the shipped builds, and this was verified against the binary.** Reverse-engineering of `libminecraftpe.so` (1.26.50.4 and 1.26.60.28) found the pack machinery but no safe live trigger: `ContentManager::reloadSources` throws unless its async init task completed (i.e. not during play), `ResourcePackManager::setStack`/`_doStackOperation` are reached only from world setup/teardown, game classes export no symbols so there is no hook anchor, and the "hot reloader" is a debug-gated texture-atlas-only feature. See `preloader-android/docs/resource-pack-reload.md`. `PreloaderInput.nativeReloadResourcePacks()` therefore has a **real, fail-safe** native body that resolves to "no hook", returns false, and lets the caller relaunch; it never throws or partially applies. The honest behaviour is: write both the global list and the running world's own `world_resource_packs.json` (which the running world does read), then report `pack_changer_reload_on_next_load` rather than claiming the change is live.
- **The live-reload attempt needs no setting; the native seam is the gate.** `InGamePackChanger.requestReload()` forwards to the installed `Reloader` exactly as before тАФ the `Reloader` is what calls `PreloaderInput.reloadResourcePacks()`, and the native side returns false unless the running build was confirmed to support a refresh. Detection is **runtime, not a version allowlist** тАФ the sweetloader marks the running build supported only after a refresh it confirmed, and never retries a failed call in the same session. There is no build list here to keep in step, and the `InGamePackChangerTest` forwarding cases stay valid.
- The `InGamePackChanger.Reloader` is installed from `InbuiltOverlayManager.showEnabledOverlays()`,
  **not** as a side effect of starting voice chat (which is how it was first wired, leaving the
  pack changer with no reloader whenever the voice module happened to be off).
