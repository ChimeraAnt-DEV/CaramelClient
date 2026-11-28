## Real Minecraft skin (engine-hook seam, wired)
- **REMOVED.** The Compose/View mod menu, the radial wheel and their layouts were deleted; the
  in-game menu is the native Dear ImGui menu now (`sweetloader/src/pl/menu/`, bridged by
  `PreloaderMenu` + `InGameMenuController`). The class names below no longer exist; this
  section is kept as history.

- **`GameSkinSource` is the fail-closed seam; `GameSkinFeed` is the live provider.** `GameSkinFeed`
  reads `PreloaderInput.nativeReadCapturedSkin()` (a 16-byte LE header `width,height,format,stampMs`
  + payload), decodes a PNG or raw RGBA8, normalises to the 64x64 atlas, and is installed in
  `NativeCosmeticsRuntime.init`. With no provider (or no capture, a bad header, a decode failure, or
  a throwing provider) it returns null and `PlayerSkinProvider.resolve` falls through (imported →
  applied pack → Steve → placeholder). `SkinResolution` is the pure order, pinned by
  `GameSkinSourceTest`; the header parse is pinned by `GameSkinFeedTest`.
- **The native capture is at the engine image-loader seam** (`sweetloader`, `mce_image_hook.cpp`): the
  loader detour sees the source bytes of every image the engine builds, recognises a base-skin-shaped
  payload (a square PNG whose IHDR edge is 64/128/256/512, or a square RGBA8 buffer of one of those
  sizes), and copies it into a bounded single slot. JNI: `nativeReadCapturedSkin` /
  `nativeHasCapturedSkin` / `nativeClearCapturedSkin`. It is bounded (one memcpy under a short mutex
  on the loader thread, one exact-size allocation on the read), so no per-frame cost.
- **`refreshSkin()`** on `CapePreviewView` and `RadialMenuView` re-resolves the skin and rebuilds the
  character/head without recreating the view; called when the Launch hero binds, when the Closet is
  shown (`CosmeticsPanel.onShown`) and when the wheel attaches.
- **Honest boundary:** the loader seam carries no player id, so the capture is *content*, not
  attribution — the most recent base-skin-shaped upload, which in practice is the local player's
  skin. Whether the game routes the player's skin through this seam is **unverified (needs device)**.
  The `SerializedSkinRef` base-skin `mce::Image` at `+0x78` would give attribution but its internal
  pixel pointer is not recoverable from the stripped binary, so that route stays documented-only.

- **The account skin is kept in sync, not fetched once.** `AccountSkinCache.Entry` carries
  `fetchedAtMs`, and `AccountSkinCache.shouldRefresh(entry, xuid, now, MAX_AGE_MS)` is the one rule:
  refresh when there is no entry for this account or it is older than the trust window (24h). Two
  callers use it: `AccountSkinService.refreshIfStale` runs as a **deferred startup step**
  (`StartupPhases.Step.ACCOUNT_SKIN_SYNC`, wired in `Application.runDeferredStartup`) so a changed
  Bedrock skin is picked up on the next launch without opening a preview, and
  `PlayerSkinProvider.readAccountSkin` re-syncs on the same rule while still returning the cached
  atlas, so a stale skin updates without the preview flashing the fallback. A 0 stamp (a cache
  written before the stamp existed) reads as stale and is refreshed once. `AccountSkinFreshnessTest`
  pins the rule, the stamp round-trip and the startup wiring.

## "Cosmetics work, the skin does not" — what that actually is

Cosmetics are drawn by us (the native player-render hook), so they work whenever that hook is
loaded. The skin is **not** ours to draw in game: the game renders the player's own skin from its
session, and the launcher has no route that overrides it. `AccountSkinService` feeds the launcher's
**preview** and the replay export, and the preview's order is imported → account → game capture →
applied pack → Steve (`SkinIdentity.resolve`), so a working account lookup always beats the capture.

Where a preview can still land on Steve, in the order worth checking on a device:

1. `AccountSkinService.isEnabled` is off for the install (`ensureEnabledForExistingAccount` turns it
   on at startup only when a XUID is present).
2. No bearer: `MsftAuthManager.minecraftServicesBearer` returns null, so only the legacy GeyserMC
   lookup runs. `LEGACY_WITHOUT_BEARER` in the new log line.
3. A Bedrock-only account: a bearer exists but `api.minecraftservices.com/minecraft/profile` carries
   no Java skin, and GeyserMC has never seen the player. `LEGACY_AFTER_OFFICIAL_EMPTY`, then no skin.
   This is a real limitation of the keyless routes, not a bug we can fix in the launcher.

`AccountSkinService.routeFor` and the `AccountSkinService` log tag now name the route (and say so
when neither produced a skin), so the next device run answers 1-3 from `adb logcat -s
AccountSkinService:*` instead of a guess. **Unverified here: everything in that list.**
