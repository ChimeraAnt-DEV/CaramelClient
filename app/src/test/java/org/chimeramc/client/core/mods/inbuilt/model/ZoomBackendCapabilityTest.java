package org.chimeramc.client.core.mods.inbuilt.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.backend.GameBackend;
import org.chimeramc.client.core.backend.GameBackend.Feature;
import org.chimeramc.client.core.backend.GameBridge;
import org.junit.After;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

/**
 * Zoom is available only when the active backend can actually change the field of view.
 *
 * <p>Zoom used to be listed as a working module on the strength of a {@code GameBackend.setZoom}
 * call that no backend in this build implements: the launcher injects keys, buttons, scroll and look
 * deltas, and none of those changes the field of view. A player could toggle Zoom on, hold the key,
 * and watch nothing happen — the module read as broken rather than as "not available on this build".
 *
 * <p>The capability is the authority. {@link GameBackend.Feature#ZOOM} is the one flag that means
 * "this backend can drive a field-of-view change" (a future backend reaches it through BerryAPI's
 * {@code render.set_zoom}); while no backend advertises it, the module is badged and its toggle is
 * refused, exactly like a module waiting on a game-data feed. The rule is asserted here so the badge
 * and the backend cannot drift apart.
 */
public class ZoomBackendCapabilityTest {

    /** A backend that advertises exactly the features a test asks for. */
    private static final class FakeBackend implements GameBackend {
        private final Set<Feature> supported;

        FakeBackend(Feature... features) {
            this.supported = features.length == 0
                    ? EnumSet.noneOf(Feature.class)
                    : EnumSet.copyOf(java.util.Arrays.asList(features));
        }

        @Override
        public String name() {
            return "fake";
        }

        @Override
        public String version() {
            return "1";
        }

        @Override
        public int apiVersion() {
            return API_VERSION;
        }

        @Override
        public boolean supports(Feature feature) {
            return supported.contains(feature);
        }

        @Override
        public boolean sendKey(int bedrockKeyCode, boolean down) {
            return true;
        }

        @Override
        public boolean sendMouseButton(int androidButton, boolean down) {
            return true;
        }

        @Override
        public boolean sendScroll(float vertical) {
            return true;
        }

        @Override
        public boolean sendLookDelta(float deltaX, float deltaY) {
            return true;
        }

        @Override
        public boolean sendPointer(float x, float y) {
            return true;
        }

        @Override
        public int readFps() {
            return -1;
        }

        @Override
        public void releaseAll() {
        }
    }

    @After
    public void tearDown() {
        GameBridge.unregister();
    }

    @Test
    public void zoomIsUnavailableWhileNoBackendProvidesFieldOfView() {
        GameBridge.unregister();
        assertTrue("zoom must be badged while no backend can change the FOV",
                ModAvailability.isUnavailable(ModIds.ZOOM));
        assertFalse("a badged module must not be interactive",
                ModAvailability.isInteractive(ModIds.ZOOM));
        assertEquals(ModAvailability.REASON_NO_BACKEND,
                ModAvailability.unavailableReason(ModIds.ZOOM));
    }

    @Test
    public void zoomBecomesAvailableWhenABackendProvidesFieldOfView() {
        GameBridge.register(new FakeBackend(Feature.ZOOM));
        assertFalse("a backend that changes the FOV makes zoom usable",
                ModAvailability.isUnavailable(ModIds.ZOOM));
        assertTrue(ModAvailability.isInteractive(ModIds.ZOOM));
        assertNull(ModAvailability.unavailableReason(ModIds.ZOOM));
    }

    @Test
    public void aBackendWithoutZoomDoesNotUnlockIt() {
        GameBridge.register(new FakeBackend(Feature.KEY, Feature.LOOK_DELTA, Feature.FPS));
        assertTrue(ModAvailability.isUnavailable(ModIds.ZOOM));
    }

    @Test
    public void onlyZoomRequiresABackendFeature() {
        assertEquals(Feature.ZOOM, ModIds.requiredBackendFeature(ModIds.ZOOM));
        assertNull(ModIds.requiredBackendFeature(ModIds.AUTO_SPRINT));
        assertNull(ModIds.requiredBackendFeature(ModIds.FPS_DISPLAY));
        assertNull(ModIds.requiredBackendFeature(null));
    }
}
