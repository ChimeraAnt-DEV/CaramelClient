## .AntEgg mod packaging (core.antegg + preloader AntEggLoader)
- **`.AntEgg` is a ZIP archive with a custom extension.** The manifest `egg.json` sits at the
  archive root and carries `name`/`version`/`author`/`type` (`native`|`script`)/`entry_point` and
  an optional `dependencies` array. `AntEggManifest` (Java) and `pl::runtime::AntEggManifest`
  (C++) parse the same grammar; both reject a missing field, a non-semver version, a type that
  disagrees with the entry-point extension, or an `entry_point` that escapes the package.
- **Two runtimes, two seams.** `type: "native"` hands the extracted `.so`'s absolute path to
  `ModManager.initializeLoadedMod` -- the same preloader injection an ordinary native mod uses;
  `.AntEgg` adds packaging/validation, not a second injection mechanism. (The original spec calls
  this API "ApexAntLamina"; no class or symbol by that name exists in this tree -- it is the `pl`
  preloader bridge.) `type: "script"` runs the entry `.lua` on the embedded Lua VM (`luaj.jse`,
  Lua 5.2) through `AntEggScriptHost`, which populates a `mod` table (`name`, `version`, `author`,
  `id`, `dir`, `sandbox`, `dependencies`).
- **Script failures are returned, never thrown** -- a broken script shows an error in the Mods tab
  instead of taking the launcher down. `AntEggScriptLoadingTest` runs a real archive through
  extract-then-execute and pins both error paths.
- **Extraction is sandboxed and atomic.** `getExternalFilesDir()/AntEggs/<mod-id>/` (internal
  fallback); every ZIP entry is resolved and checked to stay inside the sandbox (entry names are
  attacker-controlled), and the archive is staged in a temp sibling and moved into place only when
  complete, so a failed import leaves no partial mod. `.antegg` is a recognized import extension
  in `FileHandler` and an intent-filter path suffix in the manifest.
- Native: `AntEggLoader.cpp`/`AntEggJni.cpp` use zlib raw inflate (no new third-party ZIP dep),
  bound to `AntEggBridge.nativeValidate`/`nativeLoadMod`/`nativeLooksLikeAntEgg`. The symbol
  prefix is `Java_org_chimeramc_client_core_antegg_*`; verify with `llvm-nm -D`.
