package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The frame-cap to surface-rate mapping (item 261).
 *
 * <p>Pure, so the mapping is pinned without a device: a cap of 90 must request 90.0, and a cap of
 * zero (unlimited) must request the documented reset value 0 rather than a nonsense negative rate.
 */
public class FrameRateTargetTest {

    @Test
    public void aPositiveCapBecomesThatRate() {
        assertEquals(90f, FrameRateTarget.rateForCap(90), 0.001f);
        assertEquals(120f, FrameRateTarget.rateForCap(120), 0.001f);
        assertEquals(30f, FrameRateTarget.rateForCap(30), 0.001f);
    }

    @Test
    public void zeroAndNegativeClearTheRate() {
        assertEquals(0f, FrameRateTarget.rateForCap(0), 0.001f);
        assertEquals(0f, FrameRateTarget.rateForCap(-5), 0.001f);
        assertTrue(FrameRateTarget.clearsRate(0));
        assertTrue(FrameRateTarget.clearsRate(-1));
        assertFalse(FrameRateTarget.clearsRate(60));
    }

    @Test
    public void theCompatibilityAndChangeFlagsAreTheFixedSourceAndAlwaysValues() {
        // Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE and Surface.CHANGE_FRAME_RATE_ALWAYS are
        // both 1; pinning them keeps a typo from silently requesting a different platform policy.
        assertEquals(1, FrameRateTarget.COMPATIBILITY_FIXED_SOURCE);
        assertEquals(1, FrameRateTarget.CHANGE_FRAME_RATE_ALWAYS);
    }
}
