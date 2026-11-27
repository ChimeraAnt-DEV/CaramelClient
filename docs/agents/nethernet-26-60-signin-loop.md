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

**Confirmed, fixed.** With that, the launch trace reaches `Runtime preparation finished` and
`GameActivity` starts the game library:

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
