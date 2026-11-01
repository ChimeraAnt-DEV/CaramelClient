package org.chimeramc.pojavcontrols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The pointer-speed calibration rules (item 223).
 *
 * <p>Pure arithmetic, so it runs without a device. The two rules that matter are that a drag is
 * only accepted when it is long enough to be a real measurement, and that the multiplier is bounded
 * so a bad measurement cannot make the look speed unplayable.
 */
public class PointerCalibrationTest {

    @Test
    public void aDragAcrossTheWholeTargetLeavesTheSpeedUnchanged() {
        assertEquals(1f, PointerCalibration.multiplierFor(1000f, 1000f), 0.0001f);
    }

    @Test
    public void aShorterDragRaisesTheMultiplier() {
        // Half the distance means twice the sensitivity.
        assertEquals(2f, PointerCalibration.multiplierFor(1000f, 500f), 0.0001f);
    }

    @Test
    public void aLongerDragLowersTheMultiplier() {
        assertEquals(0.5f, PointerCalibration.multiplierFor(1000f, 2000f), 0.0001f);
    }

    @Test
    public void theMultiplierIsClampedAtBothEnds() {
        // A 300px drag over a 1000px target implies 3.33x, which is above the cap and clamps down.
        assertEquals(PointerCalibration.MAX_MULTIPLIER, PointerCalibration.multiplierFor(1000f, 300f), 0.0001f);
        assertEquals(PointerCalibration.MIN_MULTIPLIER, PointerCalibration.multiplierFor(1000f, 100000f), 0.0001f);
    }

    @Test
    public void aTapIsRefusedRatherThanTreatedAsMaxSensitivity() {
        assertFalse(PointerCalibration.isUsable(1000f, 5f));
        assertEquals(PointerCalibration.DEFAULT_MULTIPLIER, PointerCalibration.multiplierFor(1000f, 5f), 0.0001f);
    }

    @Test
    public void aDragAtTheMinimumFractionIsAccepted() {
        float measured = 1000f * PointerCalibration.MIN_MEASURED_FRACTION;
        assertTrue(PointerCalibration.isUsable(1000f, measured));
    }

    @Test
    public void nonPositiveOrNonFiniteInputsAreRefused() {
        assertFalse(PointerCalibration.isUsable(0f, 100f));
        assertFalse(PointerCalibration.isUsable(100f, 0f));
        assertFalse(PointerCalibration.isUsable(-1f, 100f));
        assertFalse(PointerCalibration.isUsable(Float.NaN, 100f));
        assertFalse(PointerCalibration.isUsable(100f, Float.POSITIVE_INFINITY));
        assertEquals(PointerCalibration.DEFAULT_MULTIPLIER,
                PointerCalibration.multiplierFor(Float.NaN, Float.NaN), 0.0001f);
    }

    @Test
    public void aNonsenseStoredMultiplierReadsAsTheDefault() {
        assertEquals(PointerCalibration.DEFAULT_MULTIPLIER, PointerCalibration.clamp(Float.NaN), 0.0001f);
        assertEquals(PointerCalibration.DEFAULT_MULTIPLIER,
                PointerCalibration.clamp(Float.POSITIVE_INFINITY), 0.0001f);
    }

    @Test
    public void applyScalesTheBaseSensitivity() {
        assertEquals(2.8f, PointerCalibration.apply(1.4f, 2f), 0.0001f);
        assertEquals(1.4f, PointerCalibration.apply(1.4f, PointerCalibration.DEFAULT_MULTIPLIER), 0.0001f);
    }

    @Test
    public void applyClampsAStoredMultiplierThatIsOutOfRange() {
        assertEquals(1.4f * PointerCalibration.MAX_MULTIPLIER,
                PointerCalibration.apply(1.4f, 100f), 0.0001f);
        assertEquals(1.4f * PointerCalibration.MIN_MULTIPLIER,
                PointerCalibration.apply(1.4f, 0.001f), 0.0001f);
    }

    @Test
    public void applyLeavesAnInvalidBaseAlone() {
        assertEquals(0f, PointerCalibration.apply(0f, 2f), 0.0001f);
        assertEquals(Float.NaN, PointerCalibration.apply(Float.NaN, 2f), 0.0001f);
    }
}
