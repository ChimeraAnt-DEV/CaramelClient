## Cosmetic sync (core.cosmetics.CosmeticSync*) - cross-player cosmetic visibility
- **Two discovery sources, one manifest.** The advertisement is always the same
  `CosmeticSyncProtocol` datagram; only the transport changes, so the payload format is unchanged
  between the LAN path and the relay path. `CosmeticSyncConfig.resolve(...)` owns the routing rule
  and is pure/Android-free so it is unit-tested.
  1. **LAN multicast** (`GROUP 239.255.42.100:47902`) is always on: two devices in the same Bedrock
     world share a segment, so it reaches all of them with no server. Zero-configuration default.
  2. **Relay** - when the player has configured the same Go relay Proximity Voice Chat uses, the
     manifest rides as a `VoiceProtocol.TYPE_COSMETIC_MANIFEST` (v4) frame the relay fans out with
     the audio rule (same channel, or the open `world` channel either way), so a cape is visible to
     a Caramel user **not on the same Wi-Fi**. The relay never decodes the payload.
  3. **Manual peer** - the fallback when no relay is configured: a pasted `host:port`
     (`InbuiltModManager.getCosmeticManualPeer`) the manifest is unicast straight to. A direct peer
     cannot be discovered automatically, hence manual.
  Relay and manual are mutually exclusive (a relay is the better route); multicast runs alongside
  either. `CosmeticSyncModule.routeLabel()` reports which route is active for the status line.
- **The relay gets exactly one new message type, `TypeCosmeticManifest = 9`.** It is a distinct
  type (not a beacon flag) so an older server drops it cleanly. `hub.onClientPacket` treats it like
  a beacon/audio/bye for fan-out and session bookkeeping, but the relay copies the sender's
  assigned client id in and relays the payload **unchanged** - the launcher's
  `CosmeticSyncProtocol` owns those bytes, so a new cosmetic is a client-side catalogue entry and
  the server needs no change. Counted separately (`chimera_voice_cosmetic_manifests_total`) so it
  does not inflate the audio figures. Pinned by `integration_test.go`
  `TestCosmeticManifestIsRelayedToChannelPeers` (real loopback UDP).
- **The relay client is standalone, not the voice module.** `CosmeticSyncModule` opens its own
  unicast socket, HELLOs (same address/password/channel from the shared relay prefs), keepalives,
  and reads `HELLO_ACK` (for its assigned id) + `COSMETIC_MANIFEST` frames. It is independent of
  `VoiceChatModule`, so cosmetics work with voice off and vice versa.
- A **separate wire protocol** (`CosmeticSyncProtocol`, magic `CS`) exists beside `VoiceProtocol`:
  voice's format is pinned cross-language by golden vectors, so a cosmetic field must not be folded
  into it. Cosmetic sync only ever carries catalogue **ids**; each client resolves them against its
  own catalogue, so an unknown id degrades to "no cosmetic" rather than a crash or placeholder.
- The registry keeps the **latest advert per peer** and `contains(peerId)` distinguishes a first
  sighting from a routine refresh. On a first sighting the module sends a `TYPE_REQUEST` so a
  player who joined after us is not invisible until their next timer tick - this is the "capes
  don't sync on join" fix. A request carries no cosmetics and is never stored as an advert.
- Started/stopped with the game session from `InbuiltOverlayManager` (`startCosmeticSync` / hide
  path), and the panel can toggle it via `InbuiltModManager.isCosmeticSyncEnabled`. Changing a
  cosmetic calls `CosmeticSyncModule.requestAnnounce()` so a swap is advertised immediately.
- The Cosmetics panel's sync line is honest about state: off / "starts with the game" (no session) /
  "N peers can see you (route)". The route label (`lan`/`relay`/`manual`) tells the player which
  transport is actually carrying advertisements. The panel's manual-peer row is hidden while a
  relay is configured, because the relay is the better route and the manual value is ignored then.
