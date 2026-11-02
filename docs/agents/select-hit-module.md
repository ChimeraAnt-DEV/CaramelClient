## Select Hit module (HitTimingSolver + HitTimingMod + HitTimingOverlay)
A small green/red pill centred at the top of the screen: green while a hit will land, red while
the post-hit window is still open, with the combo count beside it and a slim progress bar. Sized
(`96x26dp`) so it clears the crosshair area and hotbar, and it is draggable in HUD-editor mode
only, like the other overlays.

- **`HitTimingSolver` is pure** тАФ the timing rules are unit-testable without a Context or clock.
  `evaluate(nowMs)` returns `READY` / `HIT` / `WAIT` plus `remainingMs`, `progress` and `combo`.
- **A click inside the window must not advance the combo or push the window out.**
  `aDiscardedClickDoesNotAdvanceTheCombo` pins it: counting a discarded click would make the
  indicator lie about both the streak and when the next hit lands. A backwards clock (uptime
  wrap) is treated as a fresh engagement rather than producing a negative remaining time.
- The window is clamped (`50..2000ms`) and exposed as a config slider, because the real cooldown
  varies by version тАФ the indicator must not be silently wrong on an unlisted build.
- Attack input is recorded from the paths that actually send an attack: `pojavSendMouseButton`
  (primary) and the `dispatchKeyEvent` mouse-button path (controller). Both call
  `InbuiltOverlayManager.notifyAttack()` тЖТ `HitTimingMod.onAttack(uptimeMillis())`.
  `dispatchKeyEvent` records **before** the preloader may consume the press тАФ the player pressed
  attack either way, and the timing must be the real input timing.
- Honest scope: it reads your own attack input and never clicks for you; the server still decides
  whether a hit lands. `hit_timing_scope_note` says so in the dialog.
- **Phase 2 (the game's own combo counter) is a documented limitation, not shipped.** The pill's
  `decision.combo` is the player's own landed-in-window attacks, counted by `HitTimingSolver`;
  it is not a read of the game's server-side combo counter. Reading that needs a per-frame
  `libminecraftpe.so` feed, and the mask-scan anchors used for the local-player position do not
  expose a combo field in the shipped builds (no `combo` symbol exists in the preloader). The
  local counter is the honest deliverable; `hit_timing_scope_note` states the distinction in the
  dialog so the readout cannot be mistaken for the server value.
