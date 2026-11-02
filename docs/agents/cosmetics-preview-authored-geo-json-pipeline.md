## Cosmetics preview: authored `.geo.json` pipeline (`core.cosmetics.geometry`)
- **The preview draws authored Blockbench meshes, not silhouettes.** `resources/cosmetics/models/hats/*.geo.json` and
  `pets/*.geo.json` (plus their `.png` atlases) are packaged as assets (`build.gradle` adds `rootProject.file('resources')`
  to `sourceSets.main.assets`). `BedrockGeometryParser` тЖТ `BedrockGeometry` тЖТ `PreviewMeshModel` flattens each model's bone
  hierarchy into world-space boxes with per-face UVs, and `CapePreviewView` projects/paints them with the same
  `PreviewDepthBuffer` + 2├Ч supersampled offscreen target the character uses. No code change is needed to add a mesh тАФ drop
  the `.geo.json` in and add the id to `AuthoredHatModels.IDS` / the pet equivalent.
- **The parser is deliberately loose because real Blockbench exports are.** `format_version` is a string in 1.12 files but
  a number in 1.8 ones, a cube's `size` may be one number or three, `uv` is either an origin+size box UV or a per-face
  object, `inflate` is optional. A strict data binding would reject a valid file, so it reads the tree by hand and leaves a
  missing optional field null. It never throws: malformed input returns null and the preview falls back to the procedural
  mesh (fail-closed, so one bad asset cannot blank the preview). `BedrockGeometryPreviewTest` pins the real assets, the
  bone-parent rotation, local cube rotation, box-UV unwrap, single-number size broadcast, negative-UV mirror flag, and the
  malformed-input path.
- **A negative `uv_size` is a mirror, not an error.** `PreviewMeshModel.FaceUv.flipH`/`flipV` carry it and
  `CapePreviewView.drawMeshQuadTextured` swaps the source corners accordingly тАФ dropping the flag samples the wrong half of
  the atlas on mirrored faces.
- **The authored mesh is resolved once per selection, never per frame.** `rebuildAccessoryMesh`/`rebuildPetMesh` parse the
  asset and recycle the previous atlas; the renderer only projects (reusing `meshCorners`/`meshDrawList` scratch). Depth is
  a two-level key (`QuadDepthSorter.compare(groupDepth, depth, ...)`) тАФ the owning box's centroid first, then the face тАФ
  because a single centroid key lets two boxes' faces interleave and a hidden face paints over a nearer one.
- **Atlas decode is lazy and keyed by the mesh's texture file.** `accessoryAtlasLoaded`/`petAtlasLoaded` guard a single
  `BitmapFactory.decodeStream`; a new selection resets them and recycles the old bitmap, so switching cosmetics does not
  leak atlases.
