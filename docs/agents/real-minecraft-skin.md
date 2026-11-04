## Real Minecraft skin (engine-hook seam, wired)

- **`GameSkinSource` is the fail-closed seam; `GameSkinFeed` is the live provider.** `GameSkinFeed`
  reads `PreloaderInput.nativeReadCapturedSkin()` (a 16-byte LE header `width,height,format,stampMs`
  + payload), decodes a PNG or raw RGBA8, normalises to the 64x64 atlas, and is installed in
  `NativeCosmeticsRuntime.init`. With no provider (or no capture, a bad header, a decode failure, or
  a throwing provider) it returns null and `PlayerSkinProvider.resolve` falls through (imported →
  applied pack → Steve → placeholder). `SkinResolution` is the pure order, pinned by
  `GameSkinSourceTest`; the header parse is pinned by `GameSkinFeedTest`.
- **The native capture is at the engine image-loader seam** (`preloader`, `mce_image_hook.cpp`): the
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
