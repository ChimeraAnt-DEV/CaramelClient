## Native cosmetics: one key scheme, local-player-only draw, and the in-app diagnostic

- **There is exactly one registry key scheme.** The local player is always published under the fixed
  id `"local"` (`NativeCosmeticsBridge.LOCAL_PLAYER_ID`); a peer is published under their cosmetic-sync
  peer id. The local cape/hat/pet are *separately* keyed by role in the piece-atlas slots
  (`nativeSetPieceAtlas`), which is what the renderer actually binds. An earlier version keyed the
  local cape by its catalogue id and relied on a native "first cape override" fallback, so with more
  than one override registered the local player could be drawn with a peer's cape. `publishCape(null)`
  removed every cape override (wiping peers) as a side effect of unequipping; it now removes only the
  local entry, and `clearLocal()` exists for a local reset. `clear()` remains the deliberate
  whole-registry reset (session teardown) and is not an unequip path. `FirstCapeOverride` /
  `CopyFirstCapeOverride` were removed as dead code once the renderer moved to role-keyed atlases.
- **The native renderer draws only the local player, on purpose, until a device proves otherwise.**
  The render hook's arguments are an unknown ABI and the hook does not identify which actor is being
  drawn (no player/actor id anywhere in `NativeRenderHook`/`NativeCosmeticRenderer`), so it draws the
  local player's published set and nothing else. A per-actor draw needs a native identity read plus
  device verification; `NativeCosmeticsBridge.LOCAL_PLAYER_ID` carries a `TODO(device)` for it. No
  xuid or gamertag is transmitted anywhere.
- **The status line requires an actual draw to say "active".** `NativeCosmeticsFeed.renderState()`
  distinguishes `INACTIVE` / `HOOK_ONLY` (hook live and drawable, nothing drawn yet) / `ACTIVE`
  (at least one mesh drawn this session). `cosmetics_native_active` is shown only for `ACTIVE`;
  `HOOK_ONLY` shows `cosmetics_native_hook_only`. Saying "drawn on your character in game" on the hook
  alone was the over-claim this fixes.
- **The in-app diagnostics screen** (`CosmeticsDiagnosticsDialog` + `CosmeticsDiagnosticsReport`,
  opened from the Cosmetics panel) exists because the dev environment has no device and no adb and the
  app cannot read logcat without `READ_LOGS`. It reads the native feed directly via
  `PreloaderInput.readRenderDiagnostics()` (layout: `{renderSlot, resolvedBy, glBackend,
  eglContextCurrent, hookInstalled, framesSeen, lastError, geometryBytes, pieceAtlasMask}`) and shows
  the Minecraft version, app version, GL backend, whether an EGL context is current, and the draw
  count. The formatting half is pure and unit-tested (`CosmeticsDiagnosticsReportTest`); unknown
  readings render as "unknown", never as `0`/`no`.
- **The debug cube** (`nativeSetCosmeticDebugCube`) draws one bright cube above the local player's
  head regardless of the equipped set, so a device can prove the draw path on its own. Its position
  and appearance are **unverified on device** — that is the whole point of the toggle.
- **A2: the render slot is not changed.** Only logging was added (`LastResolvedRenderSlot` records
  which slot was used and how it was resolved). Changing the slot needs device data; a real slot fix
  is a separate, evidence-driven change.
- **A4: JNI binding parity is checked in CI.** `JniBindingParity` (rule) + `JniBindingParityTest`
  (which scans the real repository) fail the build if any Java/Kotlin `native` (or Kotlin
  `external fun`, honouring `@JvmName`) has no C++ export, or vice versa. Classes whose native
  implementation is in a **prebuilt** library with no source here (`libmtbinloader2.so` and the
  mbloader `LibBindings` bridge) are excluded from the Java side; a test asserts they are *not* also
  exported by the sweetloader, so the exclusion cannot hide a drift. This is the
  check that would have caught the `launcher_sweetloader` symbol rename. The APK build compiles
  `libsweetloader.so` from the `app/src/main/cpp/sweetloader` submodule source and bundles no prebuilt
  copy of it — verified by building `:app:assembleDebug` and finding
  `lib/arm64-v8a/libsweetloader.so` produced by CMake under `app/.cxx/.../obj/`.
- Device procedure: `docs/DEVICE_CHECKLIST.md`.
