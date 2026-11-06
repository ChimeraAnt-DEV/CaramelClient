# FreeHands — project memory

FreeHands = a fork of OpenHands Agent Canvas (`Lynxifyy/FreeHands`) used as a
coding app with DeepSeek V4.1 Flash, plus a Capacitor Android shell and a
forked agent-server SDK (`Lynxifyy/software-agent-sdk-2.0`).

## Branches and PRs
- Long-lived feature branch: `feat/discord-automations-and-bot-connect`
  (local ref `feat-discord`). It carries all the app-bundle work.
- Open PRs: FreeHands **#5** (draft, ready to merge). SDK **#2** is **merged**
  (merge commit `d0da07d`); the FreeHands SDK pin now points at that merged SHA.
- `main` has **no branch protection**, so the red fork-only checks do not block
  a merge.
- PR #4 (`feat/ui-preview-critique-tools`) **merged** to main (`840934a`).
  #4 and #5 were independent siblings off `e78c813` that both added client tools
  at the same seams, so #5 needed a merge of main first (merge commit `fc63fa24`).
  Conflicts were all "keep both": action/type-guard/base unions, the adapter
  `client_tools` list, the websocket dispatch, action titles, client-tool result
  prefixes, and `translation.json` (union of both branches' keys — merge it
  programmatically by parsing each side as JSON, never by hand).

## Git auth (important)
- `$GITHUB_TOKEN` / `$Github` are read-only for git push on Lynxifyy repos (403).
- `$GithubLynxifyy` has full `repo` scope: use it for `git push` and for the
  GitHub API (`create_pr` tool uses the read-only token and 403s — create PRs
  with a `curl` POST to the API instead).

## Fork limitations
- **Issues are disabled** on both forks (`has_issues: false`), so the
  `Validate PR description` check can never pass (it requires a linked
  `ready-for-dev` issue). `Build & Push` also fails (pushes OpenHands' ghcr).
- The `HUMAN:` section of the PR body is reserved for the human author; agents
  must not fill it, which also fails the description check by design.
- CI that *does* pass on the fork and is worth watching: `test-and-build
  (ubuntu)` and `(windows)`, `Build Android APK` (the only verifier of the
  Kotlin changes), `Check SDK version consistency`, `pr-title`. All green on
  `feat/discord-automations-and-bot-connect` @ `b2f6582`.

## Build / verify
- `npm ci` then `npm run lint` (0 errors), `npm test` (~850 files / ~9.2k tests,
  ~11 min — background it), `npm run build`, `npm run build:lib`.
- The pre-commit hook (lint-staged → tsc) needs generated files first:
  `npm run make-i18n` and `npx react-router typegen`. Do not run `make-i18n`
  concurrently (it deletes `public/locales` and races).
- Android: no Java/Gradle in the dev container — Kotlin changes are only
  verified by the CI `Build Android APK` job.

## Secrets
- Discord bot token lives only as the `DISCORD_BOT_TOKEN` secret (never in
  source). Public bot application id is `1568005572769480704`.
