# Changelog

One line per user-visible change, newest first. This file is bundled into the app
(`resources/launcher/CHANGELOG.md`) and rendered by the in-app changelog screen.

## BETA3

### Cosmetics

- The preview now renders authored Blockbench meshes for hats and pets, so a hat or pet
  shows its real model instead of a stack of boxes.
- Preview transparency is fixed: a hat, hair or jacket overlay no longer punches holes
  through the skin underneath it.
- The cape is clipped against the body per pixel, so it no longer shows through the skin
  at some camera angles.

### Modules

- Modules that need a live native game feed are badged with the reason they are badged,
  instead of being disabled silently.
- Hitboxes, Reach Indicator and Hit Prediction work from the proximity-voice peer feed,
  so they are usable without a native entity hook.

### Launcher

- Settings can be exported and imported as one JSON file.
- About names the exact build (version, commit, type) and the native library status.
- Diagnostics gained a Copy debug info action that bundles the report with the recent,
  redacted log, and a Report a bug action that copies it before opening the invite.

### Voice

- Private voice channels are joined by typing a share code; the code is the channel id.
- Per-member mute is viewer-only and is never transmitted to the peer.
