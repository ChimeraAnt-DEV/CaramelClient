## Cosmetics: equip crash, preview holes, flat look (cosmetics/fix-list)
- **Equipping a cosmetic crashed the game because `CosmeticSyncModule.start()` did blocking
  network I/O on the UI thread.** `start()` opened the multicast socket (`MulticastSocket`,
  `joinGroup`), the relay socket and the manual peer's DNS resolve inline; callers reach it from the
  UI thread (the Cosmetics panel toggling sync on, `InbuiltOverlayManager.startCosmeticSync`), so it
  threw `NetworkOnMainThreadException` and took the session down. `start()` is now fully async: it
  schedules `openAndStart()` on a `cosmetic-sync-start` daemon thread and returns immediately. A
  `cancelled` flag (set first in `stop()`) makes a start that is still opening sockets abort instead
  of coming up after a stop. On-demand `announce()` was already moved off the caller onto a
  `cosmetic-sync-broadcast` executor; keep both тАФ every route in `broadcast` does a blocking send.
- **The preview's triangular holes were a painter-sort bug, not a mesh bug.** Sorting every face of
  every box on a single centroid-depth key lets two boxes' faces interleave at yaws where their
  centroids tie (head on torso, arm against torso), so a hidden face paints over a nearer one.
  `QuadDepthSorter` orders by the **owning box's centroid depth first**, then the face's own depth
  within the box тАФ exact for a scene of disjoint convex boxes, which the player model is.
  `CapePreviewView.drawModel` projects the box centroid per box to get the group key; the overlay
  layer keeps its +0.5 nudge on both keys. `QuadDepthSorterTest` pins the ordering.
- **Flat-color textures read as plastic blobs; `PaintedAtlas` replaces the flat fill.** The old
  `FlatColorAtlas` was one solid colour per region, so no amount of geometry gave a pet or hat form
  or material. `PaintedAtlas` keeps the same two-region UV layout but adds a vertical light
  falloff (top brighter than bottom) and a deterministic surface grain, seeded per item so species
  do not share a grain. `AccessoryTexturePainter` and the pack builder's pet texture route through
  it. It is still procedural тАФ reaching hand-painted detail needs authored assets тАФ but it is a real
  step up. `PaintedAtlasTest` pins the shading and texture.
- **New accessory archetypes are added in both renderers or they drift.** `TOPHAT`, `WIZARD_HAT`,
  `TIARA` and `BEARD` are in `CosmeticCatalog.AccessoryKind` (so each is generated across all 12
  palettes), with geometry in `AccessoryGeometry.geometryJson` and matching boxes in
  `CapePreviewView.drawAccessoryFront`. A kind added to only one side renders in-game but not in the
  preview (or vice versa). `AccessoryGeometryTest.DRAWABLE` must list every drawing kind.
- **The halo was an octagon; it is now a dense ring.** 8 slats read as a faceted polygon. It is
  `HALO_SEGMENTS = 24` overlapping slats (`AccessoryGeometryTest.theHaloIsADenseRingNotAFacetedPolygon`
  pins the count and that neighbouring slats overlap).
