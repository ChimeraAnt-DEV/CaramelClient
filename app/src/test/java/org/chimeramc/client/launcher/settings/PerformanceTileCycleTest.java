package org.chimeramc.client.launcher.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.R;
import org.chimeramc.client.settings.PerformancePresetManager.Preset;
import org.junit.Test;

/**
 * The Quick Settings tile's tap cycle.
 *
 * <p>The tile is a small control with one gesture, so the cycle is worth pinning: every tap must
 * move to a different preset (a tap that looks like nothing happened reads as a broken tile), the
 * cycle must be a closed loop so it never runs off the end, and a null reading must behave like the
 * stored default rather than crashing the tile's binder thread.
 */
public class PerformanceTileCycleTest {

    @Test
    public void everyTapMovesToADifferentPreset() {
        assertNotEquals(Preset.BATTERY, PerformanceTileService.next(Preset.BATTERY));
        assertNotEquals(Preset.BALANCED, PerformanceTileService.next(Preset.BALANCED));
        assertNotEquals(Preset.PERFORMANCE, PerformanceTileService.next(Preset.PERFORMANCE));
    }

    @Test
    public void theCycleIsClosedAndCoversEveryPreset() {
        // Walking the cycle from any start must visit all three and return, never stall.
        Preset start = Preset.BATTERY;
        Preset seen = PerformanceTileService.next(start);
        int steps = 1;
        while (seen != start && steps < 10) {
            seen = PerformanceTileService.next(seen);
            steps++;
        }
        assertEquals("the tap cycle must return to its start", start, seen);
        assertEquals("the cycle must visit every preset exactly once", 3, steps);
    }

    @Test
    public void aNullReadingBehavesLikeTheDefault() {
        assertEquals(PerformanceTileService.next(Preset.BALANCED),
                PerformanceTileService.next(null));
        assertEquals(PerformanceTileService.labelFor(Preset.BALANCED),
                PerformanceTileService.labelFor(null));
    }

    @Test
    public void everyPresetHasItsOwnLabelAndIcon() {
        int battery = PerformanceTileService.labelFor(Preset.BATTERY);
        int balanced = PerformanceTileService.labelFor(Preset.BALANCED);
        int performance = PerformanceTileService.labelFor(Preset.PERFORMANCE);
        assertTrue("each preset needs its own label so the tile says which is active",
                battery != balanced && balanced != performance && battery != performance);
        // The icon is deliberately the same monochrome mark for all three; the label carries state.
        assertEquals(R.drawable.ic_glowberry_mono, PerformanceTileService.iconFor(Preset.BATTERY));
        assertEquals(R.drawable.ic_glowberry_mono, PerformanceTileService.iconFor(Preset.PERFORMANCE));
    }
}
