package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * A rolling history of frame intervals, for the FPS overlay's graph mode.
 *
 * <p>A single frames-per-second number hides stutter: a steady 60 and a 60 that hitches every few
 * frames read identically. This holds the last {@link #CAPACITY} frame intervals in milliseconds so
 * the overlay can draw them as a polyline, and a spike is visible as a spike.
 *
 * <p>Pure and Android-free so the ring arithmetic is unit-tested without a device, and
 * allocation-free on the frame path: {@link #record} writes one float into a fixed array.
 */
public final class FrameTimeHistory {

    /** The window the graph draws: 120 frames is two seconds at 60 fps. */
    public static final int CAPACITY = 120;

    /** Sentinel for "no interval recorded yet". */
    private static final float EMPTY = -1f;

    private final float[] intervals = new float[CAPACITY];
    private int cursor;
    private int count;
    private long lastStampNanos = Long.MIN_VALUE;

    public FrameTimeHistory() {
        for (int i = 0; i < CAPACITY; i++) intervals[i] = EMPTY;
    }

    /**
     * Records one frame's arrival time, deriving the interval from the previous stamp.
     *
     * <p>A duplicate stamp (some stacks deliver one) is ignored so it cannot record a zero interval
     * and read as an impossibly fast frame. The first frame after a reset has no previous stamp, so
     * it records nothing rather than inventing an interval.
     */
    public void record(long frameTimeNanos) {
        if (frameTimeNanos == lastStampNanos) return;
        long previous = lastStampNanos;
        lastStampNanos = frameTimeNanos;
        if (previous == Long.MIN_VALUE) return;
        long delta = frameTimeNanos - previous;
        if (delta < 0L) return;
        intervals[cursor] = delta / 1_000_000f;
        cursor = (cursor + 1) % CAPACITY;
        if (count < CAPACITY) count++;
    }

    /** How many intervals are recorded, up to {@link #CAPACITY}. */
    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * The interval {@code index} frames back from the newest, in milliseconds.
     *
     * <p>{@code 0} is the most recent frame, {@code size() - 1} the oldest retained one. An index
     * outside the recorded range returns {@link #EMPTY} rather than a wrapped or zero value, so a
     * caller cannot accidentally plot a frame that does not exist.
     */
    public float intervalMs(int index) {
        if (index < 0 || index >= count) return EMPTY;
        int slot = (cursor - 1 - index + CAPACITY * 2) % CAPACITY;
        return intervals[slot];
    }

    /** The largest recorded interval in the window, or 0 when nothing is recorded. */
    public float maxIntervalMs() {
        float max = 0f;
        for (int i = 0; i < count; i++) {
            float v = intervalMs(i);
            if (v > max) max = v;
        }
        return max;
    }

    /**
     * The mean interval over the newest {@code window} frames, in milliseconds, or 0 when nothing is
     * recorded. {@code window} is clamped to what is retained, so a caller can ask for a longer
     * window than has been recorded yet and get the frames it does have rather than a wrong answer.
     */
    public float averageIntervalMs(int window) {
        int n = clampWindow(window);
        if (n == 0) return 0f;
        float sum = 0f;
        for (int i = 0; i < n; i++) sum += intervalMs(i);
        return sum / n;
    }

    /**
     * The interval at {@code percentile} (0..1) over the newest {@code window} frames: the value
     * that fraction of frames are at or below. Used for the p95/p99 frame times, which show stutter
     * a mean hides. Returns 0 when nothing is recorded.
     *
     * <p>Copies at most {@link #CAPACITY} values into a reusable buffer and sorts them — this runs on
     * the overlay's once-a-second refresh, never on the frame path.
     */
    public float percentileIntervalMs(int window, float percentile) {
        int n = clampWindow(window);
        if (n == 0) return 0f;
        float p = percentile;
        if (p < 0f) p = 0f;
        if (p > 1f) p = 1f;
        float[] scratch = sortScratch;
        for (int i = 0; i < n; i++) scratch[i] = intervalMs(i);
        java.util.Arrays.sort(scratch, 0, n);
        int index = (int) Math.round(p * (n - 1));
        if (index < 0) index = 0;
        if (index >= n) index = n - 1;
        return scratch[index];
    }

    private final float[] sortScratch = new float[CAPACITY];

    private int clampWindow(int window) {
        if (window <= 0) return 0;
        return Math.min(window, count);
    }

    public void reset() {
        for (int i = 0; i < CAPACITY; i++) intervals[i] = EMPTY;
        cursor = 0;
        count = 0;
        lastStampNanos = Long.MIN_VALUE;
    }
}
