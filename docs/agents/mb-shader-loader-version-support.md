## MB shader loader (mbl2 / libmtbinloader2) — verified version support

The "MB shader loader" / "better RenderDragon for Android" is **`mtbinloader2`** (mbl2), a Rust
library vendored into this app as the prebuilt `app/src/main/jniLibs/arm64-v8a/libmtbinloader2.so`
(banner string: `Starting, mbl2 version v0.1.12`). Upstream:
`https://github.com/mcbegamerxx954/mtbinloader2`. It is **not** a renderer — it hooks
`ResourcePackManager` and the AAsset functions to load materialbins, camerafiles and OreUI files,
and it "autofixes" shaders by signature.

### How it decides what it can do

It has no version table. It scans the mapped `libminecraftpe.so` for a **byte signature** (via
`tinypatscan`) and hooks whatever the first match is. Its aarch64 pattern list (`src/lib.rs`) is
labelled `26.60`, `26.50`, `26.40`, `1.21.120.4`, `1.21.60.21`, `1.19.50-1.21.50`. Support for a
version is therefore *empirical*: the only question is whether a pattern still matches that build's
binary.

### What was verified here (not assumed)

Each APK was downloaded from mcpe-planet, `libminecraftpe.so` was extracted, and the loader's own
patterns were scanned against the real bytes. A match means the loader will install on that build.

| Version | ABIs in the APK | arm64 pattern result | Verdict |
|---|---|---|---|
| **26.60** | arm64-v8a | `26.60` exact pattern matches **1** (at `0xcf8a670`) | **works today** |
| **26.52** | arm64-v8a | `26.50` pattern matches **1** (at `0xc92b29c`) | **works today** |
| **26.51** | arm64-v8a | `26.50` pattern matches **1** (at `0xc92af44`) | **works today** |
| **26.50** | arm64-v8a | `26.50` pattern matches **1** (at `0xc92af4c`) | **works today** |
| 26.45 | armeabi-v7a **only** | no arm64 binary exists; neither ARM32 pattern matches | see below |
| 26.44 | armeabi-v7a **only** | no arm64 binary exists | see below |
| 26.40 | armeabi-v7a **only** | no arm64 binary exists | see below |
| 26.41 | — | mcpe-planet has no such page (404) | version does not exist there |

The `26.60` *wildcard* pattern (the second entry) matches 11 places in the 26.50–26.52 binaries, so
it is only a fallback; the exact `26.60` pattern is what pins 26.60, and `26.50` pins 26.50–26.52.
That is why 26.51 and 26.52 work without the loader naming them.

### The 32-bit blocker (26.40, 26.44, 26.45)

Those three APKs ship **only** `lib/armeabi-v7a/libminecraftpe.so`; there is no arm64-v8a directory
at all. Two consequences:

1. This client is **arm64-only** (`abiFilters 'arm64-v8a'` in `app/build.gradle`), so a 32-bit-only
   game build cannot be launched by it in the first place. The loader is not the limiting factor.
2. Even on a 32-bit runtime, the loader's two ARM32 patterns do not match the 26.45 binary, so mbl2
   would not install there either.

**Conclusion:** for 26.40/26.44/26.45 there is nothing to fix in the loader — the client cannot run
those builds at all. The real answer to "make it work on 26.40" is "use the arm64 build of the game",
i.e. 26.50+, which the client already supports.

### If a future arm64 build breaks the loader

1. Extract `lib/arm64-v8a/libminecraftpe.so` from the APK.
2. Find the `ResourcePackManager` constructor by scanning for the current patterns; if none match,
   re-derive the pattern from the equivalent function in the new binary (the constructor's shape is
   stable: a stack-frame prologue followed by the `F4 03 02 AA F3 03 00 AA` tail).
3. Add the new pattern to the front of `RPMC_PATTERNS` in `src/lib.rs` and rebuild with
   `cargo build --release --target aarch64-linux-android` (Rust + NDK r25+).
4. Re-run the scan described above to confirm the new pattern matches **exactly once** — a pattern
   that matches many places will hook the wrong function.

The scan helper used here is `patscan.py` / `scan2.py` (byte + wildcard, anchored search); it needs
no NDK and no device, only the APK.
