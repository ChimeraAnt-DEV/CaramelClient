# Security policy

## Reporting a vulnerability

Please report a security issue **privately**, not in a public issue:

- Open a [GitHub security advisory](https://github.com/ChimeraAnt-DEV/CaramelClient/security/advisories/new), or
- Contact a maintainer directly through the project's Discord.

Include the app version, the device and Android version, a description of the issue, and the
smallest steps that reproduce it. Do not include account tokens or other secrets in the report.

If you are unsure whether something is a security issue, report it privately anyway. A report that
turns out to be a normal bug costs us a little time; a security report posted publicly costs users.

## What to expect

- **Acknowledge** — we aim to acknowledge a report within a few days.
- **Confirm** — we reproduce it and tell you whether we consider it in scope.
- **Fix** — we work on a fix and, where one is warranted, ship it in the next release. Critical
  issues are prioritised over feature work.
- **Credit** — we credit you in the release notes if you want, and we will not credit you publicly
  without asking.

## Disclosure window

Please give us **90 days** from acknowledgement, or until a fix ships, whichever comes first,
before any public disclosure. If a fix is going to take longer we will tell you and agree a new
date with you rather than let the window lapse silently. We will also tell you if we believe an
issue is already public.

## Supported versions

Security fixes are made on the current release and shipped in the next one. We do not back-port to
older releases; the app's own update check will offer the fixed build. The preloader runtime is
versioned with the app, so a fix there reaches users through the same update.

## Scope

In scope:

- The launcher app (`org.chimeramc.client`).
- The preloader runtime (`app/src/main/cpp/preloader`).
- The bundled BerryAPI runtime and the voice/cosmetics relay protocol.
- The relay server (`server/voice-relay`) and its token format.

Out of scope:

- Minecraft: Bedrock Edition itself, and any server the game connects to.
- A device that is already rooted or otherwise compromised.
- The content of mods a user chooses to install (a mod runs with the launcher's privileges by
  design; install only mods you trust).
- A vulnerability that requires an attacker to already have the user's unlocked device.

## What the app does with your data

See [`docs/privacy-policy.md`](docs/privacy-policy.md) for the full statement and
[`docs/privacy-data-flows.md`](docs/privacy-data-flows.md) for the concrete list of endpoints and
payloads. In short:

- Sign-in tokens and account data stay on the device, are encrypted at rest with an Android Keystore
  key, and are excluded from backup and device transfer.
- Native code is loaded only from the app's private directory, never a shared or world-writable
  path.
- The voice relay carries audio and does not decode it; audio over the relay is **not**
  end-to-end encrypted, and the Voice tab says so.
- Cosmetic sync transmits catalogue **ids** only, never a name, gamertag or account id.
- Crash report upload is **off by default**; when it is on, Settings shows exactly what would be
  sent before anything is uploaded.
- **Copy Debug Info** redacts tokens, gamertags and server addresses before you share it.
