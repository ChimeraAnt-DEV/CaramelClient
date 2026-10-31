package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Frame-coalesced look deltas (item 216).
 *
 * <p>The property that matters: the travel summed across a frame is what gets sent, so several
 * move events between two frames cannot lose all but the last one.
 */
public class LookDeltaCoalescerTest {

    @Test
    public void severalEventsInOneFrameAreSummed() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        coalescer.add(1f, 2f);
        coalescer.add(3f, 4f);
        coalescer.add(-1f, 0.5f);
        float[] out = new float[2];
        assertTrue(coalescer.flush(out));
        assertEquals(3f, out[0], 0.0001f);
        assertEquals(6.5f, out[1], 0.0001f);
    }

    @Test
    public void theTotalSurvivesEvenIfOnlyTheLastEventWasLarge() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        coalescer.add(0.5f, 0f);
        coalescer.add(40f, 12f);
        float[] out = new float[2];
        assertTrue(coalescer.flush(out));
        assertEquals(40.5f, out[0], 0.0001f);
        assertEquals(12f, out[1], 0.0001f);
    }

    @Test
    public void aFlushWithNothingPendingReportsFalseAndLeavesTheArrayAlone() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        float[] out = {7f, 8f};
        assertFalse(coalescer.flush(out));
        assertEquals(7f, out[0], 0.0001f);
        assertEquals(8f, out[1], 0.0001f);
    }

    @Test
    public void aSecondFlushIsEmpty() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        coalescer.add(1f, 1f);
        float[] out = new float[2];
        assertTrue(coalescer.flush(out));
        assertFalse("the accumulator was drained", coalescer.flush(out));
    }

    @Test
    public void theAccumulatorRestartsAfterAFlush() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        float[] out = new float[2];
        coalescer.add(5f, 0f);
        coalescer.flush(out);
        coalescer.add(2f, 3f);
        assertTrue(coalescer.flush(out));
        assertEquals(2f, out[0], 0.0001f);
        assertEquals(3f, out[1], 0.0001f);
    }

    @Test
    public void hasPendingTracksTheAccumulator() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        assertFalse(coalescer.hasPending());
        coalescer.add(1f, 0f);
        assertTrue(coalescer.hasPending());
        coalescer.flush(new float[2]);
        assertFalse(coalescer.hasPending());
    }

    @Test
    public void resetDropsUnflushedTravel() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        coalescer.add(9f, 9f);
        coalescer.reset();
        assertFalse(coalescer.hasPending());
        assertFalse(coalescer.flush(new float[2]));
    }

    @Test
    public void aNullOrShortArrayIsRefused() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        coalescer.add(1f, 1f);
        assertFalse(coalescer.flush(null));
        assertFalse(coalescer.flush(new float[1]));
        assertTrue("the delta must not have been consumed by a bad flush",
                coalescer.flush(new float[2]));
    }

    @Test
    public void theFlushCountCountsFramesThatSentSomething() {
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        float[] out = new float[2];
        coalescer.flush(out);
        assertEquals(0L, coalescer.flushCount());
        coalescer.add(1f, 1f);
        coalescer.flush(out);
        coalescer.add(1f, 1f);
        coalescer.flush(out);
        assertEquals(2L, coalescer.flushCount());
    }

    @Test
    public void oppositeMotionsInOneFrameCancel() {
        // A jitter that returns to where it started should send no net turn; the game applies a
        // delta, so a zero sum is the correct "the finger did not move" signal.
        LookDeltaCoalescer coalescer = new LookDeltaCoalescer();
        coalescer.add(5f, 0f);
        coalescer.add(-5f, 0f);
        float[] out = new float[2];
        assertTrue(coalescer.flush(out));
        assertEquals(0f, out[0], 0.0001f);
    }
}
