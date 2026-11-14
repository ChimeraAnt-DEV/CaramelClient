## One UI: the Caramel UI pack, the native ImGui menu, and FPS honesty

- **The UI pack (`Lynxifyy/CaramelClient-UI-pack`) is the single design reference**, and both
  surfaces carry its tokens: the Compose launcher through `ui.compose.theme.GlowPalette.java` +
  `GlowTheme.kt` + `GlowGlass.kt`, and the in-game menu through `sweetloader/src/pl/menu/MenuTheme.*`
  + `MenuWidgets.*` (the pack's `imgui/src/CaramelTheme` + `CaramelWidgets`, token for token). The
  pack's Compose *Desktop* component files are not imported anywhere — a duplicate
  `ui/compose/caramel/Caramel*` layer was removed as dead code. Do not re-port a component;
  `docs/agents/ui-pack-review.md` lists the traps (do not wire the GLFW backend, do not port the
  window-blur model, do not take the pack's version pins).
- **The in-game Mod Menu is the native ImGui menu, and the Compose/View mod-menu surface is gone.**
  `ui/compose/modmenu/`, `overlay/ModMenuOverlay.java`, the radial wheel (`RadialMenu*`, `Wheel*`)
  and their layouts were deleted. `PreloaderMenu` (Java) ↔ `pl/menu/MenuJni.cpp` is the only bridge;
  `InGameMenuController` builds the catalogue JSON and forwards input/events. Re-adding a Compose or
  View mod menu re-splits the product and fails `ModMenuNavLayoutTest`.
- **The FPS readout may never show a fabricated number.** `FrameRateMeter` measures the *launcher's*
  own `Choreographer` vsync cadence, not the game's present rate, and says so; the game's rate comes
  from the sweetloader render hook through `FrameTickSource`. `FpsReadout` picks between them and
  `ModStatsFormatter.fps(value, approximate)` prefixes `~` whenever the number is not the game's
  exact rate. `isFrameRateExact()` travels the backend chain so a surface cannot pass an inferred
  reading off as exact. `FpsReadoutTest` + `HudReadoutLayoutTest` pin it.
- **Frame generation and motion blur are present-frame effects, not frame-rate gains.** Both are in
  `core.mods.render.PresentFrameCatalog`/`PresentFramePolicy` and run on the engine's present /
  frame-boundary seam (`eglSwapBuffers` detour, `pl/render/PresentFrameEffect.cpp`). They blend the
  previous presented frame into the new one; frame generation makes motion read smoother at the same
  render rate and motion blur softens the image by design. Neither presents an extra frame, and both
  are badged while the present hook is absent. `PresentFrameModulesTest` (JVM) and
  `PresentFrameMathTest` (host) pin the honesty contract and the clamp.
