## Mod Menu keybinds (keyboard + controller) and the in-game capture
- **REMOVED.** The Compose/View mod menu, the radial wheel and their layouts were deleted; the
  in-game menu is the native Dear ImGui menu now (`sweetloader/src/pl/menu/`, bridged by
  `PreloaderMenu` + `InGameMenuController`). The class names below no longer exist; this
  section is kept as history.
- The Mod Menu open bind is two independent prefs in `InbuiltModManager`:
  `getModMenuKeybind()`/`setModMenuKeybind` (keyboard/mouse, captured via a dialog `OnKeyListener`)
  and `getModMenuControllerBind()`/`setModMenuControllerBind` (controller). `matchesModMenuBind(
  menuBind, controllerBind, keyCode, rawKeyCode)` is the **pure** match: an unbound (0) side never
  matches, the sides do not cross-match, and a key matches on either its remapped or its **raw**
  code so a profile that remaps the bound button still opens the menu. `ModMenuBindMatchTest` pins
  all of it.
- **The in-game picker must see the press before the sweetloader can consume it.** The sweetloader
  dispatches keys first, so a captured button the sweetloader claims would never reach the picker.
  `ModMenuOverlay.deliverBindKey(keyCode)` is a static sink the picker registers into
  (`sBindCapture`), and `MinecraftActivity.dispatchKeyEvent` offers every raw ACTION_DOWN there
  before anything else may swallow it. The dialog's own key listener still covers launcher-side
  receivers. Register on open, clear on dismiss.
- The controller bind row shows a **2D map of the connected pad with the bound control lit green**
  (`refreshControllerBindIllustration` -> `ControllerIllustrationView.setRegionConfirmed`). A bare
  key name ("BUTTON L1") does not say which physical button it is. The illustration is hidden
  while no bind exists.
