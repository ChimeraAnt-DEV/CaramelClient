package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The colour-blind-safe HUD rule.
 *
 * <p>Two properties matter and both are easy to lose by tuning by eye: every level must be
 * distinguishable <em>without</em> colour (a distinct cue, or the level is colour-only), and no two
 * levels may share a colour (or the colour carries no information at all).
 */
public class HudThresholdTest {

    @Test
    public void theThresholdsAreInOrderAndLeaveNoGap() {
        assertTrue(HudThreshold.POOR_FPS < HudThreshold.GOOD_FPS);
        assertEquals(HudThreshold.Level.POOR, HudThreshold.levelForFps(HudThreshold.POOR_FPS - 1));
        assertEquals(HudThreshold.Level.FAIR, HudThreshold.levelForFps(HudThreshold.POOR_FPS));
        assertEquals(HudThreshold.Level.FAIR, HudThreshold.levelForFps(HudThreshold.GOOD_FPS - 1));
        assertEquals(HudThreshold.Level.GOOD, HudThreshold.levelForFps(HudThreshold.GOOD_FPS));
    }

    @Test
    public void anAbsentReadingIsNotALevel() {
        assertEquals(HudThreshold.Level.UNKNOWN, HudThreshold.levelForFps(0));
        assertEquals(HudThreshold.Level.UNKNOWN, HudThreshold.levelForFps(-1));
        assertEquals("an unknown must not render a number",
                ModStatsFormatter.NO_READING, HudThreshold.readout(0));
        assertEquals("", HudThreshold.cue(HudThreshold.Level.UNKNOWN));
        assertEquals("", HudThreshold.cue(null));
    }

    @Test
    public void everyLevelHasItsOwnColour() {
        int good = HudThreshold.colourFor(HudThreshold.Level.GOOD);
        int fair = HudThreshold.colourFor(HudThreshold.Level.FAIR);
        int poor = HudThreshold.colourFor(HudThreshold.Level.POOR);
        assertNotEquals("good and fair share a colour", good, fair);
        assertNotEquals("fair and poor share a colour", fair, poor);
        assertNotEquals("good and poor share a colour", good, poor);
        assertNotEquals("unknown must not look like a level",
                HudThreshold.colourFor(HudThreshold.Level.UNKNOWN), good);
    }

    @Test
    public void everyLevelHasANonColourCue() {
        for (HudThreshold.Level level : HudThreshold.Level.values()) {
            if (level == HudThreshold.Level.UNKNOWN) continue;
            String cue = HudThreshold.cue(level);
            assertFalse("a level with no cue is carried by colour alone: " + level,
                    cue == null || cue.isEmpty());
        }
        // The three cues must differ from each other, or the shape carries nothing either.
        assertNotEquals(HudThreshold.cue(HudThreshold.Level.GOOD),
                HudThreshold.cue(HudThreshold.Level.FAIR));
        assertNotEquals(HudThreshold.cue(HudThreshold.Level.FAIR),
                HudThreshold.cue(HudThreshold.Level.POOR));
    }

    @Test
    public void theReadoutCarriesTheCueAndTheNumber() {
        assertEquals(HudThreshold.cue(HudThreshold.Level.GOOD) + " 60 FPS",
                HudThreshold.readout(60));
        assertEquals(HudThreshold.cue(HudThreshold.Level.POOR) + " 12 FPS",
                HudThreshold.readout(12));
    }

    @Test
    public void thePaletteAvoidsTheGreenRedAxis() {
        // The good colour is blue-dominant and the poor colour is red-dominant: the axis runs from
        // a cool hue to a warm one, which is the axis that survives red-green colour blindness.
        int good = HudThreshold.colourFor(HudThreshold.Level.GOOD);
        int poor = HudThreshold.colourFor(HudThreshold.Level.POOR);
        int goodBlue = good & 0xFF;
        int goodRed = (good >> 16) & 0xFF;
        int poorBlue = poor & 0xFF;
        int poorRed = (poor >> 16) & 0xFF;
        assertTrue("good must read as cool", goodBlue > goodRed);
        assertTrue("poor must read as warm", poorRed > poorBlue);
    }
}
