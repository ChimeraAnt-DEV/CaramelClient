# Code review: `Lynxifyy/CaramelClient-UI-pack`

Reviewed at `main` (`CaramelClient-UI-pack-main`, 1574 Kotlin LOC under `compose/`, 677 C++ LOC
under `imgui/`, plus `previews/` and `preview/`). The pack is a **design reference**, not a
library to drop in: it is a Compose *Desktop* application and a GLFW/OpenGL3 Dear ImGui demo. The
review below separates what the pack already contributed to CaramelClient from what it cannot.

## What the pack is, and what it already gave us

The pack's own README is honest about its status: *"The Kotlin and C++ were written without a build
environment and have not been compiled."* That matches the tree — there is no Gradle wrapper, and no
tests.

The important finding is that **CaramelClient has absorbed the pack's token layer, not its component
files.** The pack's `compose/src/main/kotlin/dev/caramel/ui/components/` are Compose *Desktop*
widgets (`Buttons.kt`, `Controls.kt`, `Dropdowns.kt`, `Labels.kt`, `LoaderIcons.kt`, `Glass.kt`) and
none of them is imported anywhere in CaramelClient. What the pack actually contributed is the values,
carried by two live sources:

| Pack source                     | CaramelClient counterpart (live)                          |
| ------------------------------- | --------------------------------------------------------- |
| `theme/Theme.kt` (`C` colour object) | `ui/compose/theme/GlowPalette.java` (integer maths) + `theme/GlowTheme.kt` (the Compose wrapper) |
| `theme/Theme.kt` (`T` type scale)    | `theme/GlowDimens.java` + `GlowTheme.kt` |
| `components/Glass.kt`               | `theme/GlowGlass.kt` (`glowGlass`/`GlassSurface`) |
| `imgui/src/CaramelTheme` + `CaramelWidgets` | `sweetloader/src/pl/menu/MenuTheme.*` + `MenuWidgets.*` (the in-game ImGui menu) |

The launcher's Compose screens (`ui/compose/launch`, `account`, `connect`, `replay`) consume
`GlowTheme`/`GlowPalette`/`GlowGlass` directly; there is no second component layer, and a duplicate
`ui/compose/caramel/Caramel*` port that briefly existed was removed as dead code (nothing imported
it — the live theme package already carried the same values).

Where the port diverged from the pack it improved on it:

* **Desktop-only input dropped.** The pack's buttons read `collectIsHoveredAsState()`; CaramelClient's
  own controls use pressed state, which is what a touch device has.
* **Style normalised.** The pack uses `import androidx.compose.foundation.layout.*` in ten files; the
  live files spell out every import, which is what `check_ui_hygiene.py` and review want.
* **Documentation added.** The pack's components are one-line files with a header comment; the live
  files explain the *reason* for each value.

So the honest headline is: the pack did its job as a design reference. What follows is what remains
unported, and what would break if someone tried to port it naively.

## Findings

### 1. It is a Desktop app, not an Android app (blocker for direct reuse)

`compose/build.gradle.kts` applies `kotlin("jvm")` and `org.jetbrains.compose`, and depends on
`compose.desktop.currentOs` with `TargetFormat.Msi` / `Dmg` / `Deb`. That cannot be an Android
module. `Main.kt` is `androidx.compose.ui.window.application { Window(...) }` — an
`application`/`Window` entry point that does not exist on Android.

To port a screen the changes are mechanical but real:

* `androidx.compose.ui.window.Popup` → `androidx.compose.ui.window.Popup` **is** available on
  Android, so the dropdowns and modals port as-is. This is the one window import that is not a
  problem.
* `Window` / `application` / `rememberWindowState` / `WindowDraggableArea` (all in `Main.kt`) have
  no Android equivalent. On Android the "window" is an `Activity`, and the undecorated/transparent
  look is a theme, not a composable.
* The fixed `DpSize(1280.dp, 800.dp)` and the landscape desktop layout need a phone/tablet
  layout pass. CaramelClient's screens are `sensorLandscape`, which suits the pack's wide layout,
  but the pack assumes far more width than a phone has.

### 2. Haze blur assumes a desktop blur source (port with care)

The pack's glass is `dev.chrisbanes.haze:haze:1.3.1` with two `HazeState`s: `LocalHaze` for the
wallpaper backdrop and `LocalWindowHaze` for popups/modals that sample the whole window
(`launcher/Shell.kt:33-35`). CaramelClient already depends on the same Haze 1.3.1, so the
dependency is fine, but the *model* is desktop: `hazeSource(window)` wraps the entire app so an
overlay can blur everything behind it. On Android, blurring the whole window per frame is exactly
the cost that sinks a phone GPU. CaramelClient's own glass already avoids the whole-window source.
A port should keep the backdrop source and drop the window source, and budget the blur to the
overlay that needs it.

### 3. Hardcoded strings (63 of them)

Every label in the pack is a literal `Text("…")`: `"Sign in with Microsoft"`,
`"Open microsoft.com/link on any device and enter this code."`, `"Waiting for sign-in…"`. The pack
has no `strings.xml`. CaramelClient has a translation budget enforced by `check_ui_hygiene.py`
(`translation-baseline.txt`), so a ported screen must route every one of these through the string
resources, or it fails the hygiene gate — which is the gate working as intended.

### 4. A truncating integer division

`launcher/Tabs.kt:42` sizes an inner icon with `Modifier.size((size / 2).dp)` where `size: Int`. In
Kotlin `Int / Int` truncates, so an odd `size` loses half a dp. Harmless here, but the pattern is
worth replacing with `(size / 2f).dp` when porting, because a `size` of `1` would yield a zero-size
icon rather than a small one. Note the same file mixes `.size(size.dp)` (Int receiver) with
`.size((size / 2).dp)` in one line, so the intent is clearly "half", not "integer half".

### 5. No tests, and it has never been compiled

There is no wrapper and no test source. Any port therefore starts from code that has never seen a
compiler. That is not a reason to reject it — it is a reason to port one screen at a time behind
the existing JVM test suite and the CI `Build Android APK` job, rather than in one sweep.

### 6. The `imgui/` half is a desktop demo, and Android already has a route

`imgui/src/` is renderer-agnostic by design (the CMake comment says so: *"CaramelMenu is
renderer-agnostic: it only needs a Dear ImGui context. Reuse it from any backend (DX11, Vulkan,
hook)."*). But the only backend wired is `imgui_impl_glfw` + `imgui_impl_opengl3` via
`FetchContent` of GLFW 3.4 — a desktop windowing stack with no Android path. On Android the mod
menu is drawn through the game's own GL context (the `GamePlayerRender` / `mce_image_hook` seams in
SweetLoader), not through GLFW. So `CaramelMenu.cpp` / `CaramelWidgets.cpp` are reusable *if* an
ImGui context is ever created inside the game, and the GLFW backend is not.

### 7. Version skew

The pack pins Kotlin 2.0.21 / Compose 1.7.3. CaramelClient is on Kotlin 2.3.0 / Compose BOM
2026.01.01. A port must target CaramelClient's versions; the pack's pins are not usable. The Haze
pin (1.3.1) happens to match.

### 8. The pack's app icon is a different asset from the new launcher art

`previews/app_icon/caramel_apple_512.png` and `assets/apple_logo.png` are the pixel-art apple, not
the new `caramel_apple_icon_512.png` launcher art (93.9% of pixels differ). The launcher icon comes
from `Lynxifyy/CaramelClient-APP-ICON-BANNER-` and is generated by
`scripts/gen_launcher_icons.py`; the pack's apple is an in-app logo, a separate slot. Do not let a
port overwrite the launcher icon with the pack's preview.

## What to do with it

1. **Keep it as the reference for tokens and shape**, which is what it has already been used for.
   The port is `ui/compose/theme/GlowPalette.java` + `GlowTheme.kt` + `GlowGlass.kt` (Compose) and
   `sweetloader/src/pl/menu/MenuTheme.*` + `MenuWidgets.*` (ImGui); do not re-port the pack's
   Desktop component files, and do not add a second `Caramel*` component layer beside them.
2. **Port screens one at a time**, each behind the JVM test suite and the CI APK job, replacing the
   desktop window layer with CaramelClient's activity + `GlowTheme`, and routing strings through
   resources.
3. **Do not port the window blur model**; keep CaramelClient's existing glass and its cheaper blur
   budget.
4. **Do not wire the GLFW backend.** The in-game ImGui menu is now built: it runs on the game's GL
   context through the SweetLoader render seams (`pl/menu/MenuHost.*`), and it took the pack's
   `CaramelMenu` / `CaramelWidgets` shape (as `MenuTheme.*` / `MenuWidgets.*`), not GLFW.
5. **Use the pack's `previews/` as the visual spec** when porting a screen — they are drawn from
   the same tokens, so they are a better acceptance target than prose.
