## Native cosmetics: Java computes the transforms, the preloader consumes them per frame
- **There is no resource pack, ever.** Resource packs cannot carry a client cosmetic on this
  platform: the engine strips custom entity animations and bone retargeting unless bound to a
  registered `animation_controller` inside a world context, and the manifest/reload/disable rules
  make it unreliable. Clients like Flarial/Blueberry process geometry, poses and textures in
  Java/Kotlin and hand the result to a native render hook тАФ that is the model here.
- **`CosmeticFrame` is the JavaтЖТnative wire contract.** `CosmeticFrame.build(...)` packs the cape
  chain's per-segment lean/sway (`CapeAnimationCurve`), the pet's per-bone rotations (`PetPose`) and
  the worn hat's head-look into a little-endian buffer (magic `CHF1`); `NativeCosmeticsRuntime
  .tickFrame(...)` computes it on the game's frame tick (`InbuiltOverlayManager.tick`) and pushes it
  via `PreloaderInput.pushCosmeticFrame`. The layout is hand-rolled and both sides must agree, so
  `CosmeticFrameTest` pins the exact bytes тАФ a wrong field order is invisible in Java and only
  surfaces when the native side misreads everything after it.
- **The native side stores the frame and the render hook reads it.** `pl::runtime::SetCosmeticFrame`
  / `ReadCosmeticFrameHeader` hold and parse it; `HookRender` (the `LivePlayerRenderer::render`
  detour) calls `ReadCosmeticFrameHeader()` each frame. JNI binding:
  `nativePushCosmeticFrame` in `GameCosmeticsJni.cpp`.
- **`PetBoneRetarget` renames bones but MUST NOT flatten the hierarchy.** It previously re-parented
  every non-root bone to the `pet` root; for a real Blockbench rig (a dragon's `bodyтЖТcola0тЖТcola1тЖТтАж`
  tail chain, nested legs) that makes every pivot absolute to the root and the mesh collapses into a
  knot of detached cubes тАФ the "cursed" pet. It now renames in place and follows a renamed parent,
  preserving the authored parent edges. `PetBoneRetargetTest.theAuthoredHierarchyIsPreservedWhenRenaming`
  pins it.
- **The pet preview animates the authored mesh per bone** by rotating each resolved cube about
  `PreviewMeshModel.Box.pivot` (the bone's world-space hinge) by the `PetPose` rotation for its
  bone. `PreviewMeshModel` resolves that pivot through the ancestor chain.
