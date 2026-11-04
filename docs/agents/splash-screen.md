## Splash screen (ui.activities.SplashActivity + drawable/ic_chimera_ant)
- The splash mark is the **vector** `ic_chimera_ant`, tinted to the user's accent in `applySplashTheme` (`setImageTintList`). Orbit ring, orbit dot and the halo are `GradientDrawable`s rebuilt per accent, with the static `bg_orbit_ring`/`bg_orbit_dot`/`bg_logo_glow` drawables only as the pre-tint fallback тАФ those fallbacks carry brand tokens now, not the old green/cyan.
- **The backdrop is a live voxel world, not a flat gradient.** `SplashSceneView` (package `org.chimeramc.client.launcher.ui.splash`) paints a deep sky gradient, twinkling parallax stars with the occasional shooting star, two blocky parallax terrain ridges with lit caps, floating shaded voxel cubes (rotate тЖТ perspective divide тЖТ back-face cull тЖТ far-to-near sort, so they read as solid Minecraft blocks), and rising embers. All motion lives in the pure, unit-tested `SplashWorld`/`SplashParticles`; the view only projects and paints. One `ValueAnimator` drives every frame and self-cancels on detach. Paints, the background `LinearGradient` and the terrain `Path` are cached and only rebuilt on size/palette change тАФ never rebuild a gradient or path per frame.
- `setPalette(accent, dark)` and `setAnimationsEnabled(animate)` are set from `applySplashTheme`/`startSplashSequence`; under reduced motion the scene paints one static frame instead of going blank.
- The sequence also runs a slow **halo breathe** (`startLogoBreathe`, a 2.4s reverse scale/alpha ValueAnimator with a start delay so it does not fight the entrance) so the lockup never looks frozen on a slow warm-up.
- **The floating voxel island is the backdrop's hero.** `SplashIsland` is a pure, seeded block model
  (grass disc, tapered dirt/stone underside, a tree, drifting rocks, and a small satellite islet in
  its own group) drawn by `SplashSceneView` as a depth-sorted turntable of flat-shaded cubes on top
  of the star/terrain/cube/ember scene. Blocks are ordered far-to-near by projected depth (a nearer
  block paints over one behind it). **Each cube is drawn turned to the island's yaw** (`blockYaw`),
  not left axis-aligned: a cube whose faces stay square to the screen never shows the face shading,
  which is exactly what made the model read as "low quality boxes". Turning every cube with the
  model makes the whole thing read as one solid rotating rock. The satellite islet (`group == 1`) is
  lifted and spun by `SplashSceneView` on its own faster, counter-turning orbit
  (`SplashIsland.satelliteYaw`). `SplashIslandTest` pins the silhouette, the shared turntable
  rotation and the satellite islet; `SplashIsland.MAX_BLOCKS` sizes the view's scratch arrays, so
  the model must never exceed it.

- The sequence already animates: glow scale-in, mark scale-in, orbiting dot (`startOrbitAnimation`, 3s loop), halo pulse (`startGlowPulse`), progress tween тЖТ `navigateToMain`. Keep new motion on these ValueAnimators rather than swapping in a static PNG.
- All three tints (`orbitRing`, `orbitDot`, `logoGlow`) are accent-derived; if a colour looks off it is the accent resolver, not the drawable.
- **The ore loader has no depth buffer, so its geometry is pinned in a pure class.** `OreCrackSprites` draws a faceted gem block and an iron pickaxe as 16x16 char grids; `OreLoaderLayout` owns the two-cell placement and the "pickaxe sits outside the block" invariant. Anchoring the pickaxe at the block's centre (the original code) laid the sprite over the gem, which is the bug the redesign fixed. Draw offsets must stay inside the parentheses exactly as the controller illustration's do. `SplashLoaderTest` pins the grid size, the pickaxe's head/lit-edge/handle, the gem's lit and shaded bevelled cells, and that the two sprites stay inside the view across widths.
