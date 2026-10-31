package org.chimeramc.client.core.stats;

/**
 * The local launch/crash statistics and the rules for keeping them (item 282).
 *
 * <p>Everything here is computed from data the launcher already owns -- a launch count, a crash
 * count and the time to first frame of each launch -- and nothing leaves the device. The class is
 * Android-free so the aggregation is unit-tested: an average that includes a zero, or a crash rate
 * divided by the wrong base, is the kind of arithmetic that quietly reports a wrong number and is
 * never noticed on screen.
 *
 * <p>The numbers are stored as raw counts and a running sum, not as a pre-computed average, because
 * an average cannot be merged with a later sample. {@link #withFirstFrame(long)} keeps the total so
 * the mean stays exact across launches.
 */
public final class LaunchStats {

    /** A launch that never reported a first frame, so the mean ignores it rather than counting a zero. */
    public static final long NO_FIRST_FRAME = -1L;

    /** A launch was started (the launch screen was reached). */
    private int launches;
    /** A launch reached the game (the game activity was entered). */
    private int completedLaunches;
    /** A launch failed or the process died before a session was recorded. */
    private int crashes;
    /** The number of first-frame samples folded into {@link #firstFrameTotalMs}. */
    private int firstFrameSamples;
    /** The sum of every first-frame time, so the mean is exact. */
    private long firstFrameTotalMs;

    public LaunchStats() {
    }

    public LaunchStats(int launches, int completedLaunches, int crashes,
                       int firstFrameSamples, long firstFrameTotalMs) {
        this.launches = Math.max(0, launches);
        this.completedLaunches = Math.max(0, completedLaunches);
        this.crashes = Math.max(0, crashes);
        this.firstFrameSamples = Math.max(0, firstFrameSamples);
        this.firstFrameTotalMs = Math.max(0L, firstFrameTotalMs);
    }

    /** Records that a launch started. */
    public void recordLaunch() {
        launches++;
    }

    /**
     * Records that a launch reached the game.
     *
     * <p>Clamped to the launch count so a corrupt or duplicated record cannot make the completed
     * count exceed the total, which would make the success rate read above 100%.
     */
    public void recordCompletedLaunch() {
        completedLaunches = Math.min(launches, completedLaunches + 1);
    }

    /** Records a failed launch or a process death before a session. */
    public void recordCrash() {
        crashes++;
    }

    /**
     * Folds a first-frame time into the mean.
     *
     * <p>A negative time (the sentinel a caller passes when the frame was never observed) is
     * ignored rather than counted as zero, which would drag the average down and flatter the
     * launcher on exactly the slow launches the number exists to expose.
     */
    public void withFirstFrame(long firstFrameMs) {
        if (firstFrameMs < 0L) return;
        firstFrameSamples++;
        firstFrameTotalMs += firstFrameMs;
    }

    public int launches() {
        return launches;
    }

    public int completedLaunches() {
        return completedLaunches;
    }

    public int crashes() {
        return crashes;
    }

    public int firstFrameSamples() {
        return firstFrameSamples;
    }

    public long firstFrameTotalMs() {
        return firstFrameTotalMs;
    }

    /**
     * The mean time to first frame in milliseconds, or {@link #NO_FIRST_FRAME} when no sample was
     * ever recorded.
     *
     * <p>A sentinel rather than {@code 0}: a device that has never completed a launch has no
     * average, and showing "0 ms" would read as a measured value.
     */
    public long averageFirstFrameMs() {
        if (firstFrameSamples == 0) return NO_FIRST_FRAME;
        return firstFrameTotalMs / firstFrameSamples;
    }

    /**
     * The share of launches that failed, in the range 0..1, or {@code -1} when there is no launch
     * to divide by.
     *
     * <p>Divided by the launch count, not by completed-plus-crashed: a launch that is still running
     * counts as a launch but as neither a success nor a crash, so the two counts are not the whole.
     */
    public float crashRate() {
        if (launches == 0) return -1f;
        return (float) crashes / (float) launches;
    }

    /** The share of launches that reached the game, in the range 0..1, or {@code -1} when none did. */
    public float successRate() {
        if (launches == 0) return -1f;
        return (float) completedLaunches / (float) launches;
    }

    /**
     * A display string for a first-frame time, or an em dash when there is no sample.
     *
     * <p>The em dash matches the rule the Mod Menu's stats strip already uses: an unknown reading
     * is never rendered as a number.
     */
    public static String formatFirstFrame(long ms) {
        if (ms == NO_FIRST_FRAME || ms < 0L) return "\u2014";
        if (ms < 1000L) return ms + " ms";
        return String.format(java.util.Locale.US, "%.1f s", ms / 1000.0);
    }

    /** A display string for a 0..1 rate, or an em dash when there is no rate. */
    public static String formatRate(float rate) {
        if (rate < 0f) return "\u2014";
        return Math.round(rate * 100f) + "%";
    }
}
