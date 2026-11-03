package org.chimeramc.client.ui.dialogs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The logcat overlay's UI flush-coalescing rule. */
public class LogcatBatchPolicyTest {

    @Test
    public void nothingToFlushNeverFlushes() {
        assertFalse(LogcatBatchPolicy.shouldFlush(10_000L, 0L, 0, 200));
    }

    @Test
    public void aFullBatchFlushesAtOnce() {
        // A burst must not wait for the interval, or the bounded queue would drop lines.
        assertTrue(LogcatBatchPolicy.shouldFlush(1L, 0L, 200, 200));
    }

    @Test
    public void anIntervalFlushWaitsForTheInterval() {
        long last = 1_000L;
        assertFalse(LogcatBatchPolicy.shouldFlush(
                last + LogcatBatchPolicy.FLUSH_INTERVAL_MS - 1, last, 5, 200));
        assertTrue(LogcatBatchPolicy.shouldFlush(
                last + LogcatBatchPolicy.FLUSH_INTERVAL_MS, last, 5, 200));
    }

    @Test
    public void aSingleLineIsShownWithinOneInterval() {
        assertTrue(LogcatBatchPolicy.shouldFlush(
                LogcatBatchPolicy.FLUSH_INTERVAL_MS, 0L, 1, 200));
    }
}
