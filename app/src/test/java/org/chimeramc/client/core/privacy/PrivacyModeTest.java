package org.chimeramc.client.core.privacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The privacy-mode rules, tested without a device.
 *
 * <p>These pin the two things that are easy to get wrong: which surface belongs to which mode, and
 * that neither mode ever hides the game itself.
 */
public class PrivacyModeTest {

    @Test
    public void guestModeHidesOnlyTheFeatureSurfaces() {
        PrivacyMode mode = new PrivacyMode(true, false);
        assertTrue(mode.isGuestMode());
        assertFalse(mode.isStreamingSafeMode());

        assertTrue(mode.hidesMods());
        assertTrue(mode.hidesServers());
        assertTrue(mode.hidesVoice());

        // Streaming-safe data is untouched by guest mode.
        assertFalse(mode.hidesCoordinates());
        assertFalse(mode.hidesIdentity());
        assertFalse(mode.hidesServerAddress());
    }

    @Test
    public void streamingSafeModeMasksOnlyTheIdentifyingData() {
        PrivacyMode mode = new PrivacyMode(false, true);
        assertFalse(mode.isGuestMode());
        assertTrue(mode.isStreamingSafeMode());

        assertTrue(mode.hidesCoordinates());
        assertTrue(mode.hidesIdentity());
        assertTrue(mode.hidesServerAddress());

        // The feature surfaces stay reachable; a streamer still wants their mods.
        assertFalse(mode.hidesMods());
        assertFalse(mode.hidesServers());
        assertFalse(mode.hidesVoice());
    }

    @Test
    public void bothModesTogetherHideEverythingTheyOwn() {
        PrivacyMode mode = new PrivacyMode(true, true);
        for (PrivacyMode.Surface surface : PrivacyMode.Surface.values()) {
            assertTrue("surface " + surface + " must be hidden", mode.hides(surface));
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
    public void everySurfaceBelongsToExactlyOneMode() {
        PrivacyMode guestOnly = new PrivacyMode(true, false);
        PrivacyMode streamingOnly = new PrivacyMode(false, true);
        for (PrivacyMode.Surface surface : PrivacyMode.Surface.values()) {
            boolean byGuest = guestOnly.hides(surface);
            boolean byStreaming = streamingOnly.hides(surface);
            assertTrue("surface " + surface + " must be hidden by exactly one mode",
                    byGuest ^ byStreaming);
        }
    }

    @Test
    public void nullSurfaceIsNeverHidden() {
        assertFalse(new PrivacyMode(true, true).hides(null));
    }

    @Test
    public void anyActiveIsTrueWhenEitherModeIsOn() {
        assertFalse(new PrivacyMode(false, false).isAnyActive());
        assertTrue(new PrivacyMode(true, false).isAnyActive());
        assertTrue(new PrivacyMode(false, true).isAnyActive());
        assertTrue(new PrivacyMode(true, true).isAnyActive());
    }
}
