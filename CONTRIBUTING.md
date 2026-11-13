# Contributing

Thanks for helping. This page is the ten-minute setup and the rules a change has to meet before it
can land.

## Ten-minute setup

```bash
# 1. Clone with the two native submodules the build needs.
git clone --recurse-submodules https://github.com/ChimeraAnt-DEV/CaramelClient.git
cd CaramelClient

# Already cloned without them?
git submodule update --init --recursive
```

The native build needs `app/src/main/cpp/sweetloader` and `app/src/main/cpp/libHttpClient`. If a
build fails with a missing native source, run `git submodule sync && git submodule update --init
--recursive` and then `./gradlew clean` — a stale native build output can otherwise persist.

```bash
# 2. Toolchain: JDK 21 and an Android SDK (compileSdk 36, NDK 28.x, CMake 3.22.1).
#    Point the build at your SDK:
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 3. The fast checks.
./gradlew :app:compileDebugJavaWithJavac   # Java only, seconds
./gradlew :app:compileDebugKotlin          # Kotlin, also compiles Java
./gradlew :app:testDebugUnitTest           # the JVM unit suite
./gradlew :app:assembleDebug               # full APK, native build included
```

There is no emulator or KVM in most dev containers, so the unit suite is the primary gate. A change
that needs a device is verified against [`docs/DEVICE_CHECKLIST.md`](DEVICE_CHECKLIST.md).

## Rules a change must meet

1. **One concern per pull request, one commit per step.** A phase is a series of small commits, not
   one large one.
2. **Run `./gradlew :app:testDebugUnitTest` before you commit.** A red suite is never a reason to
   commit; fix it or say why it cannot be fixed in this change.
3. **No mocks in tests where a real code path can be exercised.** A test that mocks the thing it is
   checking proves nothing. Prefer a pure class plus a fake seam (`GameBackend`, `VoiceLink`,
   `DataSource`) over a mocking framework.
4. **Honesty over polish.** A feature that cannot work on a build must say so (a badge, a scope
   note, an "awaiting game data" state) rather than silently doing nothing. This is a project rule,
   not a nicety: a readout that fabricates a value is the same class of bug as a crash.
5. **No new hardcoded colour or text in a layout.** `LayoutResourceTest` fails the build on a raw
   `#RRGGBB` or a raw `android:text`. Use a token or `@string`.
6. **One owner per rule.** An API-level gate goes in `ApiLevels`, a nav boundary in `NavLayoutMode`,
   a module classification in `ModIds`. A second copy of the rule is how the two drift.
7. **Keep a text-bearing view `wrap_content`.** A fixed `layout_height` on a `TextView` clips its
   label at a large font scale; use `wrap_content` plus `android:minHeight`.

## What CI checks

[`.github/workflows/build.yml`](../.github/workflows/build.yml) checks out with submodules, builds
a debug APK, and fails on a broken build. The unit suite includes several tests that scan the real
repository, so they fail on a source-level mistake the compiler cannot see:

- `JniBindingParityTest` — every Java `native` method has a C++ export and the reverse.
- `LayoutResourceTest`, `SpacingScaleTest`, `TypeRoleTest`, `RadiusScaleTest` — the layout, spacing,
  type and radius scales.
- `Api28CompatTest` — no call that needs a newer API than `minSdk 28`.

## Reporting a bug

Use the bug report template and include the **Copy Debug Info** output from Settings → About (or
Diagnostics): app version, device model, Android version, ABI, and the hook/backend status. Without
it a report usually cannot be reproduced.

## Translating

The default strings live in `app/src/main/res/values/strings.xml`; a locale is a
`values-<locale>/strings.xml`. Mark a brand or technical string `translatable="false"` rather than
leaving it untranslated. See [`docs/index.md`](index.md) for the user guide.

## License

By contributing you agree your work is licensed under the project's Apache-2.0 license.
