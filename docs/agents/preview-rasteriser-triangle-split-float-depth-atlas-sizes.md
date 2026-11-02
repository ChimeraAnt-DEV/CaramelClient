## Preview rasteriser: triangle split, float depth, atlas sizes (do not regress)

Three defects were fixed together; each has an invariant.

- **The 4-corner face split is `(0,1,3)+(0,3,2)`, not `(0,1,2)+(0,2,3)`.** `SkinModel.Box.faceCorners`
  and every other face builder return row-major `[TL,TR,BL,BR]`. Splitting on the TL-BR diagonal is
  therefore `(TL,TR,BR)` and `(TL,BR,BL)`. The shipped split used the two *left* triangles, so the
  right-hand triangle was never painted and every face lost a ~22-25% notch. The comment always said
  the right split; the indices now match it. `PreviewSceneCoverageTest` (solid-quad full coverage,
  shared-diagonal gap, no see-through pixel inside every shipped hat/pet face at 8 yaws) is the gate.
  The golden PNGs were regenerated because the old ones were produced by the buggy renderer.
- **The depth buffer is `float[]`, empty = `+Infinity`, test `z < stored - 1e-4`.** It was `int[]`
  holding `floor(z)` in model pixels (~46±18), so the overlay (jacket/hat) layer biased by 0.5 landed
  in the same integer bucket as its base face about half the time and showed in stripes. Any two
  surfaces closer than one model pixel (hat band vs head) had the same problem.
- **Sample with the decoded bitmap's real size; normalise UVs against the geometry's declared
  `texture_width/height`.** `CapePreviewView` used to build the authored scene with the constants
  `AccessoryGeometry`/`PetGeometry.TEXTURE_WIDTH/HEIGHT` (64x64) while the shipped PNGs are 32x32,
  64x32, 64x48, 128x128, 512x256, ... — a 32x32 hat indexed `31*64+0 = 1984` into a 1024-int array
  and crashed the app. `PreviewAuthoredScene.addAccessory/addPet` now take both sizes; `faceUv`
  clamps against the geometry size. Defence in depth: `PreviewScene.addQuad/addBoxFace` skip (and
  record via `PreviewSceneDiagnostics`) a face whose atlas is smaller than its declared size,
  `PreviewRaster.triangle` returns early on an undersized atlas, and `CapePreviewView.onDraw` wraps
  the render in `try/catch` and paints a "preview error" notice. The diagnostics report lists per
  cosmetic id the PNG size, geometry size and cube count (`CosmeticAssetAudit`).
