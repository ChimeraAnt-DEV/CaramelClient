package org.chimeramc.client.ui.navigation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins what a long press on a destination offers.
 *
 * <p>The menu is the only place a section can be reached without opening its tab first, so the
 * rule is asserted rather than left to the listener: a tab with sections must offer them all, a tab
 * without any must offer none (an empty popup is worse than no popup), and no action may be
 * offered twice from one tab.
 */
public class NavTabLongPressTest {

    @Test
    public void everyLibrarySectionIsOfferedFromTheLibraryTab() {
        assertArrayEquals(
                "the Library tab holds Instances, Install and Replay, so all three must be reachable",
                new NavTabLongPress.Action[]{
                        NavTabLongPress.Action.LIBRARY_INSTANCES,
                        NavTabLongPress.Action.LIBRARY_INSTALL,
                        NavTabLongPress.Action.LIBRARY_REPLAY},
                NavTabLongPress.actionsFor(LauncherTab.LIBRARY));
    }

    @Test
    public void theModsTabOffersTheHubAndSettingsOffersDiagnostics() {
        assertArrayEquals(new NavTabLongPress.Action[]{NavTabLongPress.Action.MOD_HUB},
                NavTabLongPress.actionsFor(LauncherTab.MODS));
        assertArrayEquals(new NavTabLongPress.Action[]{NavTabLongPress.Action.DIAGNOSTICS},
                NavTabLongPress.actionsFor(LauncherTab.SETTINGS));
    }

    @Test
    public void theHomeTabOffersNothing() {
        // Play is the launcher's home and has no section under it. Offering a menu with one filler
        // entry would teach the user that a long press is meaningless on some tabs.
        assertEquals(0, NavTabLongPress.actionsFor(LauncherTab.LAUNCH).length);
        assertFalse(NavTabLongPress.hasActions(LauncherTab.LAUNCH));
    }

    @Test
    public void aNullTabIsNoMenuRatherThanACrash() {
        assertEquals(0, NavTabLongPress.actionsFor(null).length);
        assertFalse(NavTabLongPress.hasActions(null));
    }

    @Test
    public void noTabOffersTheSameActionTwice() {
        for (LauncherTab tab : LauncherTab.values()) {
            NavTabLongPress.Action[] actions = NavTabLongPress.actionsFor(tab);
            for (int i = 0; i < actions.length; i++) {
                for (int j = i + 1; j < actions.length; j++) {
                    assertTrue(tab + " offers " + actions[i] + " twice",
                            actions[i] != actions[j]);
                }
            }
        }
    }

    @Test
    public void everyActionNamesALabel() {
        // The popup is built from these resource ids; a zero would render an empty row.
        for (NavTabLongPress.Action action : NavTabLongPress.Action.values()) {
            assertTrue(action + " has no label resource", action.labelRes() != 0);
        }
    }

    @Test
    public void hasActionsAgreesWithTheActionList() {
        for (LauncherTab tab : LauncherTab.values()) {
            assertEquals(tab + ": hasActions disagrees with actionsFor",
                    NavTabLongPress.actionsFor(tab).length > 0,
                    NavTabLongPress.hasActions(tab));
        }
    }
}
