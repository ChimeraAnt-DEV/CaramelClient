## Proximity voice chat (core.voice + VoiceChatOverlay)
- **A launcher-side peer link between Chimera users in the same world**, over LAN multicast UDP.
  Transport = `VoiceTransport` (MulticastSocket), framing = `VoiceProtocol` (raw 16 kHz mono PCM;
  no per-ABI codec), mixing = `VoiceRegistry` + `VoiceChannel`, device = `VoiceAudioEngine`.
  Nothing talks to the game.
- **Channels are the multi-channel contract.** A listener hears their own channel plus the open
  `world` channel in both directions (so switching to a private channel never cuts off someone
  standing in front of you); two different private channels do not hear each other. Gain falls
  from 1 to 0 across the range with a linear band (`VoiceChannel.gain`). Pinned by
  `VoiceChannelTest`/`VoiceProtocolTest`.
- **The position seam is installed, and "installed" is not "live".** `LocalPlayerFeed` (in
  `core.mods.inbuilt.overlay`) installs `VoiceChatModule.setPositionSource(...)` from the same
  `ClientInstance` vtable slot 31 the nametag mic icon uses, so distance falloff applies on both
  transports once a world is loaded. The native read is **fail-closed**: with no world loaded
  (before the first frame, on a loading screen, after leaving a session) it returns null, which is
  a different state from "no feed". `isChannelMode()` reports the persistent state
  (`positionSource == null`); `hasLivePosition()` reports whether a position is readable *this
  frame*, which is what actually decides whether distance is in effect. Calling `isChannelMode()`
  on the audio path was the bug: a null read collapsed to the origin, so every peer was graded
  against a phantom (0,0,0) and dropped. The gain rule (`VoiceChatModule.gainFor(...)`, pure and
  pinned by `VoicePositionGainTest`) now treats a missing position as channel mode тАФ never as
  distance from the origin тАФ and `isChannelMode` is only a status predicate.
- **`sanitizePosition` rejects a malformed feed value.** A NaN/infinite component from a torn
  native read would propagate into the distance math and silence audio with no diagnosable cause,
  so a value that is not three finite numbers reads as "no position" and falls back to the channel
  rule. The Voice tab renders which state is active (`voice_proximity_mode`), so a player in a
  loading screen is told why everyone is at full volume.
- **`VoiceAudioEngine.ensureCapture`/`stopCapture` exist because a mic toggled on after a
  listen-only start must open the recorder**; a bare `setMicEnabled` only gates an already-running
  capture loop. `applyConfig` calls them. The RECORD_AUDIO permission is requested when voice
  starts (`InbuiltOverlayManager.REQUEST_VOICE_MIC`); if denied the module still starts listen-only.
- The module is `ModIds.VOICE_CHAT` in `GROUP_VOICE` (the `Voice` section), a non-PvP module, so
  `groupPvpLast` keeps it a single contiguous run. `VoiceModuleGroupingTest` pins the grouping.
- **Protocol v2 adds a visibility byte and a human-readable channel name, and keeps v1 decodable.**
  `VoiceProtocol.VERSION = 2`; a v1 datagram still decodes as PUBLIC on channel `world` (what v1
  described, since v1 had no private room). An unknown visibility byte clamps to PUBLIC so a
  malformed packet cannot hide a channel. Pinned by `VoiceProtocolTest`.
- **A private channel's id _is_ its join code** (`VoiceChannel.generateCode` -> `CHIMERA-7F2Q`).
  There is no invite protocol: typing the code normalises to the same channel id and the existing
  `canHear` match does the rest, so sharing the string is the whole mechanism. The alphabet excludes
  `O/0/I/1/L` because a code is read aloud and typed by hand. `isJoinCode` distinguishes a code from
  a plain public room name.
- **`VoiceChannelDirectory` is built client-side from the registry snapshot** -- no server, no
  query. Private channels are filtered out of the listing (a browsable list is public by
  definition); the latest advertisement wins for name/visibility, so a rename or a switch to public
  shows immediately. Pure and pinned by `VoiceChannelDirectoryTest`.
- **The Voice tab (`VoiceChatActivity`) is a view onto existing state.** It drives `VoiceChatModule`
  directly -- the singleton is shared with the in-game overlay -- because `InbuiltOverlayManager`
  only exists during a game session. Channel selection writes the same preference the module
  beacons; there is no second source of truth. The module dialog's free-text channel field routes
  through the same `joinVoiceChannel(...)` path, so a typed room name is advertised rather than
  falling back to the id.
- **The in-game indicator is `MicIndicatorView`**, a blocky 12x12 pixel-art meter (anti-aliasing
  off) driven by the real smoothed microphone RMS (`VoiceAudioEngine.updateLevel`), with fast attack
  / slow release so it reacts on the first syllable and does not flicker. Use RMS, not peak (a meter
  tracks loudness); a muted mic drops the level to zero rather than freezing it. `rms`/`sprite` are
  package-visible so `VoiceAudioEngineLevelTest`/`MicIndicatorViewTest` can pin them.
- **Protocol v3 adds capacity, live level and self-mute to the beacon/audio packets, and keeps v2
  and v1 decodable.** `VoiceProtocol.VERSION = 3`; `decode` accepts `VERSION_LEGACY..VERSION`
  (`version < VERSION_LEGACY || version > VERSION` rejects), and a v2 datagram decodes with
  `capacity = CAPACITY_NONE`, `level = 0`, `muted = false` rather than being dropped -- a newer peer
  must not silence an older one. `capacity` clamps to `MAX_CAPACITY`, `level` is clamped by
  `clampLevel` (NaN reads as 0). Pinned by `VoiceProtocolV3Test`.
- **Capacity is advisory, never enforcement.** `VoiceChannelCapacity` holds the rule: a public room
  is full at/above its cap, `CAPACITY_NONE` (<= 0) never reads as full, and a **private channel
  always admits by code** (`canJoin(..., private=true)` ignores the cap) because the code already
  bounds who can join. There is no server, so a host cannot eject anyone; the cap is advertised in
  the directory and a would-be joiner declines a full room. `clampHostCapacity` bounds the settings
  stepper. Pinned by `VoiceChannelCapacityTest`.
- **Per-member mute is viewer-only.** `VoiceMutes` is a local id set on `VoiceChatModule`
  (`toggleMute`/`isMuted`/`mutes()`); muting a peer drops their AUDIO frames from the mix
  (`onDatagram` returns before mixing) and is **never transmitted**, so the peer is never told. The
  beacon already carries their self-mute, so no extra signalling is needed for that half. Pinned by
  `VoiceMutesTest`.
- **The module's config dialog is trimmed to mic-only scope.** `InbuiltModuleProvider`'s voice
  schema is now just the mic master switch plus the in-world icon's look and behaviour. Channel,
  visibility, capacity and per-member mute were removed from the dialog because the Voice tab owns
  them -- two places to set the same value is how they drift. The icon keys are
  `CFG_VOICE_ICON_STYLE`/`CFG_VOICE_ICON_ANIMATE`/`CFG_VOICE_ICON_NAMETAG`.
- **`VoiceUiKit` is the shared premium treatment** for both the in-game `VoicePanel` and
  `VoiceChatActivity`, so the two views of the same feature cannot look like different apps.
  `MicIconStyle` (classic/smooth) and `NametagMicState` (muted/speaking/idle) are the icon's
  pure state, kept Android-free for the JVM tests.
- **The in-world nametag icon is a seam, exactly like the Hitboxes module's boxes.**
  `VoiceNametagMod.TagSource` supplies where each audible player's nametag is; it has **no
  provider** (`setTagSource` has zero callers) because world/nametag positions live in
  `libminecraftpe.so`, so `readTags()` returns empty and nothing is drawn -- never an icon at a
  guessed position. The level and mute state are *not* seams: they come from `VoiceChatModule`
  (with `findPeer(id)` looking across audible channels, not just the local one). `NametagIconProjector`
  is pure and shares `HitboxProjector.Camera` on purpose -- a second camera convention is how one
  drifts. Icons sit right of the label, project far-to-near, and drop when the label is too small
  to read. Pinned by `NametagIconProjectorTest`/`NametagMicStateTest`/`VoiceNametagModTest`.
