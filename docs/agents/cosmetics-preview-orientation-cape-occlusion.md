## Cosmetics preview orientation + cape occlusion (the "Steve is broken / capes invisible" fixes)
- **`SkinModel.Box.faceCorners` must return grid order `[TL, TR, BL, BR]`, not a perimeter winding.**
  The textured character is drawn with `Canvas.drawBitmapMesh`, which has no source rectangle: it maps
  the bitmap's corners to `verts` in `[TL, TR, BL, BR]` order, so `faceCorners` *is* the UV mapping.
  The old perimeter order (TL, TR, BR, BL) drew every face rotated 180 degrees тАФ the head's eyes and
  mouth landed on its forehead and the arm/leg hand and boot bands landed at the shoulder and hip.
  That was the reported "Steve preview is broken / mismatched textures". Each face now maps its
  texture *top* to the model's `+y` edge, so textures are upright on every side. `SkinModelTest`
  pins it (`everyVerticalFaceMapsItsTextureTopEdgeToTheUpperModelEdge`,
  `faceCornersAreInGridOrderNotPerimeterOrder`).
- **`CapePreviewView.drawQuad` walks the perimeter `TL тЖТ TR тЖТ BR тЖТ BL`, because the array is grid
  order.** Traversing `[TL, TR, BL, BR]` in order draws a bow-tie; the textured pass keeps the grid
  order for `drawBitmapMesh`, the flat pass uses the perimeter.
- **The cape's draw order is depth-aware, not "always behind the body".** The cloth hangs just behind
  the body, so when the camera is on the player's *back* the cape is the nearest surface and must
  paint *over* the body. Painting it unconditionally before the body (the old order) hid it whenever
  the camera was behind the player тАФ and the idle spin reaches that half the time тАФ so the cape read
  as barely visible. `onDraw` now projects the cloth once (`projectCape`), compares its average depth
  to the body centre, and paints the cape before or after the model accordingly. `paintCape` does the
  actual painting from that projection; do not fold them back together.
- **The preview cloth has an ambient breeze.** A still character's cape hung perfectly flat and read
  as a rigid board; `CapeSimulator.BREEZE_STRENGTH`/`BREEZE_FREQUENCY` sway the hem on X only (Y and Z
  stay at rest, so the hanging pose is preserved). Mesh is 9x11. `CapeAnimationCurve` flutter/sway
  amplitudes were raised (13/10 deg) so the in-game chain visibly folds; `CapeAnimationCurveTest`
  pins the literals.
