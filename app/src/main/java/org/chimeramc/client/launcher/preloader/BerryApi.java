package org.chimeramc.client.preloader;

/**
 * Bridge to the bundled BerryAPI native runtime ({@code libBerryAPI.so}).
 *
 * <p>BerryAPI is a separate Bedrock client runtime with its own version, so the launcher never links
 * it directly; the preloader {@code dlopen}s it and reports what it found over this small surface.
 * Every call is a thin read of a cached native answer, so they are cheap enough for the Diagnostics
 * screen and the launch preflight.
 *
 * <p><b>Fail-closed.</b> When the library is missing, or its C ABI does not resolve, {@link
 * #isAvailable()} is false and every capability is clear. Nothing here fabricates a reading, so a
 * feature that depends on BerryAPI is greyed out rather than appearing to work.
 */
public final class BerryApi {

    /** Capability bits, mirroring {@code pl::berry::Capability}. */
    public static final int CAP_LIBRARY_LOADED = 1;
    public static final int CAP_ATTACHED = 1 << 1;
    /**
     * A world/entity seam is bound. Set once {@link #bindWorldSeam()} has bound the launcher's
     * local-player seam into BerryAPI; until then the world/entity reads have nothing to read from
     * and the dependent modules stay unavailable.
     */
    public static final int CAP_WORLD_FEED = 1 << 2;

    private BerryApi() {
    }

    // --- Replay surface ---------------------------------------------------------------------
    //
    // BerryAPI owns the replay system (load a replay, seek it, play it, read its timeline). The
    // launcher's replay screen drives it through these; every call answers the fail-closed default
    // (false / empty) when the library or its replay ABI is absent, so the screen shows "not
    // available" rather than crashing.

    /** True when BerryAPI's replay surface resolved and passed its ABI check. */
    public static boolean replayAvailable() {
        try {
            return nativeReplayAvailable();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** True when a replay is loaded. */
    public static boolean replayLoaded() {
        try {
            return nativeReplayLoaded();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * The replay summary, or null when unavailable.
     *
     * <p>{loaded, firstTick, lastTick, sectionCount, recordCount, payloadBytes, buildId,
     * protocolVersion}.
     */
    public static long[] replayInfo() {
        try {
            long[] v = nativeReplayInfo();
            return v != null && v.length == 8 ? v : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /**
     * The playback state, or null when unavailable.
     *
     * <p>{active, playing, activeWorld, tick, speedBits}; the speed is a 32-bit float's bit pattern.
     */
    public static long[] replayPlayback() {
        try {
            long[] v = nativeReplayPlayback();
            return v != null && v.length == 5 ? v : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /** Decodes a replay from bytes. Returns a status code; 0 is success. */
    public static int replayLoad(byte[] bytes) {
        try {
            return nativeReplayLoad(bytes);
        } catch (UnsatisfiedLinkError e) {
            return 1; // not attached
        }
    }

    /** Starts playback. Returns a status code; 0 is success. */
    public static int replayPlay() {
        try {
            return nativeReplayPlay();
        } catch (UnsatisfiedLinkError e) {
            return 1;
        }
    }

    /** Seeks to an absolute tick. Returns a status code; 0 is success. */
    public static int replaySeek(long tick) {
        try {
            return nativeReplaySeek(tick);
        } catch (UnsatisfiedLinkError e) {
            return 1;
        }
    }

    public static void replaySetSpeed(float speed) {
        try {
            nativeReplaySetSpeed(speed);
        } catch (UnsatisfiedLinkError e) {
            reportUnavailable("replaySetSpeed", e);
        }
    }

    public static void replayPause() {
        try {
            nativeReplayPause();
        } catch (UnsatisfiedLinkError e) {
            reportUnavailable("replayPause", e);
        }
    }

    public static void replayStop() {
        try {
            nativeReplayStop();
        } catch (UnsatisfiedLinkError e) {
            reportUnavailable("replayStop", e);
        }
    }

    public static void replayUnload() {
        try {
            nativeReplayUnload();
        } catch (UnsatisfiedLinkError e) {
            reportUnavailable("replayUnload", e);
        }
    }

    /**
     * Reports a replay call that could not reach the library.
     *
     * <p>Once per call kind, so a screen that drives the surface while the library is absent does not
     * flood the log; the caller already knows from {@link #replayAvailable()} whether to expect it.
     */
    private static void reportUnavailable(String call, UnsatisfiedLinkError cause) {
        org.chimeramc.client.util.AppLog.warnOnce("BerryApi", "replay:" + call,
                "BerryAPI replay call " + call + " could not reach the library", cause);
    }

    /** The library's replay status line, or an empty string. */
    public static String replayStatus() {
        try {
            String s = nativeReplayStatus();
            return s == null ? "" : s;
        } catch (UnsatisfiedLinkError e) {
            return "";
        }
    }

    private static native boolean nativeReplayAvailable();

    private static native boolean nativeReplayLoaded();

    private static native long[] nativeReplayInfo();

    private static native long[] nativeReplayPlayback();

    private static native int nativeReplayLoad(byte[] bytes);

    private static native int nativeReplayPlay();

    private static native int nativeReplaySeek(long tick);

    private static native void nativeReplaySetSpeed(float speed);

    private static native void nativeReplayPause();

    private static native void nativeReplayStop();

    private static native void nativeReplayUnload();

    private static native String nativeReplayStatus();

    /**
     * Encodes the library's replay — the loaded one, or its capture of the live stream — to bytes.
     *
     * <p>This is the recorder half: BerryAPI's replay service keeps a capture of the session, and its
     * {@code save} exports it. The launcher writes the bytes to a {@code .glowreplay} file, so a
     * recording made inside the game becomes a replay the launcher can list and play.
     *
     * @return the container bytes, or an empty array when there is nothing to save or the surface is
     *         absent
     */
    public static byte[] replaySave() {
        try {
            byte[] bytes = nativeReplaySave();
            return bytes != null ? bytes : new byte[0];
        } catch (UnsatisfiedLinkError e) {
            return new byte[0];
        }
    }

    private static native byte[] nativeReplaySave();

    // --- Governor surface -------------------------------------------------------------------
    //
    // BerryAPI's adaptive quality governor. The launcher reads it for its FPS diagnostics and can
    // bound it. Every call answers the fail-closed default when the library or its governor ABI is
    // absent.

    /** True when BerryAPI's governor surface resolved and passed its ABI check. */
    public static boolean governorAvailable() {
        try {
            return nativeGovernorAvailable();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * The governor reading, or null when unavailable.
     *
     * <p>{qualityParticles, qualityEntityDistance, qualityAnimationDistance, qualityEntityCulling,
     * qualityLighting, qualityRenderDistance, thermal, thermallyLimited, platform, ceilingBits,
     * renderDistanceChunks, maxParticles, entityDistanceBits, animationDistanceBits}. The three
     * floats cross as their bit patterns.
     */
    public static long[] governorStatus() {
        try {
            long[] v = nativeGovernorStatus();
            return v != null && v.length == 14 ? v : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /** Reports platform thermal pressure. Returns the action taken, or -1 when unavailable. */
    public static int governorSetThermal(int state) {
        try {
            return nativeGovernorSetThermal(state);
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    /** Sets the device class the governor tunes for. */
    public static void governorSetPlatform(int platform) {
        try {
            nativeGovernorSetPlatform(platform);
        } catch (UnsatisfiedLinkError e) {
            org.chimeramc.client.util.AppLog.warnOnce("BerryApi", "governor:platform",
                    "BerryAPI governor platform could not reach the library", e);
        }
    }

    /** Sets a hard ceiling the governor may not exceed, 0..1. */
    public static void governorSetCeiling(float ceiling) {
        try {
            nativeGovernorSetCeiling(ceiling);
        } catch (UnsatisfiedLinkError e) {
            org.chimeramc.client.util.AppLog.warnOnce("BerryApi", "governor:ceiling",
                    "BerryAPI governor ceiling could not reach the library", e);
        }
    }

    private static native boolean nativeGovernorAvailable();

    private static native long[] nativeGovernorStatus();

    private static native int nativeGovernorSetThermal(int state);

    private static native void nativeGovernorSetPlatform(int platform);

    private static native void nativeGovernorSetCeiling(float ceiling);

    /**
     * Loads the bundled library once and returns whether it is usable. Idempotent and safe from any
     * thread; the native side caches the answer.
     */
    public static boolean ensureLoaded() {
        try {
            return nativeEnsureLoaded();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** True when the library loaded and its C ABI resolved. */
    public static boolean isAvailable() {
        try {
            return nativeIsAvailable();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** Whether BerryAPI reports a live hook backend. False when absent or its own attach failed. */
    public static boolean isAttached() {
        try {
            return nativeIsAttached();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * Attaches BerryAPI if it is not already attached, at most once per process.
     *
     * <p>The bundled library is only {@code dlopen}ed, never {@code BERRY_Inject}ed, so before this
     * call {@link #isAttached()} is always false and none of its subsystems are live. This is the
     * explicit attach the launcher performs when a game session starts.
     *
     * <p>Fail-closed and never fatal: a missing library, a missing symbol or a build with no usable
     * hook backend returns false and leaves {@link #isAttached()} false, so the dependent features
     * stay greyed rather than half-installed.
     */
    public static boolean ensureAttached() {
        try {
            return nativeEnsureAttached();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * Binds the launcher's local-player seam into BerryAPI, so its world/entity reads can return
     * real values instead of failing closed.
     *
     * <p>Idempotent and fail-closed: a missing library, a missing seam symbol, or a struct-layout
     * mismatch returns false and leaves {@link #CAP_WORLD_FEED} clear. Call once at session start,
     * after {@link #ensureAttached()}. The world feed turns on only after a successful bind.
     */
    public static boolean bindWorldSeam() {
        try {
            return nativeBindWorldSeam();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** True when the launcher's local-player seam is bound into BerryAPI. */
    public static boolean isWorldSeamBound() {
        try {
            return nativeWorldSeamBound();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** BerryAPI's own version string, or an empty string when unavailable. */
    public static String version() {
        try {
            String v = nativeVersion();
            return v != null ? v : "";
        } catch (UnsatisfiedLinkError e) {
            return "";
        }
    }

    /** The capability mask; a missing library reads as {@code 0}. */
    public static int capabilities() {
        try {
            return nativeCapabilities();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    /** Whether one capability bit is set. */
    public static boolean hasCapability(int flag) {
        return hasBit(capabilities(), flag);
    }

    /**
     * Whether every bit in {@code flag} is set in {@code mask}.
     *
     * <p>Pure, so the bit rule is unit-tested without a device: a capability query must require the
     * whole flag, not any overlap, or a caller testing {@code ATTACHED} would pass on a mask that
     * only set an unrelated bit.
     */
    public static boolean hasBit(int mask, int flag) {
        return (mask & flag) == flag;
    }

    /** A one-line status for Diagnostics and About; never empty. */
    public static String summary() {
        try {
            String s = nativeSummary();
            return s != null && !s.isEmpty() ? s : "BerryAPI: not available";
        } catch (UnsatisfiedLinkError e) {
            return "BerryAPI: not available (preloader not loaded)";
        }
    }

    private static native boolean nativeEnsureLoaded();

    private static native boolean nativeIsAvailable();

    private static native boolean nativeIsAttached();

    private static native boolean nativeEnsureAttached();

    private static native String nativeVersion();

    private static native int nativeCapabilities();

    private static native String nativeSummary();

    private static native boolean nativeBindWorldSeam();

    private static native boolean nativeWorldSeamBound();
}
