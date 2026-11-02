## Mod Menu focus highlight (the "50% white overlay")
- **The white wash is the platform's default focus highlight.** A clickable View is implicitly
  focusable, so the d-pad/analogue stick moved focus onto the overlay root and the framework
  painted a full-screen translucent white highlight. Fix: `overlay_mod_menu.xml` root is
  `focusable="false"` and `defaultFocusHighlightEnabled="false"`, and `ModMenuOverlay.
  disableFocusHighlight` sets `setFocusable(false)` on the root plus clears the highlight flag
  recursively (buttons stay focusable for controller nav). Only the highlight flag is touched,
  never a background.
- **Clearing the flag is not enough on its own тАФ the held focus must also be released.**
  `disableFocusHighlight` also walks every descendant, clears the highlight flag and releases focus
  from whichever view holds it (`clearFocus()` only if `isFocused()`). A highlight can be painted
  for the view that already holds focus, and a stick nudge can hand focus to the root *or a card
  row* *after* the helper first ran; clearing the flag does not retroactively remove the highlight
  from the currently-focused view, so without releasing focus the wash still appears mid-gesture
  for some builds. All three parts are needed: unfocusable root, cleared flag, released focus.
- **Order matters: the suppression must run *after* `setupViews()` / `loadMods()`.** Populating the
  list can hand focus to its first card, which restores the very highlight the helper had just
  removed. Both `show()` and `showFallback()` call `disableFocusHighlight(overlayView)` after the
  populate step. Moving it back before is the regression to watch for if the wash returns.
