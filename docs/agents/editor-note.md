## Editor note
- Android Activity/View APIs used: `android.hardware.input.InputManager`, `android.view.InputDevice` (getDeviceIds/getDevice/vendor/product/sources), `android.view.KeyEvent`/`MotionEvent` (getAxisValue(getAxisValue; MotionEvent has NO setAxisValue in this SDK). Custom `View` inflation requires `(Context)`/`(Context, AttributeSet)` ctors.
- Controller remaps/dead zones must be wired through `ControllerInputProcessor` (not just UI) to affect actual gameplay input; key remaps are applied to the `PreloaderInput.onKeyEvent` path in `MinecraftActivity.dispatchKeyEvent`.
