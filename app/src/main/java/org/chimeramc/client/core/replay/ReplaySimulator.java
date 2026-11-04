package org.chimeramc.client.core.replay;

/**
 * Reconstructs a replay's world state at a point in time, with no server and no network.
 *
 * <p>A Bedrock replay is not a video: the recorder captured the raw session (the local player's
 * position, view and health per tick, the camera path a player authored, and the markers on the
 * timeline), and the viewer re-simulates it locally. This is the object that answers "what does the
 * world look like at time t" from those recordings, so the renderer never touches a socket.
 *
 * <p>What it can honestly answer: the local player's own state (position, facing, health), which is
 * what the recorder captured through the local-player feed, and the camera pose, which is either the
 * authored cinematic path or a free-fly rig. It cannot answer other entities, blocks or level time,
 * because the recorder never captured them — the game exports no symbols for those, so a read would
 * have to be invented. {@link #hasWorldGeometry()} says so plainly rather than fabricating a world.
 *
 * <p>Pure and Android-free: it holds two recordings and a clock, so playback, seeking and the speed
 * multiplier are unit-tested without a device.
 */
public final class ReplaySimulator {

    /** The reconstructed state at one instant. */
    public static final class Snapshot {
        /** The playback position in ms. */
        public final long timeMs;
        /** The local player's recorded state, or null when the track has no frame at this time. */
        public final ReplayTrack.Frame player;
        /** The camera's pose at this time. */
        public final ReplayCamera camera;
        /** The marker covering this instant, or null. */
        public final ReplayMarkers.Marker marker;

        Snapshot(long timeMs, ReplayTrack.Frame player, ReplayCamera camera,
                 ReplayMarkers.Marker marker) {
            this.timeMs = timeMs;
            this.player = player;
            this.camera = camera;
            this.marker = marker;
        }

        /** True when a player state was reconstructed for this instant. */
        public boolean hasPlayer() {
            return player != null;
        }
    }

    private final ReplayTrack track;
    private final ReplayCameraPath cameraPath;
    private final ReplayMarkers markers;
    private final ReplayCamera freeCamera = new ReplayCamera();

    /**
     * How long a marker stays reported after it fires, in ms.
     *
     * <p>Separate from {@link ReplayMarkers#MERGE_WINDOW_MS}, which is how close two markers must be
     * to merge when recording. A marker is a point in time; this is how long its pill stays on screen.
     */
    public static final long MARKER_DISPLAY_MS = 750L;

    private long durationMs;
    private long positionMs;
    private float speed = 1f;
    private boolean playing;
    private boolean useCinematicPath = true;

    public ReplaySimulator(ReplayTrack track, ReplayCameraPath cameraPath, ReplayMarkers markers) {
        this.track = track != null ? track : ReplayTrack.empty();
        this.cameraPath = cameraPath != null ? cameraPath : new ReplayCameraPath();
        this.markers = markers != null ? markers : ReplayMarkers.empty();
        this.durationMs = computeDuration();
    }

    /**
     * The replay's length: the track's last frame, or the camera path's last keyframe, whichever is
     * later, so a cinematic that runs past the recorded motion is not cut short.
     */
    private long computeDuration() {
        long trackEnd = 0L;
        for (int i = 0; i < track.size(); i++) {
            trackEnd = Math.max(trackEnd, track.frameAt(i).tMs);
        }
        long pathEnd = cameraPath.isEmpty()
                ? 0L : cameraPath.keyframes().get(cameraPath.keyframes().size() - 1).timeMs;
        return Math.max(trackEnd, pathEnd);
    }

    public long durationMs() {
        return durationMs;
    }

    public long positionMs() {
        return positionMs;
    }

    public boolean isPlaying() {
        return playing;
    }

    public float speed() {
        return speed;
    }

    /** Starts (or resumes) playback. A no-op on an empty replay. */
    public void play() {
        if (durationMs <= 0L) return;
        // A forward replay that ended starts again from the top rather than sitting at the end doing
        // nothing. A backward one must not: running backwards from the end is the whole point.
        if (speed > 0f && positionMs >= durationMs) positionMs = 0L;
        playing = true;
    }

    /** Stops advancing but keeps the position, so the frame holds. */
    public void pause() {
        playing = false;
    }

    /** Stops and rewinds to the start. */
    public void stop() {
        playing = false;
        positionMs = 0L;
    }

    /**
     * Seeks to an absolute time. Clamped to the replay's span, so a scrubber drag past either end
     * holds at the end rather than wrapping.
     */
    public void seek(long timeMs) {
        this.positionMs = Math.max(0L, Math.min(durationMs, timeMs));
    }

    /** Seeks to a fraction of the replay, 0..1. */
    public void seekFraction(float fraction) {
        seek((long) (Math.max(0f, Math.min(1f, fraction)) * durationMs));
    }

    /**
     * Sets the playback speed. Refused when it is not finite or is zero, so a bad value cannot stop
     * the clock; negative speeds are allowed so a replay can be played backwards.
     */
    public boolean setSpeed(float speed) {
        if (!Float.isFinite(speed) || speed == 0f) return false;
        this.speed = speed;
        return true;
    }

    /**
     * Advances playback by a wall-clock delta.
     *
     * <p>The delta is scaled by the speed, so 2x advances twice as fast and a negative speed runs it
     * backwards. A non-finite or negative delta is ignored: a stalled or rewound clock must not
     * jump the replay. Returns the new position.
     */
    public long advance(float dtSeconds) {
        if (!playing || !Float.isFinite(dtSeconds) || dtSeconds <= 0f) return positionMs;
        long next = positionMs + (long) (dtSeconds * 1000f * speed);
        if (next >= durationMs) {
            next = durationMs;
            playing = false;  // the replay ended; it does not loop on its own
        } else if (next < 0L) {
            next = 0L;
            playing = false;
        }
        positionMs = next;
        return positionMs;
    }

    /** Whether the camera follows the authored cinematic path (else the free-fly rig). */
    public boolean usesCinematicPath() {
        return useCinematicPath;
    }

    /** Switches between the authored path and the free-fly camera. */
    public void setUseCinematicPath(boolean useCinematicPath) {
        this.useCinematicPath = useCinematicPath;
    }

    /** The free-fly camera, for a caller driving it directly. */
    public ReplayCamera freeCamera() {
        return freeCamera;
    }

    /**
     * The reconstructed state at the current position.
     *
     * <p>The camera is the authored path's pose when that is selected and the path has keyframes,
     * otherwise the free-fly rig. The player state is the track's frame at this time, or null when
     * the track has no frame covering it.
     */
    public Snapshot snapshot() {
        ReplayTrack.Frame frame = track.isEmpty() ? null : track.at(positionMs);
        ReplayCamera cam;
        if (useCinematicPath && !cameraPath.isEmpty()) {
            // The path samples into a caller-owned camera, so a per-frame read allocates nothing.
            cam = new ReplayCamera();
            cameraPath.sample(positionMs, cam);
        } else {
            cam = freeCamera;
        }
        return new Snapshot(positionMs, frame, cam, markerAt(positionMs));
    }

    /**
     * The marker covering an instant: the latest marker at or before {@code timeMs} that is still
     * within the merge window, or null. A marker is a point in time, so "covering" means the most
     * recent one the playhead has passed, not an exact timestamp match.
     */
    private ReplayMarkers.Marker markerAt(long timeMs) {
        ReplayMarkers.Marker best = null;
        for (ReplayMarkers.Marker m : markers.all()) {
            if (m.tMs <= timeMs && (best == null || m.tMs > best.tMs)) best = m;
        }
        if (best == null) return null;
        return timeMs - best.tMs <= MARKER_DISPLAY_MS ? best : null;
    }

    /**
     * Whether the replay carries world geometry (blocks, other entities, level time).
     *
     * <p>Always false: the recorder captured the local player's own state and the camera path, not
     * the world, because the stripped game exposes no symbols for it. The viewer says so rather than
     * drawing an invented world.
     */
    public boolean hasWorldGeometry() {
        return false;
    }
}
