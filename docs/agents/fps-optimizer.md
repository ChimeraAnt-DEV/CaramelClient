## FPS optimizer (org.chimeramc.client.core.minecraft.FpsOptimizer + FpsOptimizationService)
- Settings -> Basic "FPS Optimization" toggle (`FeatureSettings.isFpsOptimizerEnabled`). It is a
  **thin coordinator over existing signals**, exactly like PerformancePresetManager: it never
  touches Minecraft's own graphics or framerate, and the class docs / string say so. What it
  does is keep the *launcher* quiet while a session runs (serve cached news/update data rather
  than polling - the same path `LowLatencyNetworkManager.isGameSessionActive()` already gates)
  and shed speculative work under thermal pressure (`ThermalGovernor.severity()`), battery saver,
  or critical memory.
- `FpsOptimizer.decide(...)` is pure and unit-tested (`FpsOptimizerTest`); it returns
  NONE / QUIET_HOST / SHED_BACKGROUND. `describe()` must never contain a digit followed by "fps"
  - the launcher cannot measure the game's frame rate, and a number there would be the same
  dishonesty as promising a ping. The test pins that.
- Re-evaluated from the existing session lifecycle, not a second timer: `PlaytimeManager
  .startSession` (apply + clear low-memory) and `heartbeat()` (15s, picks up thermal/battery
  changes). `Application.onTrimMemory` feeds `setLowMemory`. `Application.onCreate` calls
  `FpsOptimizationService.init`.
- The switch re-applies immediately (`apply()`), so toggling it mid-session works without a
  restart; `syncPerformanceDependentSwitches` also re-applies after a preset change.
