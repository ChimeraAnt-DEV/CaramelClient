# 1.26.60: what actually stops a launch, and what the "sign-in loop" turned out to be

Everything below was measured on a cloud Android 14 arm64 phone (API 34) with the real
`com.mojang.minecraftpe` **1.26.60.4** (versionCode `972606004`) installed and a debug build of the
launcher on top of it. Each claim names the evidence that produced it. Nothing here is inferred from
a protocol guess.

## 1. The launcher never reached sign-in, because the game library would not open

`libminecraftpe.so` in 1.26.60.4 names `libpairipcore.so` in its `DT_NEEDED` list (read out of the
shipped ELF: `libpairipcore.so`, `libGLESv2.so`, `libPlayFabMultiplayer.so`, `libEGL.so`, `libfmod.so`,
`liblog.so`, `libHttpClient.Android.so`, `libmaesdk.so`, `libdl.so`, `libandroid.so`,
`libc++_shared.so`, `libm.so`, `libc.so`). The launcher's gate did not list it, so the load failed
with `dlopen failed: library "libpairipcore.so" not found` — before any sign-in code ran.

**Confirmed, fixed.** `NativeLibraryVersionGate.shouldLoadPairIp` opens at 1.26.60 and the library is
part of `gatedLibraries()`, so the cache check and the loader agree.

## 2. Opening it with `System.load` does not work either

`libpairipcore.so` is a PairIP anti-tamper runtime. Opened from the launcher's own process it aborts:

```
libpairipcore.so: Bad JNI version returned from JNI_OnLoad: 0xffffffff
```

That is `JNI_OnLoad` returning `-1`, i.e. a refusal, not a missing symbol. A dependency, by contrast,
is never `JNI_OnLoad`'d by ART: the linker only maps it. So the launcher maps the file with a raw
`dlopen(path, RTLD_NOW | RTLD_GLOBAL)` from its own native code (`NativeMapper` in
`src/pl/SweetLoader.cpp`) and then opens `libminecraftpe.so` normally.

**Confirmed, fixed.** The launch trace now runs end to end into the game activity:

```
I MinecraftLaunchTrace | Launch screen ready
I GamePackageManager   | Mapped libpairipcore.so before the game library
I MinecraftLaunchTrace | Runtime preparation finished
I MinecraftLaunchTrace | Entering game
```

and `GameActivity` starts the game library:

```
I GameActivity | Looking for library libsweetloader.so
I GameActivity | Found library libsweetloader.so. Loading...
D GameActivity | GameActivity_register
E MCPE        | *** setCachedDeviceId(...)
I SweetLoader | SweetLoader hook install armed; resolving signatures in the background ...
I SweetLoader | caramel_universe: initialized (client version BETA6, abi arm64-v8a)
```

So `libsweetloader.so` does export what `android.app.lib_name=sweetloader` needs, and the AGDK
`initializeNativeCode` entry point is served by `libminecraftpe.so` itself
(`Java_com_google_androidgamesdk_GameActivity_initializeNativeCode` is in its dynamic symbol table).

## 3. A crash on every install, unrelated to the game

The manifest declared two components under `org.chimeramc.client.launcher.util.*` while the classes
live in `org.chimeramc.client.util`. Every `MY_PACKAGE_REPLACED` broadcast therefore died with

```
FATAL EXCEPTION: main
java.lang.RuntimeException: Unable to instantiate receiver
  org.chimeramc.client.launcher.util.WorldBackupBootReceiver: ClassNotFoundException
```

captured from the on-device xCrash tombstone (`crash_logs/tombstone_*.java.xcrash`).

**Confirmed, fixed** (manifest now names the real classes). This is why the launcher looked like it
"lost" its state after each reinstall.

## 4. What is still open (UNKNOWN, with the evidence needed)

After 3 and 2, the game activity starts, the engine's Java runs (`setCachedDeviceId`), sweetloader
arms its hook install, and then the **process is killed ~2 s later with SIGKILL and no tombstone**
(`exits: [{pid, reason: "signaled", status: 9}]` repeated every ~3 s). SIGKILL with no xCrash
tombstone means nothing in-process handled it: it is either the launcher's own exit path or the
system (low-memory kill / `am_kill`). The launcher log buffer is 800 lines and the loop floods it, so
the last lines of the dying process are not retrievable through `logs`.

To settle it, on a 26.60 device:

1. `adb logcat -b all -v threadtime > /tmp/launch.txt` for the whole launch, then look for
   `lowmemorykiller`, `am_kill`, `Killing <pid>`, and the launcher's own `recordLaunchFailure` line.
2. `adb shell dumpsys meminfo org.chimeramc.client` right before the kill, and `free -m`: the game
   maps a 342 MB `libminecraftpe.so` plus its assets.
3. If it is the launcher's exit path, the `LaunchTrace` mark it prints last names the phase.

Nothing in this repo shows a RakNet or NetherNet transport error on 26.60 so far: the launch never
got far enough for the game to open a connection. `BedrockTransport.forVersion` exists and is unit
tested, but no launcher code path has been observed to depend on it yet, and the
`core/nethernet` scaffold stays unwired (see the README in that package).

## 5. Update, 2026-11-27: the launch reaches the title screen, so there is no sign-in loop to fix yet

Re-tested on the same cloud phone (Android 14, arm64) with Minecraft 1.26.60.4 (`972606004`) after
the PairIP ordering fix (`GameLibraryOpener.open` maps `libpairipcore.so` before every bundled
library) and the beta channel's signing change. Three observations, all from `logcat` and a
screenshot, none of them a guess:

1. **The game starts and stays up.** The loading screen finishes, `MinecraftActivity` runs
   `super.onCreate`, and the Minecraft title screen paints (`v26.60` in the corner). No `signaled /
   status 9` exit appears in `GET /v1/phones/{id}/apps/org.chimeramc.client/logs` for the session,
   so the SIGKILL loop in section 4 is gone. The path that fixed it is the library load, not auth:
   before it, `libfmod.so` failed `JNI_OnLoad` and the launch never reached the engine's Java.
2. **Sign-in was not exercised.** The title screen shows "Get started"; no Microsoft account was
   used on the test device, so the reported "sign-in loop while signed in" is still **UNVERIFIED**.
   It may well have been the same launch failure seen from the user's side (the launcher showed
   "Couldn't start Minecraft", which reads as "cannot play while signed in"). Do not claim it is
   fixed until someone signs in on a 26.60 build.
3. **The engine hooks are off on this build, by design.**
   `resources/sweetloader/sweetloader_signature_rules_source.json` (revision 7) covers
   `1.21.132` … `1.26.50.99` and has no rule for `1.26.60`, so
   `SweetLoaderSignatureRulesManager.describeCoverageFailure` refuses to hand the rules over and the
   loader logs, after eight frame-tick defers:

   ```
   SweetLoader | SweetLoader signature rules were not delivered; deferring hook install
   SweetLoader | SweetLoader hook install deferred 8 times; the signature rules never arrived, leaving the game unpatched
   ```

   That is the on-device proof behind the "52 `ENGINE_HOOK` modules need a signature pack for the
   active build" note: on 26.60 the byte-pattern hooks cannot install because no verified offsets
   exist for the build, not because the transport changed.

What is still UNKNOWN, with the exact evidence to capture on a 26.60 instance:

- The sign-in path itself: sign in with a real Microsoft account and capture
  `adb logcat -s XalSignIn:* Xal:* com.mojang.minecraftpe:* org.chimeramc.client:*`. The launcher's
  own step is `MinecraftRuntimePreparer.refreshAccountsForLaunch` →
  `XalExporter.exportAccount`; a token-free count of exported accounts and the game's first XAL call
  is enough to tell "no account reached the game" from "the game rejects the token".
- Whether NetherNet needs anything from the launcher at all. The game implements the transport; the
  launcher supplies no socket for it, and nothing in this repo has been observed to fail with
  `Transport::RAKNET` selected, so `BedrockTransport` stays a pure gate with no caller and
  `core/nethernet` stays unwired (see the README in that package).

## 6. Update, 2026-11-28: the export the game reads, and five defects in it

The report for this pass: the launcher already holds the account, the game still opens on "Sign In";
after signing in, the gamertag and Social (1) appear and Marketplace loads, but
"Signing in with your Microsoft account....." never clears and the skin stays default.

That split matters. A user token is clearly present (Social, Marketplace), so the game is not
failing to authenticate at all; what it cannot finish is the part that needs a *relying party it
does not have*. The launcher's XAL export is the only place those tokens come from, so the export
is where the defects are. Each item below is confirmed by reading the code and pinned by a JVM
test; none of them has been observed on a phone yet, and section 6.2 says what a device still has
to show.

### 6.1 Confirmed in code, with tests

1. **The game read a different directory than the exporter wrote.** `Storage.getStoragePath`
   returned plain `files/` unless `launcherManagedMcLoginEnabled` was true, and that setting
   defaults to false (`FeatureSettings`). `XalExporter` wrote `files/xal/<b64>/` unconditionally.
   On a default install the export was therefore invisible to the game.
   **Fixed:** `XalStorageContract` (in the `minecraft` module, used by both sides) owns the layout
   and the "which account" rule, and `Storage` now uses the launcher store whenever an exported
   account exists, not only when the toggle is on. `XalStorageContractTest`.

   Why the toggle exists at all, and why it was not simply forced on: it is the opt-in "the
   launcher manages the game's Microsoft sign-in" switch (`SettingsRegistry` entry
   `launcher_managed_login`, default off), and it also drives `AndroidLaunchProbe.signInRequired`,
   i.e. whether a launch is *blocked* until the user signs in. Forcing it on would rewrite a
   user-visible setting and change a launch gate; making the exporter obey it would keep exporting
   nothing on a default install, which is the reported symptom. Treating a present export as
   authoritative is the smallest rule that makes the writer and the reader agree, and it leaves the
   toggle meaning what it always meant for the launch gate and for a fresh install with no export.
2. **A refreshed token never reached the game.** `MsftAccountStore.updateAuthMaterial` saved the
   new chain and stopped. **Fixed:** it now calls `XalExporter.refreshExportedAccount`, which
   rewrites only the account the pointer names, so a background refresh of another account cannot
   re-point the launch. `XalExportWiringTest`.
3. **The export wrote the bound account, the game read the active one.** `exportAccount` (F2
   per-instance binding) wrote `<b64_bound>`, while `Storage` resolved the `active` flag in
   `Xal.Accounts.json`. **Fixed:** every export writes `Xal.Current.json` with the account id, and
   the reader prefers that pointer over the active flag.
   `XalStorageContractTest.theExportPointerWinsOverTheActiveAccount`.
4. **No Dtoken.** The export never emitted a `Dtoken` envelope, so XAL had no device token with
   which to mint a relying party that was not cached. **Fixed:** a `Dtoken` envelope is written
   from `xblDeviceToken` against `http://auth.xboxlive.com`. The library's device token carries no
   user hash, so the user token's `uhs` is reused.
   `XalExportBundleTest.theDeviceTokenIsWrittenSoXalCanMintAMissingRelyingParty`.
5. **Missing relying parties.** Only `http://xboxlive.com`, PlayFab, Realms and the `Utoken` were
   written. **Fixed:** `https://multiplayer.minecraft.net/` (from `bedrockXstsToken`) and
   `rp://api.minecraftservices.com/` (minted by the Java chain and kept in the snapshot as
   `javaXstsToken`, because the exporter runs offline and cannot mint it) are written too, and
   `refreshStoredAccount` renews the Bedrock XSTS and Realms tokens as well as MSA, Xbox Live and
   the certificate chain. A token the snapshot does not carry is omitted rather than faked.

Also changed: the export now runs on **every launch** for the account the game will read (the bound
account when there is one, otherwise the active account), not only on sign-in; and the exporter logs
one line, tag `XalExporter`, with the relying parties written, the seconds to the soonest expiry and
the target directory, and no token, id or gamertag.

### 6.2 Still UNVERIFIED, and what would settle it

There is no device in this environment, so nothing above proves XAL accepts the files. What is
proven is that the writer produces the file set, the relying parties, the `Dtoken` and the account
directory that the reader resolves — and that the reader no longer looks somewhere else.

On a 26.60 phone, with the launcher signed in:

```
adb logcat -s Xal:* XalSignIn:* XalExporter:* com.mojang.minecraftpe:*
```

Expected from the launcher, once per launch and once per refresh:

```
I XalExporter | Exported 4 file(s) to /data/user/0/org.chimeramc.client/files/xal/<b64>; relying parties [http://auth.xboxlive.com, http://xboxlive.com, https://multiplayer.minecraft.net/, https://b980a380.minecraft.playfabapi.com/, https://pocket.realms.minecraft.net/, rp://api.minecraftservices.com/]; soonest token expiry <n>s
```

- No `XalExporter` line at all means the export never ran (launch-path defect, not auth).
- A line whose `soonest token expiry` is negative means the stored chain was already expired when
  exported, i.e. the refresh did not run or failed (it is logged as `Could not refresh the stored
  sign-in material`).
- `files/xal/<b64>` that does not match the account the launcher shows means the pointer and the
  account list disagree; `Xal.Current.json` says which one the game used.
- Then watch whether the game's "Signing in with your Microsoft account....." clears. If it clears
  and the skin is still Steve, that is the separate signature-rules gap in 5.3, not auth.

### 6.3 The unverified guesses that were deliberately left alone

`MsftAuthManager.GAME_VERSION` is still `1.21.110`, the title id in the file names is still
`1739947436`, `ClientAttested` is still `false`, `IssueInstant` is still `NotAfter` minus eight
hours, `Msa.json` still carries `"foci": ""` and the access token still carries the `t=` prefix.
Each of these is a guess at the shape the real XAL client writes, and changing them without a device
would only trade one guess for another. They are the first things to compare against a
`files/xal/` directory produced by the genuine client on a 26.60 phone.
