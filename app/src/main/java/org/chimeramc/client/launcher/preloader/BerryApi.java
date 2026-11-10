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

    // --- Replay capture (recorder) surface --------------------------------------------------
    //
    // The other half of the replay system: BerryAPI records the session's packet stream, and the
    // launcher drives that here. A Bedrock session's entity spawns, movement, block updates and
    // inventory all arrive as packets, so the stream is the world data. Every call answers the
    // fail-closed default when the library or its capture ABI is absent, so a screen shows
    // "record unavailable" rather than driving a null.

    /** The ABI status a call reports when the surface (or a pointer) is absent. */
    private static final int REPLAY_STATUS_NOT_ATTACHED = 1;

    /** The ABI status a call reports when an argument is rejected. */
    private static final int REPLAY_STATUS_BAD_ARGUMENT = 3;

    /** True when BerryAPI's capture (recorder) surface resolved and passed its ABI check. */
    public static boolean replayCaptureAvailable() {
        try {
            return nativeReplayCaptureAvailable();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** Begins a capture session. Returns 0 on success, or a fail-closed status when unavailable. */
    public static int replayCaptureStart() {
        try {
            return nativeReplayCaptureStart();
        } catch (UnsatisfiedLinkError e) {
            return REPLAY_STATUS_NOT_ATTACHED;
        }
    }

    /** Ends a capture session, keeping the captured replay for {@link #replaySave()}. */
    public static int replayCaptureStop() {
        try {
            return nativeReplayCaptureStop();
        } catch (UnsatisfiedLinkError e) {
            return REPLAY_STATUS_NOT_ATTACHED;
        }
    }

    /**
     * The capture state, or -1 when unavailable.
     *
     * <p>0 idle, 1 recording, 2 throttled, 3 stopped.
     */
    public static int replayCaptureState() {
        try {
            return nativeReplayCaptureState();
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    /**
     * Records one packet. {@code dir} is 0 inbound, 1 outbound; {@code payload} is the body verbatim.
     *
     * @return 0 when stored, or a fail-closed status when unavailable or the payload is empty
     */
    public static int replayCapturePacket(int dir, int id, byte[] payload, long tick) {
        if (payload == null || payload.length == 0) {
            return REPLAY_STATUS_BAD_ARGUMENT;
        }
        try {
            return nativeReplayCapturePacket(dir, id, payload, tick);
        } catch (UnsatisfiedLinkError e) {
            return REPLAY_STATUS_NOT_ATTACHED;
        }
    }

    private static native boolean nativeReplayCaptureAvailable();

    private static native int nativeReplayCaptureStart();

    private static native int nativeReplayCaptureStop();

    private static native int nativeReplayCaptureState();

    private static native int nativeReplayCapturePacket(int dir, int id, byte[] payload, long tick);

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

    // --- Cosmetics surface -------------------------------------------------------------------
    //
    // BerryAPI owns the cosmetics service: the catalog, the local player's loadout, the wardrobe
    // presets, the remote-render gate and the service counters. The launcher's cosmetics screen
    // drives it through these. Every call answers the fail-closed default when the library or its
    // cosmetic ABI is absent, so the screen shows "not available" rather than crashing. The
    // player-model hook is installed by the preloader itself once the game's player-render target
    // resolves; Java only reads its state here.

    /** The cosmetic slot values, mirroring {@code BerryCosmeticSlotC}. The values are the ABI. */
    public static final int SLOT_NONE = 0;
    public static final int SLOT_HEAD = 1;
    public static final int SLOT_BACK = 2;
    public static final int SLOT_SHOULDERS = 3;
    public static final int SLOT_BODY = 4;
    public static final int SLOT_TRAIL = 5;
    public static final int SLOT_EFFECT = 6;
    public static final int SLOT_EMOTE = 7;

    /** Cosmetic status codes, mirroring {@code BerryCosmeticStatus}. 0 is success. */
    public static final int COSMETIC_OK = 0;
    public static final int COSMETIC_NOT_ATTACHED = 1;
    public static final int COSMETIC_BAD_ARGUMENT = 2;
    public static final int COSMETIC_UNKNOWN_ASSET = 3;
    public static final int COSMETIC_NOT_OWNED = 4;
    public static final int COSMETIC_BAD_SLOT = 5;
    public static final int COSMETIC_BAD_FORMAT = 6;
    public static final int COSMETIC_UNKNOWN_PRESET = 7;
    public static final int COSMETIC_BUFFER_TOO_SMALL = 8;
    public static final int COSMETIC_NO_HOOK_BACKEND = 9;
    public static final int COSMETIC_HOOK_REFUSED = 10;
    public static final int COSMETIC_ALREADY_HOOKED = 11;
    public static final int COSMETIC_BAD_READER = 12;

    /** One catalog asset, decoded from the {@code String[20]} the native catalog returns. */
    public static final class CosmeticRecord {
        public final String id;
        public final int slot;
        public final int bone;
        public final long geometry;
        public final long texture;
        public final float offsetX;
        public final float offsetY;
        public final float offsetZ;
        public final float pitch;
        public final float yaw;
        public final float roll;
        public final float scale;
        public final float tintR;
        public final float tintG;
        public final float tintB;
        public final float trail;
        public final String animation;
        public final float animationSpeed;
        public final String pack;
        public final String author;

        CosmeticRecord(String[] f) {
            this.id = f[0];
            this.slot = intOf(f[1]);
            this.bone = intOf(f[2]);
            this.geometry = longOf(f[3]);
            this.texture = longOf(f[4]);
            this.offsetX = floatOf(f[5]);
            this.offsetY = floatOf(f[6]);
            this.offsetZ = floatOf(f[7]);
            this.pitch = floatOf(f[8]);
            this.yaw = floatOf(f[9]);
            this.roll = floatOf(f[10]);
            this.scale = floatOf(f[11]);
            this.tintR = floatOf(f[12]);
            this.tintG = floatOf(f[13]);
            this.tintB = floatOf(f[14]);
            this.trail = floatOf(f[15]);
            this.animation = f[16];
            this.animationSpeed = floatOf(f[17]);
            this.pack = f[18];
            this.author = f[19];
        }

        private static int intOf(String v) {
            try {
                return (int) Float.parseFloat(v);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static long longOf(String v) {
            try {
                return (long) Double.parseDouble(v);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        private static float floatOf(String v) {
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
    }

    /** The cosmetic counters, decoded from the {@code long[8]} the native stats call returns. */
    public static final class CosmeticCounters {
        public final long frames;
        public final long submitted;
        public final long skippedNoBone;
        public final long skippedNotOwned;
        public final long cacheHits;
        public final long cacheMisses;
        public final long registryCount;
        public final long presetCount;

        CosmeticCounters(long[] v) {
            this.frames = v[0];
            this.submitted = v[1];
            this.skippedNoBone = v[2];
            this.skippedNotOwned = v[3];
            this.cacheHits = v[4];
            this.cacheMisses = v[5];
            this.registryCount = v[6];
            this.presetCount = v[7];
        }
    }

    /** True when BerryAPI's cosmetics surface resolved and passed its ABI check. */
    public static boolean cosmeticAvailable() {
        try {
            return nativeCosmeticAvailable();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * Reloads the catalog from a pack directory and an asset directory.
     *
     * <p>Returns the number of assets registered, or 0 when the surface is absent or the load was
     * refused. A null argument asks the library to use its own configured default.
     */
    public static int cosmeticReload(String packDir, String assetDir) {
        try {
            return nativeCosmeticReload(packDir, assetDir);
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    /** The number of registered cosmetic assets, or 0 when unavailable. */
    public static int cosmeticRegistryCount() {
        try {
            return nativeCosmeticRegistryCount();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    /** The catalog, decoded; empty when unavailable. */
    public static java.util.List<CosmeticRecord> cosmeticCatalog() {
        java.util.List<CosmeticRecord> out = new java.util.ArrayList<>();
        try {
            String[][] rows = nativeCosmeticCatalog();
            if (rows == null) return out;
            for (String[] row : rows) {
                if (row != null && row.length >= 20) out.add(new CosmeticRecord(row));
            }
        } catch (UnsatisfiedLinkError e) {
            // best-effort: fail closed to an empty catalog when the library is absent.
        }
        return out;
    }

    /** Equips an asset on the local player. Returns a {@code COSMETIC_*} status. */
    public static int cosmeticEquip(String assetId) {
        try {
            return nativeCosmeticEquip(assetId);
        } catch (UnsatisfiedLinkError e) {
            return COSMETIC_NOT_ATTACHED;
        }
    }

    /** Removes whatever is in {@code slot}. Returns a {@code COSMETIC_*} status. */
    public static int cosmeticUnequip(int slot) {
        try {
            return nativeCosmeticUnequip(slot);
        } catch (UnsatisfiedLinkError e) {
            return COSMETIC_NOT_ATTACHED;
        }
    }

    /** The asset id equipped in {@code slot}; "" when empty or unavailable. */
    public static String cosmeticEquipped(int slot) {
        try {
            String id = nativeCosmeticEquipped(slot);
            return id != null ? id : "";
        } catch (UnsatisfiedLinkError e) {
            return "";
        }
    }

    /** Empties the local loadout. Returns a {@code COSMETIC_*} status. */
    public static int cosmeticClearLoadout() {
        try {
            return nativeCosmeticClearLoadout();
        } catch (UnsatisfiedLinkError e) {
            return COSMETIC_NOT_ATTACHED;
        }
    }

    /** Saves the current loadout as {@code name}. Returns a {@code COSMETIC_*} status. */
    public static int cosmeticSavePreset(String name) {
        try {
            return nativeCosmeticSavePreset(name);
        } catch (UnsatisfiedLinkError e) {
            return COSMETIC_NOT_ATTACHED;
        }
    }

    /** Replaces the loadout with preset {@code name}. Returns a {@code COSMETIC_*} status. */
    public static int cosmeticApplyPreset(String name) {
        try {
            return nativeCosmeticApplyPreset(name);
        } catch (UnsatisfiedLinkError e) {
            return COSMETIC_NOT_ATTACHED;
        }
    }

    /** 1 when preset {@code name} exists, 0 when not, -1 when unavailable. */
    public static int cosmeticHasPreset(String name) {
        try {
            return nativeCosmeticHasPreset(name);
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    /** The wardrobe preset names; empty when unavailable. */
    public static String[] cosmeticPresetNames() {
        try {
            String[] names = nativeCosmeticPresetNames();
            return names != null ? names : new String[0];
        } catch (UnsatisfiedLinkError e) {
            return new String[0];
        }
    }

    /** Sets the remote-render gate. Returns a {@code COSMETIC_*} status. */
    public static int cosmeticSetLocalOnly(boolean localOnly) {
        try {
            return nativeCosmeticSetLocalOnly(localOnly);
        } catch (UnsatisfiedLinkError e) {
            return COSMETIC_NOT_ATTACHED;
        }
    }

    /** The local-only render gate: 1 on, 0 off, -1 when unavailable. */
    public static int cosmeticLocalOnly() {
        try {
            return nativeCosmeticLocalOnly();
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    /** The service counters, or null when unavailable. */
    public static CosmeticCounters cosmeticStats() {
        try {
            long[] v = nativeCosmeticStats();
            return v != null && v.length == 8 ? new CosmeticCounters(v) : null;
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    /** 1 when a player-model hook is installed, 0 when not, -1 when unavailable. */
    public static int cosmeticHasPlayerModelHook() {
        try {
            return nativeCosmeticHasPlayerModelHook();
        } catch (UnsatisfiedLinkError e) {
            return -1;
        }
    }

    private static native boolean nativeCosmeticAvailable();

    private static native int nativeCosmeticReload(String packDir, String assetDir);

    private static native int nativeCosmeticRegistryCount();

    private static native String[][] nativeCosmeticCatalog();

    private static native int nativeCosmeticEquip(String assetId);

    private static native int nativeCosmeticUnequip(int slot);

    private static native String nativeCosmeticEquipped(int slot);

    private static native int nativeCosmeticClearLoadout();

    private static native int nativeCosmeticSavePreset(String name);

    private static native int nativeCosmeticApplyPreset(String name);

    private static native int nativeCosmeticHasPreset(String name);

    private static native String[] nativeCosmeticPresetNames();

    private static native int nativeCosmeticSetLocalOnly(boolean localOnly);

    private static native int nativeCosmeticLocalOnly();

    private static native long[] nativeCosmeticStats();

    private static native int nativeCosmeticHasPlayerModelHook();
}
