package org.chimeramc.client.launcher.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The live raw-vs-filtered reading behind the dead-zone editor.
 *
 * <p>The value of this class is that the picture agrees with the game, so the tests pin it to the
 * same {@link ControllerResponse} the input path runs rather than to a copy of the maths.
 */
public class StickResponsePreviewTest {

    private static ControllerProfile profileWith(float leftZone, float rightZone,
                                                 boolean antiDrift) {
        ControllerProfile profile = new ControllerProfile("test");
        profile.setLeftDeadZone(leftZone);
        profile.setRightDeadZone(rightZone);
        profile.setAntiDriftEnabled(antiDrift);
        return profile;
    }

    @Test
    public void aReadingInsideTheDeadZoneProducesNoOutput() {
        ControllerResponse response = new ControllerResponse(profileWith(0.2f, 0.2f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);
        preview.setRaw(0.05f, 0.05f);

        assertEquals(0f, preview.filteredX(), 1e-6f);
        assertEquals(0f, preview.filteredY(), 1e-6f);
        assertFalse(preview.isOutsideDeadZone());
    }

    @Test
    public void aReadingPastTheDeadZoneProducesOutput() {
        ControllerResponse response = new ControllerResponse(profileWith(0.2f, 0.2f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);
        preview.setRaw(0.9f, 0f);

        assertTrue(preview.isOutsideDeadZone());
        assertTrue("output should be positive", preview.filteredX() > 0f);
        assertEquals(0f, preview.filteredY(), 1e-6f);
    }

    @Test
    public void theRawReadingIsKeptUnchangedWhileTheOutputIsShaped() {
        ControllerResponse response = new ControllerResponse(profileWith(0.3f, 0.3f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);
        preview.setRaw(0.8f, 0f);

        assertEquals(0.8f, preview.rawX(), 1e-6f);
        assertNotEquals("the shape should move the output away from the raw reading",
                preview.rawX(), preview.filteredX(), 1e-4f);
    }

    @Test
    public void theDirectionOfPushIsPreserved() {
        ControllerResponse response = new ControllerResponse(profileWith(0.1f, 0.1f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);
        preview.setRaw(0.5f, 0.5f);

        // Equal raw components must stay equal after shaping: only the length changes.
        assertEquals(preview.filteredX(), preview.filteredY(), 1e-5f);
    }

    @Test
    public void eachSideUsesItsOwnDeadZone() {
        ControllerResponse response = new ControllerResponse(profileWith(0.05f, 0.6f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);

        preview.setSide(StickResponsePreview.Side.LEFT);
        preview.setRaw(0.4f, 0f);
        assertTrue("left zone is small, so this is outside it", preview.isOutsideDeadZone());

        preview.setSide(StickResponsePreview.Side.RIGHT);
        preview.setRaw(0.4f, 0f);
        assertFalse("right zone is large, so the same reading is inside it",
                preview.isOutsideDeadZone());
    }

    @Test
    public void theDeadZoneRadiusMatchesTheResponse() {
        ControllerResponse response = new ControllerResponse(profileWith(0.15f, 0.45f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);

        preview.setSide(StickResponsePreview.Side.LEFT);
        assertEquals(response.driftThreshold(true), preview.deadZoneRadius(), 1e-6f);
        preview.setSide(StickResponsePreview.Side.RIGHT);
        assertEquals(response.driftThreshold(false), preview.deadZoneRadius(), 1e-6f);
    }

    @Test
    public void aMissingResponseReadsAsCentred() {
        StickResponsePreview preview = new StickResponsePreview();
        preview.setRaw(0.9f, 0.9f);

        assertNull(preview.getResponse());
        assertEquals(0f, preview.filteredMagnitude(), 1e-6f);
        assertFalse(preview.isOutsideDeadZone());
        assertEquals(0f, preview.deadZoneRadius(), 1e-6f);
    }

    @Test
    public void aNonFiniteReadingIsTreatedAsCentred() {
        // A torn native read must not throw the preview into a bogus corner.
        ControllerResponse response = new ControllerResponse(profileWith(0.1f, 0.1f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);
        preview.setRaw(Float.NaN, Float.POSITIVE_INFINITY);

        assertEquals(0f, preview.rawX(), 1e-6f);
        assertEquals(0f, preview.rawY(), 1e-6f);
        assertFalse(preview.isOutsideDeadZone());
    }

    @Test
    public void aReadingBeyondFullTravelIsClamped() {
        ControllerResponse response = new ControllerResponse(profileWith(0.1f, 0.1f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);
        preview.setRaw(3f, -4f);

        assertEquals(1f, preview.rawX(), 1e-6f);
        assertEquals(-1f, preview.rawY(), 1e-6f);
    }

    @Test
    public void clearingReturnsTheReadingToTheCentre() {
        ControllerResponse response = new ControllerResponse(profileWith(0.1f, 0.1f, true), false);
        StickResponsePreview preview = new StickResponsePreview();
        preview.setResponse(response);
        preview.setRaw(0.9f, 0.9f);
        preview.clear();

        assertEquals(0f, preview.rawX(), 1e-6f);
        assertEquals(0f, preview.filteredMagnitude(), 1e-6f);
        assertFalse(preview.isOutsideDeadZone());
    }

    @Test
    public void aNullSideFallsBackToTheLeftStick() {
        StickResponsePreview preview = new StickResponsePreview();
        preview.setSide(StickResponsePreview.Side.RIGHT);
        preview.setSide(null);
        assertEquals(StickResponsePreview.Side.LEFT, preview.getSide());
    }
}
