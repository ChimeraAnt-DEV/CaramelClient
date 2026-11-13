# Data flows: what leaves your device

This page lists every network request the launcher makes, what each one sends and what it receives
back. It is the short, concrete companion to the [Privacy Policy](/privacy-policy): the
policy states the commitments, this page names the actual endpoints and payloads.

Nothing here is sent in the background on first run. Each flow below is either triggered by an
action you take or is a check you can turn off.

## Microsoft / Xbox sign-in

- **When.** Only while you are signing in or refreshing an account you added.
- **Sends.** Your Microsoft account credentials, handled by the Microsoft identity and Xbox Live
  endpoints. The launcher stores the resulting token material on the device only.
- **Receives.** The account's gamertag, XUID and Minecraft profile.
- **Where.** `login.live.com`, `user.auth.xboxlive.com`, `xsts.auth.xboxlive.com`,
  `api.minecraftservices.com`.
- **Stored on device.** The serialized auth manager is encrypted at rest with a key held in the
  Android Keystore, and it is excluded from Android Auto Backup and device-to-device transfer.

## Update check

- **When.** On launcher start and when you tap Check for updates.
- **Sends.** A plain `GET` to the GitHub releases API. No account, device or install identifier is
  attached; the request carries only what any HTTP client sends (an IP address, which GitHub sees).
- **Receives.** The latest release tag and its asset URLs.
- **Where.** `api.github.com/repos/ChimeraAnt-DEV/CaramelClient/releases/latest`.
- **Turn off.** Disable the update check in Settings; the app then never calls the API.

## Preloader signature rules

- **When.** On launcher start, to keep the native hook rules current.
- **Sends.** A plain `GET` for the rules file.
- **Receives.** A signed JSON rule set. The signature is verified before the rules are used, so a
  tampered file is rejected and the bundled offline snapshot is kept instead.
- **Where.** `raw.githubusercontent.com/ChimeraAnt-DEV/CaramelClient/main/resources/preloader/`.

## Package mirror (Installations tab)

- **When.** Only while you are browsing the Installations tab and downloading a game package.
- **Sends.** A plain `GET`/`POST` to the mirror to resolve a download link. The request carries no
  account or device identifier.
- **Receives.** The listing page, the version page and the game package itself.
- **Where.** `mcpedl.org` and its file host (this is the one host the app talks to over plain HTTP,
  because the mirror serves it that way; see the network security config).
- **Note.** The game package is downloaded to the app's private storage and imported locally.

## Content browsing (CurseForge / Modrinth)

- **When.** Only while you are browsing those screens and only if you have configured an API key
  where one is required.
- **Sends.** The search query you typed, your configured API key (CurseForge), and a game-version
  filter. No account or device identifier.
- **Receives.** Project metadata and download URLs.
- **Where.** `api.curseforge.com`, `api.modrinth.com`.

## Crash report upload

- **When.** Only if you turn on Crash report upload **and** this build has a collector configured.
  Off by default.
- **Sends.** The saved crash report body, the app version, the report's file name and whether it was
  truncated. No account, gamertag or device id.
- **Where.** The collector address built into the app (`CRASH_UPLOAD_ENDPOINT`). The shipped source
  has none, so the switch is disabled and says why.
- **See it first.** Settings shows a **What is sent** preview, and turning the switch on confirms
  that exact content before anything is uploaded.

## Voice chat (opt-in)

- **When.** Only after you accept the Voice notice. Off by default.
- **Sends.** Microphone audio, and an advertisement (a per-session voice id, the display name you
  choose, your in-world position and view direction, and your channel).
- **To whom.** Other players in the same world on your local network, or — if you configure one —
  the relay server you name.
- **Not end-to-end encrypted over a relay.** The relay carries the audio and does not decode it, but
  it can observe it. The Voice tab says so before you join.
- **Turn off.** Withdraw consent from the Voice tab, which also turns the feature off.

## Cosmetic sync (opt-in)

- **When.** With a game session, when cosmetic sync is enabled.
- **Sends.** Catalogue **ids** only — the id of your equipped cape, hat and pet, plus a per-session
  peer id and the display name you choose. Never a gamertag, account id or xuid.
- **To whom.** Other players on the same local network (multicast), or through the relay you
  configured.
- **Turn off.** Disable cosmetic sync in the Cosmetics panel.

## What is never sent

- Your sign-in tokens or account data (they stay on the device).
- Your gamertag or account id in any analytics or update request.
- Your world coordinates, except inside a voice advertisement while voice chat is on.
- Anything at all if the relevant feature is off or has no endpoint configured.

## Copy Debug Info

When you copy a debug bundle to share in a bug report, tokens, authorization headers, gamertags and
server addresses are redacted before the text reaches the clipboard. Streaming-safe mode additionally
masks world coordinates and server addresses in overlays and logs.
