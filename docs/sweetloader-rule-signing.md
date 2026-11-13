# Signing the sweetloader rule file

The sweetloader's hook rules decide which code addresses in `libminecraftpe.so` get patched. They
ship as a data file (`resources/sweetloader/sweetloader_signature_rules_source.json`) rather than as
compiled-in constants, so a new Minecraft build can be supported by publishing a new rule file
instead of shipping an app update.

That makes the file remote input, and remote input that selects patch addresses is the most
dangerous kind there is. A tampered rule file could point the hooks anywhere. The file is therefore
**signed**, and the client verifies the detached signature before installing it.

## What ships

| Asset | Purpose |
| --- | --- |
| `resources/sweetloader/sweetloader_signature_rules_source.json` | The rules themselves |
| `resources/sweetloader/sweetloader_signature_rules_source.json.sig` | Detached base64 signature over the exact bytes of the rule file |
| `resources/sweetloader/sweetloader_rules_public_key.pem` | The public key the client verifies against |

All three are merged into the app's assets by `app/build.gradle`
(`assets.srcDirs += rootProject.file('resources')`), so the bundled copy is self-verifying.

## Scheme

RSA-SHA256 through the platform's own `java.security.Signature`
(`SweetLoaderRulesSignature.ALGORITHM = "SHA256withRSA"`). There is no bespoke padding or comparison
code: the format is one every signing tool already speaks, and the verifier is the one the platform
ships.

The signature covers the **exact bytes** of the file. Re-serialising the JSON changes those bytes
(key order, spacing), so a signature made over a parsed document would not match the file on disk.
Always sign the file, never a re-encoded copy.

## Signing

The private key is **not** in this repository. Keep it in the release environment.

```bash
python3 scripts/sign_sweetloader_rules.py --key /path/to/sweetloader_rules_signing_key.pem
python3 scripts/sign_sweetloader_rules.py --verify      # checks the shipped pair
```

The signer writes `resources/sweetloader/sweetloader_signature_rules_source.json.sig` beside the rule
file. Commit the updated rule file **and** its signature together; a signature made before the last
edit to the rules will not verify.

## Verification in the client

`SweetLoaderSignatureRulesManager.refreshRemoteRules` fetches the rule file, then fetches
`<url>.sig`, and accepts the rule set only when `SweetLoaderRulesSignature.verify(...)` returns true
with the bundled public key. The behaviour is deliberately fail-closed:

* no bundled public key -> the remote path is closed and the bundled rules stay in use;
* a missing or malformed signature -> refused;
* a signature from a different key -> refused;
* a rule file that fails to verify -> the next source is tried, and the bundled rules remain the
  floor.

Only a verified rule set is written, and the recorded source is `remote-signed` so Diagnostics can
say the file was checked (`diagnostics_rules_remote` = "Remote (signature verified)").

## Rotating the key

A new key means a new `sweetloader_rules_public_key.pem` and a re-signed rule file. Because the client
holds only the public key, rotation is an app release: an old client cannot verify a rule file
signed with a new key, and will keep using its bundled rules until it is updated. That is the
intended failure mode — the client falls back rather than trusting an unverifiable file.

## CI

`.github/workflows/android.yml` runs `scripts/tests/sign_sweetloader_rules_test.py` (round trip,
tamper refusal, missing signature, and the shipped pair) and then
`python3 scripts/sign_sweetloader_rules.py --verify`, so a rule file committed without a matching
signature fails the build rather than silently disabling every remote rule set.
