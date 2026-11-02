## Voice relay server (server/voice-relay, Go) тАФ internet voice
- A standalone Go UDP relay so voice works between players **not on the same Wi-Fi**. LAN multicast
  stays the default path (`VoiceTransport`); the relay is a second `VoiceLink`
  (`VoiceRelayTransport`) the user opts into with a server address. Nothing in the launcher's
  `VoiceChatModule` knows which one it has beyond the `VoiceLink` interface.
- **`VoiceLink` is the transport seam.** `prefersOpus()` decides the codec: the LAN path returns
  false (raw PCM, no codec dependency on a shared network), the relay returns true. `VoiceCodec`
  wraps Concentus (pure-Java Opus, BSD-3) through **reflection**, so a missing jar degrades to PCM
  rather than making the voice package un-loadable. `VoiceCodecTest` asserts Concentus is on the
  test classpath тАФ otherwise `isAvailable()` would be false and every relay session would silently
  fall back to raw PCM, which is the whole bandwidth saving lost with no error.
- **`VoiceJitterBuffer` is per peer, and its drain semantics are a priming problem, not a
  per-call threshold.** It releases only after the buffer holds the adaptive target depth *or* the
  head has waited past `MAX_WAIT_MS`; re-requiring the depth on every `poll` stalls the stream the
  moment it dips below the cushion. It also anchors `nextSequence` at the **lowest buffered
  sequence**, not the first arrival тАФ an early frame that overtook the one that arrived first is
  not late. `offer` only rejects `position < nextSequence` once actually primed. Sequence
  wraparound and non-zero start values are handled by `extend` (reconstructs the full 64-bit
  position from the 32-bit wire value relative to the last one).
- **`VoiceMixer` sums sources sample-by-sample and saturates.** Each source is applied at its own
  offset (so a talker that starts mid-frame does not shift the others) and a zero/short buffer
  contributes silence rather than dragging the frame to zero.
- **Wire format is pinned cross-language by golden vectors.** `VoiceProtocol` v4 adds the
  server-assigned `clientId` (8 bytes after the type) and the codec byte (just before the payload
  length), and carries **three** length-prefixed strings before the visibility byte: `peerId`,
  `name`, `channel`. The Go parser must consume all three or every field after is read from the
  wrong offset тАФ a mistake that only surfaces when a real client meets a real server.
  `VoiceProtocolGoldenVectorTest` (Java) fixes the exact bytes; `server/voice-relay/vector_test.go`
  asserts the same literals parse to the same fields. Change the layout and one suite fails instead
  of the mismatch shipping. v1/v2/v3 remain decodable (`VERSION_LEGACY..VERSION`); an unknown codec
  byte reads as PCM.
- **The relay never decodes audio.** It carries opaque payloads and the codec byte through, patches
  the sender's assigned id into one copy, and fans it out тАФ so a 1 vCPU / 1 GB VPS is enough.
  `CanHear` mirrors `VoiceChannel.canHear` exactly (same channel, or the open `world` channel in
  both directions) so a team-channel switch behaves the same over the relay as on the LAN.
- **Keepalive keeps phone NAT mappings open.** The client pings every `HeartbeatMs` (server tells
  it the interval in `HELLO_ACK`); the server answers PONG and evicts a session idle past
  `IdleTimeoutSec`. `RelayReconnectPolicy` backs off reconnects; a phone that re-HELLOs from the
  same socket replaces its session rather than accumulating one.
- **Abuse controls are per client and per IP**: token-bucket packet and byte rate limits, a
  per-channel member cap (a refused move is a no-op + NOTICE, not a disconnect), `max_channels`,
  `max_clients`, `max_clients_per_ip`, and an optional shared password. `/healthz`, `/metrics`
  (Prometheus) and `/status` are the monitoring surface. See `server/voice-relay/README.md` for
  deploy (systemd unit in `deploy/`), firewall and NAT notes.
- **Token auth replaces the replayable password.** Set `TokenSecret` (`-token-secret` /
  `VOICE_TOKEN_SECRET`) and the relay requires a signed join token instead: the launcher holds the
  secret, never sends it, and mints `VoiceToken.issue(secret, deviceId, ttl, now)` per HELLO тАФ
  re-minted on every reconnect, so an expiry mid-session cannot lock a client out. The token is
  `v1.<base64url(deviceID|expiryUnix)>.<base64url(hmacSHA256(secret,"v1|"+payload))>`; the HMAC
  covers the version prefix, the base64 is **URL-safe without padding**, and `MAX_TTL_SECONDS`
  caps a hand-minted far-future expiry. A device id is sanitized on both sides (control bytes and
  `|` stripped, 64 chars max) because it becomes part of a signed payload. **The format is pinned
  across languages**: `VoiceTokenTest.GOLDEN` (Java) and
  `token_test.go:TestTokenGoldenVectorMatchesTheLauncher` (Go) assert the same literal тАФ a drift
  here is not a compile error but a relay that refuses every connection.
- **Bans: auto, static and live.** Repeated bad credentials auto-ban an IP for `BanMinutes`
  (`AuthFailuresBeforeBan`, 0 disables); `BannedIPs` (IP or CIDR) and `BannedDevices` are permanent
  and the device ban survives an IP change. The admin endpoint (`AdminListen`, localhost/private
  only, `AdminToken` bearer) exposes `GET /admin/bans`, `POST /admin/ban` and `POST /admin/unban`
  (`{"ip"|"device", "minutes"}`); a ban takes effect immediately and `DisconnectBanned` drops any
  matching live session. The device id rides the v4 wire as its own field (`Packet.DeviceID`, after
  `name`), and every relayed frame is stamped with it so a ban can be bound to a device.
- **The device id is generated on first use, never a hardware id.** `InbuiltModManager
  .getVoiceDeviceId()` returns a random `UUID` kept in prefs тАФ enough to bind a token and a ban to
  an install without being a cross-app tracking identifier. Clearing the pref is the intended
  "start over" escape hatch.
- **A default relay address can be baked in at build time.** `local.properties` тЖТ
  `voice.relay.address` тЖТ `BuildConfig.DEFAULT_VOICE_RELAY_ADDRESS` (empty by default). The pref
  wins when set; `getVoiceRelayAddress()` falls back to the build value so a release ships a
  working server with no first-run typing. The value is not a package path and holds no secret.
- **Honest scope:** the password is a plain shared secret over UDP, not authenticated identity, and
  audio is **not end-to-end encrypted** тАФ the server carries it. `voice_relay_scope_note` says so in
  the Voice tab. The relay only swaps transports; distance falloff applies on both paths via the
  installed `PositionSource` (see the proximity-voice section тАФ a missing live read falls back to
  channel mode).
