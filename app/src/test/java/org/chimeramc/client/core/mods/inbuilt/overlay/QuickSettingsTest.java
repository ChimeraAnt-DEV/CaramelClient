package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the in-game quick settings model.
 *
 * <p>The panel, the options writer and the tests must agree about the offered values and how an
 * out-of-range value is snapped, because a chip set with nothing selected reads as "nothing
 * applied" and a cap the display cannot honour must be reported rather than pretended.
 */
public class QuickSettingsTest {

    @Test
    public void theResolutionChoicesAreTheOfferedScales() {
        int[] values = QuickSettings.RESOLUTION_SCALES;
        assertEquals(3, values.length);
        assertEquals(50, values[0]);
        assertEquals(75, values[1]);
        assertEquals(100, values[2]);
    }

    @Test
    public void theFrameCapsIncludeTheUnlimitedSentinelLast() {
        int[] values = QuickSettings.REFRESH_CAPS;
        assertEquals(5, values.length);
        assertEquals(QuickSettings.CAP_UNLIMITED, values[values.length - 1]);
    }

    @Test
    public void aResolutionScaleOutsideTheSetSnapsToTheNearest() {
        assertEquals(50, QuickSettings.clampResolutionScale(0));
        assertEquals(50, QuickSettings.clampResolutionScale(49));
        assertEquals(50, QuickSettings.clampResolutionScale(60));
        assertEquals(75, QuickSettings.clampResolutionScale(76));
        assertEquals(100, QuickSettings.clampResolutionScale(100));
        assertEquals(100, QuickSettings.clampResolutionScale(1000));
    }

    @Test
    public void aFrameCapOutsideTheSetSnapsToTheNearest() {
        assertEquals(30, QuickSettings.clampRefreshCap(20));
        assertEquals(60, QuickSettings.clampRefreshCap(61));
        assertEquals(120, QuickSettings.clampRefreshCap(240));
        assertEquals(QuickSettings.CAP_UNLIMITED, QuickSettings.clampRefreshCap(10));
    }

    @Test
    public void membershipMatchesTheOfferedSets() {
        assertTrue(QuickSettings.isResolutionScale(75));
        assertFalse(QuickSettings.isResolutionScale(80));
        assertTrue(QuickSettings.isRefreshCap(90));
        assertFalse(QuickSettings.isRefreshCap(144));
        assertTrue(QuickSettings.isRefreshCap(QuickSettings.CAP_UNLIMITED));
    }

    @Test
    public void theOptionValueIsTheClampedScale() {
        assertEquals("75", QuickSettings.resolutionOptionValue(75));
        assertEquals("50", QuickSettings.resolutionOptionValue(0));
        assertEquals("100", QuickSettings.resolutionOptionValue(99));
    }

    @Test
    public void theResolutionChoicesCarryTheirLabelsInOrder() {
        int[] labels = {1, 2, 3};
        assertEquals(3, QuickSettings.resolutionChoices(labels).size());
        assertEquals(1, QuickSettings.resolutionChoices(labels).get(0).labelRes);
        assertEquals(3, QuickSettings.resolutionChoices(labels).get(2).labelRes);
    }

    @Test
    public void aMissingLabelFallsBackToZeroRatherThanThrowing() {
        assertEquals(0, QuickSettings.resolutionChoices(null).get(0).labelRes);
        assertEquals(0, QuickSettings.refreshChoices(new int[]{7}).get(1).labelRes);
    }
}
