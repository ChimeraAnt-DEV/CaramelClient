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
