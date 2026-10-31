package org.chimeramc.client.ui.dialogs;

/**
 * When the logcat overlay flushes its pending lines to the UI (item 208).
 *
 * <p>The reader thread appends to a bounded queue and a background loop drains it; every flush ends
 * in a main-thread post that rebuilds the adapter's item list. Posting on every drain tick put up to
 * ten rebuilds a second on the UI thread while a chatty session streamed logs, which is exactly the
 * stutter the overlay is opened to diagnose.
 *
 * <p>This is the coalescing rule, pure and clock-injected: flush when the interval has elapsed or
 * when the batch is full. A full batch must never wait, or a burst would be truncated by the queue's
 * capacity rather than shown.
 */
public final class LogcatBatchPolicy {

    /** The longest a line waits before it is shown; four flushes a second reads as live. */
    public static final long FLUSH_INTERVAL_MS = 250L;

    private LogcatBatchPolicy() {
    }

    /**
     * Whether a drain should be flushed to the UI now.
     *
     * @param nowMs      the current uptime in milliseconds
     * @param lastFlushMs when the previous flush happened; 0 means none yet
     * @param batchSize  how many lines are buffered
     * @param maxBatch   the batch capacity
     */
    public static boolean shouldFlush(long nowMs, long lastFlushMs, int batchSize, int maxBatch) {
        if (batchSize <= 0) return false;
        if (maxBatch > 0 && batchSize >= maxBatch) return true;
        return nowMs - lastFlushMs >= FLUSH_INTERVAL_MS;
    }
}
