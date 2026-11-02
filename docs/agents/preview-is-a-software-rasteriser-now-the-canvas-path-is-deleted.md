## Preview is a software rasteriser now (Phase D) — the Canvas path is deleted

- **The preview no longer draws through `Canvas`.** `CapePreviewView.onDraw` renders the whole scene
  into an `int[]` ARGB buffer with `PreviewRaster` and blits the buffer to a `Bitmap`. There is no
  `Canvas.drawBitmapMesh`, no `drawBitmapMesh` UV mapping, no `QuadDepthSorter` painter's sort, no
  `PreviewDepthBuffer` whole-scene pass, no MULTIPLY shade-filter cache and no poly-to-poly
  authored-mesh path — all of that was deleted in `preview-s5` once its parity tests passed. Do not
  reintroduce a `Canvas` draw path for the preview.
- **`PreviewRaster`** is the pure rasteriser: a per-pixel z-buffer (smaller depth is nearer, so draw
  order never matters), nearest-neighbour sampling (never bilinear, so a transparent texel cannot
  bleed), alpha cutout at `SkinAlphaFilter.ALPHA_THRESHOLD`, and deferred blending (an opaque texel
  writes over, a translucent one blends). The base-skin path is forced opaque; the overlay/cosmetic
  path alpha-tests. `PreviewRasterTest` pins every rule.
- **`PreviewScene`** holds textured and flat-coloured quads and renders them through one camera
  `Fit`. `Face` carries a per-face `depthBias` so a coplanar overlay layer wins the depth test, and
  a `doubleSided` flag. Because the depth buffer resolves everything, the scene draws in one pass.
- **The scene builders** are pure and each has its own test: `PreviewCharacterScene` (the six player
  boxes, base + overlay, head look), `PreviewPetScene` (procedural pet from `PetModel`, flat-shaded,
  culled by the unrotated normal and shaded by the rotated normal), `PreviewCapeScene` (the
  `CapeSimulator` cloth as flat quads, colour from `CapePatterns`), `PreviewAccessoryScene` (every
  procedural `AccessoryKind`), `PreviewAuthoredScene` (authored Blockbench meshes, per-face UVs with
  the negative-`uv_size` mirror, normals converted by `CosmeticOrientation`). `PreviewRotations`
  shares the Z-then-Y-then-X rotate/pivot and the pet bone vocabulary with the geometry builders.
- **The camera fits the whole scene.** `PreviewScene.fit` projects every corner once, measures the
  screen bounds and derives a scale + centring origin, so the legs and a ground-level pet are never
  clipped (the old preview clipped them). `CapePreviewView.fitScene` unions the sub-scenes' bounds
  and renders them all through the one fit.
- **Golden-image tests** (`PreviewSceneGoldenTest`) render 7 scenes × 8 yaw angles into 56 golden
  PNGs under `app/src/test/resources/golden-preview/`; the first run writes the baseline, later runs
  compare with a per-pixel tolerance and write a `*.diff.png` on failure. It also asserts no face
  vanishes across a full turn and the fit keeps every corner on screen.
- **Every preview stage is unverified on device.** The pure pipeline is verified by the tests and the
  goldens; whether the wiring renders as expected in the app can only be confirmed on a device.
- **Resource packs are not used for cosmetics.** There is no pack builder and no pack route: a
  cosmetic reaches the game only through the native player-render hook, and when that is unavailable
  cosmetics show in the preview only. `NativeCosmeticsFeed.Mode` is `NATIVE_RENDER` / `UNAVAILABLE`
  (the old `PACK_FALLBACK` and its "use the resource-pack route" wording are gone). Skin packs
  (`SkinPackActivator` etc.) are a **separate** feature and still exist.
