## Cosmetics are native-only тАФ the resource-pack route is deleted
- **The legacy cosmetic resource pack is gone, not hidden.** `CapeResourcePackBuilder`,
  `CapeInGameInstaller`, `CosmeticsDiagnostics`, `CosmeticsStatusActivity`,
  `activity_cosmetics_status.xml` and their tests were deleted. The only route a cosmetic reaches
  the game is the native registry + the sweetloader's player-render hook. Do not reintroduce a pack
  builder or an "Apply in game" button. (Skin packs are a **separate** feature тАФ `SkinPackActivator`
  / `SkinPackBuilder` / `SkinsSettingsFragment` stay.)
- **`NativeCosmeticsRuntime` is the single initialisation point.** `LauncherApplication.onCreate`
  calls `NativeCosmeticsRuntime.init(context)`, which publishes the equipped set and installs the
  peer-advert sink; `MinecraftRuntimePreparer` calls `syncEquipped(context)` before every launch;
  `InbuiltOverlayManager.showEnabledOverlays` calls `init` again so a session that starts without
  the Application having run still has the system wired. Adding a second init path is how the two
  drift.
- **`NativeCosmeticsBridge.publishLocal` now also publishes geometry.** After the cape/accessory/pet
  textures it frames the three geometry blobs (`CapeGeometry` chain, `AccessoryGeometry` hat,
  `PetGeometry` pet) into one length-prefixed byte blob and hands it to
  `PreloaderInput.setRenderGeometry`. Build the blob with a `ByteArrayOutputStream`, never a
  `StringBuilder` тАФ a `String` round-trip corrupts the binary length prefixes.
- **`CosmeticSyncModule` renders a received advert through a static `AdvertListener`.** The module
  owns the socket; `NativeCosmeticsRuntime.renderPeerAdvert` resolves the peer's advertised ids
  against the local catalogue and calls `NativeCosmeticsBridge.publishPeer`, keyed by the peer id
  (cape) or a player-scoped texture id (`<id>/hat`, `<id>/pet`). A throwing listener is swallowed so
  a bad payload cannot stop the receive loop.
- **The Cosmetics panel has no Apply/Remove any more.** `refreshGameStatus` reports
  `NativeCosmeticsFeed.isNativeRenderLive()` as `cosmetics_native_active` / `_inactive`; equipping a
  cosmetic calls `publishLocal` directly, so it appears with no reload. `cosmetics_apply_*`,
  `cosmetics_status_*` and the pack-only strings were removed.
- **`CosmeticEffects` (pack particle effects) was deleted with the pack.** `CapeGeometry` survives
  because it is the cape chain the native renderer draws; its identifier constant moved to
  `CapeGeometry.GEOMETRY_ID` (it used to live on the deleted builder).
