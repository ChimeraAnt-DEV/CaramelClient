package org.chimeramc.client.launcher.ui.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.ui.components.UiTokens;
import org.junit.Test;

/**
 * The launch ring's phase and colour rules, pinned without a device.
 *
 * <p>The ring replaces the old progress bar and log wall: its colour has to say "preparing",
 * "working", "ready" or "failed" on its own, so the mapping is asserted here rather than left to a
 * hand-tuned view.
 */
public class LaunchRingPaletteTest {

    @Test
    public void thePhaseFollowsFailureReadinessThenProgress() {
        assertEquals(LaunchRingPalette.Phase.FAILED,
                LaunchRingPalette.phaseFor(true, true, 90));
        assertEquals(LaunchRingPalette.Phase.READY,
                LaunchRingPalette.phaseFor(false, true, 90));
        assertEquals(LaunchRingPalette.Phase.PREPARING,
                LaunchRingPalette.phaseFor(false, false, 0));
        assertEquals(LaunchRingPalette.Phase.PREPARING,
                LaunchRingPalette.phaseFor(false, false, 3));
        assertEquals(LaunchRingPalette.Phase.WORKING,
                LaunchRingPalette.phaseFor(false, false, 40));
    }

    @Test
    public void everyPhaseHasDistinctNearAndFarColours() {
        for (LaunchRingPalette.Phase p : LaunchRingPalette.Phase.values()) {
            assertTrue(p + " near and far colours must differ",
                    LaunchRingPalette.nearColor(p) != LaunchRingPalette.farColor(p));
        }
    }

    @Test
    public void thePreparingRingIsNavyToSnowBlue() {
        // The calm default must be the navy/snow family, matching the theme.
        int near = LaunchRingPalette.nearColor(LaunchRingPalette.Phase.PREPARING);
        int far = LaunchRingPalette.farColor(LaunchRingPalette.Phase.PREPARING);
        assertTrue(blueDominant(near));
        assertTrue(blueDominant(far));
        assertTrue("snow blue must read on the navy end",
                UiTokens.contrastRatio(far, near) >= 2.0);
    }

    @Test
    public void theReadyAndFailedPhasesReadGreenAndRed() {
        int ready = LaunchRingPalette.farColor(LaunchRingPalette.Phase.READY);
        assertTrue("ready is green-dominant", ((ready >>> 8) & 0xFF) > ((ready >>> 16) & 0xFF));
        int failed = LaunchRingPalette.farColor(LaunchRingPalette.Phase.FAILED);
        assertTrue("failed is red-dominant", ((failed >>> 16) & 0xFF) > ((failed >>> 8) & 0xFF));
    }

    @Test
    public void colorAtClampsAndEndsOnThePhaseColours() {
        LaunchRingPalette.Phase p = LaunchRingPalette.Phase.WORKING;
        assertEquals(LaunchRingPalette.nearColor(p), LaunchRingPalette.colorAt(p, 0f));
        assertEquals(LaunchRingPalette.farColor(p), LaunchRingPalette.colorAt(p, 1f));
        // Out of range clamps rather than wrapping.
        assertEquals(LaunchRingPalette.nearColor(p), LaunchRingPalette.colorAt(p, -1f));
        assertEquals(LaunchRingPalette.farColor(p), LaunchRingPalette.colorAt(p, 2f));
    }

    @Test
    public void theSweepIsSteadyAndWraps() {
        assertEquals(0f, LaunchRingPalette.sweepDegrees(0, 1000f), 0.01f);
        assertEquals(180f, LaunchRingPalette.sweepDegrees(500, 1000f), 0.01f);
        // A full period wraps back to zero.
        assertEquals(0f, LaunchRingPalette.sweepDegrees(1000, 1000f), 0.01f);
        // A zero period must not divide by zero.
        assertEquals(0f, LaunchRingPalette.sweepDegrees(123, 0f), 0.01f);
    }

    private static boolean blueDominant(int c) {
        int r = (c >>> 16) & 0xFF, g = (c >>> 8) & 0xFF, b = c & 0xFF;
        return b >= r && b >= g;
    }
}
