package org.chimeramc.client.core.replay;

/**
 * The launcher's playback source for a BerryAPI-format replay.
 *
 * <p>The launcher's own recorder produces its clip format, played by {@code ReplayPlayerView}. BerryAPI
 * owns a second, richer replay system: it decodes a replay container, reconstructs a tick and draws a
 * review. That system is reached through the replay C ABI the preloader bridges
 * ({@code BerryApi.replay*}). This class is the one place the panel talks to it, so the panel does not
 * depend on the native surface directly and the routing is testable through {@link Backend}.
 *
 * <p><b>Fail-closed.</b> When the library or its replay ABI is absent, {@link #available()} is false,
 * every call answers the "not available" default and nothing is loaded. The panel keeps its own
 * recorder path, so a build without BerryAPI behaves exactly as before.
 */
public final class BerryReplaySource {

    /** The native surface, injected so the routing is testable without the library. */
    public interface Backend {
        boolean available();

        boolean loaded();

        /** {loaded, firstTick, lastTick, sections, records, bytes, buildId, protocol}, or null. */
        long[] info();

        /** {active, playing, activeWorld, tick, speedBits}, or null. */
        long[] playback();

        int load(byte[] bytes);

        int play();

        int seek(long tick);

        void pause();

        void stop();

        void unload();

        void setSpeed(float speed);

        String status();

        /**
         * Encodes the library's replay (the loaded one, or its capture) to container bytes.
         *
         * <p>The recorder half: a recording made inside the game is exported here so the launcher can
         * write it as a replay file and list it.
         */
        byte[] save();
    }

    /** The default backend: the preloader bridge onto BerryAPI. */
    public static final class NativeBackend implements Backend {
        @Override public boolean available() {
            return org.chimeramc.client.preloader.BerryApi.replayAvailable();
        }
        @Override public boolean loaded() {
            return org.chimeramc.client.preloader.BerryApi.replayLoaded();
        }
        @Override public long[] info() {
            return org.chimeramc.client.preloader.BerryApi.replayInfo();
        }
        @Override public long[] playback() {
            return org.chimeramc.client.preloader.BerryApi.replayPlayback();
        }
        @Override public int load(byte[] bytes) {
            return org.chimeramc.client.preloader.BerryApi.replayLoad(bytes);
        }
        @Override public int play() {
            return org.chimeramc.client.preloader.BerryApi.replayPlay();
        }
        @Override public int seek(long tick) {
            return org.chimeramc.client.preloader.BerryApi.replaySeek(tick);
        }
        @Override public void pause() {
            org.chimeramc.client.preloader.BerryApi.replayPause();
        }
        @Override public void stop() {
            org.chimeramc.client.preloader.BerryApi.replayStop();
        }
        @Override public void unload() {
            org.chimeramc.client.preloader.BerryApi.replayUnload();
        }
        @Override public void setSpeed(float speed) {
            org.chimeramc.client.preloader.BerryApi.replaySetSpeed(speed);
        }
        @Override public String status() {
            return org.chimeramc.client.preloader.BerryApi.replayStatus();
        }
        @Override public byte[] save() {
            return org.chimeramc.client.preloader.BerryApi.replaySave();
        }
    }

    /** One reading of the loaded replay, in plain values. */
    public static final class Snapshot {
        public final boolean loaded;
        public final long firstTick;
        public final long lastTick;
        public final int sections;
        public final int records;
        public final long payloadBytes;
        public final boolean playing;
        public final long tick;
        public final float speed;

        Snapshot(boolean loaded, long firstTick, long lastTick, int sections, int records,
                 long payloadBytes, boolean playing, long tick, float speed) {
            this.loaded = loaded;
            this.firstTick = firstTick;
            this.lastTick = lastTick;
            this.sections = sections;
            this.records = records;
            this.payloadBytes = payloadBytes;
            this.playing = playing;
            this.tick = tick;
            this.speed = speed;
        }

        /** The clip length in ticks, or 0 when empty. */
        public long tickLength() {
            return loaded && lastTick >= firstTick ? lastTick - firstTick : 0L;
        }
    }

    private final Backend backend;

    public BerryReplaySource() {
        this(new NativeBackend());
    }

    /** A source over an injected backend, for tests. */
    public BerryReplaySource(Backend backend) {
        this.backend = backend;
    }

    /** True when BerryAPI's replay surface is present and usable. */
    public boolean available() {
        return backend.available();
    }

    /** True when a replay is loaded. */
    public boolean loaded() {
        return available() && backend.loaded();
    }

    /**
     * Decodes a replay from bytes. Returns the ABI status code; 0 is success, and anything else
     * leaves the previously loaded replay untouched (the library guarantees that).
     */
    public int load(byte[] bytes) {
        if (!available()) return ReplayAbi.NOT_ATTACHED;
        if (bytes == null || bytes.length == 0) return ReplayAbi.BAD_ARGUMENT;
        return backend.load(bytes);
    }

    /** Reads the loaded replay and its playback state, or an empty snapshot when unavailable. */
    public Snapshot snapshot() {
        if (!available()) return empty();
        long[] info = backend.info();
        if (info == null || info.length < 8 || info[0] == 0) return empty();
        long[] pb = backend.playback();
        boolean playing = pb != null && pb.length >= 5 && pb[1] != 0;
        long tick = pb != null && pb.length >= 5 ? pb[3] : 0L;
        float speed = pb != null && pb.length >= 5
                ? Float.intBitsToFloat((int) pb[4]) : 1f;
        return new Snapshot(true, info[1], info[2], (int) info[3], (int) info[4], info[5],
                playing, tick, speed);
    }

    /** Starts playback. Returns the ABI status code. */
    public int play() {
        return available() ? backend.play() : ReplayAbi.NOT_ATTACHED;
    }

    /** Seeks to an absolute tick. Returns the ABI status code. */
    public int seek(long tick) {
        return available() ? backend.seek(tick) : ReplayAbi.NOT_ATTACHED;
    }

    /** Pauses playback; a no-op when unavailable. */
    public void pause() {
        if (available()) backend.pause();
    }

    /** Stops playback and releases the world; a no-op when unavailable. */
    public void stop() {
        if (available()) backend.stop();
    }

    /** Drops the loaded replay; a no-op when unavailable. */
    public void unload() {
        if (available()) backend.unload();
    }

    /**
     * Sets the playback speed. Returns false when the speed is out of range or the surface is
     * unavailable, so the caller does not show a speed that was not applied.
     */
    public boolean setSpeed(float speed) {
        if (!available() || !Float.isFinite(speed) || speed <= 0f || speed > 32f) return false;
        backend.setSpeed(speed);
        return true;
    }

    /** The library's status line, or an empty string. */
    public String status() {
        return available() ? backend.status() : "";
    }

    /**
     * Encodes the library's replay to container bytes, or an empty array when there is nothing to
     * save or the surface is absent.
     *
     * <p>This is the recorder half of the BerryAPI route: the library keeps a capture of the live
     * session, and this exports it. The caller writes the bytes to a replay file.
     */
    public byte[] save() {
        if (!available()) return new byte[0];
        byte[] bytes = backend.save();
        return bytes != null ? bytes : new byte[0];
    }

    private static Snapshot empty() {
        return new Snapshot(false, 0, 0, 0, 0, 0, false, 0, 1f);
    }
}
