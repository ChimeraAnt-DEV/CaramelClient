package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Sums look deltas so exactly one shaped delta is sent per frame.
 *
 * <p>Touch, mouse and controller look can each deliver several {@code ACTION_MOVE} events between
 * two game frames. Sending one native look call per event means the game samples only the last one
 * (its own tick has already been decided), so the earlier travel is silently dropped and a fast
 * flick under-turns. Summing the deltas and sending the total once per frame preserves the whole
 * travel, and it also collapses the shaping work from one call per event to one per frame.
 *
 * <p>Shaping happens on the summed delta, at the flush, rather than per event: the shaping filters
 * are per-gesture, and running them on sub-frame fragments would reset the burst state several
 * times a frame.
 *
 * <p>Pure and Android-free. The accumulation and the flush are guarded by one lock because an input
 * callback and the frame tick are not guaranteed to be the same thread; the lock is uncontended in
 * practice and one acquire per event is far cheaper than the shaping plus the native call it
 * replaces.
 */
public final class LookDeltaCoalescer {

    private final Object lock = new Object();
    private float pendingX;
    private float pendingY;
    private boolean hasPending;
    private long flushCount;

    /** Adds one event's delta to the frame's total. */
    public void add(float deltaX, float deltaY) {
        synchronized (lock) {
            pendingX += deltaX;
            pendingY += deltaY;
            hasPending = true;
        }
    }

    /**
     * Takes the frame's total.
     *
     * @param out a caller-owned two-element array to receive the delta
     * @return true when there was something to send; false leaves {@code out} untouched
     */
    public boolean flush(float[] out) {
        if (out == null || out.length < 2) return false;
        synchronized (lock) {
            if (!hasPending) return false;
            out[0] = pendingX;
            out[1] = pendingY;
            pendingX = 0f;
            pendingY = 0f;
            hasPending = false;
            flushCount++;
            return true;
        }
    }

    /** True when deltas have arrived since the last flush. */
    public boolean hasPending() {
        synchronized (lock) {
            return hasPending;
        }
    }

    /** Drops any unflushed delta, so a paused session cannot deliver stale look on resume. */
    public void reset() {
        synchronized (lock) {
            pendingX = 0f;
            pendingY = 0f;
            hasPending = false;
        }
    }

    /** How many frames have sent a delta; a diagnostic, not a control input. */
    public long flushCount() {
        synchronized (lock) {
            return flushCount;
        }
    }
}
