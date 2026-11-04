package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A button presses: its face depresses and its edge shrinks.
 *
 * <p>A button with no pressed state is the "animated UI buttons look trash" report: tapping one
 * changed nothing, so the control felt dead. The press is a real geometric change — the face drops
 * toward the edge and the edge shrinks — and it is pure arithmetic, so it is unit-tested without a
 * device. The colour side (a slightly brighter fill on press) is pinned by {@code UiTokensTest}.
 */
public class UiButtonPressTest {

    @Test
    public void theFaceDepressesByTheEdgeDepthOnPress() {
        float edgeDepth = UiButton.EDGE_DEPTH_DP;
        // At rest the face sits above the edge by the edge depth.
        assertEquals(0f, UiButton.faceOffsetPx(false, edgeDepth), 0.001f);
        // Pressed, the face drops by exactly the edge depth, so the edge vanishes under it.
        assertEquals(edgeDepth, UiButton.faceOffsetPx(true, edgeDepth), 0.001f);
    }

    @Test
    public void theFaceOffsetIsNeverNegative() {
        // A negative edge depth (a bad caller) must not lift the face off the button.
        assertEquals(0f, UiButton.faceOffsetPx(true, -5f), 0.001f);
        assertEquals(0f, UiButton.faceOffsetPx(false, -5f), 0.001f);
    }

    @Test
    public void theEdgeDepthMatchesTheDrawnSlab() {
        // The drawable (bg_dock_play) draws the edge 3dp proud at the bottom; the code constant must
        // agree, or the pressed face lands in the wrong place.
        assertEquals(3f, UiButton.EDGE_DEPTH_DP, 0.001f);
    }

    @Test
    public void theEdgeIsHiddenOnceFullyPressed() {
        assertTrue("at rest the edge shows", UiButton.edgeVisiblePx(false, 3f) > 0f);
        assertEquals("fully pressed, the edge is gone", 0f,
                UiButton.edgeVisiblePx(true, 3f), 0.001f);
    }

    @Test
    public void aPressNeverGrowsTheEdge() {
        // Monotonic: more press is never a taller edge.
        assertTrue(UiButton.edgeVisiblePx(true, 3f) <= UiButton.edgeVisiblePx(false, 3f));
        assertTrue(UiButton.edgeVisiblePx(true, 0f) <= UiButton.edgeVisiblePx(false, 0f));
    }

    @Test
    public void thePressedFillIsBrighterThanTheRestingFill() {
        int resting = 0xFF7FBEF7;
        int pressed = UiButton.pressFill(resting);
        // The pressed face catches the light, so it is lighter, never darker.
        assertTrue("the pressed fill must be lighter than the resting fill",
                luminance(pressed) > luminance(resting));
        // And the alpha is untouched, so a translucent button stays translucent.
        assertEquals(resting >>> 24, pressed >>> 24);
    }

    @Test
    public void aDisabledButtonDoesNotPress() {
        // A disabled control must not give press feedback; the fill is left as-is.
        int resting = 0xFF7FBEF7;
        assertEquals(resting, UiButton.pressFill(resting, false));
    }

    private static double luminance(int argb) {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
}
