# JNI entry points
Every symbol the sweetloader exports as `Java_…` and the Java/Kotlin class that owns it.

This table exists because a JNI symbol encodes its owning class's **fully-qualified** name, so a
class that moves package silently stops resolving: the Java side still declares `native`, the
linker still exports the old mangled name, and the only symptom is an `UnsatisfiedLinkError` the
first time the method is called. That is exactly how the `launcher_sweetloader` cosmetic-sync
regression shipped. `JniBindingParityTest` scans both sides and `theJniTableListsEveryExport`
below fails the build if this table drifts from the code.

Column meanings:

- **Owner** — the FQN of the class declaring the `native` methods.
- **Source** — the C++ file that defines the exports (a class may be split across files).
- **Exports** — how many `Java_…` symbols that file defines for the owner.

| Owner class | Source | Exports |
| --- | --- | --- |
| `org.chimeramc.client.core.mods.ModManager` | `src/pl/SweetLoader.cpp` | 8 |
| `org.chimeramc.client.core.mods.inbuilt.ExternalModBridge` | `src/pl/runtime/ExternalModBridgeJni.cpp` | 17 |
| `org.chimeramc.client.core.mods.inbuilt.MoreButtonsSvgBridge` | `src/pl/runtime/ExternalModBridgeJni.cpp` | 1 |
| `org.chimeramc.client.preloader.BerryApi` | `src/pl/runtime/BerryApiJni.cpp` | 9 |
| `org.chimeramc.client.preloader.PreloaderInput` | `src/pl/SweetLoader.cpp` | 2 |
| `org.chimeramc.client.universe.UniverseLoader` | `rust/universe-jni/src/lib.rs` | 8 |
| `org.chimeramc.client.singularity.SingularityEngine` | `rust/singularity-jni/src/lib.rs` | 6 |
|  | `src/pl/cosmetics/animation/AnimationJni.cpp` | 1 |
|  | `src/pl/cosmetics/network/CosmeticSyncJni.cpp` | 3 |
|  | `src/pl/runtime/GameCosmeticsJni.cpp` | 48 |
|  | `src/pl/runtime/InputJni.cpp` | 19 |
|  | `src/pl/runtime/OptifineJni.cpp` | 8 |

Total: **133 exports** across **8 owner classes**.

The two `rust/…-jni` rows are the `.universe` / `.singularity` loader cdylibs. Their exports are
Rust (`#[no_mangle] … fn Java_org_chimeramc_client_…`), not C++; `scanCppExports` walks `.rs` files
alongside `.cpp`/`.h` so the parity check covers both halves of the JNI surface.

## Thread rules

Callbacks and hooks the sweetloader installs run on the game's own threads, never the launcher's. A
JNI call made from a detour therefore has to be safe from that thread:

- **`PreloaderInput` input entry points** (`nativeOnTouch`, `nativeOnKeyEvent`, `nativeOnTextInput`,
  `nativeOnMouse`) run on the game's input thread. They forward straight into the launcher's
  registered callback and must not block or allocate per event; see `src/pl/runtime/InputBridge.cpp`.
- **`PreloaderInput` cosmetic frame push** (`nativePushCosmeticFrame`) is called by the launcher on
  the game's frame tick and stores the buffer; it does not draw. The render-thread detour reads the
  stored frame, and the draw callbacks registered through it run on the render thread.
- **`PreloaderInput` readers** (`nativeRead*`, `nativeIs*`, `native*Count`) are safe from any thread:
  they read a seqlock-protected or atomically-published snapshot and return a copy. That is the
  contract that lets the launcher poll them from the UI thread without a lock.
- **`PreloaderInput` writers** (`nativeSet*`, `nativeClear*`, `nativeRemove*`) publish a new snapshot
  under a short mutex; call them from the launcher's own thread, not from a detour.
- **`PreloaderInput` configuration** (`nativeConfigureSignatureRules`, `nativeSetSignatureCacheDir`)
  runs once during launch preparation on the launcher thread. The cache directory is private app
  storage; an empty path disables the cache.
- **`ModManager` lifecycle calls** (`nativeLoadMod`, `nativeEnableLoadedMods`, …) run during launch
  preparation on the launcher thread; they are not safe to call from a game callback.
- **`ExternalModBridge`** is called by the launcher and by mods; treat it as
  launcher-thread unless the specific method documents otherwise.
- **`BerryApi`** calls attach to the JVM on demand through a thread-local guard, so a script thread
  can call them; they are the one surface designed for a non-launcher thread.

If a new export must be callable from a detour, say so here and make the reader snapshot-based.

## The C ABI surface

Beside the JNI exports the sweetloader exposes a small, versioned C surface that a mod or an embedder
may resolve by name. It is deliberately tiny: the JNI table above is for the launcher, this is for
native code that already runs inside the game process.

| Symbol | Thread | Contract |
| --- | --- | --- |
| `pl_get_abi_version()` | any | Returns `PL_SWEETLOADER_ABI_VERSION`. Pure read, no state. A host compares it against its own expected value and refuses the surface on a mismatch rather than calling into it blind. |

`PreloaderInput.EXPECTED_ABI_VERSION` on the Java side and `PL_SWEETLOADER_ABI_VERSION` in
`src/pl/SweetLoader.cpp` are bumped together; `PreloaderInput.abiMatches(int)` is the pure comparison
and is unit-tested, so a mismatch is a red build rather than a runtime surprise.

## Thread rules in one place

Every surface the sweetloader publishes falls into one of four thread classes. Call a surface from the
wrong class and the failure is a data race, not an exception, so the class matters more than the
signature:

| Class | Threads it may be called from | Surfaces |
| --- | --- | --- |
| **Input thread** | the game's input thread only (the sweetloader calls *you*) | `PreloaderInput.nativeOnTouch`, `nativeOnKeyEvent`, `nativeOnTextInput`, `nativeOnMouse` — the sweetloader forwards to your registered callback |
| **Render thread** | the game's render thread (the sweetloader calls *you*) | the post-render callbacks registered through the player-render hook; cosmetic draw callbacks |
| **Any thread (read)** | any, including the launcher UI thread | `PreloaderInput.nativeRead*`, `nativeIs*`, `native*Count`, `pl_get_abi_version` — each returns a copy of an atomically published or seqlock-protected snapshot |
| **Launcher thread (write)** | the launcher's own thread, never a detour | `PreloaderInput.nativeSet*`, `nativeClear*`, `nativeRemove*`, `ModManager` lifecycle calls, `ExternalModBridge` unless the method says otherwise |
| **Script thread** | any non-launcher thread | `BerryApi` calls, which attach to the JVM on demand through a thread-local guard |

The rule for a new export: if a detour must call it, it belongs in the **any thread (read)** class
and must be snapshot-based. Anything that mutates shared state stays on the launcher thread and the
detour publishes into a snapshot the launcher reads.

