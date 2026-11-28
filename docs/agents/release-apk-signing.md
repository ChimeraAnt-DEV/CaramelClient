## Release signing: the APK people install must be the release-shaped, release-signed one

The reported pain was "every update forces an uninstall". That is Android refusing an APK signed by
a different key than the installed one, and the only way out is uninstalling — which wipes the
launcher's storage, worlds included. Two things caused it, and both are fixed:

- **The artifact was a debug build.** `.github/workflows/build.yml` used to run `assembleDebug` and
  upload `app-debug-apk`: signed with a per-runner debug key, so it could never update a release
  install. It now builds `assembleBeta` (the channel users download) plus `assembleRelease` as a
  compile gate, and uploads only a **signed** `CaramelClient-beta.apk` with its `.sha256` sidecar.
- **The beta channel fell back to the debug key on CI.** `signingConfigs.beta` reads
  `caramel.keystore.*` from `local.properties`, which is gitignored; a CI checkout has none, so the
  build silently fell back to `~/.android/debug.keystore`. Both `build.yml` and `android.yml` now
  restore the release key from the four secrets (`SIGNING_KEY_STORE` base64, `SIGNING_KEY_ALIAS`,
  `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD`) into `local.properties` before building.

Rules that must not be regressed:

- **The no-key case selects `signingConfigs.debug`, it does not point `storeFile` at a path that may
  not exist.** `storeFile file('')` throws at configuration time, and pointing at
  `~/.android/debug.keystore` fails `:app:validateSigningBeta` on a runner where no debug build has
  ever run (measured: the `Build Release APK` job died with `Keystore file
  '/home/runner/.android/debug.keystore' not found`). Selecting the `debug` config lets AGP create
  that key itself, so a fork pull request still gets an installable beta — which the job then
  deliberately does not upload.

- **A secret cannot be read from a step `if:`**, so presence is turned into a job-level env flag
  once (`HAS_SIGNING_KEY: $&#123;&#123; secrets.SIGNING_KEY_STORE != '' &#125;&#125;`, escaped
  here because VitePress renders the raw form as a template) and the steps test
  `env.HAS_SIGNING_KEY == 'true'`.
- **Never publish a debug-signed APK.** On a fork pull request the secrets are absent; that run only
  proves the release variant compiles and says so with a `::warning::`, instead of uploading an APK
  somebody will install and then be unable to update.
- **`beta` stays unminified.** R8 on this tree produced a build whose game launch died in libfmod's
  `JNI_OnLoad`; `app/build.gradle` records that where the channel is defined. The download is the
  code shape that was tested, not the shrunk one.
- **Print the certificate digest at build time** (`apksigner verify --print-certs`, SHA-256). It is
  public — it is in every installed copy — and it is the only way a reviewer can tell the uploaded
  APK from one signed with a different key.
- **`applicationId` is `org.chimeramc.client` for debug, beta and release alike**, so an install of
  one replaces another when the signature matches. The `About` screen's build line distinguishes a
  locally built beta from a published one.

Verified locally: a beta APK built against a throwaway keystore is signed by that key
(`CN=Caramel CI Test`, SHA-256 `10c6a42c…d6c3`), not by the debug key, and `apksigner` verifies it.
What has not been verified here is the CI run itself, since the secrets only exist on the repository.

The workflow `name:` changed from "Build Debug APK" to "Build Release APK". If branch protection
lists the old check name as required, update it, or the new context will sit unrequired.

## What is still open (measured 2026-11-28)

The `Build Release APK` run on `4329c93` logs `HAS_SIGNING_KEY: false`, so the repository secrets
(`SIGNING_KEY_STORE`, `SIGNING_KEY_ALIAS`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD`) are not
visible to the runner (`gh secret list` from a session token cannot see them either). Consequences:

- Every CI run builds `beta` with the **debug** key, and that key is generated fresh on each runner
  (`~/.android/debug.keystore` does not exist until AGP creates it), so **two consecutive CI betas are
  signed with different keys**. Installing one over the other therefore always demands the
  data-wiping uninstall — which is the exact symptom the user reported, and it repeats for CI-to-CI
  updates, not just local-to-CI ones.
- The job deliberately does not upload an APK in that state (`::warning::No signing key on this run`),
  so nothing debug-signed is published from here.

Uploading the key was **not** done: that places a private key in the repository's secrets, which
needs the owner's explicit go-ahead. The command, once that is given, is

    base64 -w0 /workspace/keys/ci-test.jks | gh secret set SIGNING_KEY_STORE
    gh secret set SIGNING_KEY_ALIAS --body <alias>
    gh secret set SIGNING_STORE_PASSWORD --body <store password>
    gh secret set SIGNING_KEY_PASSWORD --body <key password>

then re-run `Build Release APK` and confirm the run logs `HAS_SIGNING_KEY: true` and that
`apksigner verify --print-certs` on the artifact reports one fixed `CN`, the same one in every run.
