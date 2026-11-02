## Preview fixes: transparency, hat shading, hat attachment, pet animation
- **Never shade a textured face by drawing a black quad over it and then cutting it with a `DST_IN`
  mask.** The `DST_IN` pass multiplies the destination alpha wherever the mask is transparent, so a
  hat/hair/jacket overlay's transparent texels punched holes through the base face already painted
  under them тАФ the character turned see-through and the sides of the head vanished at side/front
  angles. The alpha test is now **baked into the face crop** (`crop()` quantises alpha to 0/255 into
  a standalone copy) and the authored atlas is baked the same way in `buildCutout()`. Shading a
  textured face is a **`PorterDuffColorFilter(MULTIPLY)` on the bitmap** (`shadeFilter(grey)`),
  which keeps a transparent texel transparent so it never darkens what is behind it. There is no
  `maskPaint`, no `applyAlphaCutout`, no `meshMaskCache`, no `baseFaceMasks`/`overlayFaceMasks`.
- **`crop()` must return a standalone copy, never `Bitmap.createBitmap(atlas, ...)`.** The shared
  atlas must not be recycled; `release()` recycles every face crop.
- **`PreviewLighting.shadeGreyFor*` is the textured-face ramp** (a grey multiplier), distinct from
  `shadeColor*` (the flat fallback) and `overlayAlphaFor*` (only the flat fallback uses the alpha
  overlay now). `shadeGreyForOverlayLayer` is softer than the base so hair/eyes stay readable.
- **`PreviewMeshModel.Box.pivot` is the bone's world-space hinge.** `resolveCube` runs the bone's
  pivot through its ancestor chain so a renderer can swing a cube about it. The preview's
  `drawAuthoredPetMesh` (via `drawAuthoredMeshAt(..., animatePet=true)`) rotates each authored-pet
  cube about this pivot by `petBoneRotation(bone)` (the `PetPose` rotation for `head`/`tail`/
  `wing_l`/`wing_r`/`leg_a-c`), plus a whole-body `bodyBob`/`crouchDrop` offset тАФ that is what makes
  every authored pet animated instead of a static mesh. A cube on an unnamed bone keeps its authored
  pose.
- **A hat is attached to the head by rotating it about the neck pivot**, `HeadLookTransform.PIVOT_Y
  = 24`, with the same angles the head box uses. The authored hat mesh (`orig_witch` etc.) is
  authored with `pivot: [0, 32, 0]` / origins around the head, so a neck-pivot rotation is a close
  approximation that keeps the hat on the head while the look turns. `AccessoryKind.followsHead()`
  decides which kinds track the head (hats/glasses/veils/antlers yes; scarf/backpack/wings/bowtie/
  beard no).
