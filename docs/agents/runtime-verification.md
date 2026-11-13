## Runtime verification (no KVM/emulator in dev)
- No Android emulator/KVM here тАФ verify via `./gradlew :app:compileDebugJavaWithJavac` (fast), `:app:compileDebugKotlin`, and a full `nohup ./gradlew :app:assembleDebug > /tmp/build_apk.log 2>&1 &` then grep for `BUILD SUCCESSFUL`/`FAILED`. To test on a device later (adb, ARM64 Android), install `app/build/outputs/apk/debug/app-debug.apk`, and:
  - Playtime: launch a version, wait ~20s, close; repeat тАФ SharedPreferences `playtime_tracker` (total_ms_&lt;profileId&gt;) should accumulate тЙИ real elapsed wall time (┬▒ a heartbeat interval).
  - Reduced-motion: enable Settings тЖТ Personalize тЖТ "show animations" OFF, confirm hero-card pulse stops (no translationZ/alpha oscillation) and press feedback is instant/no spring.
  - Glow: toggle "enable glow" OFF, confirm card elevation returns to 0 and gradient strokes disappear.
  - Latency: with the toggle ON, use `tcpdump`/`strace` or bpf to confirm TCP_NODELAY on launcher sockets, no DNS re-resolve during an active session, and that NewsRepository/GithubReleaseUpdater skip the network while a game is running.
  - Controller response: `./gradlew :app:testDebugUnitTest` covers the math. On device, with Low Input Delay ON the stick should reach full look output with less physical travel, and a resting drifting stick must still read as centred (no slow camera creep).
  - Font scale: set display font size to largest; no button label may be clipped in any activity (fixed `layout_height` on a text view is the cause).
  - Touch feedback: in ModsFullscreen, drag-to-reorder via the handle must still work тАФ `applyPressScale` replacing that handle's `OnTouchListener` is the regression to watch for. Scrolling a list must not leave rows stuck at the pressed scale.
