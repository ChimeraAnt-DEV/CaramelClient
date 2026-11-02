## Module availability: badge, do not silently no-op
- **`ModIds.requiresGameData` is the single source of truth for a module that can never produce
  output in this build**, and `ModAvailability.isUnavailable` derives from it (pinned by
  `availabilityFollowsRequiresGameDataWithoutDrifting`). Trajectory Prediction (needs the held item)
  and Custom Kill Effects (needs the opponent-death feed) are now in that set alongside Armor HUD
  and Crystal Optimizer. A module in the set carries the red `mod_unavailable_message` badge and
  `isInteractive` returns false, so the toggle cannot be flipped to no effect.
- **The peer route is what keeps Hitboxes, Reach Indicator and Hit Prediction usable.** They read
  other players from the proximity-voice feed and the local view, neither of which needs a native
  hook, so they must stay out of `requiresGameData`. Greying them out would make the working half
  unreachable; only their native-only outputs are disabled per-option via `outputRequiresGameData`.
- **`HitboxMod.hasPeerFeed()` must ask whether the route exists, not whether peers are listed.**
  `PeerHitboxSource.readPeers()` returns an empty list (never null) when voice is down, so a null
  check always read true and suppressed the honest "waiting for game data" notice even with no route
  at all. `PeerHitboxSource.hasFeed()` reports the route's existence and the overlay shows the
  notice only when neither route is available.
- **The Hitboxes overlay reads `LocalPlayerFeed.localCamera()` directly, not the voice nametag
  seam.** `VoiceNametagOverlay.cameraSource()` is only installed when voice chat starts, so a player
  with voice off could never draw the peer boxes even though the peer feed worked. The rest of the
  PvP suite already read the camera directly for the same reason.
