## BerryAPI integration (sweetloader bridge + runtime start)

BerryAPI (`libBerryAPI.so`, https://github.com/Lynxifyy/BerryAPI) is a separate Bedrock client
runtime. The launcher never links it at build time: the sweetloader `dlopen`s the bundled
`app/src/main/jniLibs/arm64-v8a/libBerryAPI.so` and talks to it through its small C ABI
(`BERRY_Inject`/`BERRY_Detach`/`BERRY_IsAttached`/`BERRY_Version`, the world seam
`BERRY_SetWorldSeam`/`BERRY_HasWorldSeam`/`BERRY_WorldSeamStructSize`/`BERRY_WorldSeamAbiVersion`,
and the replay/governor surfaces `BERRY_GetReplayApi`/`BERRY_GetGovernorApi` + their
`StructSize`/`AbiVersion` getters).

The bridge lives in `sweetloader/src/pl/runtime/BerryApiBridge.cpp` + `BerryApiJni.cpp`, with the
mirrored structs in `BerrySeamProvider.hpp`, `BerryReplayMirror.hpp` and `BerryGovernorMirror.hpp`.
The Java surface is `org.chimeramc.client.preloader.BerryApi` (file under
`launcher/sweetloader/`, package `org.chimeramc.client.preloader` to match the JNI names).

**The ABI is verified against the shipped binary, not assumed.** Disassemble the bundled `.so`
(`llvm-objdump -d` from the NDK) and read the constants the getters return. They currently are:

| surface  | `sizeof` | ABI | mirror `static_assert` |
|----------|---------:|----:|------------------------|
| world seam | 112 | 8 | `SeamMirror` == 112, `kHostSeamAbiVersion` == 8 |
| replay     | 144 | 2 | `ReplayApiMirror` == 144, `kHostReplayAbiVersion` == 2 |
| governor   |  48 | 1 | `GovernorApiMirror` == 48, `kHostGovernorAbiVersion` == 1 |

A mismatch is refused by a number (`MirrorMatches`), so a layout drift can never be bound through.

**The core entry points return `int`, not C++ `bool`.** BerryAPI's header says so explicitly: a
1-byte `bool` return leaves the upper result-register bytes undefined, so a caller that declared
`bool (*)()` can read a live `BERRY_IsAttached()` as false. The bridge resolves `BERRY_Inject` and
`BERRY_IsAttached` as `int (*)()` and compares `!= 0`, and `verify_berryapi_abi` fails the build if
the typedefs drift back to `bool`.

**The runtime must be started, or every BerryAPI feature is silently dark.** `BerryApi.ensureAttached()`
and `BerryApi.bindWorldSeam()` are called from `ModManager.pumpBerryRuntime()`, which
`MinecraftActivity.tick()` pumps once per process — the same deferred point the native hooks install
from, because attaching in `onCreate` sees a partially mapped `libminecraftpe.so` and bails. It is
fail-closed and one-shot: a missing library or a failed attach leaves the capabilities off, and
Diagnostics/About report `BerryApi.summary()`. `BerryRuntimeWiringTest` pins this wiring, because it
was missing once and the build could ship with the runtime never attached.

**What the seam can answer, and what it cannot.** The sweetloader fills only the local-player slot
(`get_local_player`), read from the `ClientInstance` vtable slot the render feed already uses. It
cannot enumerate other entities, read a block, or read level time: the game strips its symbols, so
there is no anchor, and inventing one is the over-claim this module exists to avoid. A world seam
alone therefore turns on nothing that needs an entity feed — see the update-list audit note and the
native entity/camera feed note. The modules that need that feed stay badged rather than reporting a
reading that was never taken.
