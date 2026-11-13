## Select Hit touch ordering
- **`dispatchTouchEvent` must classify the attack tap before the sweetloader can consume the event.**
  `PreloaderInput.onTouch` returning true means a registered callback claimed the touch, and the
  old code returned early -- so `TouchTapDetector.onUp` was never delivered for that pointer. The
  next DOWN re-armed tracking from a fresh timestamp, and a tap made right after a consumed
  gesture was timed from the wrong start, read as a hold, and was dropped. The detector is a
  passive observer of the gesture the game already acted on, so it sees every event regardless of
  who consumes it -- the same reason `dispatchKeyEvent` records the mouse-button press before the
  sweetloader may consume it. `overlayManager.handleTouchEvent` moved up with it (it also maintains
  gesture state). Pinned by `TouchTapDetectorTest.aSwallowedUpDoesNotPoisonTheNextTap`.
