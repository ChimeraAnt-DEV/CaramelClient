package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the invariants the preloader's render-distance governor relies on (items 382, 383).
 *
 * <p>The native side clamps each value, so a bad default would not crash — it would silently
 * behave differently from what the Settings screen describes. These are the bounds that make the
 * hysteresis meaningful: a range to move within, a threshold above zero, a step of at least one
 * chunk, and a non-negative dwell.
 */
public class RenderDistanceDefaultsTest {

    @Test
    public void theRangeHasRoomToMove() {
        assertTrue("MIN_CHUNKS must be below MAX_CHUNKS or the governor cannot step",
                RenderDistanceDefaults.MIN_CHUNKS < RenderDistanceDefaults.MAX_CHUNKS);
        assertTrue("MIN_CHUNKS must be at least one chunk",
                RenderDistanceDefaults.MIN_CHUNKS >= 1);
    }

    @Test
    public void theThresholdIsPositive() {
        assertTrue("a non-positive FPS_THRESHOLD would step down every sample",
                RenderDistanceDefaults.FPS_THRESHOLD > 0);
    }

    @Test
    public void theStepMovesAtLeastOneChunk() {
        assertTrue("a step below one chunk is a no-op",
                RenderDistanceDefaults.STEP_SIZE >= 1);
    }

    @Test
    public void theDwellIsNonNegative() {
        assertTrue("a negative dwell is undefined",
                RenderDistanceDefaults.MIN_DWELL_MS >= 0L);
    }
}
