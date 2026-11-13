# Project memory

Index only. Detail and session notes live in the daily logs (`YYYY-MM-DD.md`).

> Other repos this workspace has touched: **FreeHands** (`Lynxifyy/FreeHands`, a fork of OpenHands
> Agent Canvas) + `Lynxifyy/software-agent-sdk-2.0` + litellm. Its notes are in the 2026-11-06 /
> 11-09 / 11-10 daily logs. Nothing below applies to it.

## CaramelClient (`ChimeraAnt-DEV/CaramelClient`) — Android launcher + SweetLoader

The repo was called **GlowberryClient** before; the package is `org.chimeramc.client` either way.

### Native engine (git submodule `app/src/main/cpp/sweetloader`)
- Submodule repo is `ChimeraAnt-DEV/SweetLoader` (the old `preloader-android` name 301-redirects).
  **The parent must pin a commit that is on the submodule's `main`** — a `feat/*` tip can be
  unmerged, and then a clean checkout depends on that branch surviving. `scripts/check_submodules.sh`
  gates it. Check with `git -C <sub> merge-base --is-ancestor <pin> origin/main`, and note a pin can
  be *behind main by one* while still looking "on a branch" (`git log --oneline <pin>..origin/main`).
- **`.universe` and `.singularity` are the only mod formats** (`.AntEgg`/`.chimerapack`/`.levipack`
  are gone). Rust workspace `sweetloader/rust/`: `universe-core/-jni`
  (manifest/archive/signature/loadorder/permission/config/report/runtime/fault/version/patterns),
  `singularity-core/-jni` (noise/biome/chunk/mesher/fluid/physics/structure), `nethernet-core/-jni`,
  `caramel-cli`. `:app:buildRustEngine` cross-compiles arm64-v8a and **skips with a warning when cargo
  is absent**, so CI MUST install it (`rustup target add aarch64-linux-android`); `android.yml` also
  runs `cargo test --workspace`. **The dev container has no cargo/rustc**, so the Rust suite is
  CI-only.
- JNI owners: `org.chimeramc.client.universe.UniverseLoader`, `.singularity.SingularityEngine`; both
  fail closed on a host JVM instead of throwing. `JniBindingParityTest` also scans the `.rs` sources,
  so a new Rust export needs a matching Java `native` method (else `stale-export`).
- **A signature that matches more than once is refused** (`src/pl/memory/Signature.cpp`): it used to
  take the first hit, but `pauseMenuDtorSig` is a bare prologue matching **183 times** in 1.26.62.
  Ambiguous → resolves to 0 with a warning. **The rules file is signed and the private key is NOT in
  the repo** (`scripts/sign_sweetloader_rules.py --key <pem>`; `android.yml` runs `--verify`), so it
  must not be hand-edited. Shipped rules top out at 1.26.50.99.
- **BerryAPI**: ABI guard `scripts/verify_berryapi_abi.py` (+ its test) pins the 14 dlsym'd `BERRY_*`
  symbols and the seam/replay/governor ABI numbers against the bundled `libBerryAPI.so`.
  **`BERRY_Inject` / `BERRY_IsAttached` return `int` (0/1), not C++ `bool`** — a 1-byte bool return
  leaves the upper result register undefined. Upstream is `Lynxifyy/BerryAPI` (clone to /tmp for
  headers; ABI 8). Cosmetic C ABI is bound (PR #176 + SweetLoader #33) but **the bone half is not
  done**: `set_player_model_hook` needs the preloader to resolve the per-build player-render target
  itself, so nothing renders yet and the JNI entries only report hook state.
- **Module catalogue**: `PvpModuleCatalog` = 200 entries (38 `link()` + 162 `add()`); honestly
  **50 reachable/toggleable** and **112 disabled/badged** (52 `ENGINE_HOOK` + 60 `SERVER_DECIDED`).
  Pinned by `PvpModuleScopesTest.theBadgeCountsMatchTheAuditNote`. The 52 need a **complete world
  seam** (the preloader binds only `get_local_player`; the stripped `libminecraftpe.so` exposes no
  entity-list/level anchor) plus a signature pack for the active build; the 60 cannot be enabled by
  any client code.
- **Setup wizard**: `SetupWizardActivity` is a self-sizing popup over `MainActivity`; each step shows
  only its own block; finishing calls `finishWizard()` → `MainActivity` (never a controller screen).
  Pinned by `SetupWizardLastTabsTest`.

### UI
- The UI pack (`Lynxifyy/CaramelClient-UI-pack`) is a Compose **Desktop** app plus a GLFW Dear ImGui
  demo, not an Android library. Its `components/` layer is **already ported** to
  `ui/compose/caramel/` (`CaramelButtons`/`Controls`/`Dropdowns`/`Labels`/`LoaderIcons`, `GlowGlass`,
  `GlowTheme`), improved where it diverged (touch `collectIsPressedAsState` not desktop
  `collectIsHoveredAsState`; explicit imports not `layout.*`; docs explaining *why*). Remaining
  hazards for a naive port: the whole-window `HazeState` blur model, 63 hardcoded `Text("...")`
  literals with no `strings.xml`, `(size / 2).dp` truncating `Int` division, no wrapper, no tests,
  Kotlin 2.0.21/Compose 1.7.3 vs the client's 2.3.0/BOM 2026.01.01. The pack's preview apple is a
  *different* asset from the launcher art. Full review: `docs/agents/ui-pack-review.md`.
- **Animated values that cross zero belong on `offset`, never `padding`.** `Modifier.padding` throws
  `IllegalArgumentException: Padding must be non-negative`; a drift of `(value - 0.5f) * 6f` is
  negative on the first frame, so `LaunchChangelogCard` threw on **every** launch.
  `check_ui_hygiene.py` now has a 4th, baseline-free rule enforcing this (only `padding` —
  `size`/`width`/`height` legitimately take computed values).
- **Two Android CI gates fail locally-silent changes** (both run before the Gradle build, so they fail
  fast): `scripts/check_ui_hygiene.py` fails on *new* `findViewById` beyond a per-file baseline (read
  the generated ViewBinding instead, or `--update` with a reason) and on a runtime `padding` argument;
  `./gradlew :app:lintDebug` fails on a new `MissingSuperCall` — do not override `onBackPressed()`
  without `super`, register an `OnBackPressedCallback` instead.

### Git / PR flow
- `$GITHUB_TOKEN` has **full repo scope here** — `git push`, REST, GraphQL and the async merge all
  work. `$GithubLynxifyy` is **not injected** (401) — do not reference it.
  `$OH_GIT_CREDENTIAL_ACCESS_TOKEN_GITHUB` is git-credential-helper only. The parent `origin` remote
  embeds a working `ghu_...` credential, so a plain `git push` works; **the submodule remote has no
  credential helper** — push it with
  `git -c credential.helper='!f() { echo username=x-access-token; echo password=$GITHUB_TOKEN; }; f' push`.
- **`git push` can be rejected GH007 "publish a private email address"** when the configured
  `user.email` is private on GitHub. Fix: set the repo-local identity to the noreply address
  (`297587416+Lynxifyy@users.noreply.github.com`) then
  `git rebase origin/main --exec 'git commit --amend --no-edit --reset-author'`.
- Commits here: `Lynxifyy <297587416+Lynxifyy@users.noreply.github.com>`, no co-authors, **no AI
  mentions in PRs**. Conventional PR titles only or the `pr-title` check fails (fixable with
  `PATCH /pulls/{n}`).
- **PR flow**: `create_pr` opens a **draft** and the merge API refuses drafts; REST `PATCH
  {"draft":false}` does NOT clear it — use GraphQL `markPullRequestReadyForReview` with the
  `node_id`. Then `PUT /pulls/{n}/merge-async {"merge_method":"squash","sha":"<40-char head>"}` (a
  short sha is rejected) and poll `.../merge-async/{uuid}`. `gh` CLI works.

### Verify
- `JAVA_HOME=/workspace/tools/jdk-21.0.13+9 ANDROID_HOME=/workspace/android-sdk ./gradlew
  :app:testDebugUnitTest` (**3761 tests, 0 failures, 1 skipped**; targeted via
  `--tests "org.chimeramc.client.<pkg>.*"`). `:app:assembleDebug` is the only thing that compiles the
  NDK C++ (`Signature.cpp` is not host-compiled).
- `bash sweetloader/tests/cosmetics/run_tests.sh` (host C++), `cargo test --workspace` (needs cargo),
  `scripts/check_ui_hygiene.py`, `scripts/gen_launcher_icons.py --check`,
  `scripts/verify_berryapi_abi.py`, `scripts/check_submodules.sh`,
  `scripts/validate_release_content.py`, `actionlint .github/workflows/android.yml`.
- **Device-only work cannot be measured here**: no device/emulator, so FPS, launch time, memory and
  any hook/scanner behaviour against a real `libminecraftpe.so` cannot be executed. Say so rather
  than claiming a measurement. Most remaining "engine hook" features need a game-side anchor the
  stripped `libminecraftpe.so` does not expose — `docs/agents/update-list-audit-*.md` records this
  per module.
- `version.properties` (`version.name`) is what CI stamps when a shallow checkout has no tags; keep
  it ahead of the last tag or CI builds name a stale version.
- **SweetLoader CI** (`.github/workflows/ci.yml`, 4 gates: host-tests, rust, native-build,
  repo-integrity). Two traps: a CI run with 0 jobs and no annotation = the workflow FILE was rejected
  (diagnose with `actionlint`, not the API); and `set -o pipefail` + `cmd | grep -q` exits 74
  (SIGPIPE) even on a match — dump to a file and grep the file. The Rust tree is rustfmt-normalized;
  keep it so.
