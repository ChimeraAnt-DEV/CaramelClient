package org.chimeramc.client.preloader;

import org.chimeramc.client.util.AppLog;

import android.view.MotionEvent;

import java.io.File;

public class PreloaderInput {

    /**
     * The C ABI version this client expects from the sweetloader.
     *
     * <p>Bumped together with {@code pl_get_abi_version()} in the sweetloader; a mismatch means the
     * client and the native library were built from different ABI revisions, so the client refuses
     * the surface rather than calling into it with the wrong expectations.
     */
    public static final int EXPECTED_ABI_VERSION = 1;

    /** The sweetloader's own C ABI version, or 0 when the library or the symbol is unavailable. */
    public static int abiVersion() {
        try {
            return nativeAbiVersion();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    /**
     * Whether the loaded sweetloader matches this client's expected ABI version.
     *
     * <p>Pure comparison of two integers, so the rule is testable without a device: an unavailable
     * library reads as version 0 and therefore does not match.
     */
    public static boolean abiMatches(int reported) {
        return reported == EXPECTED_ABI_VERSION;
    }

    /** Whether the loaded sweetloader's ABI matches this client's expectation. */
    public static boolean isAbiCompatible() {
        return abiMatches(abiVersion());
    }

    private static native int nativeAbiVersion();

    /**
     * Bounds the sweetloader's cached module handles.
     *
     * <p>Resolution handles are cached so a repeated install does not reopen a module, but a session
     * that hot-reloads mods (or resolves many distinct mod {@code .so}s) grows the cache without
     * bound. The host calls this from {@code Application.onTrimMemory}. Returns the number of
     * handles closed; the game module's handle is never closed. A build without the library returns
     * 0.
     */
    public static int closeUnusedModuleHandles() {
        try {
            return nativeCloseUnusedModuleHandles();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    private static native int nativeCloseUnusedModuleHandles();
    // The mouse button state machine lives in PreloaderMouseState so its rules are unit-testable
    // without a native library; the native declarations have to stay on this class, because a JNI
    // symbol resolves from the declaring class.
    private static final PreloaderMouseState MOUSE_STATE = new PreloaderMouseState();
    /** Scratch for the per-event button diff, reused so the dispatch path allocates nothing. */
    private static final int[] MOUSE_DIFF = new int[2];

    public static native boolean nativeOnTouch(int action, int pointerId, float x, float y);
    public static native boolean nativeOnKeyEvent(int keyCode, int unicodeChar, boolean isKeyDown);
    public static native boolean nativeOnTextInput(String text);
    public static native boolean nativeOnMouse(int button, boolean isDown);
    public static native void nativeOnDocumentResult(boolean success, String path, String displayName, String error);
    public static native void nativeSetActivity(Object activity);
    public static native void nativeClearActivity();
    public static native boolean nativeIsPauseMenuOpen();
    public static native boolean nativeIsHudScreenOpen();
    public static native boolean nativeIsShowingMenu();
    public static native boolean nativeShouldForceGlobalModMenu();
    public static native boolean nativeIsLocalPlayerAvailable();
    public static native float[] nativeReadLocalPlayerPosition();
    public static native float[] nativeReadLocalPlayerRotation();
    public static native float[] nativeReadLocalPlayerHealth();

    /**
     * Asks the running game to re-read the active resource-pack list.
     *
     * <p>Optional by design: a build that has not resolved the per-version hook for this returns
     * false and the caller falls back to "applies on the next world load". Declared here so the
     * seam exists and the Java side needs no change when the hook lands.
     */
    public static native boolean nativeReloadResourcePacks();

    /**
     * True once the game's own player-model renderer has run this session.
     *
     * <p>This is the native-cosmetics capability probe: the render hook is only useful if the
     * renderer is actually running, so the launcher treats "hook live" as "the native path can
     * draw cosmetics this session" and otherwise reports the pieces as preview-only.
     */
    public static native boolean nativeIsPlayerRenderHookLive();

    /**
     * Whether the engine's present / frame-boundary hook is installed (the {@code eglSwapBuffers}
     * detour). Frame generation and motion blur both act on the presented frame, so this is the probe
     * that decides whether they can run on this build: false on a Vulkan-only build with no libEGL
     * present call, in which case the modules stay badged instead of toggling to no effect.
     */
    public static native boolean nativeIsPresentHookLive();

    /**
     * Pushes the present-frame effect plan (frame generation + motion blur) to the native seam.
     *
     * <p>Values are clamped on the native side, so a stored option can never drive an out-of-range
     * blend. A no-op when the seam is not live: the launcher keeps both modules badged and the plan
     * is simply never acted on.
     */
    public static native void nativeSetPresentFramePlan(
            boolean frameGenerationEnabled, float interpolationStrength, int maxFramesPerRendered,
            boolean reduceEdgeArtifacts, boolean motionBlurEnabled, float blurStrength,
            float blurBudget, boolean weightByViewSpeed);

    /**
     * Whether the present-frame effect seam is installed and live. False until the engine's present
     * boundary has been seen with a current EGL context and the composite program has compiled, so a
     * module never reads as running before it can actually draw.
     */
    public static native boolean nativeIsPresentFrameEffectLive();

    /**
     * The render tick and per-frame call count, as
     * {@code {renderTick, callsThisFrame, totalCalls, msSinceLastRender}}, or null when the
     * renderer has not run. {@code callsThisFrame > 1} means the renderer drew more than one
     * player model this frame, i.e. the hook also covers non-local players.
     */
    public static native int[] nativeReadPlayerRenderStats();

    public static native void nativeConfigureSignatureRules(String rulesPath, String minecraftVersion);

    /**
     * Sets the directory the sweetloader caches a resolved signature scan in. The
     * cache is keyed by the game library's ELF build id and stores offsets from its load base, so a
     * repeat launch of the same game build skips the byte-pattern scan and a game update cannot read
     * the previous build's offsets. An empty string disables the cache.
     */
    public static native void nativeSetSignatureCacheDir(String directory);

    // --- Game Performance Mode -------------------------------------------------------------

    /**
     * Applies a flat {@code key=value} optifine configuration blob. The blob is built by
     * {@code OptifineConfigBlob}; the sweetloader parses it so the two sides share one grammar.
     */
    public static native void nativeConfigureOptifineMode(String blob);

    /**
     * The current state of every optifine item as a flat string array, six entries per item:
     * {@code {id, enabled, tier2, status, detail, needsRestart}}. Null when the native library is
     * absent.
     */
    public static native String[] nativeReadOptifineState();

    /** True when the master switch is on and at least one item is active. */
    public static native boolean nativeIsOptifineModeActive();

    /** Records the refresh rate the launcher resolved, for the refresh-rate item's report. */
    public static native void nativeSetOptifineRefreshTarget(int hz);

    /** Configures the dynamic-render-distance governor bounds and step rule. */
    public static native void nativeConfigureRenderDistance(int minDistance, int maxDistance,
                                                           int fpsThreshold, int stepSize,
                                                           long minDwellMs);

    /** The number of accepted render-distance changes so far. */
    public static native int nativeRenderDistanceChangeCount();

    /** The tick count the callback-trimming hook has observed. */
    public static native long nativeOptifineTickCount();

    /** The HUD update count the OreUI-stripping hook has observed. */
    public static native long nativeOptifineHudUpdateCount();

    // --- Native cosmetics registry ---------------------------------------------------------
    // The launcher fills these; the native skin/cape and texture hooks read the same tables. Every
    // call is guarded by the caller via the try/catch wrappers below, so a build without the
    // library degrades to the resource-pack path rather than throwing.

    /** Registers a cape override (raw RGBA) for a player id. */
    public static native void nativeSetCapeOverride(long playerKey, byte[] rgba, int width, int height);

    /** Clears every cape override. */
    public static native void nativeClearCapeOverrides();

    /** Removes one cape override by player key, leaving other players' capes intact. */
    public static native void nativeRemoveCapeOverride(long playerKey);

    /** Number of cape overrides currently registered. */
    public static native int nativeCapeOverrideCount();

    /** Registers a texture override (raw RGBA) for a texture id. */
    public static native void nativeSetTextureOverride(long textureId, byte[] rgba, int width, int height);

    /** Clears every texture override. */
    public static native void nativeClearTextureOverrides();

    /** Removes one texture override by id, leaving other entries intact. */
    public static native void nativeRemoveTextureOverride(long textureId);

    /** Number of texture overrides currently registered. */
    public static native int nativeTextureOverrideCount();

    /** Publishes the geometry blob the render hook should draw for the local player. */
    public static native void nativeSetRenderGeometry(byte[] data);

    /**
     * Pushes one frame of cosmetic transform data (the {@code CosmeticFrame} wire layout) to the
     * native render hook. Called every game frame; the native side stores it and the render hook
     * reads it while drawing the player. Fail-closed: an absent symbol is a no-op.
     */
    public static native void nativePushCosmeticFrame(byte[] frame);

    /**
     * Replaces the cape image inside a {@code SerializedSkinRef} at a raw address. The image must
     * be the engine's own 0x30-byte {@code mce::Image} struct (built by its loader), not raw RGBA.
     */
    public static native boolean nativeSwapCapeImage(long skinRefAddress, byte[] imageBytes);

    /** The address of the engine's image loader, or 0 when unresolved on this build. */
    public static native long nativeImageLoaderAddress();

    /**
     * Builds a valid engine {@code mce::Image} (0x30 bytes) from PNG bytes, or null when the loader
     * is unresolved or the engine rejects the bytes.
     */
    public static native byte[] nativeBuildCapeImage(byte[] pngBytes);

    /** True once the native cosmetics skin/cape or texture hook has fired this session. */
    public static native boolean nativeIsCosmeticsHookLive();

    /**
     * Sets one local cosmetic piece's texture: 0 cape, 1 hat, 2 pet. A null array clears the slot.
     * Keyed by role, not catalogue id, so the hat and pet do not share the cape's artwork.
     */
    public static native void nativeSetPieceAtlas(int piece, byte[] rgba, int width, int height);

    /** Clears all three per-piece local textures. */
    public static native void nativeClearPieceAtlases();

    /** Reads {skinCapeCalls (-1 when unavailable), textureCalls, capeOverrides, textureOverrides}. */
    public static native int[] nativeReadCosmeticsStats();

    /**
     * Reads the skin-path seam observation: {@code {stage, packetHookInstalled,
     * packetCalls, textureCalls, skinCaptured}}.
     *
     * <p>This exists because the reported "my skin is not visible to other players" bug cannot be
     * diagnosed from the payload: the game strips its symbols, so the join packet's fields are
     * unreadable and the detours only count calls. The stage says which point the skin path
     * reached, which is what routes the bug (client never reached the join handler, versus reached
     * it and the outcome is still unproven). Null when the native library is absent.
     */
    public static native int[] nativeReadSkinSeam();

    /** The one-line explanation for a {@link #nativeReadSkinSeam()} stage. */
    public static native String nativeDescribeSkinSeam(int stage);

    /**
     * Returns the player's captured base-skin texture, or null when none is held.
     *
     * <p>The bytes are a 16-byte little-endian header {@code (width, height, format, stampMs)}
     * followed by the payload, which is either a PNG (format 0) or raw RGBA8 (format 4). The native
     * side captures it at the engine's image-loader seam, so the payload is the engine's own source
     * bytes — the skin the game is actually using. There is no player id at that seam, so the held
     * texture is the most recent base-skin-shaped upload (content capture, not attribution).
     */
    public static native byte[] nativeReadCapturedSkin();

    /** True while a captured base skin is held. */
    public static native boolean nativeHasCapturedSkin();

    /** Discards the held base-skin capture (session teardown). */
    public static native void nativeClearCapturedSkin();

    /**
     * Returns the player's captured cape texture, or null when none is held.
     *
     * <p>Same 16-byte header layout as {@link #nativeReadCapturedSkin}: {@code (width, height,
     * format, stampMs)} then the payload (PNG when format 0, raw RGBA8 when format 4). A cape is a
     * 2:1 panel (vanilla 64x32), so the native side tells it apart from a square skin atlas on the
     * one seam that carries both.
     */
    public static native byte[] nativeReadCapturedCape();

    /** True while a captured cape is held. */
    public static native boolean nativeHasCapturedCape();

    /** Discards the held cape capture. */
    public static native void nativeClearCapturedCape();

    /** Native declaration: registers the local player's custom cape pixels (2:1 RGBA8 panel). */
    public static native void nativeSetCapeSubstitution(byte[] rgba, int width, int height);

    /** Native declaration: removes the local cape substitution. */
    public static native void nativeClearCapeSubstitution();

    /** Native declaration: how many cape substitutions have fired this session. */
    public static native int nativeCapeSubstitutionCount();

    /** Native declaration: {hasCapture, capturedBytes, width | (substitutions << 32), height}. */
    public static native long[] nativeReadCapeCaptureDiagnostics();

    /**
     * A one-shot native render-path diagnostic, readable inside the app (logcat is not available to
     * the app without READ_LOGS, so the diagnostics screen reads this instead).
     *
     * <p>Layout: {renderSlot, resolvedBy, glBackend, eglContextCurrent, hookInstalled, framesSeen,
     * lastError, geometryBytes, pieceAtlasMask, lastDrawDecision, localPlayerLive, rulesRevision,
     * rulesMergeConflicts, rulesHaveRenderIndex, rulesLoadAttempted, sweetloaderModuleMappings,
     * trianglesDrawn}. All entries are -1 when the native library is absent. Never fabricates a
     * reading: an unresolved slot or a missing GLES surface is reported as such. The last two
     * entries (lastDrawDecision, trianglesDrawn) belong to the retired GLES rasteriser and now
     * always read -1; the array shape is kept so an older report reader still parses it.
     */
    public static native int[] nativeReadRenderDiagnostics();
    public static native int nativeRenderHookFailureReason();

    /** Tells the native side the display refresh rate, so it can decide vsync lock. */
    public static native void nativeSetDisplayRefreshHz(int hz);

    /**
     * The render-thread frame-pacing reading, as {valid, averageUs, worstUs, flags} where
     * flags bit0 = jittering and bit1 = vsync-locked. Length 1 (or null) means "no reading yet".
     */
    public static native int[] nativeReadFramePacing();

    /**
     * The percentile + hitch reading over the long frame window, as
     * {valid, p50Us, p95Us, p99Us, hitchesPerMinute}. Length 1 (or null) means "no reading yet".
     */
    public static native int[] nativeReadFramePacingPercentiles();

    /**
     * This process's memory footprint as
     * {valid, residentKb, proportionalKb, fromPss}. A missing source reads as invalid.
     */
    public static native int[] nativeReadProcessMemory();

    /**
     * The local-player feed as {live, xBits, yBits, zBits, yawBits, pitchBits}. Floats are the raw
     * IEEE-754 bits of the position and view rotation; the caller decodes them.
     */
    public static native int[] nativeReadLocalPlayerFeed();

    /**
     * Whether the last frame's cosmetic anchor came from the engine's view-projection: 1 engine,
     * 0 first-person fallback, -1 unknown. A device test must not confuse the fallback with a
     * resolved engine camera.
     */
    public static native int nativeCameraAnchorSource();

    /**
     * Native declaration: the classified GPU family, or -1 when not yet read.
     *
     * <p>0 unknown, 1 Adreno, 2 Mali, 3 Immortalis, 4 PowerVR, 5 Xclipse, 6 NVIDIA, 7 Apple,
     * 8 software rasteriser, 9 generic. See {@link org.chimeramc.client.core.cosmetics.GpuFamily}.
     */
    public static native int nativeGpuFamily();

    /**
     * Samples the native cape chain: {@code {lean1, sway1, lean2, sway2, ...}} in degrees for
     * {@code segments} segments, or empty when unavailable. Lets the preview and the native render
     * share one motion curve instead of each carrying its own amplitudes.
     */
    public static native float[] nativeSampleCapeChain(
            double moveSpeed, boolean jumping, double verticalSpeed,
            double distanceMoved, double capeFlap, double bodyYawDegrees, int segments);

    /** Encodes a cosmetic-sync advertisement datagram (byte-compatible with the Java protocol). */
    public static native byte[] nativeEncodeCosmeticAdvert(
            String peerId, String name, String capeId, String accessoryId, String petId);

    /**
     * Decodes a cosmetic-sync datagram into {@code {type, peerId, name, capeId, accessoryId, petId}},
     * or null when it is not a cosmetic packet.
     */
    public static native String[] nativeDecodeCosmeticAdvert(byte[] data);

    /** The cosmetic-sync datagram magic bytes. */
    public static native byte[] nativeCosmeticSyncMagic();

    public static void configureOptifineMode(String blob) {
        try {
            nativeConfigureOptifineMode(blob == null ? "" : blob);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:351", "Ignored a failure at PreloaderInput:351", e);
        }
    }

    public static String[] readOptifineState() {
        try {
            return nativeReadOptifineState();
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    public static boolean isOptifineModeActive() {
        try {
            return nativeIsOptifineModeActive();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static void setOptifineRefreshTarget(int hz) {
        try {
            nativeSetOptifineRefreshTarget(hz);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:375", "Ignored a failure at PreloaderInput:375", e);
        }
    }

    public static void configureRenderDistance(int minDistance, int maxDistance, int fpsThreshold,
                                               int stepSize, long minDwellMs) {
        try {
            nativeConfigureRenderDistance(minDistance, maxDistance, fpsThreshold, stepSize,
                    minDwellMs);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:385", "Ignored a failure at PreloaderInput:385", e);
        }
    }

    /** The number of accepted render-distance changes, or 0 when the sweetloader is absent. */
    public static int renderDistanceChangeCount() {
        try {
            return nativeRenderDistanceChangeCount();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    public static long optifineTickCount() {
        try {
            return nativeOptifineTickCount();
        } catch (UnsatisfiedLinkError e) {
            return 0L;
        }
    }

    public static long optifineHudUpdateCount() {
        try {
            return nativeOptifineHudUpdateCount();
        } catch (UnsatisfiedLinkError e) {
            return 0L;
        }
    }

    /**
     * Points the sweetloader's on-disk signature scan cache at a private directory. A null or empty
     * directory disables the cache, which is the safe fallback when no writable directory is known.
     */
    public static void setSignatureCacheDir(File directory) {
        try {
            nativeSetSignatureCacheDir(directory == null ? "" : directory.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            android.util.Log.w("SweetLoaderSigs",
                    "Signature scan cache not configured: libsweetloader.so is not loaded");
        }
    }

    public static void configureSignatureRules(File rulesFile, String minecraftVersion) {
        try {
            nativeConfigureSignatureRules(
                    rulesFile == null ? "" : rulesFile.getAbsolutePath(),
                    minecraftVersion == null ? "" : minecraftVersion
            );
        } catch (UnsatisfiedLinkError e) {
            // Silently swallowing this is what made a "revision 0" report possible with no clue
            // why: the rules call never reached the library. Log it, with the path, so the
            // diagnostics report and logcat both name the failing step.
            android.util.Log.w("SweetLoaderSigs",
                    "Signature rules were not delivered: libsweetloader.so is not loaded"
                            + " (rules=" + (rulesFile == null ? "<none>" : rulesFile.getAbsolutePath())
                            + ", version=" + (minecraftVersion == null ? "<none>" : minecraftVersion) + ")");
        }
    }

    /**
     * The revision the delivered rules file declares, or 0 when it cannot be read. Exposed so the
     * diagnostics report can compare what was shipped against what was delivered.
     */
    public static int deliveredRevision(File rulesFile) {
        if (rulesFile == null || !rulesFile.exists()) {
            return 0;
        }
        return PreloaderRules.revision(
                org.chimeramc.client.util.JsonIOUtils.read(rulesFile));
    }

    public static boolean isPauseMenuOpen() {
        try {
            return nativeIsPauseMenuOpen();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean isHudScreenOpen() {
        try {
            return nativeIsHudScreenOpen();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean isShowingMenu() {
        try {
            return nativeIsShowingMenu();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean shouldForceGlobalModMenu() {
        try {
            return nativeShouldForceGlobalModMenu();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * True once the game has handed us a live local player this session.
     *
     * <p>Fail-closed: an unavailable native library reads as "no player", so the
     * callers that project in-world icons draw nothing rather than guessing.
     */
    public static boolean isLocalPlayerAvailable() {
        try {
            return nativeIsLocalPlayerAvailable();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** The local player's world position, or null when no live read is possible. */
    public static float[] readLocalPlayerPosition() {
        try {
            float[] value = nativeReadLocalPlayerPosition();
            return value != null && value.length >= 3 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /** The local player's view rotation as {yaw, pitch} degrees, or null when unavailable. */
    public static float[] readLocalPlayerRotation() {
        try {
            float[] value = nativeReadLocalPlayerRotation();
            return value != null && value.length >= 2 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * The local player's health, or null when no live read is possible.
     *
     * <p>Fail-closed in two ways: the native library being absent reads as "no data", and the
     * native side reports "no data" when the per-version health offset is not configured or the
     * field does not hold a plausible health value. A highlight trigger therefore never fires
     * from an unverified offset.
     */
    public static float[] readLocalPlayerHealth() {
        try {
            float[] value = nativeReadLocalPlayerHealth();
            return value != null && value.length >= 1 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * Asks the game to re-read its active resource packs.
     *
     * @return true when the running game refreshed; false when no live hook is installed, so the
     *         caller reports the change will apply on the next world load instead of implying it
     *         took effect now.
     */
    public static boolean reloadResourcePacks() {
        try {
            return nativeReloadResourcePacks();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * True once the game's own player-model renderer has run this session.
     *
     * <p>Fail-closed: an unavailable native library, an unresolved slot, or a build whose
     * renderer never ran all read as false, so the launcher keeps the resource-pack path rather
     * than assuming a native path it does not have.
     */
    public static boolean isPlayerRenderHookLive() {
        try {
            return nativeIsPlayerRenderHookLive();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * True once the engine's present / frame-boundary hook is installed.
     *
     * <p>Fail-closed: an unavailable native library, or a build with no libEGL present call, reads as
     * false, so frame generation and motion blur stay badged rather than acting on a frame boundary
     * that was never observed.
     */
    public static boolean isPresentHookLive() {
        try {
            return nativeIsPresentHookLive();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * Pushes the present-frame effect plan to the native seam.
     *
     * <p>Fail-closed: an unavailable native library is a no-op, so the plan simply never reaches a
     * seam and both modules stay badged.
     */
    public static void setPresentFramePlan(boolean frameGenerationEnabled, float interpolationStrength,
                                           int maxFramesPerRendered, boolean reduceEdgeArtifacts,
                                           boolean motionBlurEnabled, float blurStrength,
                                           float blurBudget, boolean weightByViewSpeed) {
        try {
            nativeSetPresentFramePlan(frameGenerationEnabled, interpolationStrength,
                    maxFramesPerRendered, reduceEdgeArtifacts, motionBlurEnabled, blurStrength,
                    blurBudget, weightByViewSpeed);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:present-frame-plan",
                    "No native present-frame seam; the plan is not applied", e);
        }
    }

    /** True once the present-frame effect seam is installed and live. Fail-closed. */
    public static boolean isPresentFrameEffectLive() {
        try {
            return nativeIsPresentFrameEffectLive();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * The render tick and call counts, or null when the renderer has not run.
     *
     * @return {@code {renderTick, callsThisFrame, totalCalls, msSinceLastRender, tickExact}}, or
     *     null. {@code tickExact} is 1 when the tick is the engine's exact present boundary (the
     *     {@code eglSwapBuffers} hook is live) and 0 when it is inferred from render-entry timestamps.
     *     A reader that derives a frame rate must label an inferred tick as approximate. Older
     *     libraries returned four elements; a shorter array is accepted and treated as inexact.
     */
    public static int[] readPlayerRenderStats() {
        try {
            int[] value = nativeReadPlayerRenderStats();
            return value != null && value.length >= 4 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * Whether the render tick is the engine's exact present boundary rather than an inference.
     *
     * <p>Fail-closed: a missing library, a null reading or a short array all read as false, so a
     * derived frame rate is labelled approximate rather than presented as the game's exact rate.
     */
    public static boolean isRenderTickExact() {
        int[] value = readPlayerRenderStats();
        return value != null && value.length >= 5 && value[4] == 1;
    }

    /**
     * Samples the native cape chain, or null when the native library is unavailable.
     *
     * <p>The preview and the native render share one motion curve through this call, so the two
     * cannot disagree about how far the cloth leans.
     */
    public static float[] sampleCapeChain(double moveSpeed, boolean jumping, double verticalSpeed,
                                          double distanceMoved, double capeFlap,
                                          double bodyYawDegrees, int segments) {
        try {
            float[] value = nativeSampleCapeChain(moveSpeed, jumping, verticalSpeed, distanceMoved,
                    capeFlap, bodyYawDegrees, segments);
            return value != null && value.length >= 2 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    // --- Guarded cosmetics registry wrappers -----------------------------------------------
    // Each returns a safe default on a build whose native library lacks the symbol, so the
    // cosmetics path degrades to the resource pack rather than throwing into the UI.

    public static void setCapeOverride(long playerKey, byte[] rgba, int width, int height) {
        try {
            nativeSetCapeOverride(playerKey, rgba, width, height);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:609", "Ignored a failure at PreloaderInput:609", e);
        }
    }

    public static void clearCapeOverrides() {
        try {
            nativeClearCapeOverrides();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:617", "Ignored a failure at PreloaderInput:617", e);
        }
    }

    /** Removes one player's cape override; falls back to clearing all on an older native library. */
    public static void removeCapeOverride(long playerKey) {
        try {
            nativeRemoveCapeOverride(playerKey);
        } catch (UnsatisfiedLinkError e) {
            clearCapeOverrides();
        }
    }

    public static int capeOverrideCount() {
        try {
            return nativeCapeOverrideCount();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    public static void setTextureOverride(long textureId, byte[] rgba, int width, int height) {
        try {
            nativeSetTextureOverride(textureId, rgba, width, height);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:642", "Ignored a failure at PreloaderInput:642", e);
        }
    }

    public static void clearTextureOverrides() {
        try {
            nativeClearTextureOverrides();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:650", "Ignored a failure at PreloaderInput:650", e);
        }
    }

    /** Removes one texture override; falls back to clearing all on an older native library. */
    public static void removeTextureOverride(long textureId) {
        try {
            nativeRemoveTextureOverride(textureId);
        } catch (UnsatisfiedLinkError e) {
            clearTextureOverrides();
        }
    }

    public static int textureOverrideCount() {
        try {
            return nativeTextureOverrideCount();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    public static void setRenderGeometry(byte[] data) {
        try {
            nativeSetRenderGeometry(data);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:675", "Ignored a failure at PreloaderInput:675", e);
        }
    }

    /** Pushes one cosmetic frame; a no-op on a build without the native symbol. */
    public static void pushCosmeticFrame(byte[] frame) {
        try {
            nativePushCosmeticFrame(frame);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:684", "Ignored a failure at PreloaderInput:684", e);
        }
    }

    public static boolean swapCapeImage(long skinRefAddress, byte[] imageBytes) {
        try {
            return nativeSwapCapeImage(skinRefAddress, imageBytes);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static long imageLoaderAddress() {
        try {
            return nativeImageLoaderAddress();
        } catch (UnsatisfiedLinkError e) {
            return 0L;
        }
    }

    public static byte[] buildCapeImage(byte[] pngBytes) {
        try {
            return nativeBuildCapeImage(pngBytes);
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    public static boolean isCosmeticsHookLive() {
        try {
            return nativeIsCosmeticsHookLive();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** Sets one local piece's texture (0 cape, 1 hat, 2 pet); no-op on a build without the symbol. */
    public static void setPieceAtlas(int piece, byte[] rgba, int width, int height) {
        try {
            nativeSetPieceAtlas(piece, rgba, width, height);
        } catch (UnsatisfiedLinkError ignored) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:725", "Ignored a failure at PreloaderInput:725", ignored);
        }
    }

    /** Clears all three per-piece local textures; no-op on a build without the symbol. */
    public static void clearPieceAtlases() {
        try {
            nativeClearPieceAtlases();
        } catch (UnsatisfiedLinkError ignored) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:734", "Ignored a failure at PreloaderInput:734", ignored);
        }
    }

    public static int[] readCosmeticsStats() {
        try {
            int[] value = nativeReadCosmeticsStats();
            return value != null && value.length >= 4 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * The skin-path seam observation, or null when the native library is absent.
     *
     * <p>{@code {stage, packetHookInstalled, packetCalls, textureCalls, skinCaptured}}; the stage is
     * 0 join hook absent, 1 hook idle, 2 joins observed.
     */
    public static int[] readSkinSeam() {
        try {
            int[] value = nativeReadSkinSeam();
            return value != null && value.length >= 5 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /** The one-line explanation for a skin-seam stage, or "" when the library is absent. */
    public static String describeSkinSeam(int stage) {
        try {
            String text = nativeDescribeSkinSeam(stage);
            return text == null ? "" : text;
        } catch (UnsatisfiedLinkError e) {
            return "";
        }
    }

    /** The native render-path diagnostic, or null when the native library is absent. */
    public static int[] readRenderDiagnostics() {
        try {
            int[] value = nativeReadRenderDiagnostics();
            if (value == null || value.length < 9) return null;
            if (value.length >= 17) return value;
            // A native library older than this launcher returns fewer fields; pad the additions with
            // -1 ("unknown") so the report can read a fixed layout either way.
            int[] padded = new int[17];
            System.arraycopy(value, 0, padded, 0, value.length);
            for (int i = value.length; i < padded.length; i++) padded[i] = -1;
            return padded;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * The exact gate that refused the native render hook, for the diagnostics report:
     * 0 live (or reason predates the field), 1 version gate, 2 slot unresolved, 3 not executable,
     * 4 hook() failed; -1 when the native library is absent.
     */
    public static int renderHookFailureReason() {
        try {
            return nativeRenderHookFailureReason();
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    /** Forwards the display refresh rate to the native frame-pacing reading. */
    public static void setDisplayRefreshHz(int hz) {
        try {
            nativeSetDisplayRefreshHz(hz);
        } catch (UnsatisfiedLinkError ignored) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:807", "Ignored a failure at PreloaderInput:807", ignored);
        }
    }

    /**
     * The render-thread frame-pacing reading, or null when there is no reading yet. The
     * array is {valid, averageUs, worstUs, flags}; flags bit0 = jittering, bit1 = vsync-locked.
     */
    public static int[] readFramePacing() {
        try {
            int[] value = nativeReadFramePacing();
            return value != null && value.length >= 4 && value[0] == 1 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * The percentile + hitch reading, or null when there is no reading yet. The
     * array is {valid, p50Us, p95Us, p99Us, hitchesPerMinute}.
     */
    /**
     * The process footprint, or null when no source could be read. The array is
     * {valid, residentKb, proportionalKb, fromPss}.
     */
    public static int[] readProcessMemory() {
        try {
            int[] value = nativeReadProcessMemory();
            return value != null && value.length >= 4 && value[0] == 1 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    public static int[] readFramePacingPercentiles() {
        try {
            int[] value = nativeReadFramePacingPercentiles();
            return value != null && value.length >= 5 && value[0] == 1 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * The local-player feed as {live, xBits, yBits, zBits, yawBits, pitchBits}, or null when the
     * library is absent. Floats travel as their raw int bits; decode with
     * {@link Float#intBitsToFloat(int)}.
     */
    public static int[] readLocalPlayerFeed() {
        try {
            int[] value = nativeReadLocalPlayerFeed();
            return value != null && value.length >= 6 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * Whether the cosmetic anchor for the last frame came from the engine's view-projection.
     *
     * @return 1 engine, 0 first-person fallback, -1 unknown (library or frame absent)
     */
    public static int cameraAnchorSource() {
        try {
            return nativeCameraAnchorSource();
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    /**
     * The classified GPU family, or -1 when the native side has not read the driver
     * strings yet (no library, or no GLES context at the time of the call).
     *
     * <p>Map it with {@link org.chimeramc.client.core.cosmetics.GpuFamily#fromNativeCode}; a
     * negative reading means "unknown", never a family.
     */
    public static int gpuFamily() {
        try {
            return nativeGpuFamily();
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    // --- The mce::Image pipeline (image-buffer substitution) --------------------------------
    // These drive the engine-owned pixel substitution: the launcher registers "whenever the
    // engine loads these exact source bytes, build the image from this RGBA instead", so a
    // custom cape is handed to the engine at upload time and the engine still owns the lifetime.
    // Each call is guarded, so a build without the native library degrades to the pack path.

    /** Native declaration: registers a source-bytes -> replacement-RGBA substitution. */
    public static native void nativeSetContentSubstitution(byte[] sourceBytes, byte[] rgba,
                                                           int width, int height);

    /** Native declaration: clears every content-addressed substitution. */
    public static native void nativeClearContentSubstitutions();

    /** Native declaration: number of registered content substitutions. */
    public static native int nativeSubstitutionCount();

    /** Native declaration: arms the one-shot override for the very next engine image build. */
    public static native void nativeArmNextImageOverride(byte[] rgba, int width, int height);

    /** Native declaration: disarms the one-shot override. */
    public static native void nativeClearNextImageOverride();

    /** Native declaration: true once the mce::Image pipeline hook has run this session. */
    public static native boolean nativeIsMceImageHookLive();

    /** Native declaration: true when the init probe verified the engine image loader. */
    public static native boolean nativeIsImagePathVerified();

    /** Native declaration: {hookCalls, substitutions, overrides, bufferBytesSeen}. */
    public static native int[] nativeReadMceImageHookStats();

    /** Native declaration: true when a proven texture-cache flusher is installed. */
    public static native boolean nativeIsTextureCacheFlushAvailable();

    /** Native declaration: requests a texture-cache refresh; true when a flusher ran. */
    public static native boolean nativeRequestTextureCacheFlush();

    /** Registers a source -> replacement substitution, or clears it when rgba is null. */
    public static void setContentSubstitution(byte[] sourceBytes, byte[] rgba,
                                              int width, int height) {
        try {
            nativeSetContentSubstitution(sourceBytes, rgba, width, height);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:935", "Ignored a failure at PreloaderInput:935", e);
        }
    }

    public static void clearContentSubstitutions() {
        try {
            nativeClearContentSubstitutions();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:943", "Ignored a failure at PreloaderInput:943", e);
        }
    }

    public static int substitutionCount() {
        try {
            return nativeSubstitutionCount();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    public static void armNextImageOverride(byte[] rgba, int width, int height) {
        try {
            nativeArmNextImageOverride(rgba, width, height);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:959", "Ignored a failure at PreloaderInput:959", e);
        }
    }

    public static void clearNextImageOverride() {
        try {
            nativeClearNextImageOverride();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:967", "Ignored a failure at PreloaderInput:967", e);
        }
    }

    /** The player's captured cape texture bytes, or null when none is held. */
    public static byte[] readCapturedCape() {
        try {
            return nativeReadCapturedCape();
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    public static boolean hasCapturedCape() {
        try {
            return nativeHasCapturedCape();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static void clearCapturedCape() {
        try {
            nativeClearCapturedCape();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:992", "Ignored a failure at PreloaderInput:992", e);
        }
    }

    /** Registers the local player's custom cape pixels; a null/empty panel clears the substitution. */
    public static void setCapeSubstitution(byte[] rgba, int width, int height) {
        try {
            nativeSetCapeSubstitution(rgba, width, height);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:1001", "Ignored a failure at PreloaderInput:1001", e);
        }
    }

    public static void clearCapeSubstitution() {
        try {
            nativeClearCapeSubstitution();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:1009", "Ignored a failure at PreloaderInput:1009", e);
        }
    }

    public static int capeSubstitutionCount() {
        try {
            return nativeCapeSubstitutionCount();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    /** {hasCapture, capturedBytes, width | (substitutions << 32), height}, or null when absent. */
    public static long[] readCapeCaptureDiagnostics() {
        try {
            long[] value = nativeReadCapeCaptureDiagnostics();
            return value != null && value.length >= 4 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    public static boolean isMceImageHookLive() {
        try {
            return nativeIsMceImageHookLive();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean isImagePathVerified() {
        try {
            return nativeIsImagePathVerified();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static int[] readMceImageHookStats() {
        try {
            int[] value = nativeReadMceImageHookStats();
            return value != null && value.length >= 4 ? value : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    public static boolean isTextureCacheFlushAvailable() {
        try {
            return nativeIsTextureCacheFlushAvailable();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean requestTextureCacheFlush() {
        try {
            return nativeRequestTextureCacheFlush();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean onTouch(int action, int pointerId, float x, float y) {
        try {
            return nativeOnTouch(action, pointerId, x, y);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean onKeyEvent(int keyCode, int unicodeChar, boolean isKeyDown) {
        try {
            return nativeOnKeyEvent(keyCode, unicodeChar, isKeyDown);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean onTextInput(CharSequence text) {
        if (text == null || text.length() == 0) {
            return false;
        }
        try {
            return nativeOnTextInput(text.toString());
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static void onDocumentResult(boolean success, String path, String displayName, String error) {
        try {
            nativeOnDocumentResult(
                    success,
                    path == null ? "" : path,
                    displayName == null ? "" : displayName,
                    error == null ? "" : error
            );
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:1108", "Ignored a failure at PreloaderInput:1108", e);
        }
    }

    public static void cancelDocumentRequest(String reason) {
        onDocumentResult(false, "", "", reason == null ? "Import cancelled" : reason);
    }

    private static boolean dispatchMouseTransition(int button, boolean isDown) {
        // A duplicate transition (the same state as tracked) must not reach the native side, so the
        // state machine decides that before the call.
        boolean alreadyDown = (MOUSE_STATE.buttonsDown() & button) != 0;
        if (alreadyDown == isDown) {
            return MOUSE_STATE.lastTransitionConsumed(button);
        }
        boolean consumed;
        try {
            consumed = nativeOnMouse(button, isDown);
        } catch (UnsatisfiedLinkError e) {
            consumed = false;
        }
        return MOUSE_STATE.transition(button, isDown, consumed);
    }

    public static synchronized boolean onMouse(int button, boolean isDown) {
        if (button == 0) {
            return false;
        }
        return dispatchMouseTransition(button, isDown);
    }

    public static synchronized boolean onMouseMotion(int action, int actionButton, int buttonState) {
        int normalizedButtonState = buttonState;
        if (action == MotionEvent.ACTION_DOWN && normalizedButtonState == 0 && actionButton == 0) {
            normalizedButtonState = MotionEvent.BUTTON_PRIMARY;
        }
        if (action == MotionEvent.ACTION_BUTTON_PRESS) {
            normalizedButtonState |= actionButton;
        } else if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
            normalizedButtonState &= ~actionButton;
        } else if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_POINTER_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            normalizedButtonState = 0;
        }

        MOUSE_STATE.diff(normalizedButtonState, MOUSE_DIFF);
        int pressedButtons = MOUSE_DIFF[0];
        int releasedButtons = MOUSE_DIFF[1];
        int blockedBeforeTransitions = MOUSE_STATE.blockedButtons();
        boolean transitionConsumed = false;

        for (int button : PreloaderMouseState.buttons()) {
            if ((pressedButtons & button) != 0) {
                transitionConsumed |= dispatchMouseTransition(button, true);
            }
        }
        for (int button : PreloaderMouseState.buttons()) {
            if ((releasedButtons & button) != 0) {
                transitionConsumed |= dispatchMouseTransition(button, false);
            }
        }

        boolean blockedButtonActive = (normalizedButtonState & MOUSE_STATE.blockedButtons()) != 0;
        boolean duplicateTransitionConsumed = actionButton != 0 &&
                MOUSE_STATE.lastTransitionConsumed(actionButton);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_BUTTON_PRESS:
                if (transitionConsumed || blockedButtonActive || duplicateTransitionConsumed) {
                    MOUSE_STATE.setBlockedPointerStream(true);
                }
                return MOUSE_STATE.blockedPointerStream();
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_HOVER_MOVE:
                return MOUSE_STATE.blockedPointerStream() || blockedButtonActive;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_BUTTON_RELEASE:
            case MotionEvent.ACTION_CANCEL:
                boolean consumeRelease = transitionConsumed ||
                        MOUSE_STATE.blockedPointerStream() ||
                        blockedBeforeTransitions != 0 ||
                        duplicateTransitionConsumed;
                if (MOUSE_STATE.buttonsDown() == 0 || action == MotionEvent.ACTION_CANCEL) {
                    MOUSE_STATE.setBlockedPointerStream(false);
                }
                return consumeRelease;
            default:
                return MOUSE_STATE.blockedPointerStream() || blockedButtonActive;
        }
    }

    public static synchronized void resetMouseState() {
        MOUSE_STATE.reset();
    }

    public static void setActivity(Object activity) {
        resetMouseState();
        try {
            nativeSetActivity(activity);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:1212", "Ignored a failure at PreloaderInput:1212", e);
        }
    }

    public static void clearActivity() {
        resetMouseState();
        try {
            nativeClearActivity();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce("CaramelClient", "PreloaderInput:1221", "Ignored a failure at PreloaderInput:1221", e);
        }
    }
}
