# Binary analysis — Minecraft Bedrock 1.26.44 → 1.26.62 (arm64-v8a)

Static analysis of `lib/arm64-v8a/libminecraftpe.so` for every retail build in the 26.44–26.62
range, produced offline by `tools/verify-binary`. Nothing here is a claim about runtime behaviour;
each row is what the analyzer derived from the real library. The libraries and the APKs are **not**
committed — this page records the values so a rule can be derived without re-downloading.

## Why this range

`docs/sweetloader-compatibility.md` shows the shipped rules stopping at **1.26.50.99**. The builds
after it (1.26.51+) have no rule, and the 1.26.50.26 byte signatures were confirmed below to stop
matching at 1.26.60. This page is the raw material for those missing rules.

## Versions present on mcpedl.org

Only these versions of the range exist to download; the rest are not hosted (or not in the range):

| Version | Page | arm64 file |
|---|---|---|
| 26.44 | `minecraft-pe-26-44-apk` | `minecraft-26-44-arm64-v8a.apk` |
| 26.45 | `minecraft-pe-26-45-apk` | `minecraft-26-45-arm64-v8a.apk` |
| 26.50 | `minecraft-pe-26-50-apk` | `minecraft-26-50-arm64-v8a.apk` |
| 26.51 | `minecraft-pe-26-51-apk` | `minecraft-26-51-*.apk` |
| 26.52 | `minecraft-pe-26-52-apk` | `minecraft-26-52-arm64-v8a.apk` |
| 26.60 | `minecraft-pe-26-60-apk` | `minecraft-26-60-*.apk` |
| 26.62 | `minecraft-pe-26-62-apk` | `minecraft-26-62-*.apk` |

26.44 and 26.45 ship a **32-bit (`armeabi-v7a`) `minecraft-26-4x.apk` first**; the arm64 build is a
separate file (`-arm64-v8a`), and picking the smallest download silently selects the 32-bit one.
The `apk_range.py` helper in the recon scratch dir fails over candidates until the arm64 member is
present, which is the reliable selection rule.

## Per-build seam table (all `arm64-v8a`)

Produced by `tools/verify-binary <lib> --version 1.26.<v>`. `typeinfo_va` is what the runtime
`findTypeInfo` returns; `vtable` is the true `_ZTV` address point.

| Build | Size (bytes) | typeinfo (`_ZTI`) | vtable (`_ZTV`+8) | render slot | render fn | ClientInstance vtable | local-player slot 31 | SHA-256 |
|---|---|---|---|---|---|---|---|---|
| 1.26.44 | 313,919,840 | `0x11e2a360` | `0x11e298c8` | 17 | `0x92b756c` | `0x11e3b978` | `0x9443370` | `433db4ba…309e754` |
| 1.26.45 | 313,903,456 | `0x11e29cc0` | `0x11e29228` | 17 | `0x92b74cc` | `0x11e3b2d8` | `0x94432d0` | `cc5e5a04…31003e` |
| 1.26.50 | 328,419,680 | `0x12bb1708` | `0x12bb0ee0` | 17 | `0x967e118` | `0x12bc2c80` | `0x9807efc` | `fac2be5b…f2927e1` |
| 1.26.51 | 328,419,680 | `0x12bb1a28` | `0x12bb1200` | 17 | `0x967e138` | `0x12bc2fa0` | `0x9807f1c` | `0b5b4a79…7ef47120` |
| 1.26.52 | 328,419,680 | `0x12bb1d88` | `0x12bb1560` | 17 | `0x967e1d8` | `0x12bc3300` | `0x9807fbc` | `1929d005…f3fc6b26f3` |
| 1.26.60 | 342,231,392 | `0x13880d98` | `0x138802f0` | 17 | `0x9a2761c` | `0x1388fe70` | `0x9b7f610` | `b6ffe008…ca221a80` |
| 1.26.62 | 342,231,392 | `0x138813d8` | `0x13880930` | 17 | `0x9a276dc` | `0x138904b0` | `0x9b7f6d0` | `2abbb298…dec6d7b9` |

Facts that hold across the whole range:

- **`playerRenderVtableIndex = 17` on every build.** The slot index never moved across 26.44–26.62,
  so the index fallback alone is sufficient for the render seam; no per-build index is needed.
- **`ClientInstance` slot 31 is the local-player accessor on every build.** The pinned
  `kGetLocalPlayerSlot = 31` survives the range.
- **The RTTI type name `18LivePlayerRenderer` and the diagnostic string
  `variable.player_x_rotation` are present in every build.** Both anchors the resolver keys on exist
  throughout.
- **`mce::ImageUtils::loadImageFromMemory` is present by name in every build** (`1` occurrence).

## The `SerializedSkinRef` accessor table is gone

`docs/agents/native-cosmetics-seams-verified-offsets.md` recovered the `+0xa8` cape offset from the
`SerializedSkinRef::getX()` accessor-name strings on **1.26.60.28**. Those names do **not** survive
any of the seven retail builds above:

| Accessor name | 26.44–26.62 | 1.26.60.28 |
|---|---|---|
| `getImageData` | present | present |
| `getCapeImageData` / `…Cereal` | **absent** | present |
| `getAnimatedImageData`, `getGeometryData`, `getGeometryDataMutable` | **absent** | present |
| `getAnimationData`, `getCapeId`, `getSkinColor` | **absent** | present |
| `getIsTrustedSkinFlag`, `getProfileHash` | **absent** | present |
| `SerializedSkinRef` | present | present |

The 1.26.60.28 build was re-downloaded and re-analyzed to confirm the split is real (368,150,880
bytes, SHA-256 `c7036646…2a4e7c82`; all nine accessors present). On 26.44–26.62 only `getImageData`
survives, so the `+0xa8` offset **cannot be re-derived on these builds by the accessor-name route**
and the cape swap must stay fail-closed there. Re-deriving it on 26.60+ needs a different anchor
(a `mce::Image`-returning method reachable by another string, or a disassembly pass), which is
outside this analysis.

## Finding: the string scan of `resolveVtableSlotReferencingString` resolved nothing (fixed)

`resolveVtableSlotReferencingString` finds the RTTI name, calls `findTypeInfo` for a base address,
then walks `base + 8 + slot*8` as if `base` were the vtable. But `findTypeInfo` returns the
**typeinfo struct** (`_ZTI`), and in the Itanium layout the vtable (`_ZTV`) is a *separate* object
that merely points back at the typeinfo:

```
_ZTI  (typeinfo)   findTypeInfo returns this
  +0x00  vtable pointer ─────┐   (the _ZTV typeinfo-pointer slot, value == _ZTI)
  +0x08  name pointer ──┐    │
                        │    │
_ZTV  (vtable)  <───────┼────┘
  +0x00  offset-to-top │
  +0x08  typeinfo ptr ─┘
  +0x10  slot 0 …  slot 17 -> render fn
```

Because `findTypeInfo` is the value of the vtable's own typeinfo-pointer slot, walking
`findTypeInfo + 8` starts at the `_ZTI` name pointer (slot 0 = the `.rodata` name VA, which is not in
`.text` and is skipped), then reads the `_ZTI`'s other fields as if they were slots. The real
vtable's `slot 17` is never reached.

Verified by emulating `Vtable.cpp` exactly against four real libraries — the shipped walk finds no
slot that references the needle, while a walk based at `_ZTV + 8` finds **slot 17 on all four**:

| Library | shipped walk | corrected walk (`_ZTV + 8`) |
|---|---|---|
| 26.52 | none | slot 17 |
| 26.60.28 | none | slot 17 |
| 26.62 | none | slot 17 |

The consequence was contained, not fatal: when the string scan returned 0 the player-render feed fell
back to `resolveVtableFunction(type, 17)` (the index path), which uses `findPrimaryVtableSlot` and is
**correct** — it locates the slot whose value is the typeinfo with offset-to-top 0, and resolves slot
17 on every build. So the render seam worked *only* through the fallback and the string scan was dead
weight.

**Fixed** in the sweetloader: `resolveVtableSlotReferencingString` now locates the vtable's
typeinfo-pointer slot (`findPrimaryVtableOffset`, a word equal to the typeinfo preceded by
offset-to-top 0) and walks from there. `tests/cosmetics/VtableResolveTest.cpp` pins it with a fixture
that builds the real object graph; it resolves slot 26 against the old code and slot 17 against the
fix. The index fallback stays in place for a build whose slot moves.

## Method

1. Resolve each version to its arm64 APK URL (the `mcpedl.org` download form; the arm64 file is a
   distinct `-arm64-v8a` asset).
2. Download the APK, extract `lib/arm64-v8a/libminecraftpe.so`, analyze with `tools/verify-binary
   <lib> --version 1.26.<v>`.
3. For the string-scan finding, re-implement `findTypeInfo` / `resolveVtableSlotReferencingString` /
   `FunctionReferencesAddress` in Python over the `.rela.dyn` `R_AARCH64_RELATIVE` map (vtable slots
   are relocated, so raw section bytes are wrong) and compare the shipped walk base against the
   `_ZTV` base.

APK sizes: the arm64 builds are 313–342 MB of `libminecraftpe.so` inside ~380–790 MB APKs.
