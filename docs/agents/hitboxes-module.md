## Hitboxes module (HitboxProjector + HitboxMod + HitboxOverlay)
Draws entity bounding boxes plus two combat guides, and is the one overlay that is **not**
draggable тАФ boxes are world-positioned, so the whole screen is the canvas and it is created with
`FLAG_NOT_TOUCHABLE` so it can never eat a tap or a look gesture. `HitboxMod.EntitySource` is the
game-data seam; with no provider `readFrame()` returns null and **nothing is drawn**, because an
invented box is worse than no box тАФ the player would aim at it.

- **`HitboxProjector` is pure** (no Android/game types) and holds all the arithmetic: the camera
  basis, the box projection, the crit line, the combo box, and the crosshair slab test. The
  overlay only paints what it returns. `HitboxProjectorTest` is the gate.
- **Yaw is clockwise from +Z (the Minecraft convention), so `Camera.forward()` negates its X
  component.** The naive spherical form `(sin yaw, тАж)` is counter-clockwise and mirrors every
  projected box horizontally тАФ a mismatch that is invisible until a real feed supplies yaw.
  The test `turningTheCameraMovesTheBoxOffCentre` caught exactly this. `pitchingUpMovesтАж` and
  `aHigherEntityProjectsHigherтАж` pin the up vector, which nothing else covers.
- **The look line is a screen-space anchor, not a world ray.** A camera's own forward ray
  projects to a single point (the centre), so a world ray would have nothing to draw. The line
  runs from a point below centre up to the crosshair; an entity turns blue when the crosshair
  actually points at it, which is decided by `crosshairHits` (a slab test) so the highlight
  cannot disagree with the geometry drawn.
- **Colours are the contract, kept in one place**: entity boxes white, aimed-at box blue, crit
  line red (blue when aimed), combo box red (blue when aimed). Guides are players only тАФ
  `onlyPlayersGetCombatGuides` pins that mobs/items/projectiles get a plain box.
- Far-to-near sort so a nearer box paints over one behind it. A box behind the camera produces
  no screen rect and is skipped.

### Peer boxes are drawn even with no native feed (the reachable half)
- **The module is not greyed out and must not be.** `HitboxMod.EntitySource` has no provider, so
  the native frame is always null тАФ but `HitboxOverlay.drawPeerBoxes` still draws other players
  from the proximity-voice feed (`PeerHitboxSource` тЖТ `VoiceChatModule.audiblePeers()`), which
  needs no game hook at all. `ModIds.requiresGameData` therefore **excludes** `HITBOX`; only the
  options that genuinely need the entity list are disabled, via
  `ModIds.outputRequiresGameData(modId, configKey)` (mobs/items/projectiles/thrown items/crit
  line/combo box) which `InbuiltModuleProvider.configNode` turns into `"disabled": true` and
  `ModAvailability.isOptionInteractive` exposes. `ModAvailabilityTest` pins both halves. Greying
  the whole module (an earlier state) made the working peer boxes unreachable тАФ do not restore it.
- **The aim line for a peer is the peer's own advertised yaw/pitch** (protocol v5), never a
  locally guessed heading; `PeerHitboxSource.from` treats 0/0 as the "unknown" sentinel and
  `hasRotation == false` draws a box with no line.
- **The chest-height target box is `hitbox_show_target_box`** (default on), drawn inside a peer
  box by `drawPeerGuides`; it is in the config schema, the configs list and the setter switch, so
  it is user-toggleable. It is a peer-route output, so it is *not* in the native-outputs set.
