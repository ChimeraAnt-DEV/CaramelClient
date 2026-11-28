## JNI/native packaging (org.chimeramc only)
- **There is no `org.levimc` package and no prebuilt `libgxcore.so`/`libinbuiltmods.so`.** They were closed-source arm64 binaries with no source in this repository, and every Java class bound to them was deleted with them. The only prebuilts left in `app/src/main/jniLibs/arm64-v8a/` are `libPlayFabMultiplayer.so`, `libmaesdk.so` and `libmtbinloader2.so`, none of which binds a Java `native` method through an `org.levimc` symbol.
- The sweetloader submodule source (`app/src/main/cpp/sweetloader`) is branded `org.chimeramc.*`, so `ModManager`, `ExternalModBridge`, `PreloaderInput`, `MoreButtonsSvgBridge` and the `nativemod.*` wrappers (`AutoSprintMod`, `FpsMod`, `HotbarSlotMod`, `MoreButtonsMod`, `PojavControlsMod`, `SnaplookMod`, `ZoomMod`) all live under `org.chimeramc`. `JniBindingParityTest` scans the real tree and fails if a Java `native` declaration has no C++ export (or the reverse), so a symbol rename cannot ship silently.
- **The bundled `libPlayFabMultiplayer.so` and `libmaesdk.so` are fallbacks, not defaults.** Both
  appear in `libminecraftpe.so`'s `DT_NEEDED` on 1.21.110+ (PlayFab from 1.21.130), and Android's
  linker binds a soname to whichever copy is already loaded. `NativeLibraryVersionGate.loadOrder`
  therefore opens the version-gated libraries **before** the game library when the build ships its
  own copies, so the game uses the copies extracted from its APK; `loadLibraryDetailed` falls back
  to `System.loadLibrary` only when the extracted file is absent (old builds). Do not reorder
  `loadAllLibraries` back to "base libs then `systemLoadedLibs`": on 26.60 that made the game run
  against the launcher's stale PlayFab, which is one shape of a "Connecting…" loop.
