## Cosmetics preview alpha cutout + head-look (Phase 2)
- **REMOVED.** The Compose/View mod menu, the radial wheel and their layouts were deleted; the
  in-game menu is the native Dear ImGui menu now (`sweetloader/src/pl/menu/`, bridged by
  `PreloaderMenu` + `InGameMenuController`). The class names below no longer exist; this
  section is kept as history.
- **`drawBitmapMesh` bilinear-filters and has no source rect, so a transparent atlas edge bleeds into
  every opaque texel.** That is why the character read as a hollow, see-through sheet. The fix is
  alpha testing, realised as a hard-edged cutout mask applied with `PorterDuff.DST_IN`:
  `SkinAlphaFilter` owns the rule (`ALPHA_THRESHOLD = 25`, `hasAnyOpaque`, `isFullyOpaque`,
  `buildMask`) and is Android-free through its `PixelSource`, so `SkinAlphaFilterTest` covers it with
  no device. `CapePreviewView.maskFor` builds a per-face mask only when the crop has a hole (a fully
  opaque base layer needs none), and the shading tint must be applied **before** the cutout тАФ tinting
  after re-fills the discarded pixels and the hole returns. The cutout is wired into both the quad
  pass and the authored-mesh pass. The per-face masks are cached in `baseFaceMasks`/`overlayFaceMasks`
  and rebuilt with the skin, and the authored-mesh mask cache is cleared whenever an accessory/pet
  mesh is rebuilt, or a new hat would keep the previous atlas's masks.
- **A hat must turn with the head in the preview, exactly as the in-game `acc` bone does.**
  `HeadLookTransform` (pure, Android-free, `HeadLookTransformTest`) rotates a point/direction by head
  pitch (X) then yaw (Y) about the neck pivot `(0, 24, 0)` тАФ the same pivot the `acc` bone uses and
  the same queries (`target_x_rotation`/`target_y_rotation`) the in-game `animation.chimera_hat_tilt`
  reads. `CosmeticCatalog.AccessoryKind.followsHead()` is the single rule for which accessories track
  the head (TOPHAT/CROWN/GLASSES/VEIL/ANTLERS yes; SCARF/BOWTIE/BACKPACK/WINGS/BEARD no) and is
  pinned by `CosmeticCatalogTest`. The preview applies it to the head box corners and to a head-worn
  accessory's authored mesh, so the two cannot disagree.
- **Dropdowns must be dismissed when the menu closes or a session starts.** `Spinner` opens its
  popup in a separate window that outlives the host view, so a `CosmeticsPanel` left open over the
  game kept a stale popup floating. `CosmeticsPanel.DismissibleSpinner` (a subclass, because
  `Spinner.onDetachedFromWindow()` is protected) records itself in `dropdowns`; `dismissDropdowns()`
  and `onHidden()` close them, and `ModMenuOverlay.hide()`/`hideCosmetics()` call both plus
  `CapePreviewView.stopPreview()` so the idle animation stops when the panel is not visible.
