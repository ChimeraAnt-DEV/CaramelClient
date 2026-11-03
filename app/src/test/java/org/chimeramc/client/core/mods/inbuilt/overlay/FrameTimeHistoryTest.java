package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The rolling frame-interval history behind the FPS overlay's graph mode.
 *
 * <p>Pins the cases that would otherwise make the graph lie: the first frame (no interval yet), a
 * duplicate stamp, a backwards clock, the ring wrapping at capacity, and an out-of-range index.
 */
public class FrameTimeHistoryTest {

    private static final long MS = 1_000_000L;

    @Test
    public void anEmptyHistoryHasNoReading() {
        FrameTimeHistory history = new FrameTimeHistory();
        assertEquals(0, history.size());
        assertTrue(history.isEmpty());
        assertEquals(-1f, history.intervalMs(0), 0f);
        assertEquals(0f, history.maxIntervalMs(), 0f);
    }

    @Test
    public void theFirstFrameRecordsNoInterval() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        assertEquals("one stamp is not yet an interval", 0, history.size());
    }

    @Test
    public void theIntervalIsDerivedFromConsecutiveStamps() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        history.record(16L * MS);
        history.record(33L * MS);
        assertEquals(2, history.size());
        // index 0 is the newest: the 17 ms gap between the 33 ms and 16 ms stamps.
        assertEquals(17f, history.intervalMs(0), 0.01f);
        assertEquals(16f, history.intervalMs(1), 0.01f);
    }

    @Test
    public void aDuplicateStampIsIgnored() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        history.record(16L * MS);
        history.record(16L * MS);
        assertEquals("a duplicate stamp must not add a zero interval", 1, history.size());
    }

    @Test
    public void aBackwardsClockRecordsNothing() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(100L * MS);
        history.record(50L * MS);
        assertEquals(0, history.size());
    }

    @Test
    public void theRingWrapsAndKeepsTheNewestFrames() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        long t = 0;
        for (int i = 0; i < FrameTimeHistory.CAPACITY + 10; i++) {
            t += 20L * MS;
            history.record(t);
        }
        assertEquals(FrameTimeHistory.CAPACITY, history.size());
        // Every retained interval is the steady 20 ms cadence; nothing from before the wrap.
        assertEquals(20f, history.intervalMs(0), 0.01f);
        assertEquals(20f, history.intervalMs(FrameTimeHistory.CAPACITY - 1), 0.01f);
    }

    @Test
    public void anIndexOutsideTheWindowReadsEmpty() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        history.record(16L * MS);
        assertEquals(-1f, history.intervalMs(-1), 0f);
        assertEquals(-1f, history.intervalMs(2), 0f);
    }

    @Test
    public void theMaxIntervalCapturesAHitch() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        long t = 0;
        for (int i = 0; i < 10; i++) {
            t += 16L * MS;
            history.record(t);
        }
        t += 80L * MS; // a hitch
        history.record(t);
        assertEquals(80f, history.maxIntervalMs(), 0.01f);
        assertFalse(history.isEmpty());
    }

    @Test
    public void theAverageIsTheMeanOverTheRequestedWindow() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        long t = 0;
        for (int i = 0; i < 9; i++) {
            t += 10L * MS;
            history.record(t);
        }
        t += 100L * MS; // one very long frame at the newest end
        history.record(t);
        // Over all 10 intervals the hitch drags the mean up.
        assertEquals(19f, history.averageIntervalMs(10), 0.01f);
        // Over only the newest 2 intervals it is dominated by the hitch.
        assertEquals(55f, history.averageIntervalMs(2), 0.01f);
    }

    @Test
    public void aWindowLongerThanTheHistoryUsesWhatIsRecorded() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        history.record(16L * MS);
        history.record(32L * MS);
        assertEquals(16f, history.averageIntervalMs(1000), 0.01f);
    }

    @Test
    public void anEmptyWindowAveragesToZero() {
        FrameTimeHistory history = new FrameTimeHistory();
        assertEquals(0f, history.averageIntervalMs(60), 0f);
        assertEquals(0f, history.averageIntervalMs(0), 0f);
    }

    @Test
    public void the99thPercentileIsTheWorstFrame() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        long t = 0;
        for (int i = 0; i < 50; i++) {
            t += 16L * MS;
            history.record(t);
        }
        t += 200L * MS; // a single bad frame
        history.record(t);
        assertEquals(200f, history.percentileIntervalMs(60, 0.99f), 0.01f);
        // The 50th percentile (median) is a normal frame, not the outlier.
        assertEquals(16f, history.percentileIntervalMs(60, 0.5f), 0.01f);
    }

    @Test
    public void thePercentileClampsOutOfRangeFractions() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        history.record(16L * MS);
        history.record(32L * MS);
        // Below 0 reads the smallest, above 1 reads the largest, rather than an index error.
        assertEquals(16f, history.percentileIntervalMs(10, -1f), 0.01f);
        assertEquals(16f, history.percentileIntervalMs(10, 2f), 0.01f);
    }

    @Test
    public void resetClearsTheWindow() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        history.record(16L * MS);
        history.reset();
        assertEquals(0, history.size());
        assertEquals(-1f, history.intervalMs(0), 0f);
    }
}
