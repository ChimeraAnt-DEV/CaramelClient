# SweetLoader changelog

One entry per change a client or a mod author can observe. Newest first.

This lives with the client's docs rather than inside the `preloader-android` submodule, because it is
published alongside the client release notes: when the submodule commit is bumped, the entry below
that matches the new sweetloader goes into the client's own changelog.

The sweetloader is versioned by its **C ABI version** (`pl_get_abi_version()`, currently `1`) and its
**rule revision** (the `revision` field of `sweetloader_signature_rules_source.json`). A client that
bundles a sweetloader records which rule revision it was tested against; a mod is only
required to care about the ABI version.

## Tested-against matrix

Each client release records the sweetloader ABI version and rule revision it shipped and tested with.
The client verifies the ABI at load and refuses a mismatch, so the row is what a bug report is
compared against.

| Client release | SweetLoader ABI | Rule revision |
| --- | --- | --- |
| current development branch | 1 | 7 |

## Unreleased

- **Signature scan cache** — the resolver stores a scan's offsets, keyed by the game library's ELF
  build id, in a private directory the launcher sets with `nativeSetSignatureCacheDir`. A repeat
  launch of the same game build skips the byte-pattern scan; a game update cannot read the previous
  build's offsets, and a cached offset that falls outside the module is dropped. An empty directory
  disables the cache.
- **Render-distance governor** — `ConfigureOptifineRenderDistance` takes a step size and a minimum
  dwell, and `RenderDistanceChangeCount` reports how many times the governor has moved the distance.
  The dwell stops a drop and a restore from landing on consecutive sustain windows, which read as a
  flickering render distance. Pinned by `RenderDistanceGovernorTest`.
- **`BerryApi`** — the launcher can attach the bundled BerryAPI runtime and read its world seam
  (position and rotation) through the same snapshot the local-player feed publishes.
- **`pl_get_abi_version()`** — the stable C ABI version is published and mirrored on the Java side as
  `PreloaderInput.EXPECTED_ABI_VERSION`, so a client/sweetloader mismatch is refused with a clear
  message rather than a missing symbol.
- **Dead export removed** — `nativeSetupRuntime` is gone; it came from closed-source code and had no
  caller.

## 1.0.0 — the client-facing surface

- Signature resolution by byte pattern and by RTTI-named vtable slot, over executable mappings only,
  with `process_vm_readv` region reads and an ELF-build-id-keyed cache.
- Hook installation through the vendored GlossHook backend, with a prologue verification so a stale
  pattern cannot corrupt code.
- Input bridging (touch, key, text, mouse) to registered callbacks.
- The local-player feed (`ClientInstance` slot 31) publishing a seqlock-protected snapshot.
- The player-render hook (`LivePlayerRenderer` slot 17) with a passthrough detour.
- Native cosmetics: the engine image pipeline, per-piece atlases and the render-geometry blob.
- The Mod Menu / HUD bridge and the external mod bridge.
