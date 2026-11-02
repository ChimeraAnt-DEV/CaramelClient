## Replay highlight triggers, in-overlay playback and the smaller gaps
- **The Tier-4 triggers are wired, and the seam is `ReplayTriggerFeed`.** `ReplayManager.onDeath()`,
  `onCombo(int)` and `onKillStreak(int)` were built but had zero callers, so highlights only ever
  came from a manual record. `ReplayTriggerFeed` drives all three from signals the client already
  owns: the local player feed's health for death, `HitTimingMod`'s landed-hit detection for combos,
  and a consecutive-opponent-death counter for streaks. Do not add a native hook for these - the
  whole point is that none is needed.
- **Clips play inside the overlay, not through `ACTION_VIEW`.** `ReplayPanel` hosts a
  `ReplayPlayerView` in its own window (scrub/play-pause/2x/close); handing the file to an external
  player backgrounded Minecraft. `ReplayPlaybackGate` records that playback is happening and
  `ReplayPanel.openClip` stops an in-flight capture, so the encoder and decoder never fight over the
  same hardware. Keep both halves: the gate is what the capture service checks.
- **Share is `ACTION_SEND` + FileProvider, not the gallery.** `ReplayShareSheet.share` builds a
  content uri via `${applicationId}.fileprovider` and adds `FLAG_GRANT_READ_URI_PERMISSION`
  (without the grant the receiver cannot read the uri and the share fails on its side).
  `ReplaySharing` holds the pure naming/typing rules and is unit-tested.
- **The scrub preview decodes at an arbitrary timestamp, unlike the static thumbnail.**
  `ReplayThumbnails.frameAt(clip, fraction)` clamps a non-finite/out-of-range fraction and caches
  per whole second so a drag does not re-decode per pixel. `ClipCardView` layers the gesture on the
  shared press feedback through the two-argument `DynamicAnim.applyPressScale(view, delegate)`; it
  consumes its up so scrubbing never also opens the clip, while a long press without movement still
  opens the action sheet.
- **Stitching is a container-level mux and adopts the first clip's tracks.** `ReplayExporter.stitch`
  remaps later clips' samples onto the first clip's tracks **by MIME** - track indices differ per
  file, so copying index `i` would mux audio into the video track. `ReplayStitch` holds the pure
  rules (order preserved, de-dupe by path, drop missing files, `MAX_CLIPS` cap) and is unit-tested.
