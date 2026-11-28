package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A press on a floating overlay button that does not move is a tap.
 *
 * <p>The in-game Mod Menu button was reported as doing nothing when pressed. One cause was the tap
 * window: a tap had to be released within 200 ms, and a deliberate press on a small button is
 * routinely slower than that. Such a press was neither a tap (too slow) nor a drag (no travel), so
 * the click handler never ran and the button looked dead.
 */
public class OverlayTapRuleTest {

    @Test
    public void aQuickPressIsATap() {
        assertTrue(OverlayTapRule.isTap(0f, 0f, 80, false));
    }

    @Test
    public void aDeliberateSlowPressIsStillATap() {
        assertTrue("a 400 ms press with no travel must open the menu",
                OverlayTapRule.isTap(0f, 0f, 400, false));
        assertTrue(OverlayTapRule.isTap(3f, 2f, OverlayTapRule.MAX_TAP_MS, false));
    }

    @Test
    public void aHoldIsNotATap() {
        assertFalse(OverlayTapRule.isTap(0f, 0f, OverlayTapRule.MAX_TAP_MS + 1, false));
        assertFalse(OverlayTapRule.isTap(0f, 0f, -5, false));
    }

    @Test
    public void travelMakesItADrag() {
        assertFalse(OverlayTapRule.isTap(OverlayTapRule.MOVE_SLOP_PX + 1, 0f, 100, false));
        assertFalse(OverlayTapRule.isTap(0f, -(OverlayTapRule.MOVE_SLOP_PX + 1), 100, false));
        assertTrue(OverlayTapRule.isTap(OverlayTapRule.MOVE_SLOP_PX, OverlayTapRule.MOVE_SLOP_PX,
                100, false));
    }

    @Test
    public void theHudEditorOwnsThePress() {
        assertFalse("in the HUD editor a press selects and resizes, it does not act",
                OverlayTapRule.isTap(0f, 0f, 100, true));
    }
}
