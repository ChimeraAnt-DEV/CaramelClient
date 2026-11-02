## Launcher UI (nav indicator, hero pulse)
- `nav_bar.xml` wraps the tab row and a `nav_active_indicator` in a `FrameLayout` (a
  `HorizontalScrollView` takes one child). `BaseActivity.setActiveNavTab` slides the indicator under
  the active tab via `moveNavIndicator` тАФ spring for a move, fade-in for the first placement
  (`navIndicatorPlaced`), instant under reduced motion. Its width tracks the tab so a wider label
  stays centred.
- The hero-card pulse lifts (`translationZ` + a hair of scale) and never touches `alpha`; dimming the
  card faded its own text and read as a flicker.
