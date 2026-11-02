## JavaBridge: Java Edition mod auto-porter (core.javabridge + ui.activities.JavaBridgeActivity)
- **It rewrites Java mods, it does not run them.** A Java Edition `.jar` is decompiled (CFR, MIT,
  `libs.cfr`; Procyon is named in the spec but is not on the classpath) and the source is sent to a
  user-configured OpenAI-compatible LLM, which generates a Bedrock `.AntEgg` mod. There is no Java
  Edition runtime (see the Java Edition section) тАФ the porter is the honest alternative to one.
  `javabridge_scope_note` says this in-app.
- **The flow is ordered cheapest-first and refuses before the network.** `JavaPorter.assess` runs
  from the jar alone: `JarInspector` reads the central directory + `fabric.mod.json` / `META-INF/
  mods.toml` / `mcmod.info`, `PortabilityReport` scores it, `CheatPatternDetector` refuses known
  cheats. Only then, on an explicit Port press, does `CfrDecompiler` decompile and the LLM get
  called. A refused or unportable mod never reaches the network. `JavaPorterTest` drives the whole
  thing with fake `Decompiler`/`LlmClient` (no real jar, no request).
- **Consent is recorded and versioned, never implied.** `LlmConsent.isAllowed(consentGranted,
  configured, userInitiated)` requires all three; `LlmSettings` stores the endpoint/key/model and
  the accepted `CONSENT_VERSION`. `OpenAiCompatibleLlmClient` re-checks `isConfigured()` as the
  last line of defence. Nothing is uploaded until Port is pressed.
- **The generated package round-trips through the ordinary loader.** `AntEggPackageWriter` writes
  the archive (no `zip` binary, no third-party archive lib) and `AntEggLoader.loadMod` loads it, so
  a ported mod uses the same path as any import. `AntEggPackageWriterTest` writes then reads back.
- **Provenance is written and surfaced, not decorative.** `AntEggManifest.ported(...)` always sets
  `aiPorted` and the parser reads `ai_ported`/`origin_mod`/`origin_author`; the writer emits them
  and the JavaBridge review dialog shows "AI-ported from <origin>" before the user enables the mod
  (`buildProvenanceLine`). Do not add a construction path that yields a ported manifest without the
  flag.
- **Import routing.** A `.jar` picked through the Mod Import screen is not a Bedrock package, so
  `FileHandler.processIncomingFilesWithConfirmation` hands it to the `JavaBridgeHandoff` callback
  (implemented by `ModsFullscreenActivity.ImportCallback` / `MainActivity.IncomingCallback`) rather
  than reporting it unsupported; `.jar` is also a pathPattern/pathSuffix in the manifest's import
  intent filters. `looksLikeJavaJar` keys on the extension only тАФ a jar has the same magic bytes as
  a zip, so the name is the only discriminator.
- Template and format docs: `examples/antegg-template/`.
