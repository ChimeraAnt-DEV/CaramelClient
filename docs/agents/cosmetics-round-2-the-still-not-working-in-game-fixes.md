## Cosmetics round 2 тАФ the "still not working in game" fixes (do not regress these)

These were the defects behind "cosmetics render in the preview but vanish on the live player". Each
has a specific invariant.

- **The post-render callbacks must run on EVERY render call, not once per "frame".**
  `GamePlayerRender::HookRender` used to gate the callback dispatch behind `now - last > 1000ms`
  ("first call of the frame"). During play the renderer runs every ~16ms, so that test was true only
  for the first frame after a second-long stall тАФ the cosmetics draw callback fired once at startup
  and then **never again**, which is exactly why nothing appeared in game while the preview worked.
  The dispatch now runs on every call; consumers dedupe per published frame version themselves.
- **Native mesh UVs are normalised to 0..1.** `GeometryMeshBuilder::build` emitted UVs in atlas
  *pixels* but the shader samples `texture2D(uTexture, vUv)` (0..1), so every sample came from the
  top-left texel тАФ a flat smear. The builder now divides by the model's `texture_width`/
  `texture_height`. `BedrockModelParserTest` asserts UVs stay in 0..1.
- **Shader attribute locations are bound before linking.** The vertex format is set up at fixed slots
  0/1/2, but GLSL ES does not guarantee `aPosition`/`aUv`/`aNormal` land there; `EnsureProgram` now
  calls `glBindAttribLocation` for each. Without it the driver could feed position data to the UV
  attribute.
- **Per-vertex lighting.** The mesh builder already resolved a per-face normal; the renderer threw it
  away and drew every face at full brightness. The vertex format now carries the normal (stride 32)
  and the shader applies a fixed headlight (`uLightDir`), so a face is shaded by how it faces the
  viewer тАФ the fix for the "flat, unshaded" look.
- **`bind_pose_rotation` is read.** A bone stating only `bind_pose_rotation` (no `rotation`) was left
  unrotated, dropping the cape chain's 180┬░ facing. The parser falls back to it.
- **Authored pets are rescaled to the body-plan height.** An authored Blockbench pet is in its own
  model units (a dragon is ~23 tall, ~48 wide) while the preview/native anchors are on the ~14-unit
  model-pixel scale, so an un-normalised mesh rendered several times too large and off-screen тАФ the
  "some pets don't appear" symptom. `PreviewMeshModel.normalizeToHeight` (Java) and `ScaleForHeight`
  (native) rescale and ground the mesh. `PreviewMeshModelScaleTest` pins it. Authored *hats* are
  already in player model space (yтЙИ31..40) and must NOT be rescaled.
- **Geometry is parsed once per blob, not per frame.** `NativeCosmeticRenderer` caches the parsed
  cape/hat/pet meshes keyed by the blob's FNV-1a hash, so a steady frame does no parsing.
- **VFX aura.** `CosmeticParticles` is a pure fixed-pool particle system (host-tested); the renderer
  emits and draws an additive billboard aura when `CosmeticFrame.FLAG_VFX` (bit 3) is set. Opt-in via
  `InbuiltModManager.isCosmeticVfxEnabled()` (default off), toggled in `CosmeticsPanel`.
- **The geometry-blob guard is `flags & 0xF`.** Bit 3 is the VFX flag, so a VFX-only frame must not be
  rejected by the old `flags & 0x7` check (renderer and `NativeRenderHook`).
