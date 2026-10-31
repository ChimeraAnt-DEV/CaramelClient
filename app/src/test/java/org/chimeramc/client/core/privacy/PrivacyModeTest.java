package org.chimeramc.client.core.privacy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The guest-mode rule (item 272), tested without a device.
 *
 * <p>These pin the two things that are easy to get wrong: which surface guest mode owns, and that
 * it never hides the game itself.
 */
public class PrivacyModeTest {

    @Test
    public void guestModeHidesTheFeatureSurfaces() {
        PrivacyMode mode = new PrivacyMode(true);
        assertTrue(mode.isGuestMode());

        assertTrue(mode.hidesMods());
        assertTrue(mode.hidesServers());
        assertTrue(mode.hidesVoice());
    }

    @Test
    public void everySurfaceBelongsToGuestMode() {
        PrivacyMode guest = new PrivacyMode(true);
        for (PrivacyMode.Surface surface : PrivacyMode.Surface.values()) {
            assertTrue("surface " + surface + " must be hidden", guest.hides(surface));
        }
    }

    @Test
    public void openModeHidesNothing() {
        PrivacyMode mode = PrivacyMode.open();
        assertFalse(mode.isAnyActive());
        for (PrivacyMode.Surface surface : PrivacyMode.Surface.values()) {
            assertFalse("surface " + surface + " must be shown", mode.hides(surface));
        }
    }

    @Test
    public void nullSurfaceIsNeverHidden() {
        assertFalse(new PrivacyMode(true).hides(null));
    }

    @Test
    public void anyActiveFollowsTheFlag() {
        assertFalse(new PrivacyMode(false).isAnyActive());
        assertTrue(new PrivacyMode(true).isAnyActive());
    }
}
