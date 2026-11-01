package org.chimeramc.client.launcher.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The controller layer-modifier capture, the mapping model lifted out of the settings fragment
 * (item 218). No mocks: this drives the real state machine and a real {@link RemapLayer}.
 */
public class LayerModifierCaptureTest {

    private static final int BUTTON_L1 = 102;
    private static final int BUTTON_R1 = 103;

    @Test
    public void aPressWithNoCaptureRunningPassesThrough() {
        LayerModifierCapture capture = new LayerModifierCapture();
        assertEquals(LayerModifierCapture.Decision.PASSTHROUGH,
                capture.offer(BUTTON_L1, true));
        assertEquals(LayerModifierCapture.Decision.PASSTHROUGH,
                capture.offer(BUTTON_L1, false));
    }

    @Test
    public void aPressCompletesTheCaptureAndBindsThatButton() {
        LayerModifierCapture capture = new LayerModifierCapture();
        RemapLayer layer = new RemapLayer("Combat");
        capture.begin(layer);
        assertTrue(capture.isCapturing());

        assertEquals(LayerModifierCapture.Decision.CAPTURED, capture.offer(BUTTON_L1, true));
        assertEquals(BUTTON_L1, layer.getModifierKeyCode());
        assertFalse("the capture is finished", capture.isCapturing());
        assertNull(capture.pending());
    }

    @Test
    public void aReleaseDuringACaptureIsSwallowedNotBound() {
        LayerModifierCapture capture = new LayerModifierCapture();
        RemapLayer layer = new RemapLayer("Combat");
        capture.begin(layer);

        assertEquals(LayerModifierCapture.Decision.SWALLOWED, capture.offer(BUTTON_L1, false));
        assertEquals("a release must not become the modifier",
                RemapLayer.NO_MODIFIER, layer.getModifierKeyCode());
        assertTrue("the capture is still waiting for a press", capture.isCapturing());
    }

    @Test
    public void theCaptureStillCompletesAfterASwallowedRelease() {
        LayerModifierCapture capture = new LayerModifierCapture();
        RemapLayer layer = new RemapLayer("Combat");
        capture.begin(layer);
        capture.offer(BUTTON_L1, false);
        assertEquals(LayerModifierCapture.Decision.CAPTURED, capture.offer(BUTTON_R1, true));
        assertEquals(BUTTON_R1, layer.getModifierKeyCode());
    }

    @Test
    public void theSecondPressIsTheOneBound() {
        LayerModifierCapture capture = new LayerModifierCapture();
        RemapLayer layer = new RemapLayer("Combat");
        capture.begin(layer);
        capture.offer(BUTTON_L1, true);
        // A later press is no longer a capture, so it must not re-bind.
        assertEquals(LayerModifierCapture.Decision.PASSTHROUGH,
                capture.offer(BUTTON_R1, true));
        assertEquals(BUTTON_L1, layer.getModifierKeyCode());
    }

    @Test
    public void beginningAgainReplacesThePendingLayer() {
        LayerModifierCapture capture = new LayerModifierCapture();
        RemapLayer first = new RemapLayer("First");
        RemapLayer second = new RemapLayer("Second");
        capture.begin(first);
        capture.begin(second);
        assertSame(second, capture.pending());

        capture.offer(BUTTON_L1, true);
        assertEquals("the newest request wins", BUTTON_L1, second.getModifierKeyCode());
        assertEquals(RemapLayer.NO_MODIFIER, first.getModifierKeyCode());
    }

    @Test
    public void cancelAbandonsTheCapture() {
        LayerModifierCapture capture = new LayerModifierCapture();
        RemapLayer layer = new RemapLayer("Combat");
        capture.begin(layer);
        capture.cancel();
        assertFalse(capture.isCapturing());
        assertEquals(LayerModifierCapture.Decision.PASSTHROUGH,
                capture.offer(BUTTON_L1, true));
        assertEquals(RemapLayer.NO_MODIFIER, layer.getModifierKeyCode());
    }

    @Test
    public void anUnbindableKeyCodeClearsTheModifier() {
        // RemapLayer treats a non-positive code as "no modifier", so a stray 0 press must not
        // leave a layer claiming a modifier it cannot be selected by.
        LayerModifierCapture capture = new LayerModifierCapture();
        RemapLayer layer = new RemapLayer("Combat");
        layer.setModifierKeyCode(BUTTON_L1);
        capture.begin(layer);
        assertEquals(LayerModifierCapture.Decision.CAPTURED, capture.offer(0, true));
        assertEquals(RemapLayer.NO_MODIFIER, layer.getModifierKeyCode());
    }
}
