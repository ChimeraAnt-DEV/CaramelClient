## Replay system (core.replay + ReplayPanel in the Mod Menu and the launcher screen)
- **One backend, several hosts.** `core.replay` (`ReplayManager`, `ReplayCaptureService`,
  `ReplayClipRepository`, `ReplayLibrary`, `ReplayStorage`, `ReplaySidecar`, `ReplayExporter`,
  `ReplayTrim`, `ReplayFormat`, `ReplayMetadata`, `ReplayQuality`) owns capture, the on-disk clip
  library, trimming and export. The UI is `ReplayPanel`, and the *same* instance type is hosted by
  the Mod Menu (page `REPLAY`, in `replay_container`) and by the launcher's `ReplayActivity`. No
  host has its own clip list, sort, filter or export code - that is the point of the shared panel.
- **One menu, one navigation home.** Replay is a `ModMenuPage` (the `REPLAY` page), so the rail and
  the shoulder cycle cover it like every other page; the launcher reaches the same panel through
  `ReplayActivity`. `ModMenuPageTest` pins the page order - moving a page must not leave a stale
  hard-coded cycle behind.
- **The Mod Menu keeps its panel alive between opens** (it owns a background executor and a live
  recorder listener) and calls `onHidden()` when the page is left; `hide()` also calls `onHidden()`.
  A host that re-inflates its tree must instead drop the panel, or it would draw into a detached
  tree.
- **Controller binds match the on-screen hint.** The hint (`replay_controller_hint` vs
  `replay_keyboard_hint`) follows the *detected input*, not the host: `ReplayPanel
  .setGamepadDetected(...)` is set from a real `InputManager` gamepad scan. A controller hint on a
  keyboard-only player teaches buttons that do nothing.
- **Touch users act on clips through a long-press sheet.** `ReplayClipActionsDialog.showActions`
  offers play/trim/export/favorite/rename/delete and `showRename` is the rename prompt. Both screens
  call it; the controller routes (`cycleSelection`/`playSelected`/`deleteSelectedOrFirst`/
  `favoriteSelected`) act on the same `selected` clip, so touch and pad cannot diverge.
- `ReplayClipRepository.sanitizeName` strips path separators and control characters but **keeps
  dots**, so `"../my/clip"` sanitizes to `"..myclip"` - the name is only ever a file-name base, not
  a path, and the repository joins it to the replays directory. `ReplayClip.sizeBytes` comes from
  the real file (`File.length()`), never a caller-supplied hint, so a test that passes a duration
  must expect `0` for a path that does not exist on disk.
- Screen capture is a platform consent flow: `ReplayManager.requestStart(activity)` launches
  `createScreenCaptureIntent()`, `MinecraftActivity.onActivityResult` forwards to
  `ReplayManager.onActivityResult`, and `ReplayCaptureService` starts. The Replay tab can request
  consent without holding its own activity reference.
- Tests: `ReplayLogicTest` (pure rules - sanitize, sidecar merge, missing-sidecar listing, filter
  and sort), `VipMenuLogicTest` (tab order). No mocks.

- **The portable container is `.glowreplay`.** A replay used to be scattered across a sidecar's flat
  `key=value` keys and the native capture file, so it was not a thing you could move or share. `ReplayDocument`
  is the spec's container: a self-describing JSON document (a `format` tag and a `version` on the wire)
  holding the metadata a replay needs to make sense on its own (player skin, world seed, time of day,
  server address), plus the track, markers and camera path, each via its existing codec
  (`ReplayTrack.serializeLine`, `ReplayMarkers.serialize`, `ReplayCameraPathCodec`) rather than a fourth
  format. It is JSON, not binary, because the recording is small and the pieces already serialise to
  text; a future binary payload (the world geometry the recorder cannot capture) can be a base64 field
  without changing the envelope. `ReplayStorage.isClip` lists `.mp4` and `.glowreplay`, and
  `ReplayClipRepository.readClip` reads a `.glowreplay` as a document (no sidecar, no media probe).
  `ReplayDocument.toClip`/`fromClip` bridge it to the library, and `ReplaySharing.mimeType` declares it
  `application/json` (a JSON document declared `video/mp4` makes a receiver fail to play it). A
  malformed, truncated or foreign file parses to an empty recording, never a throw; a missing field
  reads as empty (or `-1` for the time of day), never a plausible 0. `ReplayDocumentTest` pins the
  round trip, the self-describing envelope, the file write/read, the fail-closed rules and the bridge.

- **The cinematic rig's motion controls live in the pure path.** The spec asks the timeline to "set the
  FOV", "add smooth interpolation (Bezier curves)" and a "path smoothing" control; the path had
  presets and Catmull-Rom but none of the three. `ReplayCamera` gained a clamped field of view
  (`MIN_FOV`..`MAX_FOV`, default 70), each `ReplayCameraPath.Keyframe` carries its own `fovDeg` (the
  6-arg constructor still defaults it, so existing callers are unchanged), and `ReplayCameraPath` has
  an `Easing` (`LINEAR`/`EASE_IN`/`EASE_OUT`/`EASE_IN_OUT`, real cubic-Bezier timings solved by
  Newton with a bisection fallback) and a `smoothing` (0 = straight hop, 1 = full Catmull-Rom curve).
  `sample` reshapes the segment time by the easing first, then blends the curve toward the straight
  line by the smoothing, and interpolates the FOV along the reshaped segment so a zoom rides the move.
  `ReplayCinematicControls` holds the button arithmetic (step size, direction, clamp, easing cycle) so
  the editor's three rows cannot drift; `ReplayCinematicDialog` wires them. The codec writes
  `preset;e=<Easing>;s=<smoothing>;time,x,y,z,yaw,pitch,fov;...` and still parses the old six-field,
  no-metadata form. `ReplayCameraFovTest` and `ReplayCinematicControlsTest` pin it.

- **The keyframe timeline is a Compose strip.** The spec asks for a keyframe timeline the user can
  place keyframes on. `ReplayTimelineStrip` (package `ui.compose.replay`) draws the camera path's
  keyframes as diamonds, the clip's markers as ticks below the bar, and the scrub position as a
  playhead; tapping or dragging the bar reports the time under the finger, snapping to a marker within
  900 ms so a marker is reachable by touch. The strip draws from an immutable `ReplayTimelineData`
  snapshot (keyframe fractions, marker times/fractions, duration, position), rebuilt by the dialog
  whenever a keyframe is added or cleared, so the strip redraws where mutating the path would not
  recompose. All the arithmetic is in the pure `ReplayTimelineLayout` (`fraction`, `xForTime`,
  `timeForX`, `nearestMarker`, `keyframeFractions`), so the snapping rule is unit-tested off-device.
  `ReplayTimelineHost` is the Java-facing seam, and `ReplayCinematicDialog` mounts the strip and
  samples the path on scrub so the camera previews the shot. `ReplayTimelineLayoutTest` pins it.

- **The recorder writes a portable `.glowreplay`, not only a sidecar.** `ReplayRecorder` is the
  capture half of the engine: it owns the local-player track, the markers and the metadata, and
  produces a `ReplayDocument`. It works in two modes on one object — a `ReplayTrackRecorder.Source`
  it pulls from on `tick` (what `ReplayManager` uses), or pushed `sample` calls (what a test uses) —
  so the marker-merge and finish rules are shared. `start` adds the `JOIN` marker at time zero,
  `finish` is idempotent and refuses a later sample. `ReplayManager` now holds one `ReplayRecorder`
  for the clip in flight: `recordMarker` goes through it, `onRecordingFinished` builds the document
  once and writes both the sidecar and the portable container beside the MP4
  (`writePortableReplay`), and the sidecar now carries the camera path too. Honest scope: the build
  exposes only the local player's position, facing and health through the feed, so the recorder
  captures exactly that and leaves world/server/seed/time-of-day empty rather than inventing them;
  the same limitation the rest of the replay system states. `ReplayRecorderTest` pins the two modes,
  the markers, the metadata and the file write.
