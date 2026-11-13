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

## FreeHands agent-tools workstream (different repo: `Lynxifyy/FreeHands` + `Lynxifyy/software-agent-sdk-2.0` + litellm)
Phases done two per PR then merged. #6 live task list + in-app settings; #7 model-aware thinking
status + Oli presence; #8 Discord in Recommended automations + bot-token modal; #9
`always_keep_sandbox_alive`; SDK #3 cloud_terminal + file_builder + builtin `oli` subagent;
litellm #2 deepseek-v4 streaming metadata. Detail: `.openhands/memory/2026-11-06.md`,
`2026-11-09.md`, `2026-11-10.md`. SDK/litellm venvs are ad hoc (`uv venv /tmp/sdkvenv` +
`uv pip install -e <pkg> pytest`).

## Git auth (important)
- `$GITHUB_TOKEN` / `$Github` are read-only for git push on Lynxifyy repos (403),
  **but on `ChimeraAnt-DEV` it is `Lynxifyy` with push rights** — the `origin` remote already
  embeds it, so a plain `git push` works.
- Set `git -c user.name=Lynxifyy -c user.email=297587416+Lynxifyy@users.noreply.github.com`
  for CaramelClient commits (no co-authors, no AI mentions in PRs).
- `$GithubLynxifyy` has full `repo` scope: use it for `git push` and for the
  GitHub API (`create_pr` tool uses the read-only token and 403s — create PRs
  with a `curl` POST to the API instead).
- Merge convention: PRs are **squash**-merged. Use the async API
  `PUT /repos/{o}/{r}/pulls/{n}/merge-async` with `{"merge_method":"squash","sha":"<head>"}`
  (returns 202 + a UUID; poll `.../merge-async/{uuid}` until `status` is `merged`).
- PR titles must be conventional (`feat:`/`fix:`/…) or the `pr-title` check fails.
  A wrong title is fixable with `PATCH /pulls/{n}` after opening.
- PR #7 CI: `test-and-build (ubuntu)` + `(windows)`, `Build Android APK`,
  `pr-title`, `check-pr-artifacts` all green. Fork-only reds: `Validate PR
  description` (issues disabled) and `Build & Push` (pushes OpenHands' ghcr).

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

## GlowberryClient (ChimeraAnt-DEV/GlowberryClient) — Android launcher
- **Native code lives in a git submodule**: `app/src/main/cpp/preloader`
  (remote `ChimeraAnt-DEV/preloader-android`, working branch `lynxifyy/preloader-hardening`).
  A change under `app/src/main/cpp/preloader/**` must be committed **inside the submodule**,
  pushed, then the parent repo must commit the bumped submodule pointer. `git add` of a
  submodule path in the parent stages the gitlink, not the file.
  **Always check the pin is not behind the submodule's `main`** (`git -C <sub> merge-base
  --is-ancestor origin/main HEAD`). A `feat/*` submodule branch can silently skip a large
  `main` PR: the pin here was 13 commits behind main (frame-boundary clock, seqlock fixes,
  Optifine device/thread fixes, atomic sig-cache write, snapshot channel + TSAN). Merge
  `origin/main` into the working branch, resolve, run `tests/cosmetics/run_tests.sh`, push the
  submodule, then bump the parent pin. Merging main can also add JNI exports with no Java
  declaration → `JniBindingParityTest` fails with `stale-export`; add the Java `native` methods
  (`app/src/main/java/.../launcher/preloader/BerryApi.java`).
- **Build/test**: `JAVA_HOME=/workspace/tools/jdk-21.0.13+9 ANDROID_HOME=/workspace/android-sdk
  ./gradlew :app:testDebugUnitTest` (~3843 tests, ~25s, all green). Targeted:
  `--tests "org.chimeramc.client.<pkg>.*"`. Native host tests:
  `bash app/src/main/cpp/preloader/tests/cosmetics/run_tests.sh` (needs g++ + zlib + nlohmann).
- **BerryAPI ABI guard**: `python3 scripts/verify_berryapi_abi.py` (test:
  `scripts/tests/verify_berryapi_abi_test.py`). Pins the 14 dlsym'd `BERRY_*` symbols and the
  seam/replay/governor ABI numbers against the bundled `libBerryAPI.so`. **`BERRY_Inject` and
  `BERRY_IsAttached` return `int` (0/1), not C++ `bool`** — BerryAPI's header says a 1-byte bool
  return leaves the upper result register undefined. Official BerryAPI: `Lynxifyy/BerryAPI`
  (clone to /tmp to read headers; ABI 8, `BERRY_ABI_VERSION`).
- **Module catalogue**: `PvpModuleCatalog` = 200 entries (38 `link()` + 162 `add()`). Honest split:
  **50 reachable/toggleable**, **112 disabled/badged** (52 `ENGINE_HOOK` + 60 `SERVER_DECIDED`).
  Pinned by `PvpModuleScopesTest.theBadgeCountsMatchTheAuditNote`. The 52 ENGINE_HOOK need a
  **complete world seam** (the preloader binds only `get_local_player`; the stripped
  `libminecraftpe.so` gives no entity-list/level anchor) + a signature pack for the active build.
  The 60 SERVER_DECIDED cannot be enabled by any client code.
- **Setup wizard**: `SetupWizardActivity` is a self-sizing popup over `MainActivity`; each step
  shows only its own block; finishing calls `finishWizard()` → `MainActivity` (never a controller
  screen). Test `SetupWizardLastTabsTest` pins it.
- **Git author for this repo's commits**: `Lynxifyy <297587416+Lynxifyy@users.noreply.github.com>`,
  no co-authors, no AI mentions in PRs. Push with `$GithubLynxifyy` (has repo scope; `create_pr`
  tool's token is read-only here → use a `curl` POST to the API).
- **BerryAPI cosmetic C ABI (bound, PR #176 + preloader PR #33)**: BerryAPI owns the cosmetics
  service; the preloader mirrors `berry/cosmetic_c.h` (`BerryCosmeticMirror.hpp`) and checks
  `size`/`abi_version` before a pointer; `BerryCosmetics` (core.cosmetics) pushes the launcher's
  equipped set into BerryAPI's loadout. **The bone half is not done** — `set_player_model_hook`
  needs the preloader to resolve the per-build player-render target itself, so nothing renders yet;
  the JNI entries only report the hook state. `NoSilentCatchTest` allows an empty catch only when
  the body contains the exact substring `best-effort`.
- **Two Android CI gates that fail locally-silent changes** (both run in `android.yml` before the
  Gradle build, so they fail fast): `python3 scripts/check_ui_hygiene.py` fails on *new*
  `findViewById` calls beyond a per-file baseline — read the view from the generated ViewBinding
  instead (or `--update` with a reason). `./gradlew :app:lintDebug` fails on a new
  `MissingSuperCall` — do not override `onBackPressed()` without calling `super`; register an
  `OnBackPressedCallback` on `getOnBackPressedDispatcher()` instead. Full unit suite is
  **3844 tests, 0 failures, 1 skipped** (~25s warm).

## CaramelClient (ChimeraAnt-DEV/CaramelClient) — launcher + SweetLoader
- **The native engine is a git submodule at `app/src/main/cpp/sweetloader`, and the submodule repo
  was renamed to `ChimeraAnt-DEV/SweetLoader`** (the old `ChimeraAnt-DEV/preloader-android` name
  301-redirects, so old remotes still work). The parent must pin a commit that is on the
  submodule's `main`: a `feat/*` tip can be unmerged and unreachable, and then a clean checkout
  depends on that branch still existing. `scripts/check_submodules.sh` gates this in CI.
- **`.universe` and `.singularity` are the only mod formats** (`.AntEgg`/`.chimerapack`/`.levipack`
  are gone). Rust workspace `sweetloader/rust/`: `universe-core/-jni` (manifest/archive/signature/
  loadorder/permission/config/report/runtime/fault/version/patterns), `singularity-core/-jni`
  (noise/biome/chunk/mesher/fluid/physics/structure), `nethernet-core/-jni`, `caramel-cli`.
  `:app:buildRustEngine` cross-compiles for arm64-v8a and **skips with a warning** when cargo is
  absent, so CI MUST install the toolchain (`rustup target add aarch64-linux-android`); `android.yml`
  also runs `cargo test --workspace`. **There is no cargo/rustc in the dev container**, so the Rust
  suite can only be verified in CI.
- JNI owners: `org.chimeramc.client.universe.UniverseLoader`, `.singularity.SingularityEngine`,
  both fail-closed on a host JVM instead of throwing. `JniBindingParityTest` also scans the `.rs`
  sources, so a new Rust export needs a matching Java `native` method.
- **The signature scan refuses an ambiguous pattern** (`src/pl/memory/Signature.cpp`): it used to
  take the first hit, but `pauseMenuDtorSig` is a bare prologue that matches **183 times** in
  1.26.62. An ambiguous signature now resolves to 0 with a warning. **The rules file is signed and
  the private key is NOT in the repo** (`scripts/sign_sweetloader_rules.py --key <pem>`, and
  `android.yml` runs `--verify`), so it must not be hand-edited — new build rules come from the
  release environment. Shipped rules top out at 1.26.50.99.
- **UI**: the UI pack is a Compose **Desktop** project and is ported to Android Compose under
  `app/src/main/java/org/chimeramc/client/ui/compose/caramel/` (theme, glass via Haze blur,
  buttons, controls, labels, loader icons, dropdowns). Not a drop-in: classpath fonts and hover
  states need adapting, and the pack's dropdown used a constant trigger height.
- **Git auth (2026-11)**: `$GITHUB_TOKEN` has **full repo scope** here — `git push`, REST,
  GraphQL and the async merge all work. `$GithubLynxifyy` is **no longer injected** (401); do not
  reference it. `$OH_GIT_CREDENTIAL_ACCESS_TOKEN_GITHUB` is git-credential-helper only.
  **The submodule remote has no credential helper** — push it with
  `git -c credential.helper='!f() { echo username=x-access-token; echo password=$GITHUB_TOKEN; }; f' push`.
- **PR flow**: `create_pr` opens a **draft** and the merge API refuses drafts; REST `PATCH
  {"draft":false}` does NOT clear it — use the GraphQL `markPullRequestReadyForReview` with the
  `node_id`. Then `PUT /pulls/{n}/merge-async {"merge_method":"squash","sha":"<40-char head>"}`
  (a short sha is rejected) and poll `.../merge-async/{uuid}`. Conventional PR titles only.
- **Verify locally**: `JAVA_HOME=/workspace/tools/jdk-21.0.13+9 ANDROID_HOME=/workspace/android-sdk
  ./gradlew :app:testDebugUnitTest` (**3761 tests, 0 failures, 1 skipped**). `:app:assembleDebug`
  is the only thing that compiles the NDK C++ (`Signature.cpp` is not host-compiled). Also
  `bash sweetloader/tests/cosmetics/run_tests.sh`, `scripts/check_ui_hygiene.py`,
  `scripts/verify_berryapi_abi.py`, `scripts/check_submodules.sh`, `scripts/validate_release_content.py`.
- **Device-only work cannot be measured here**: no Android device/emulator, so FPS, launch-time,
  memory, APK-version checks and any hook/scanner behaviour against a real `libminecraftpe.so`
  cannot be executed. Say so rather than claiming a measurement. Most of the remaining
  "engine hook" features need a game-side anchor (entity list / level object) the stripped
  `libminecraftpe.so` does not expose — `docs/agents/update-list-audit-*.md` records this per module.
