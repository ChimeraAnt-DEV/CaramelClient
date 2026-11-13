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

## CaramelClient — Rust .universe/.singularity engine (PR #178, branch `feat/caramel-rebrand-and-ui`)
- The sweetloader submodule already carried the Rust workspace `app/src/main/cpp/sweetloader/rust/`
  (`universe-core/-jni`, `singularity-core/-jni`, `caramel-cli`) but **nothing built or shipped it**.
  Now `:app:buildRustEngine` (in `app/build.gradle`) cross-compiles for arm64-v8a and copies
  `libcaramel_universe.so` / `libcaramel_singularity.so` into `jniLibs`; it **skips with a warning**
  when cargo/NDK is absent, so a Java-only build still works. `*.so` is gitignored → CI MUST
  install the Rust toolchain (`rustup target add aarch64-linux-android`), which `android.yml` and
  `build.yml` now do; `android.yml` also runs `cargo test --workspace`.
- JNI owners: `org.chimeramc.client.universe.UniverseLoader`, `.singularity.SingularityEngine`;
  both fail-closed (return an "unavailable" answer on a host JVM instead of
  `UnsatisfiedLinkError`), proguard-kept, and covered by `UniverseLoaderTest` /
  `SingularityEngineTest`. `JniBindingParityTest` now scans the Rust `.rs` sources too.
- Counts as of this branch: `cargo test --workspace` 164, Android suite **3663/0/0/1 skipped**,
  lint green, `validate_release_content.py` green. Dedicated log: `.openhands/memory/2026-11-13.md`.
- **Only the loader/plumbing exists.** The listed engine features (render hooks, ragdoll, shader
  translation, custom blocks/dimensions, `.singularity` noise/mesher/fluid pipeline) and the
  perf targets (500 ms launch, FPS, 60 FPS, <200 MB) are NOT implemented or measurable without a
  device. The UI-pack replacement is blocked: no pack was provided.
