## SweetLoader signature rules (sweetloader.SweetLoaderSignatureRulesManager)
- Rule sources resolve in a fixed order: the host's **own** copy is authoritative, Levi's upstream repo is the fallback, and the bundled asset (`resources/sweetloader/sweetloader_signature_rules_source.json`) is the last resort for offline launches. Keep the bundled snapshot refreshed тАФ it must contain every rule the live source has (e.g. the 1.26.50 rule), or an offline launch will reject a version the online source would accept.

### What the gate does and does not cover (2026-11-28)
- The gate only covers the byte-pattern hooks. The EGL present boundary, the RTTI/vtable seams and
  the ImGui menu shell need no rule and are installed before `GameHookRulesConfigured()` is even
  consulted (`InitGameHooks` / `PumpGameHookInstall` in `src/pl/runtime/GameHooks.cpp`). On an
  uncovered build the menu draws; the modules behind it report themselves unavailable.
- Newest builds seen while this was written: **1.26.66** (26.66, 2026-11-19) and **1.26.70.26**
  (beta, 2026-11-17). The `NativeLibraryVersionGate` thresholds are at-least comparisons, so those
  builds keep the 26.60 library set (PairIP mapped, `libHttpClient.Android.so` / `libmaesdk.so` /
  `libPlayFabMultiplayer.so` loaded from the APK) and `BedrockTransport` keeps them on NetherNet;
  `NativeLibraryVersionGateTest` and `BedrockTransportTest` pin both.
- The pinned default in `MsftAuthManager.DEFAULT_GAME_VERSION` is `1.26.66`. It is only a fallback:
  the preparer sets the active instance's version for every launch.

### The shipped ceiling is 1.26.50.99 (do not fake a newer rule)
- The bundled rules (revision 7) cover Bedrock builds only up to **1.26.50.99**. Nothing above that
  is covered: `PreloaderRules.newerThanRules` reports "newer than the rules" and the launcher warns
  (GAME_NEWER_THAN_RULES) rather than handing the sweetloader a rule that matches nothing. The
  RTTI-slot seams (player render slot 17, `ClientInstance` local-player slot 31) survive the range
  because they resolve by name, so those features are not tied to the byte rules; the menu/HUD
  byte-pattern hooks are what go dark. `PreloaderRulesTest.theShippedRulesStopAtOneTwoSixFiftyNinetyNine`
  pins the ceiling.
- Closing the gap for 1.26.51–1.26.62 needs two things that cannot be produced inside this repo:
  1. **The real `libminecraftpe.so`** for the build, to derive the byte patterns with
     `tools/gen-rules` (which runs `tools/verify-binary`). No game binary is committed and none can
     be fetched here. `docs/binary-analysis-26.44-26.62.md` records the offsets the analyzer already
     derived for that range, but the **byte signatures** the menu/HUD hooks need were not captured
     there.
  2. **The rules signing private key**, to re-sign the file. Only the public key
     (`resources/sweetloader/sweetloader_rules_public_key.pem`) is in the repo; a remote rule set is
     accepted only after `SweetLoaderRulesSignature.verify` passes, and the bundled copy is written
     only when `hasValidRules` holds. The bundled copy is trusted without its own `.sig` check at
     runtime, so a hand-added unsigned rule would not be *refused*, but the shipped `.sig` would no
     longer match the file and CI's `scripts/sign_sweetloader_rules.py --verify` would fail.
- The supported route is out of band: run `tools/gen-rules --version 1.26.<build> --apk <path>` on a
  machine with the APK, re-sign with the private key, bump `revision`, regenerate the
  `docs/sweetloader-compatibility.md` table, and refresh the bundled asset. The remote refresh
  (`SweetLoaderSignatureRulesManager.refreshRemoteRules`) delivers such a file to existing installs
  without an app update.
