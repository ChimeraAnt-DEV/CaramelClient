# Device compatibility

What the app runs on, and what to expect per device class. Update this table from user reports.

## What the build requires

| Requirement | Value | Why |
| --- | --- | --- |
| Android | 9.0 (API 28) or newer | `minSdkVersion 28`. Every feature is gated with a fallback, so nothing needs a newer version. |
| CPU ABI | **arm64-v8a** | The APK ships `arm64-v8a` only, so Android fixes the process to 64-bit at install. |
| Storage | A few GB free | One imported game is several hundred MB, plus extraction space. |
| Game | Minecraft: Bedrock, imported from an APK you own | The launcher does not ship the game. |

## The ABI rule, in plain terms

The launcher is a 64-bit process. A version is **launchable** when the APK you import contains an
`arm64-v8a` copy of the game's native library. Most Bedrock APKs ship both `arm64-v8a` and
`armeabi-v7a`, and the 64-bit half is used. A version is only refused when its APK contains **no
arm64 libraries at all**; the launch preflight then names the ABI it found.

There is no setting that runs a 32-bit-only version: Android cannot run part of an app 32-bit and
part 64-bit, and the prebuilt `libgxcore.so`-era native library is arm64-only by design. The honest
deliverable is the preflight message, which fires only when nothing can load.

## Test matrix

A change that touches the nav, an overlay or the launch path is checked against this spread where a
device is available.

| Device class | Example | Android | What it exercises |
| --- | --- | --- | --- |
| Low-RAM phone | 2 GB, 720p | oldest and newest supported | Memory pressure, the FPS optimizer's shed path, first-frame time |
| Mid-range phone | 4–6 GB, 1080p | oldest and newest supported | The common case |
| Flagship | 8 GB+, 120 Hz | newest | The high-refresh request and the frame cap |
| Tablet | 10", 1280dp | newest | The expanded nav rail and a large window |
| Foldable | unfolded | newest | The `NavLayoutMode` change on unfold |

## Reporting a device result

Use the device report issue template. Include the **Copy Debug Info** output (app version, device
model, Android version, ABI, hook and backend status, and the last log lines) — a report without it
usually cannot be reproduced. The checklist in [`DEVICE_CHECKLIST.md`](DEVICE_CHECKLIST.md) is the
step-by-step device pass for the native cosmetic draw, which is the one thing the build container
cannot verify.
