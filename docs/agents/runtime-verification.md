## Runtime verification (no KVM/emulator in dev)
- No Android emulator/KVM here — verify via `./gradlew :app:compileDebugJavaWithJavac` (fast), `:app:compileDebugKotlin`, and a full `nohup ./gradlew :app:assembleDebug > /tmp/build_apk.log 2>&1 &` then grep for `BUILD SUCCESSFUL`/`FAILED`.

### There is a real phone now: Mobilebox (Android 14, arm64)
A cloud phone is reachable from this container, so "no device" no longer means "cannot look". It costs
$0.01 a minute while the phone runs and nothing while it is parked, so park it the moment you stop.

```bash
export MOBILESANDBOX_API_KEY=...            # the workspace key; already claimed
curl -sS https://mobilebox.dev/v1/account -H "Authorization: Bearer $MOBILESANDBOX_API_KEY"
curl -sS -X POST 'https://mobilebox.dev/v1/phones?wait=ready' -H "Authorization: Bearer $MOBILESANDBOX_API_KEY" \
  -H 'content-type: application/json' -d '{"name":"caramel-test"}'          # -> .id
curl -sS -X POST "https://mobilebox.dev/v1/phones/$PH/apps" -H "Authorization: Bearer $MOBILESANDBOX_API_KEY" \
  -H 'content-type: application/octet-stream' --data-binary @app/build/outputs/apk/debug/app-debug.apk
curl -sS -X POST "https://mobilebox.dev/v1/phones/$PH/actions" -H "Authorization: Bearer $MOBILESANDBOX_API_KEY" \
  -H 'content-type: application/json' -d '{"actions":[{"type":"tap","target":{"x":111,"y":900}},{"type":"wait","ms":3000}]}'
curl -sS "https://mobilebox.dev/v1/phones/$PH/screenshot" -H "Authorization: Bearer $MOBILESANDBOX_API_KEY" -o shot.png
curl -sS -X POST "https://mobilebox.dev/v1/phones/$PH/park" -H "Authorization: Bearer $MOBILESANDBOX_API_KEY"
```

Traps that cost time the first run:
- **The APK needs the Rust engine.** `:app:assembleDebug` fails with *"cargo was not found"* until
  `rustup` plus `rustup target add aarch64-linux-android` is installed; the NDK (28.2) is already in
  `/workspace/android-sdk`. A clean build of the arm64 APK takes about three minutes.
- **`open_app` does not launch this app** (the phone answers "No apps can perform this action", or
  the launcher drawer), although it launches `com.android.settings` fine. Use: `key home`, swipe up
  from `(540,1900)` to `(540,600)`, then `tap` the text `"CaramelClient"`. The icon is in the drawer
  and a plain tap works.
- The launcher is landscape: the observe coordinates are `x∈[0,2400], y∈[0,1080]`. The left rail's
  six destinations are at `x=111`, `y` = 211 Launchpad, 353 Mods, 495 Servers, 637 Cosmetics,
  779 News, 900 CurseForge, 1006 Settings.
- `GET /v1/phones/{id}/apps/{package}/logs` returns the app's own logcat (last 200 lines).

### Verified on the cloud phone, 2026-11-28 (BETA9 debug build)
- Cold start reaches Launchpad with no crash; dark and light themes both render (light cards are
  tinted glass, not white boxes).
- Friends card shows the signed-out state; Mods says "No mods installed"; Servers says "No servers
  saved" — the honest empty states, not blanks.
- News tab renders the bundled BETA9 post with its bullets plus the "More modules on the way" and
  "Christmas cosmetics shop" posts, banners included.
- CurseForge tab lists **30 results before any search** with section chips (All, Add-ons, Texture
  packs, Scripts, Worlds, Skins) and real project thumbnails and download buttons.
- Cosmetics tab shows the category chips, the "Not signed in / Sign in to load your Minecraft skin"
  state and the owned catalogue entries.
- Startup logcat: `DeferredRenderProbe` threw `NoSuchMethodException:
  PackageManager.getSystemFeatureVersion` once per probe (three stack traces per launch) on Android
  14; it is cached and logged once per process now. `BerryApi.nativeCosmeticAvailable()` throws
  `UnsatisfiedLinkError` with no Minecraft installed, which is the expected fail-closed path.

### Still needs a phone with the game signed in
Anything past the launcher's own screens: the XAL export the game reads, the in-game "Signing in
with your Microsoft account…" text, the real skin in game and every engine hook. A Microsoft sign-in
on the cloud phone needs the account owner's password and 2FA, so the run stops at "Sign In". The
launcher-side XAL file set is covered by JVM tests; the game-side behaviour needs the adb command in
`docs/agents/nethernet-26-60-signin-loop.md` on a 26.60 phone.

- Older device checklist (still valid once you have a phone):
  - Playtime: launch a version, wait ~20s, close; repeat — SharedPreferences `playtime_tracker` (total_ms_&lt;profileId&gt;) should accumulate ≈ real elapsed wall time (± a heartbeat interval).
  - Reduced-motion: enable Settings → Personalize → "show animations" OFF, confirm hero-card pulse stops (no translationZ/alpha oscillation) and press feedback is instant/no spring.
  - Glow: toggle "enable glow" OFF, confirm card elevation returns to 0 and gradient strokes disappear.
  - Latency: with the toggle ON, use `tcpdump`/`strace` or bpf to confirm TCP_NODELAY on launcher sockets, no DNS re-resolve during an active session, and that NewsRepository/GithubReleaseUpdater skip the network while a game is running.
  - Controller response: `./gradlew :app:testDebugUnitTest` covers the math. On device, with Low Input Delay ON the stick should reach full look output with less physical travel, and a resting drifting stick must still read as centred (no slow camera creep).
  - Font scale: set display font size to largest; no button label may be clipped in any activity (fixed `layout_height` on a text view is the cause).
  - Touch feedback: in ModsFullscreen, drag-to-reorder via the handle must still work — `applyPressScale` replacing that handle's `OnTouchListener` is the regression to watch for. Scrolling a list must not leave rows stuck at the pressed scale.
