## Native cosmetics seams тАФ verified offsets (1.26.60.28 arm64-v8a)
Recovered from the real `lib/arm64-v8a/libminecraftpe.so` (368,150,880 bytes) extracted from
`minecraft-26-60-28.apk` (mcpedl.org, `file_id` 7572 -> `https://file.mcpedl.org/uploads_files/22-09-2026/minecraft-26-60-28.apk`).
Method: standalone typeinfo-name strings in `.rodata`, vtables resolved through the
`R_AARCH64_RELATIVE` entries in `.rela.dyn`, function identified by the diagnostic strings its body
references (the same method `Vtable.cpp` uses). These are pinned in
`resources/sweetloader/sweetloader_signature_rules_source.json`.

- **`Skin` is not polymorphic.** The binary has **no `Skin` typeinfo name**, so there is no
  `Skin::getCapeImage` vtable to hook. The reachable 2D-cape seam is the renderer that holds the
  skin: **`18LivePlayerRenderer`**, vtable address point `0x15144a78`, **slot 17 -> `0xaeaefb4`**
  (the same render entry the player-render feed already uses; `playerRenderVtableIndex` = 17). The
  `SerializedSkinRef::getCapeImageData` / `getCapeImageDataCereal` methods exist but are plain
  (non-virtual) methods on a non-polymorphic type, so they cannot be reached by slot.
- **Player-join packet seam: `20ClientNetworkHandler`**, vtable address point `0x1520ef98`,
  **slot 40 -> `0xb80f518`**, whose body reaches the `AddPlayerPacket` handler at `0xb82dbcc`
  (identified by `"AddPlayerPacket: NaN position sent by server"`). The `PlayerListPacketPayload::
  AddEntry` / `RemoveEntry` handlers sit at `0xb8007d0` / `0xb8007ec` and peers but their exact
  slot mapping is unconfirmed, so no index is set from them.
- `GamePlayerRender` (existing), `pl::cosmetics::NativeRenderHook` (Task 2) and `GameCosmetics`
  (new) all target the `LivePlayerRenderer` slot; the cosmetics module owns the registry (per-player
  cape pixels, per-texture-id pixels, the render-geometry blob + hash) and the JNI surface. All
  detours are **pure passthroughs** тАФ they forward the full argument register set and dereference
  nothing, so an unverified slot cannot fault the render thread; the texture-bind seam has no
  verified slot and stays fail-closed.
  - **The Task 2 interception is installed from `GameHooks.cpp`, beside `InitPlayerRenderSource`
    and on the same slot.** `pl::memory::hook` *chains* onto an already-hooked target, so the two
    detours both run (the render feed records frame ticks, `NativeRenderHook` owns the bone-matrix
    anchor) without either trampoline stomping the other. Installing the cosmetics hook alone
    (which the first cut did тАФ it was built but never called, so the interception was dead) would
    have replaced the render feed's detour instead of sitting alongside it. If a third consumer of
    the slot is added, add it the same way; do not "own" the slot with a second `GlossHook`.
- **Still not implemented:** the pixel substitution itself (writing the override into the cape
  buffer / binding a swapped texture). The seams, registry and JNI are real and build; substituting
  needs each function's argument layout, which the stripped binary does not expose. The game-hook
  section's conclusion stands: game classes export no symbols, so this is a disassembler pass, not a
  symbol lookup.

### SerializedSkinRef layout (recovered from 1.26.60.28) тАФ the cape substitution map
Each `SerializedSkinRef::getX()` accessor logs its own name and returns `this + offset`; the
constant-folded `add x0, x19, #imm` before the `ret` is the field offset. Recovered:

- `getImageData()` тЖТ `+0x78` (base skin, `mce::Image`)
- `getCapeImageData()` тЖТ `+0xa8` (**the cape**, `mce::Image`)
- `getAnimatedImageData()` тЖТ `+0xd8`; `getGeometryData()` `+0x100`; `getGeometryDataMutable()`
  `+0x120`; `getAnimationData()` `+0x130`; `getCapeId()` `+0x148`; `getSkinColor()` `+0x1a8`;
  `getIsTrustedSkinFlag()` `+0x1b8`; `getProfileHash()` `+0x1c0`.
- `+0x78 тЖТ +0xa8` is exactly 0x30 with nothing between, so **`mce::Image` is 0x30 bytes** and the
  cape is one struct at `ref + 0xa8`.

`SwapCapeImage(ref, image)` copies a caller-supplied **0x30-byte image struct** over `ref + 0xa8`
(null ref/image/short buffer refused; exactly 0x30 bytes written). It copies the whole struct, not a
pointer inside it, because **`mce::Image`'s internal field layout is not recoverable** from the
stripped binary. Feeding it a valid image therefore needs an `mce::Image` the *engine* built (its
own loader); the launcher cannot synthesise one whose internal buffer pointer is correct. That is
the remaining piece тАФ a data problem, not an offset problem. The render seam itself is
`LivePlayerRenderer` slot 17 (see above).

### Engine image loader тАФ the cape's PNG тЖТ `mce::Image` path
`mce::ImageUtils::loadImageFromMemory(mce::Image &out, ImageFormat, const unsigned char *, size_t, bool)`
returns `brstd::expected<void, error_condition>` via the hidden sret pointer in `x8`. The class
exports no symbol, so it is resolved by a per-version byte signature (`imageLoaderSig` in the rules);
the pinned pattern matches **exactly once** at `0x14df7ce4` in 1.26.60.28. `BuildCapeImageFromPng`
calls it with custom PNG bytes and gets back a fully **engine-constructed** `0x30`-byte `mce::Image`
тАФ which is what makes `SwapCapeImage` usable, because the engine fills the struct's internal buffer
pointer rather than the launcher guessing at it. `NativeCosmeticsBridge.applyCapePng` is the whole
path: PNG тЖТ engine image тЖТ swap at `SerializedSkinRef + 0xa8`. Fail-closed everywhere: no loader, a
bad PNG, or a null skin address leaves the vanilla cape alone, and an unmatched signature keeps the
resource pack.

- **The loader call was previously declared with the WRONG ABI, and that was the crash/black-texture
  cause. Fixed in `mce_image_hook.cpp` (sweetloader).** The real convention, read from the disassembly
  at `0x14df7ce4`, is: the return aggregate goes through **`x8`** (`mov x19, x8` in the prologue,
  payload byte written at `[x19, #0x10]`), and the arguments are **`x0 = out image`, `x1 =
  ImageFormat`, `x2 = data`, `x3 = size`, `x4 = bool`**. The old `using LoadImageFn =
  void(*)(void* sret, void* out, u32 format, const u8* data, size_t size, bool)` mapped `x0 = sret,
  x1 = out, x2 = format, ...` and never supplied `x8` at all тАФ so it passed the out-struct where the
  format belongs and returned the wrong way. The fix is a *type* fix: declare the return type as a
  struct >16 bytes (`LoaderReturn`) and Clang emits the `x8` sret ABI itself (verified in the built
  object: `add x8, sp, #0x8` before the `blr`, flag read from `sret+0x10`). Do not reintroduce a
  hand-written `void(...)` signature.
- **`ImageFormat` values the loader accepts are `{0, 1, 3, 4}`** (from the dispatch in the same
  routine). `0` is auto-detect тЖТ `stb_image`, which decodes PNG (the cape/hat texture route); `4` is
  RGBA8 (a memcpy). `renoir::ThirdParty::stbi_*` is present in the binary, confirming the PNG path.
- **The detour is gated on a live probe, not a version list.** `InitMceImageHook` builds a real 1├Ч1
  PNG through the loader first; success proves both the address and the ABI, and only then is the
  `loadImageFromMemory` detour installed (it forwards the same `x8` return pointer, so it is
  transparent). A failed probe refuses the detour and keeps the already-proven `SerializedSkinRef`
  struct-swap at `+0xa8`, so the feature does not regress. Substitutions are keyed by the FNV-1a of
  the incoming bytes (`SetContentSubstitution`) plus a one-shot `ArmNextImageOverride`, because the
  loader seam carries no player/texture id. The texture-cache flush is a caller-proven callback
  (`SetTextureCacheFlusher`), not fabricated тАФ `mce::TextureGroup`/`SkinRepository` are not
  RTTI-resolvable. See `sweetloader/docs/mce-image-pipeline.md`.

- **The `SerializedSkinRef` accessor-name strings do NOT survive 1.26.60.30.** Only `getImageData`
  (1 hit) and `loadImageFromMemory` (1 hit) are still present in `.rodata`; `getCapeImageData`,
  `getCapeImageDataCereal`, `getAnimatedImageData`, `getGeometryData`, `getAnimationData`,
  `getCapeId`, `getSkinColor`, `getIsTrustedSkinFlag`, `getProfileHash` and `SerializedSkinRef` are
  all **0 hits**. The `+0xa8` cape offset was recovered from those accessors on 1.26.60.28, so on
  1.26.60.30 it is unconfirmed and `SwapCapeImage` must stay fail-closed (it already is: a null/
  mismatched skin address leaves the vanilla cape alone). Re-deriving the offset on a new build means
  the accessor-name route is gone тАФ it needs a different anchor.


### Preview occlusion and quality
- **The cape is clipped against the body with a real depth buffer** (`PreviewDepthBuffer`), not a
  whole-cape before/after choice. The cape hangs just outside the back plane while the body spans a
  volume in front of it, so no whole-object order is correct at most camera angles тАФ part of the
  cloth is always nearer than the skin and part farther, which is why the cape "showed through the
  skin". The body's faces are recorded per pixel and each cape quad is drawn only where it is at
  least as near as the skin. **Smaller depth is nearer** (the camera projects `distance - z2`;
  `SkinModelTest` pins it), so the buffer keeps the *minimum* depth. `PreviewDepthBufferTest`
  pins the rule тАФ including that an empty pixel never occludes and that a nearer surface wins.
- The frame is drawn at **2x** into an offscreen bitmap and scaled down, so cloth edges and the
  silhouette are anti-aliased instead of jagged (the "low quality preview" complaint).
- **The cape size is correct, and was never actually thin.** Measured, the cloth projects to
  roughly 12.2 x 17.1 model px versus vanilla's 10 x 16 тАФ slightly *larger* than vanilla. The
  "thin" appearance was the occlusion bug hiding most of the cloth plus edge-on viewing angles, not
  a geometry error. `CapeSimulator` is 9x11 over a 0.625 x 1.0 block quad; `CapeGeometry` is already
  a 16-segment chain.

### Java Edition renderer code does not apply
This is a **Bedrock Edition** launcher (`namespace`/`applicationId` `org.chimeramc.client`, no
`libminecraftpe` Java API). The Java Edition `LayerRenderer`/`PlayerRenderer`/`NativeImage`/
`DynamicTexture` classes (`net.minecraft.client.*`, `com.mojang.blaze3d.*`) do not exist here and
cannot compile or run. `MinecraftActivity` extends the game's `com.mojang.minecraftpe.MainActivity`.
Cosmetics reach Bedrock through the generated resource pack plus the native seams documented above,
not through an entity-layer renderer.
