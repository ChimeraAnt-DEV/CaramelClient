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

        /** True when the surface provides the packet capture (ABI v2). */
        boolean captureAvailable();

        /** Begins a capture session. Returns the ABI status. */
        int captureStart();

        /** Ends a capture session, keeping the sections for {@link #save()}. */
        int captureStop();

        /** The capture state: 0 idle, 1 recording, 2 throttled, 3 stopped, -1 unavailable. */
        int captureState();

        /** Records one packet. Returns the ABI status. */
        int capturePacket(int dir, int id, byte[] payload, long tick);
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
        @Override public boolean captureAvailable() {
            return org.chimeramc.client.preloader.BerryApi.replayCaptureAvailable();
        }
        @Override public int captureStart() {
            return org.chimeramc.client.preloader.BerryApi.replayCaptureStart();
        }
        @Override public int captureStop() {
            return org.chimeramc.client.preloader.BerryApi.replayCaptureStop();
        }
        @Override public int captureState() {
            return org.chimeramc.client.preloader.BerryApi.replayCaptureState();
        }
        @Override public int capturePacket(int dir, int id, byte[] payload, long tick) {
            return org.chimeramc.client.preloader.BerryApi.replayCapturePacket(dir, id, payload, tick);
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

    /**
     * Whether the surface provides the packet capture (ABI v2).
     *
     * <p>The recorder half: the packet stream is the world data, so a host that can record it
     * reconstructs the world with no game-memory offset. A library built before the capture ABI
     * answers false, and the recorder then records nothing rather than being driven through a null.
     */
    public boolean captureAvailable() {
        return available() && backend.captureAvailable();
    }

    /** Begins a capture session. Returns the ABI status; {@link ReplayAbi#NOT_ATTACHED} when absent. */
    public int captureStart() {
        return captureAvailable() ? backend.captureStart() : ReplayAbi.NOT_ATTACHED;
    }

    /** Ends a capture session, keeping the captured replay for {@link #save()}. */
    public int captureStop() {
        return captureAvailable() ? backend.captureStop() : ReplayAbi.NOT_ATTACHED;
    }

    /** The capture state: 0 idle, 1 recording, 2 throttled, 3 stopped, or -1 when unavailable. */
    public int captureState() {
        return captureAvailable() ? backend.captureState() : -1;
    }

    /**
     * Records one packet from the network stream.
     *
     * @param inbound true for a received packet, false for a sent one
     * @return the ABI status; {@link ReplayAbi#BAD_ARGUMENT} for an empty payload
     */
    public int capturePacket(boolean inbound, int id, byte[] payload, long tick) {
        if (!captureAvailable()) return ReplayAbi.NOT_ATTACHED;
        if (payload == null || payload.length == 0) return ReplayAbi.BAD_ARGUMENT;
        return backend.capturePacket(inbound ? 0 : 1, id, payload, tick);
    }

    private static Snapshot empty() {
        return new Snapshot(false, 0, 0, 0, 0, 0, false, 0, 1f);
    }
}
