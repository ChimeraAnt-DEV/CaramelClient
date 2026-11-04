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
