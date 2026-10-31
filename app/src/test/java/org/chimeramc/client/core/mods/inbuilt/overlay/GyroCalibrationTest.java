package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Gyroscope bias capture (item 194).
 *
 * <p>Pins the rules that decide when a calibration is usable: enough samples, a mean that ignores a
 * dropped (NaN) frame, and a capture that is inert until begun.
 */
public class GyroCalibrationTest {

    @Test
    public void aCaptureIsInertUntilBegun() {
        GyroCalibration c = new GyroCalibration();
        assertFalse(c.isActive());
        c.add(1f, 2f, 3f);
        assertEquals(0, c.sampleCount());
        assertEquals(0f, c.biasX(), 0f);
    }

    @Test
    public void theBiasIsTheMeanOfTheSamples() {
        GyroCalibration c = new GyroCalibration();
        c.begin();
        // A still device with a small constant offset.
        for (int i = 0; i < GyroCalibration.MIN_SAMPLES; i++) {
            c.add(0.02f, -0.01f, 0.005f);
        }
        assertTrue(c.isReady());
        assertTrue(c.canCommit());
        assertEquals(0.02f, c.biasX(), 1e-5);
        assertEquals(-0.01f, c.biasY(), 1e-5);
        assertEquals(0.005f, c.biasZ(), 1e-5);
    }

    @Test
    public void notReadyBeforeTheMinimumSampleCount() {
        GyroCalibration c = new GyroCalibration();
        c.begin();
        for (int i = 0; i < GyroCalibration.MIN_SAMPLES - 1; i++) c.add(0.1f, 0f, 0f);
        assertFalse(c.isReady());
        assertFalse(c.canCommit());
        assertTrue(c.progress() < 1f);
    }

    @Test
    public void aNonFiniteSampleIsIgnored() {
        GyroCalibration c = new GyroCalibration();
        c.begin();
        c.add(Float.NaN, 0f, 0f);
        c.add(0f, Float.POSITIVE_INFINITY, 0f);
        c.add(0.1f, 0.2f, 0.3f);
        assertEquals(1, c.sampleCount());
        assertEquals(0.1f, c.biasX(), 1e-5);
    }

    @Test
    public void stopFreezesTheSamplesAndBeginClearsThem() {
        GyroCalibration c = new GyroCalibration();
        c.begin();
        c.add(1f, 1f, 1f);
        c.stop();
        assertFalse(c.isActive());
        c.add(9f, 9f, 9f);
        assertEquals(1, c.sampleCount());
        // A restart discards the previous run so a re-calibration is not averaged with the old one.
        c.begin();
        assertEquals(0, c.sampleCount());
        assertTrue(c.isActive());
    }

    @Test
    public void progressReachesOneAtTheMinimum() {
        GyroCalibration c = new GyroCalibration();
        c.begin();
        for (int i = 0; i < GyroCalibration.MIN_SAMPLES; i++) c.add(0f, 0f, 0f);
        assertEquals(1f, c.progress(), 1e-5);
    }
}
