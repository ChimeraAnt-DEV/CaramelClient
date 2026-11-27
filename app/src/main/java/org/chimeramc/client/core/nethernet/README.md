# `core/nethernet` — unwired on purpose

Nothing outside this package references `ConnectionManager`, `NetherNetTransport`, `MsaAuthClient`
or `NativeNetherNet`. That is deliberate, and this file records why so the next reader does not wire
it up "to add NetherNet support".

The game implements its own transport. 1.26.60 dropped RakNet from `libminecraftpe.so` and joins
servers over NetherNet (WebRTC), but the *client* half of that lives inside the game: the launcher
hands the game its libraries, its storage and its sign-in data, and the game opens the connection.
Replacing that transport from the launcher is not possible, so a second implementation here would
only add a code path that never runs.

What the launcher *did* need for 26.60 was different, and it is fixed elsewhere:

- the game library's new `libpairipcore.so` dependency — `NativeLibraryVersionGate.shouldLoadPairIp`
  plus the raw `dlopen` in `NativeMapper` (see `docs/agents/nethernet-26-60-signin-loop.md`);
- a manifest that names its own classes (see the same doc).

The version gate itself is `org.chimeramc.client.core.minecraft.BedrockTransport`, which is unit
tested at the 1.26.60 boundary and returns `RAKNET` for anything unparseable. Any future feature that
really is transport-specific must branch on it; nothing in this package is on the launch path, so a
failure to load `libsweetloader_nethernet.so` cannot break a launch.
