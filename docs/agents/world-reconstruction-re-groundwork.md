## World reconstruction — RE groundwork and the remaining wall

The goal is to fill BerryAPI's world seam (`AllEntities`, `GetBlock`, `GetTime`, …) from the
preloader so its replay capture reconstructs the whole world, not just the local player. This is
the state of that work: what is proven, what is tooled, and the one thing still missing.

### The binary and the tooling (done, reproducible)

- The game binary used for this pass is `libminecraftpe.so` from
  `minecraft-26-60-28.apk` (mcpedl.org `file_id` 7572), 368,150,880 bytes. The APK is 382 MB; the
  `.so` is stored deflated, so extract with Python's `zipfile` (no `unzip` in this container).
- The RTTI/vtable resolver the preloader uses (`resolveVtableFunction`) is **validated** against
  this binary: emulating the `Vtable.cpp` algorithm over `.rela.dyn` (`R_AARCH64_RELATIVE`
  addends) reproduces the doc's known values exactly — `18LivePlayerRenderer` vtable
  `0x15144a78` slot 17 → `0xaeaefb4`, and `20ClientNetworkHandler` vtable `0x1520ef98` slot 40 →
  `0xb80f518` (whose body references `"AddPlayerPacket: NaN position sent by server"`). Any new
  slot resolved this way can be trusted the same way.
- Resolved vtables on this build: `ClientInstance` `0x151551b0`, `Level` `0x156c8f70`,
  `Actor` `0x1557c870`, `BlockSource` `0x1562d7b0`, `Dimension` `0x150a8aa0`,
  `LocalPlayer` `0x152ba2f0`, `Player` `0x152b55a8`, `LivePlayerRenderer` `0x15144a78`,
  `ClientNetworkHandler` `0x1520ef98`.
- An ADRP+ADD/LDR xref scanner (numpy over `.text`) locates the code that references a given
  `.rodata` address. It is validated the same way (it finds the `AddPlayerPacket` handler at
  `0xb82dbd0`, matching the doc).
- **The binary retains source-path debug strings** (`D:\a\_work\1\s/handheld/src/common/entity/…`,
  `…/common/world/level/…`) and assertion strings (`"Level has no actor."` at `0x2a05943`,
  `"Duplicated entity, entity trying to load with player's Actor"`), so there are real anchors even
  without exported symbols.

### The local-player seam (already shipped, re-confirmed)

`ClientInstance` slot 31 (`0xb013810`) is the local-player accessor: it takes `x0 = self`,
materialises `self + 0x240` into `x1` and calls a helper. The preloader hooks this slot and reads
`Actor+0x230` (position) and `Actor+0x238` (view rotation). That is the *only* slot this build fills.

### The remaining wall: `AllEntities`

BerryAPI's recorder (`ReplayCapture::CaptureTick`) fills its keyframes from the seam's
`AllEntities()`, so the world reconstruction is exactly "implement `AllEntities`". That needs the
Level's actor list, and this is where it stops:

- **`Level`'s small vtable slots are field getters, not list accessors.** Disassembled, they are
  `ldr x0, [x0, #off]; ret` one-liners (`+0x2f8`→`+0x18`, `+0x538`, `+0x180`, `+0x598`, `+0x120`,
  `+0x128`, `+0xe0`, `+0xc0`, `+0xf8`, `+0x130`, `+0x138`, `+0x148`, `+0x140`). None returns a
  vector; the actor list is behind a container whose iterator/begin/end are inlined into the
  callers, not exposed as a slot.
- **The entity manager is not RTTI-named.** There is no `ActorManager`/`EntityRegistry` typeinfo
  string to resolve (the string at `0x247c3db` is a log literal, not a type). The container is
  reached by chasing a Level field, which means identifying *which* field and *what* its
  begin/end accessors are — a per-function disassembly job over a 220 MB `.text`.
- **The actor-fetch path uses asserts, not accessors.** `"Level has no actor."` sits in a small
  non-virtual method (vtable `0x1562e7b0`, dtor `0x11bb4fac`) reached through a `Level` reference
  held elsewhere; the honest read is that this is the `Level::fetchActor(runtimeId)` path, which
  answers one id, not the list.

### What a complete job needs (and why it is not a one-session change)

1. Identify the Level field holding the entity/actor manager, by disassembling the functions that
   build a `BlockSource` from a `Level` (they take the `Level*` and reach the actor container).
2. Recover the container's `begin()`/`end()` (or `size()`/`at()`) so `AllEntities` can iterate.
3. Recover each entity's runtime id, type, position, velocity, rotation, health and name from the
   `Actor` object (the same `Actor` the local-player seam already dereferences, so its layout is
   partly known: `+0x230` position, `+0x238` rotation).
4. Do the same for `GetBlock` (BlockSource's block accessor) and `GetTime` (the LevelData getter).
5. Each of these needs a fresh pass per game build, because the game strips its symbols — the
   byte-pattern rules drift (the doc records `pauseMenuDtorSig` matching 0 places on 26-60-30), so
   the RTTI/vtable route is the only stable one, and only for the slots it can reach.

**No entity data was invented.** The seam's `all_entities` slot stays null, which BerryAPI
documents as the fail-closed default, so the recorder captures what it honestly can (the local
player's own session plus the markers and the camera path) and the replay viewer says the world is
not reconstructed rather than drawing a fabricated one.
