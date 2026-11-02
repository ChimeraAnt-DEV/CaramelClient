## Mod Menu live stats strip (ModStatsFormatter)
- **FPS / ping / battery in the top bar, refreshed on a one-second tick only while the menu is
  open** (`ModMenuOverlay.statsTick`, registered in `showInternal`, removed in `hide`). FPS reads
  the same inbuilt `FpsMod` the FPS overlay uses so the two cannot disagree; battery reads the
  sticky `ACTION_BATTERY_CHANGED` broadcast.
- **An unknown reading renders an em dash, never `0`.** A "0 FPS" or "0 ms" reads as a real
  measurement. `ModStatsFormatter` owns that rule and is pure, so `ModStatsFormatterTest` pins it.
  Ping has no source in this build and correctly shows the em dash.
