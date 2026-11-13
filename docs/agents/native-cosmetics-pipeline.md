## Native cosmetics pipeline (sweetloader `pl::cosmetics` + `pl::hooks`)

The cosmetic system moves beyond the resource-pack fallback toward a direct native runtime pipeline
inside the sweetloader (`app/src/main/cpp/sweetloader`), delivered in four Tasks.

### Task 1 тАФ native `.geo.json` parser + mesh builder (`pl::cosmetics::geometry`)
- `include/pl/cosmetics/geometry/BedrockGeometry.hpp` + `src/pl/cosmetics/geometry/BedrockModelParser.cpp`.
  Parses Bedrock `.geo.json` (format versions **1.12.0** and **1.21.0**, plus legacy 1.8.0) into
  bones/cubes/UVs and builds interleaved vertex/index buffers, **with no resource pack and no
  `PlayerSkinProvider`** тАФ the geometry is a raw JSON string from memory.
- **The rules mirror the Java preview pipeline (`core.cosmetics.geometry`) exactly** so a model that
  looks right in the preview and one the native seam attaches are the same geometry: rotation order
  ZтЖТYтЖТX, chain resolution innermost-first, box-UV unwrap (`2*(w+d)` ├Ч `d+h`), per-face UVs, and the
  negative-`uv_size` mirror folded into increasing texture coordinates. Face order is Top/Bottom/
  Left/Right/Front/Back with corner indices in grid order (`ix|iy<<1|iz<<2`).
- Pure C++ (no Android types), so it is **host-unit-tested** тАФ `tests/cosmetics/BedrockModelParserTest.cpp`
  (52 checks: hat 1.12, wing 1.21 with per-face UV, a six-bone pet, malformed inputs, box-UV layout;
  run `tests/cosmetics/run_tests.sh`). `summarize()` provides the one-line Logcat confirmation line
  without adding a logging dependency to the parser.


### Task 2 тАФ hook manager + render interception (`pl::hooks`, `pl::cosmetics`)
- `include/pl/hooks/DobbyHookManager.hpp` + `src/pl/hooks/DobbyHookManager.cpp`: a Dobby-shaped API тАФ
  `resolve(module, symbol)`, `resolveOffset(module, offset)` (load bias + offset), `hook(target,
  detour, &original, name)`, `unhook`, plus `DobbySymbolResolver`/`DobbyHook`/`DobbyUnhook`
  free-function aliases so ARM64 instrumentation code reads like the Dobby pattern.
- **The backend is the repo-vendored GlossHook (`pl::memory::hook`), not an upstream Dobby build.**
  Dobby's own `master` at every reachable commit fails to build for Android arm64: its
  `closure_bridge_arm64.asm` uses Apple `@PAGE/@PAGEOFF` the ELF assembler rejects, and its
  runtime C++-assembler fallback and `PlatformUtil` are a half-finished refactor (missing
  `core/arch/Cpu.h`, `RuntimeModule::load_address` removed while callers remain). GlossHook is the
  same in-place trampoline primitive, linked arm64 already, and already the engine hook backend тАФ so
  the named manager wraps the proven one. Vendoring a fresh Dobby later is additive (only this file
  changes).
- `include/pl/cosmetics/NativeModelMatrix.hpp`: column-major `Mat4` and `composeCosmeticMatrix`,
  `M_final = world * player_bone * cosmetic_offset`. Rotation about a pivot applies ZтЖТYтЖТX, matching
  the geometry builder so a bone's authored rotation and its anchoring matrix agree.
- `include/pl/cosmetics/NativeRenderHook.hpp` + `.cpp`: intercepts `LivePlayerRenderer::render`
  (slot 17) through `DobbyHookManager`; the detour is a **register-preserving passthrough** (forwards
  x0тАУx7, dereferences nothing), so an unknown ABI cannot fault the render thread. Publishes a frame
  tick and call count. `BoneMatrixSource` is the game-data seam: with no provider
  `BuildCosmeticMatrix` returns false and nothing is drawn тАФ never an invented matrix. The hook
  installs only inside the verified version range (1.26.60.00тАУ1.26.61.00), the same guard the
  player-render feed uses.
- Host test `tests/cosmetics/NativeModelMatrixTest.cpp` (12 checks: identity/translation, the three
  axes, pivot fixed point, composition order, NaN guard).


### Task 3 тАФ keyframe animation + cloth solver (`pl::cosmetics::animation`)
- `include/pl/cosmetics/animation/AnimationSolver.hpp` + `src/pl/cosmetics/animation/AnimationSolver.cpp`:
  - `AnimationSolver` parses Bedrock animation JSON and samples tracks at a time, honouring
    per-key interpolation (step, linear, Catmull-Rom тАФ the "smooth" curve Blockbench emits). A
    malformed document returns false; a time outside the keys returns the nearest key's value.
  - `CapeMotion` mirrors the launcher's Java `CapeAnimationCurve` constants and formulas **exactly**
    (walk 28┬░, flap 42┬░, jump 12┬░, vertical 10┬░, flutter 13┬░/60, sway 10┬░/44, phase lag 0.022, turn
    sway 4┬░), so the preview and the native chain lean by the same amount. Segment shares sum to 1,
    so segmenting redistributes the bend instead of changing how far the cape leans.
  - `ClothSolver` is a Verlet cape/tail solver (9├Ч11; gravity 14, damping 0.22, stiffness 52, max
    stretch 1.35, breeze 12/1.4) mirroring the Java `CapeSimulator`. The game's Molang cape has no
    physics driver, so the native path solves real folds instead.
- `AnimationJni.cpp` exposes `nativeSampleCapeChain(...)` (via `PreloaderInput.sampleCapeChain`) so
  the launcher preview and the native render share one motion curve rather than duplicating the
  amplitudes.
- Host test `tests/cosmetics/AnimationSolverTest.cpp` (27 checks: keyframe parse, Catmull-Rom hits
  the key, step holds, shares sum to 1, walking/jumping lean, cloth settles below the anchor but does
  not fall away, the anchored row follows the shoulder, malformed rejected).


### Task 4 тАФ out-of-band cosmetic sync (`pl::cosmetics::network`)
- `include/pl/cosmetics/network/CosmeticSocketProtocol.hpp` + `.cpp` is the **native encoder/decoder
  for the existing `CosmeticSyncProtocol` datagram**, byte-for-byte compatible with the Java class:
  magic `CS`, version `1`, a type byte (advertise 1 / request 2), then five 2-byte-big-endian
  length-prefixed UTF-8 strings (`peerId`, `name`, `capeId`, `accessoryId`, `petId`, each clipped to
  64 bytes). The peer identity is `peerId`; the cosmetic id set is the three ids. Only catalogue ids
  travel, so a datagram stays under 200 bytes. A missing trailing id reads as `none`, so a future
  version degrades to "wearing nothing" rather than failing to parse.
- `CosmeticSocketClient` is the LAN UDP transport on the same multicast group the launcher uses
  (`239.255.42.100:47902`); `open`/`close`, `send`, and a non-blocking `receive` with a select
  timeout, so it can be polled from a frame tick without stalling the render thread. Fail-closed: a
  failed `open` leaves it closed and no packets flow.
- `CosmeticSyncJni.cpp` exposes `nativeEncodeCosmeticAdvert` / `nativeDecodeCosmeticAdvert` /
  `nativeCosmeticSyncMagic` (via `PreloaderInput`), so the launcher can use the native codec and the
  two implementations cannot drift.
- **A separate protocol from voice, on purpose** (voice is pinned cross-language by golden vectors);
  the differing magic means each module ignores the other's packets.
- Host test `tests/cosmetics/CosmeticSocketProtocolTest.cpp` (21 checks: the exact byte layout,
  round-trip, missing ids тЖТ none, over-long id clamped, magic/version/type rejection).
