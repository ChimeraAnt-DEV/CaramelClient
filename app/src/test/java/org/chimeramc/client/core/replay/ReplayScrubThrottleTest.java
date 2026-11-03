package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The scrub decode-rate rule.
 *
 * <p>A drag reports a position per touch move; without a cap the decodes queue behind the finger.
 */
public class ReplayScrubThrottleTest {

    @Test
    public void theFirstMoveAlwaysDecodes() {
        ReplayScrubThrottle throttle = new ReplayScrubThrottle();
        assertTrue(throttle.shouldDecode(0L, 0.5f, 60_000L));
    }

    @Test
    public void aSecondMoveInTheSameSecondIsSkipped() {
        ReplayScrubThrottle throttle = new ReplayScrubThrottle();
        assertTrue(throttle.shouldDecode(0L, 0.10f, 60_000L));
        // 0.11 through a 60s clip is still second 6: the frame cache returns the same bitmap.
        assertFalse(throttle.shouldDecode(500L, 0.11f, 60_000L));
    }

    @Test
    public void aMoveIntoANewSecondDecodes() {
        ReplayScrubThrottle throttle = new ReplayScrubThrottle();
        assertTrue(throttle.shouldDecode(0L, 0.10f, 60_000L));
        assertTrue(throttle.shouldDecode(500L, 0.20f, 60_000L));
    }

    @Test
    public void aFastDragIsCappedByTheInterval() {
        ReplayScrubThrottle throttle = new ReplayScrubThrottle();
        // Two moves 10ms apart but a whole second apart in clip position: the second is held back.
        assertTrue(throttle.shouldDecode(0L, 0.00f, 60_000L));
        assertFalse(throttle.shouldDecode(10L, 0.50f, 60_000L));
        assertTrue(throttle.shouldDecode(ReplayScrubThrottle.MIN_INTERVAL_MS, 0.50f, 60_000L));
    }

    @Test
    public void resetLetsTheNextDragDecodeImmediately() {
        ReplayScrubThrottle throttle = new ReplayScrubThrottle();
        assertTrue(throttle.shouldDecode(0L, 0.10f, 60_000L));
        assertFalse(throttle.shouldDecode(10L, 0.10f, 60_000L));
        throttle.reset();
        assertTrue(throttle.shouldDecode(11L, 0.10f, 60_000L));
    }

    @Test
    public void theBucketIsClampedForAStrayCoordinate() {
        assertEquals(0, ReplayScrubThrottle.bucketOf(-5f, 60_000L));
        assertEquals(59, ReplayScrubThrottle.bucketOf(5f, 60_000L));
        assertEquals(0, ReplayScrubThrottle.bucketOf(Float.NaN, 60_000L));
        assertEquals(0, ReplayScrubThrottle.bucketOf(0.5f, 0L));
    }
}
