package org.chimeramc.client.ui.navigation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the navigation attention dot (item 83).
 *
 * <p>A dot that is always on, or that appears for a healthy state, is noise: the user learns to
 * ignore it and then misses the one case that mattered. So the rule is asserted in both
 * directions - which signals earn a dot, and which destinations never do.
 */
public class NavTabBadgeTest {

    @Test
    public void aHealthyLauncherShowsNoDotAnywhere() {
        NavTabBadge.Signals none = NavTabBadge.Signals.none();
        for (LauncherTab tab : LauncherTab.values()) {
            assertFalse(tab + " must not carry a dot with no signals",
                    NavTabBadge.showsBadge(tab, none));
        }
        assertFalse(NavTabBadge.anyBadge(none));
    }

    @Test
    public void aFailedModPutsTheDotOnMods() {
        NavTabBadge.Signals signals = new NavTabBadge.Signals(true, false, false, false);
        assertTrue(NavTabBadge.showsBadge(LauncherTab.MODS, signals));
        assertTrue(NavTabBadge.anyBadge(signals));
        // The failure belongs to one screen; showing it everywhere would make it meaningless.
        assertFalse(NavTabBadge.showsBadge(LauncherTab.LAUNCH, signals));
        assertFalse(NavTabBadge.showsBadge(LauncherTab.LIBRARY, signals));
        assertFalse(NavTabBadge.showsBadge(LauncherTab.SETTINGS, signals));
    }

    @Test
    public void anAutoDisabledOrCrashLoopedModAlsoPutsTheDotOnMods() {
        assertTrue(NavTabBadge.showsBadge(LauncherTab.MODS,
                new NavTabBadge.Signals(false, true, false, false)));
        assertTrue(NavTabBadge.showsBadge(LauncherTab.MODS,
                new NavTabBadge.Signals(false, false, true, false)));
    }

    @Test
    public void anAvailableUpdatePutsTheDotOnSettings() {
        NavTabBadge.Signals signals = new NavTabBadge.Signals(false, false, false, true);
        assertTrue(NavTabBadge.showsBadge(LauncherTab.SETTINGS, signals));
        assertFalse(NavTabBadge.showsBadge(LauncherTab.MODS, signals));
    }

    @Test
    public void nullInputsNeverThrow() {
        assertFalse(NavTabBadge.showsBadge(null, NavTabBadge.Signals.none()));
        assertFalse(NavTabBadge.showsBadge(LauncherTab.MODS, null));
        assertFalse(NavTabBadge.anyBadge(null));
    }
}
