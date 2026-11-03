package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The zoom scroll sensitivity and smoothing filter.
 *
 * <p>Pins the multiplier, the pass-through at smoothing 0, the one-pole easing that keeps the
 * configured fraction of the previous value, the clamp that stops smoothing 100 freezing the output,
 * and the reset that stops a new gesture inheriting the last one's easing.
 */
public class ZoomScrollFilterTest {

    @Test
    public void sensitivityScalesTheNotch() {
        ZoomScrollFilter filter = new ZoomScrollFilter();
        assertEquals(1f, filter.apply(1f, 100, 0), 0.0001f);
        assertEquals(2f, filter.apply(1f, 200, 0), 0.0001f);
        assertEquals(0.5f, filter.apply(1f, 50, 0), 0.0001f);
    }

    @Test
    public void zeroSmoothingPassesTheValueThrough() {
        ZoomScrollFilter filter = new ZoomScrollFilter();
        assertEquals(3f, filter.apply(3f, 100, 0), 0.0001f);
        assertEquals(3f, filter.apply(3f, 100, 0), 0.0001f);
    }

    @Test
    public void smoothingEasesTowardsTheTarget() {
        ZoomScrollFilter filter = new ZoomScrollFilter();
        // 50% smoothing: the first value is half the target, the next closes half the gap again.
        float first = filter.apply(1f, 100, 50);
        assertEquals(0.5f, first, 0.0001f);
        float second = filter.apply(1f, 100, 50);
        assertEquals(0.75f, second, 0.0001f);
    }

    @Test
    public void smoothingNeverFreezesTheOutput() {
        ZoomScrollFilter filter = new ZoomScrollFilter();
        // A smoothing above the cap clamps, so the value still moves towards the target.
        float first = filter.apply(1f, 100, 200);
        assertTrue("a clamped smoothing must still move the value", first > 0f);
        float second = filter.apply(1f, 100, 200);
        assertTrue(second > first);
    }

    @Test
    public void aResetForgetsThePreviousEasing() {
        ZoomScrollFilter filter = new ZoomScrollFilter();
        filter.apply(1f, 100, 50);
        filter.reset();
        assertEquals(0f, filter.current(), 0.0001f);
        // The first value after a reset behaves as a first value again.
        assertEquals(0.5f, filter.apply(1f, 100, 50), 0.0001f);
    }

    @Test
    public void currentReportsTheLastValue() {
        ZoomScrollFilter filter = new ZoomScrollFilter();
        filter.apply(2f, 100, 0);
        assertEquals(2f, filter.current(), 0.0001f);
    }
}
