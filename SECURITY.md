# Security policy

## Reporting a vulnerability

Please report a security issue **privately**, not in a public issue:

- Open a [GitHub security advisory](https://github.com/ChimeraAnt-DEV/GlowberryClient/security/advisories/new),
  or
- Contact a maintainer directly through the project's Discord.

Include the app version, the device and Android version, a description of the issue, and the
smallest steps that reproduce it. Do not include account tokens or other secrets in the report.

## What to expect

- We aim to acknowledge a report within a few days.
- We will confirm the issue, work on a fix, and credit you in the release notes if you want.
- Please give us a reasonable window to ship a fix before any public disclosure.

## Scope

In scope:

- The launcher app (`org.chimeramc.client`).
- The preloader runtime (`app/src/main/cpp/preloader`).
- The bundled BerryAPI runtime and the voice/cosmetics relay protocol.

Out of scope:

- Minecraft: Bedrock Edition itself, and any server the game connects to.
- A device that is already rooted or otherwise compromised.
- The content of mods a user chooses to install (a mod runs with the launcher's privileges by
  design; install only mods you trust).

## What the app does with your data

See [`docs/privacy-policy.md`](privacy-policy.md) for the full statement. In short:

- Sign-in tokens and account data stay on the device and are excluded from backup.
- Native code is loaded only from the app's private directory, never a shared or world-writable
  path.
- The voice relay carries audio and does not decode it; audio over the relay is **not**
  end-to-end encrypted, and the Voice tab says so.
- Cosmetic sync transmits catalogue **ids** only, never a name, gamertag or account id.
- **Copy Debug Info** redacts tokens, gamertags and server addresses before you share it.
