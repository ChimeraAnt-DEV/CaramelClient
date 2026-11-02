## Cosmetics fix roll-up (native render path тАФ do not regress these)

The list of defects below was fixed together; each has a specific invariant that must survive future
edits.

- **Version gate is a floor, not a window.** The native render hook used to install only on
  1.26.60.00тАУ1.26.61.00, so every other build drew nothing. The gate is now `MatchesConfiguredVersion
  ("1.21.132", "")` (min floor, no upper bound). Newer builds keep the attempt because safety comes
  from two runtime checks, not the version: `resolveVtableSlotReferencingString` finds the render slot
  by the diagnostic its body references, and `isExecutableAddress()` refuses a hook target that is not
  in an executable mapping. `GamePlayerRender.cpp` and `NativeRenderHook.cpp` both carry the floor.
- **GL state save/restore uses the real query enums.** The renderer used to read `GL_CURRENT_PROGRAM`
  as `0x8B31` (that is `GL_VERTEX_SHADER`), `GL_TEXTURE_BINDING_2D` as `0x0DE1` (`GL_TEXTURE_2D`) and
  `GL_VERTEX_ARRAY_BINDING` as `0x8CA6` (a framebuffer pname), so the restore ran `glUseProgram(0)`
  and bound a framebuffer id as a VAO тАФ corrupting the game's state every frame. The correct pnames are
  `kCurrentProgram 0x8B8D`, `kTextureBinding2D 0x8069`, `kVertexArrayBinding 0x85B5`,
  `kArrayBufferBinding 0x8894`, `kElementArrayBufferBinding 0x8895`, `kDepthWritemask 0x0B72`. The
  sampler is restored only when the id is still live (`RestoreSamplerBinding`).
- **Per-piece textures.** The cape, hat and pet each get their own native texture
  (`SetPieceAtlas`/`CopyPieceAtlas`, roles 0/1/2), instead of all three binding the first cape override
  тАФ a single shared atlas drew the hat and pet in the cape's colours. The render path copies the pixels
  out under the registry lock (`CopyFirstCapeOverride`/`CopyPieceAtlas`) so it never holds a pointer the
  writer can free.
- **├╖16 scale.** `NativeCosmeticRenderer` multiplies each piece's anchor by `kModelPixelToBlocks = 1/16`
  (one Bedrock model pixel is 1/16 block). Without it the meshes are 16├Ч too big.
- **Bedrock orientation.** `CosmeticOrientation` (Java) and the `bedrockToPlayer` `rotationY(180)` in
  the native renderer convert Bedrock cosmetic space (тИТZ front) to the player's +Z front, for the hat
  and pet. The cape chain bakes its own facing, so it is not converted twice.
- **Placement.** `GetCosmeticWorld()` anchors via the resolved real view-projection
  (`pl::render::CameraSource`, then `viewProjection * toPlayer`), falling back to a real first-person
  `ComputeViewProjection` from the live player pose, and only then to a plain translation тАФ never the
  old position-only anchor that put geometry off-screen.
- **Status honesty.** `nativeIsRendererDrawable()`/`nativeCosmeticDrawCount()` distinguish "the hook
  fired" from "a mesh was drawn"; `NativeCosmeticsFeed.isRendererDrawable()`/`drawCount()` surface
  them, and `CosmeticsPanel.refreshGameStatus` requires drawability before saying "active".
- **Peer lifecycle.** `CosmeticSyncRegistry.pruneOlderThan` drops peers not heard within
  `CosmeticSyncModule.PEER_TTL_MS` (30 s) on the announce timer and reports them; the
  `AdvertListener.onPeerLeft` default method lets `NativeCosmeticsRuntime` clear that peer's overrides.
- **Unequip no longer wipes everyone.** `NativeCosmeticsBridge.setCapeFor(..., null)` and
  `setTextureFor(..., null)` remove **just that key** (`nativeRemoveCapeOverride`/
  `nativeRemoveTextureOverride`), instead of `clearCapeOverrides()`/`clearTextureOverrides()` which
  erased every peer's cosmetic.
- **Authored models reach the native path.** `NativeCosmeticsBridge.publishLocal(Context, ...)`
  publishes each piece's authored `.geo.json` mesh (via `AuthoredHatModels`/`AuthoredPetModels`) and
  its authored `.png` texture, so an authored hat/pet renders as its real model in game instead of the
  procedural stand-in. Called from `NativeCosmeticsRuntime.syncEquipped`.
- **Preview fixes.** `PreviewMeshModel` folds a cube's own rotation into the shading/cull chain (tilted
  cubes no longer lose faces); `CapePreviewView` records every face into the shared `PreviewDepthBuffer`
  and clips each cosmetic/cape quad to the per-scanline visible band, so body-vs-cosmetic occlusion is
  per-pixel rather than a whole-pass over/under choice; the base skin layer is forced opaque; authored
  meshes are orientated with `CosmeticOrientation`.
- **Version compare is pure + tested.** `pl/runtime/VersionCompare.hpp` holds `CompareVersions`
  (drops a leading `1.` so `26.52` and `1.26.50.00` compare in the same space); `VersionCompareTest.cpp`
  pins it.
- **Signature rules widened to `revision 5`.** A floor rule `1.21.132`тАУ`1.26.39.99` reuses the 1.26.30
  signatures and the render slot, and the 1.26.40 rule gained `playerRenderVtableIndex`/
  `skinCapeVtableIndex`, so builds across the supported range attempt the native path (the runtime
  guards still fail closed on a slot that does not resolve).
- **Phase D research recorded, not shipped.** `docs/native-cosmetic-render-route.md` explains why the
  engine-geometry route (adding cosmetic bones to the engine's own player model) is not reachable
  from this build: game classes export no symbols, the render entry's arguments are an opaque ABI,
  and the only supported engine-geometry path is the resource pack this project moved away from. It
  is a per-build disassembler project, not a one-off change.
- **The `mce::Image` pipeline is a research seam with no launcher caller.**
  `NativeCosmeticsBridge.applyCapePng`/`registerSubstitution`/`armNextImageOverride` are real and
  fail-closed, but nothing in the launcher drives them: the render hook is the live path, and the
  image-loader signature is build-specific. They are kept (with `CapeImagePipelineTest` pinning the
  no-engine fallback) as the documented seam for a future engine-owned pixel substitution тАФ do not
  describe them as a working feature.
