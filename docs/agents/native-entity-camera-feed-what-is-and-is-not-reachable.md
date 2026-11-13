## Native entity/camera feed тАФ what is and is not reachable (verified against 1.26.50.04_RC3, 1.26.60.28 and 1.26.60.30)
Verified by downloading the real `lib/arm64-v8a/libminecraftpe.so`. First against `26-50-arm64-v8a`
(363,050,262-byte APK; the `.so` is stored deflated at 98,404,093 bytes so only that range needs
fetching, then inflate with a raw zlib stream). Then re-verified independently against
`26-60-28` (382,360,914-byte APK from mcpedl.org, `file_id` 7572 -> `minecraft-26-60-28.apk`;
`libminecraftpe.so` is 368,150,880 bytes, deflate ratio ~0.30), and again against `26-60-30`
(386,141,547-byte APK, `file_id` 7618 -> `minecraft-26-60-30.apk`; `.so` is 370,379,104 bytes).
Do not re-run this RE from scratch; these are the conclusions.


- **Game classes are stripped of exported symbols.** The 90,665 defined dynamic symbols in
  1.26.60.28 are third-party only (v8/cohtml/webrtc/Xal/xbox/leveldb/`std`). There is no
  `_ZN...Actor...`, `_ZTV...Level...` or any Minecraft method symbol, so `dlsym`/`resolveSignature`-
  by-name cannot reach game state. The only `Java_*` exports are host plumbing (`MainActivity`,
  `BatteryMonitor`, `NetworkMonitor`, `JellyBeanDeviceManager`, Xbox/XAL interop) -- there is no
  export that returns a `Level*`/`ClientInstance*`/player position.
- **The `.text` is real AArch64, not encrypted.** A `ret` (`c0 03 5f d6`) appears ~22k times and
  section entropy is ~6.6 bits/byte, so byte-pattern signatures *can* in principle be derived from
  the shipped file. That does not make the feed reachable -- see the fragility note below.
- **RTTI *name strings* exist, and `resolveVtableFunction` does resolve them.** Standalone
  `11LocalPlayer`, `5Level`, `14ClientInstance`, `11BlockSource`, `5Actor`, `6Player` strings are
  present in `.rodata` (each exactly once) and each has exactly one RELATIVE relocation resolving
  its address, so the typeinfo and its vtable are found. Emulating `Vtable.cpp`'s algorithm against
  1.26.60.28 resolves `14ClientInstance` slot 151 to a `.text` address, so **the shipped
  `isShowingMenuVtableIndex` mechanism survives a version bump** and needs no new binary.
- **The vtables reached this way are the pure-virtual *interface* bases.** Confirmed again on
  1.26.60.28: `11LocalPlayer`'s primary vtable slots 0,1,4,5 are **identical** to `6Player`'s
  (`0x11232294`, `0x11a115b4`, `0x115d674c`, `0x11a2e908`), the shared-base pattern. Slot indices
  are also version-fragile (`isShowingMenuVtableIndex` is 151 on 1.26.50 but 150 on 1.26.40).
- **The shipped 1.26.50 byte patterns mostly do not match 1.26.60.28.** Scanning `.text` with the
  bundled rules as-is: `pauseMenuOpenSig` matches 2 places (ambiguous), `hudScreenOpenSig` and
  `pauseMenuDtorSig` match 0. `resolveSignatures` takes the first hit, so this is the concrete
  evidence that a byte-pattern feed needs a fresh pattern per build and cannot be inherited.
- **The RTTI relocations are the vtable structures, not calls to a named method.** Of 610,452
  unique `R_AARCH64_RELATIVE` addends, 110,370 point into `.rodata`, but **none** point at the
  method-name strings (`Actor::getFilteredNameTag`, `Player::setupCamera`, ...) even though those
  strings exist -- so they cannot be xref'd via the vtable structures. Finding the function that
  reads a player's position needs a disassembler pass over the 220 MB `.text`; `resolveVtableFunction`
  gives a vtable, but a static vtable address alone does not yield a live `Actor*`.
- **The RTTI seams survive the newest build (re-verified on 1.26.60.30, `.so` 370,379,104 bytes).**
  Emulating the `Vtable.cpp` algorithm against `.rela.dyn` (1,055,159 entries) resolves, by RTTI name
  with **no per-build code address**:
  - `14ClientInstance` typeinfo `0x15364468`, vtable address point `0x15363398`, **slot 31 ->
    `0xb0fbd7c`** (the local-player accessor the Voice nametag/position feed hooks);
  - `18LivePlayerRenderer` typeinfo `0x15353670`, vtable `0x15352bc8`, **slot 17 -> `0xaf98e28`**
    (the player-render feed / cosmetics render seam);
  - `20ClientNetworkHandler` typeinfo `0x1541fd88`, vtable `0x1541eb68`, **slot 40 -> `0xb8fcc9c`**
    (the player-join packet seam).
  `LocalPlayer` also resolves (vtable `0x154ccda0`), but its primary vtable is the pure-virtual
  interface base тАФ slots 0,1,4,5 are identical to `Player`'s тАФ the same shared-base pattern as
  1.26.60.28, so it does not yield a live `Actor*` on its own.
- **The byte-pattern rules DO drift on the newest build; the RTTI seams do not.** Re-running the
  bundled `1.26.50.00` signatures against 1.26.60.30 `.text` (`0x7cfd000`, size `0xd3a8700`):
  `pauseMenuDtorSig` 0 hits, `hudScreenOpenSig` 0 hits, `pauseMenuOpenSig` 2 hits (ambiguous, first
  is wrong), `hudScreenDtorSig` 1 hit. The pinned `imageLoaderSig` matches **0** times. This is the
  concrete evidence that a byte-pattern feed needs a fresh pattern per build, while the
  `isShowingMenuVtableIndex` / `playerRenderVtableIndex` / `packetReadVtableIndex` slots resolve by
  RTTI name across 1.26.50 тЖТ 1.26.60.30.
- **A fresh image-loader pattern is derivable.** `mce::ImageUtils::loadImageFromMemory`'s
  "loadImageFromMemory" string sits at `.rodata 0x2de856a`; its function prologue was located by
  scanning `.text` backwards from the string xref (`ADRP`+`ADD` to the string at `0x15002824`) to
  the enclosing frame prologue at **`0x1500257c`**. The 1.26.60.28 function started
  `FF 43 04 D1 FD 7B 0B A9 ...`; the 1.26.60.30 one starts `FD 7B 0C A9 A9 0D 6F FC ...` (a
  different frame layout), which is why the inherited pattern misses. Deriving the new pattern needs
  a disassembler pass (available: `llvm-objdump` in the NDK toolchain) тАФ but note the shipped
  cosmetics path is **fail-closed**: an unmatched `imageLoaderSig` simply leaves the resource-pack
  fallback in place, so the cosmetic still works via the pack.

- **The *entity* feed a hitbox/crystal/armor/nametag module needs is therefore not derivable
  statically.** A `dynamic_cast` on a live `Actor*` would identify `LocalPlayer`, but there is no
  way to obtain the `Level*`/`ClientInstance*` to start from without either a byte-pattern signature
  per build or a `_ZTI...`-style mangled symbol, and this build exports neither for game classes.
  The in-repo `resolveSignature` is the right mechanism but needs patterns derived from the binary.
- **The *local-player* feed, however, is implemented and is a different problem.** The in-world
  Voice nametag icon needs only the listener's own view, and the local player is reachable without
  any signature: `ClientInstance`'s vtable **slot 31** returns the local player, and that slot is
  resolved **by RTTI name** through `resolveVtableFunction("14ClientInstance", 31, ...)` -- no
  per-build code address. The sweetloader hooks that slot and snapshots position (`Actor+0x230`) and
  view rotation (`Actor+0x238`); both field offsets are shared by 1.26.33.1 / 1.26.50.4 / 1.26.51.1
  (confirmed against the 1.26.50.4 and 1.26.60.28 binaries). Live pointer dereference happens **only
  inside the hook** (game thread, right after the game returned the pointer); Java reads a
  seqlock-protected copy with a staleness window, so a session ending cannot leave the UI thread
  reading freed memory. See `GameLocalPlayer.cpp` in the sweetloader. This does **not** unblock the
  combat modules -- they need *other* entities' positions, which is the unreachable problem above.
- **The game's Java layer is no help.** The dex contains 59 `com/mojang/minecraftpe/*` classes in
  1.26.60.28 (down from 68), all Android host plumbing (MainActivity, PlayIntegrity, FilePicker,
  Braze, WorldRecovery) plus PairIP-obfuscated stubs. There is no player/entity/position/camera API
  and no `native` method declaring one -- all game state lives in `libminecraftpe.so`.
- **The sweetloader exposes no ergonomic player/level accessor.** Its `PL_EXPORT` surface is memory
  primitives (`resolveSignature`/`resolveVtableFunction`/`hook`/`writeBytes`), input callbacks, and
  Mod Menu/HUD submission -- nothing that returns a world position. So even a native mod has to
  build the feed from the same signatures.
- **`WorldSource` / `DataSource` / `EntitySource` have no provider and are never installed**
  (`setWorldSource`/`setDataSource`/`setEntitySource` have zero callers). Crystal Optimizer, Armor
  HUD and Hitboxes therefore cannot produce output; they correctly show their "awaiting game data"
  state. Implement one only with a verified signature set, and keep the honest empty state.
  `TagSource` is the exception: the in-world Voice nametag icon *is* installed now (see the
  local-player feed bullet above), because the peer positions it needs arrive over the voice
  protocol and only the local view comes from the game.
- **A cape feature is not reachable through this.** The in-game cape is a resource pack (see the
  cosmetics section), a supported mechanism rather than a native hook.
- **The Hitboxes module is *not* greyed out, and must not be.** `HitboxMod.EntitySource` has no provider, so the native
  entity frame is always null тАФ but `HitboxOverlay.drawPeerBoxes` still draws other players from the proximity-voice feed
  (`PeerHitboxSource` тЖТ `VoiceChatModule.audiblePeers()`), which needs no game hook at all. `ModIds.requiresGameData`
  therefore **excludes** `HITBOX`; only the options that genuinely need the entity list are disabled, via
  `ModIds.outputRequiresGameData(modId, configKey)`. Peer boxes need `LocalPlayerFeed.localCamera(...)` for the local view,
  which is installed unconditionally (independent of the voice module), so a player with voice off still gets boxes for any
  peer whose position arrived. See the Hitboxes section above for the full contract.
- Static-analysis caution: RTTI pointers in this PIE binary live in `.data.rel.ro` as
  `R_AARCH64_RELATIVE` relocation addends, so a plain byte scan for the pointer finds **nothing**
  and produces a false "RTTI is absent" conclusion. Read `.rela.dyn` instead. Equally,
  `re.finditer` over a 328 MB `.so` is unusable -- extract `strings` once and work from that.
- **Re-verified on 1.26.60.30 (2026-10).** `14ClientInstance` resolves with vtable address point `0x15364470`; slot 31
  (`getLocalPlayer`, `self+0x240` out-param) тЖТ `0xb0fbd7c`; the `getX`-style getters at slots 26/27/30 are also
  `self+0x240`. Slot 150 is `pause_screen` (a `getScreen(name)` thunk), 151 `progress_screen`, 152
  `world_loading_progress_screen`, 153 `realms_stories_loading_progress_screen`, 154 `death_screen`, 155
  `third_party_server_screen` тАФ so the menu-screen index this build expects is **150**, and `isShowingMenuVtableIndex: 151`
  in the bundled 1.26.50 rule is off by one for 1.26.60.x. Every `pauseMenuDtorSig`/`pauseMenuOpenSig`/`hudScreenDtorSig`/
  `hudScreenOpenSig` byte pattern (and `imageLoaderSig`) matches **zero** places in 1.26.60.30, so those overlay hooks and the
  cape image-loader seam stay off and the launcher fails safe (no menu overlays, resource-pack cape) тАФ the documented
  "a byte pattern needs a fresh pattern per build" finding, now confirmed on a third build. Fixing the menu hook for a build
  means adding a rule with that build's `min` and freshly derived patterns *and* the correct `isShowingMenuVtableIndex`
  (resolve the `getScreen` thunk whose `adrp/add x1` string is `"pause_screen"`, or scan the slots for it).
- **`getScreen`-style thunks are how a slot's meaning is identified.** A thunk that calls a common `getScreen(const
  std::string&)` helper passes the screen name in `x1`; disassemble the thunk, read the `adrp x1, #page` + `add x1, x1,
  #off`, and read the C string at `page+off`. That is how 150тЖТ`pause_screen` was pinned without a symbol table.
