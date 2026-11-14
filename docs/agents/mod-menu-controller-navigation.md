## Mod Menu controller navigation (ModMenuNavigation + ModMenuAdapter)
- **REMOVED.** The Compose/View mod menu, the radial wheel and their layouts were deleted; the
  in-game menu is the native Dear ImGui menu now (`sweetloader/src/pl/menu/`, bridged by
  `PreloaderMenu` + `InGameMenuController`). The class names below no longer exist; this
  section is kept as history.
- **The unfocusable overlay root means the framework never moves focus for you.** The white-wash
  fix sets `focusable=false` on the overlay root, so a d-pad or stick press does not move focus
  onto a card. Controller navigation is therefore computed in Java and applied to the list, not
  left to `focusSearch`.
- **`ModMenuNavigation` lays the grid out explicitly.** A group header spans the whole row (the
  adapter's `SpanSizeLookup`), so a header owns a row of its own; plain `index +/- columns`
  arithmetic stepped onto headers and appeared to skip a row. The class is pure and Android-free,
  and `ModMenuNavigationTest` pins every edge (a run of header rows, moving off the end, an
  all-unavailable menu, the single-column compact list).
- **`ModMenuAdapter` owns the selection** (`handleControllerNavigation`/`handleControllerSelect`)
  and draws an accent ring on the selected card via `setForeground`, because the platform's own
  highlight is exactly the wash that was removed. Selection is skipped for unavailable modules,
  matching the touch path.
- **The game activity offers raw keys and stick motion to the open menu before the sweetloader.**
  `MinecraftActivity.dispatchKeyEvent` -> `InbuiltOverlayManager.handleMenuControllerKey` and
  `dispatchGenericMotionEvent` -> `handleMenuControllerMotion`. Without the early offer a pad button
  the menu wants for navigation would be swallowed by the game first. Stick navigation has a
  repeat delay so a held stick does not race through the list.
