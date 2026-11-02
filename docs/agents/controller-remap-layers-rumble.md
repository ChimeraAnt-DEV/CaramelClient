## Controller remap layers & rumble (RemapLayer + RumbleCurve + ControllerRumble)
- **A layer is an alternate button map selected by a held modifier.** `RemapLayer` holds a
  modifier key code and its own `Map<Integer,Integer>`; `ControllerProfile.getRemapLayers()`
  carries them and `ControllerProfileCodec` persists them. `ControllerResponse` precomputes one
  flat `int[]` per layer so the hot path (`remapKey(keyCode, modifier)`) still allocates nothing.
- **A button the layer does not list falls through to the base map**, not to identity тАФ otherwise
  holding a modifier would silently un-remap every button the layer does not mention.
- **The modifier is tracked by *raw* key code**, before any remap: a base remap that changes the
  modifier button's meaning must not stop it selecting. `ControllerInputProcessor.handleLayerModifier`
  returns true for the modifier press so `MinecraftActivity.dispatchKeyEvent` can swallow it тАФ the
  modifier is a switch, not an action. `resetLayerState()` runs in `setActiveProfile`.
- **Layers only apply to a gamepad.** `ControllerResponse.isGamepadKeyCode` is an explicit set, not
  a range test: the lettered faces sit at 96..110 while `KEYCODE_BUTTON_1..16` sit at 188..203, so
  no min/max works. The d-pad codes are included. A keyboard key sharing a code must not swap a
  player's whole map while they type.
- **`ControllerRumble` reaches the pad through API 31+ `InputDevice.getVibrator()`, behind a nested
  holder** тАФ `getVibrator` does not exist on minSdk 28 and referencing it from a reachable method
  would class-init crash. Below 31 it returns false and the caller falls back; `RumbleCurve` owns
  the strengthтЖТamplitude math (a 1% strength must stay above `MIN_AMPLITUDE`, not round to a value
  a vibrator cannot render). Pinned by `RemapLayerTest` / `RumbleCurveTest` (no mocks).
