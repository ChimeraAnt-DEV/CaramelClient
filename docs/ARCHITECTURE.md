# Architecture

CaramelClient is three cooperating pieces: the **launcher app**, the **sweetloader** native
runtime, and an optional **script runtime** (BerryAPI). This page is the one diagram and the
"who calls whom" that the plan asks for.

## The three pieces

```mermaid
flowchart TD
    subgraph App["Launcher app (org.chimeramc.client, Kotlin + Java)"]
        UI["Activities & fragments<br/>launcher/ui"]
        OVER["In-game overlays<br/>launcher/core/mods/inbuilt/overlay"]
        CORE["Feature core<br/>core/* (replay, cosmetics, voice, mods, …)"]
        BRIDGE["GameBridge<br/>core/backend"]
        PREIN["PreloaderInput / BerryApi<br/>launcher/sweetloader"]
    end

    subgraph SweetLoader["libsweetloader.so (C++, submodule)"]
        SIG["Signature + hook<br/>pl::memory"]
        RUNTIME["Game hooks<br/>pl::runtime"]
        COSM["Cosmetics render<br/>pl::cosmetics"]
        INP["InputBridge"]
    end

    subgraph Game["libminecraftpe.so (the game, no symbols)"]
        GAME["Minecraft Bedrock"]
    end

    subgraph Script["libBerryAPI.so (optional)"]
        BERRY["Script runtime"]
    end

    UI --> CORE
    OVER --> CORE
    OVER --> BRIDGE
    CORE --> BRIDGE
    BRIDGE --> PREIN
    BRIDGE --> BERRY
    PREIN --> SIG
    PREIN --> INP
    RUNTIME --> SIG
    COSM --> RUNTIME
    SIG --> GAME
    INP --> GAME
    GAME -->|input events| INP
```

## Who calls whom

| Caller | Callee | Why |
| --- | --- | --- |
| Launcher feature (overlay, mod) | `GameBridge` | Every capability (send key, look delta, FPS) is a `GameBackend` call, so a missing backend degrades quietly instead of crashing. |
| `GameBridge` | `LauncherGameBackend` | The in-app implementation, which forwards to the sweetloader's input bridge. |
| `GameBridge` | BerryAPI | The script runtime can implement the same interface; `GameBridge.register` swaps it in live. |
| Launcher (`ModManager`) | `System.loadLibrary("sweetloader")` | Maps the sweetloader into the game's own process, so a hook runs in the address space of `libminecraftpe.so`. |
| SweetLoader | game binary | Only through a resolved signature or vtable slot: game classes export no symbols. |
| Game | sweetloader `InputBridge` | Touch, key, text and mouse events are forwarded to registered callbacks. |

## Key seams

- **`GameBackend` / `GameBridge`** (`core/backend`) — the client's input and FPS seam. With no
  backend registered every call is a quiet no-op returning `false`, so the launcher still starts
  and the overlays degrade quietly.
- **`PreloaderInput`** (`launcher/sweetloader`) — the JNI surface to the sweetloader: signature rules,
  input injection, render geometry, cosmetic frames, diagnostics.
- **`BerryApi`** (`launcher/sweetloader`) — a thin, fail-closed read of the bundled BerryAPI runtime.
  When the library is missing every capability is clear, so a dependent feature is greyed out
  rather than appearing to work.
- **Pure-rule extraction.** Rules that must be unit-tested live in Android-free classes
  (`ApiLevels`, `DisplayModePreference`, `LauncherTab`, `NavLayoutMode`, `ModAvailability`,
  `CapeAnimationCurve`, …); the Android type only paints or applies what the pure class returns.
  This is why a JVM test can pin behaviour that a device would otherwise be needed to see.

## Where a feature lives

| Concern | Package |
| --- | --- |
| Launch flow, versions, storage | `launcher.core.minecraft`, `launcher.core.versions` |
| In-game overlays (HUD, stats) | `launcher.core.mods.inbuilt.overlay` |
| The Mod Menu itself | native Dear ImGui: `sweetloader/src/pl/menu/`, bridged by `launcher.preloader.PreloaderMenu` + `launcher.core.mods.inbuilt.overlay.InGameMenuController` |
| Mod loading | `launcher.core.mods`, `launcher.core.mods.inbuilt` |
| Cosmetics (catalogue, preview, native feed) | `core.cosmetics`, `launcher.core.mods.inbuilt.overlay` |
| Replay | `core.replay` |
| Voice and cosmetic sync | `core.voice`, `core.cosmetics` |
| Native bridge and compat | `core.jni`, `util` |
