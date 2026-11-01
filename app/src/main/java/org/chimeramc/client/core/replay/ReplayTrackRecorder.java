package org.chimeramc.client.core.replay;

/**
 * Samples the local-player feed into a {@link ReplayTrack} while a capture runs.
 *
 * <p>The recorder is a thin, clock-injected driver: the caller ticks it with the elapsed time since
 * the capture started, and it pulls one reading through the {@link Source} seam and hands it to the
 * track. Keeping the feed behind a seam is what makes the sampling rules testable without a device
 * or a native library — the {@link ReplayTrack} owns the cadence, and this class owns only the
 * "when do we read, and what do we read" decision.
 *
 * <p>The seam returns arrays exactly as {@code PreloaderInput} does ({@code {x,y,z}} for position,
 * {@code {yaw,pitch}} for rotation, {@code {health}}), so the live implementation is a direct
 * delegation and the test implementation is a small script. A missing array reads as
 * {@link ReplayTrack#UNKNOWN}, never as zero.
 */
public final class ReplayTrackRecorder {

    /** Reads one sample of the local-player state; any part may be null when unavailable. */
    public interface Source {
        /** World position {@code {x,y,z}}, or null. */
        float[] position();

        /** View rotation {@code {yaw,pitch}} in degrees, or null. */
        float[] rotation();

        /** Health {@code {health}}, or null. */
        float[] health();
    }

    private final Source source;
    private final ReplayTrack track = ReplayTrack.empty();

    public ReplayTrackRecorder(Source source) {
        this.source = source;
    }

    /**
     * Reads the feed once and stores the sample when the cadence allows.
     *
     * @param elapsedMs milliseconds since the capture started (the clip's own timeline)
     * @return true when the sample was stored
     */
    public boolean tick(long elapsedMs) {
        if (source == null) return false;
        float[] position = safePosition();
        float[] rotation = safeRotation();
        float[] health = safeHealth();
        return track.sample(elapsedMs,
                position != null ? position[0] : ReplayTrack.UNKNOWN,
                position != null ? position[1] : ReplayTrack.UNKNOWN,
                position != null ? position[2] : ReplayTrack.UNKNOWN,
                rotation != null ? rotation[0] : ReplayTrack.UNKNOWN,
                rotation != null ? rotation[1] : ReplayTrack.UNKNOWN,
                health != null ? health[0] : ReplayTrack.UNKNOWN);
    }

    /** The track sampled so far. */
    public ReplayTrack track() {
        return track;
    }

    private float[] safePosition() {
        try {
            float[] value = source.position();
            return value != null && value.length >= 3 ? value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private float[] safeRotation() {
        try {
            float[] value = source.rotation();
            return value != null && value.length >= 2 ? value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private float[] safeHealth() {
        try {
            float[] value = source.health();
            return value != null && value.length >= 1 ? value : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
