package org.chimeramc.client.core.backend;

import java.util.EnumMap;
import java.util.Map;

/**
 * Counts backend calls so a runaway script is visible before it is felt (item 504).
 *
 * <p>A per-frame look delta is a legitimate call, so a raw total says nothing. What matters is the
 * <em>rate</em>: a script that calls {@code sendKey} a few thousand times a second is a bug that
 * shows up as a stutter long before anyone reads a log. This keeps a one-second bucket per
 * {@link GameBackend.Feature} and reports the count of the last completed second, which is the number
 * a developer overlay can show.
 *
 * <p>Pure and clock-injected: {@code record(feature, nowMs)} takes the time as a parameter, so the
 * bucket rotation is unit-tested without sleeping and the counter never reads a clock on the call
 * path.
 *
 * <p>Deliberately not thread-safe. It is incremented from whichever thread makes the backend call
 * (the UI thread, or the game's tick), and a counter that is read for a debug line does not need
 * cross-thread precision. Marking it synchronized would put a lock on the input path, which is the
 * opposite of what this class is for.
 */
public final class BackendCallMeter {

    /** The bucket width. One second matches how a developer reads "calls per second". */
    public static final long BUCKET_MS = 1000L;

    private final Map<GameBackend.Feature, Integer> current = new EnumMap<>(GameBackend.Feature.class);
    private final Map<GameBackend.Feature, Integer> lastSecond =
            new EnumMap<>(GameBackend.Feature.class);
    private long bucketStartMs;
    private boolean started;

    /** Records one call of {@code feature} at {@code nowMs}. */
    public void record(GameBackend.Feature feature, long nowMs) {
        if (feature == null) return;
        if (!started) {
            started = true;
            bucketStartMs = nowMs;
        }
        if (nowMs - bucketStartMs >= BUCKET_MS) {
            // A long gap (the game was paused) rotates once rather than replaying every missed
            // bucket: the last completed second is what a reader wants, and the intermediate zeros
            // carry no information.
            rotate(nowMs);
        }
        current.merge(feature, 1, Integer::sum);
    }

    /**
     * The count recorded in the last completed second for {@code feature}, or 0.
     *
     * <p>Reading does not rotate the bucket: a reader asking twice in one second must not see the
     * count reset under it.
     */
    public int lastSecondCount(GameBackend.Feature feature) {
        if (feature == null) return 0;
        Integer value = lastSecond.get(feature);
        return value == null ? 0 : value;
    }

    /** The total of the last completed second across every feature. */
    public int lastSecondTotal() {
        int total = 0;
        for (Integer value : lastSecond.values()) {
            if (value != null) total += value;
        }
        return total;
    }

    /**
     * Rotates the bucket if {@code nowMs} has crossed a boundary, and returns true when it did.
     *
     * <p>Called from {@link #record} and from a periodic reader, so the reported figure advances even
     * on a second in which nothing was called (a script that stops is worth seeing).
     */
    public boolean tick(long nowMs) {
        if (!started) {
            started = true;
            bucketStartMs = nowMs;
            return false;
        }
        if (nowMs - bucketStartMs >= BUCKET_MS) {
            rotate(nowMs);
            return true;
        }
        return false;
    }

    /** Clears every counter. Called when the backend is swapped, so the new one starts clean. */
    public void reset() {
        current.clear();
        lastSecond.clear();
        started = false;
        bucketStartMs = 0L;
    }

    private void rotate(long nowMs) {
        lastSecond.clear();
        lastSecond.putAll(current);
        current.clear();
        // Anchor on the new time rather than advancing by whole buckets: a pause should not make the
        // next rotation happen immediately after the game resumes.
        bucketStartMs = nowMs;
    }
}
