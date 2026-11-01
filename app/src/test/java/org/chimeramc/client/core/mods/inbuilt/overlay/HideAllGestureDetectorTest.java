package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The three-finger hide-all tap (item 213). */
public class HideAllGestureDetectorTest {

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int MOVE = 2;
    private static final int CANCEL = 3;
    private static final int POINTER_DOWN = 5;
    private static final int POINTER_UP = 6;

    private static boolean threeFingerTap(HideAllGestureDetector detector, long duration) {
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        detector.onEvent(POINTER_DOWN, 2, 520f, 500f, 10L);
        detector.onEvent(POINTER_DOWN, 3, 540f, 500f, 20L);
        return detector.onEvent(UP, 3, 540f, 500f, duration);
    }

    @Test
    public void aQuickThreeFingerTapFires() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        assertTrue(threeFingerTap(detector, 120L));
    }

    @Test
    public void aSingleFingerTapDoesNotFire() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        assertFalse(detector.onEvent(UP, 1, 500f, 500f, 100L));
    }

    @Test
    public void twoFingersDoNotFire() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        detector.onEvent(POINTER_DOWN, 2, 520f, 500f, 10L);
        assertFalse(detector.onEvent(UP, 2, 520f, 500f, 100L));
    }

    @Test
    public void aFourthFingerCancelsTheGesture() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        detector.onEvent(POINTER_DOWN, 2, 520f, 500f, 10L);
        detector.onEvent(POINTER_DOWN, 3, 540f, 500f, 20L);
        detector.onEvent(POINTER_DOWN, 4, 560f, 500f, 30L);
        assertFalse(detector.onEvent(UP, 4, 560f, 500f, 100L));
    }

    @Test
    public void aThreeFingerSwipeDoesNotFire() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        detector.onEvent(POINTER_DOWN, 2, 520f, 500f, 10L);
        detector.onEvent(POINTER_DOWN, 3, 540f, 500f, 20L);
        detector.onEvent(MOVE, 3, 700f, 500f, 60L);
        assertFalse(detector.onEvent(UP, 3, 700f, 500f, 100L));
    }

    @Test
    public void aSlowThreeFingerHoldDoesNotFire() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        assertFalse(threeFingerTap(detector, HideAllGestureDetector.MAX_DURATION_MS + 1));
    }

    @Test
    public void theBoundaryDurationStillFires() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        assertTrue(threeFingerTap(detector, HideAllGestureDetector.MAX_DURATION_MS));
    }

    @Test
    public void aCancelDropsTheGesture() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        detector.onEvent(POINTER_DOWN, 2, 520f, 500f, 10L);
        detector.onEvent(POINTER_DOWN, 3, 540f, 500f, 20L);
        detector.onEvent(CANCEL, 3, 540f, 500f, 30L);
        assertFalse(detector.onEvent(UP, 3, 540f, 500f, 60L));
    }

    @Test
    public void aResetDropsAnInFlightGesture() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        detector.onEvent(POINTER_DOWN, 2, 520f, 500f, 10L);
        detector.onEvent(POINTER_DOWN, 3, 540f, 500f, 20L);
        assertTrue(detector.isTracking());
        detector.reset();
        assertFalse(detector.isTracking());
        assertFalse(detector.onEvent(UP, 3, 540f, 500f, 60L));
    }

    @Test
    public void aPointerUpFromThreeFingersDoesNotCancel() {
        // One of the three fingers lifting first is the normal way a tap ends; only a pointer count
        // above three means a fourth finger was involved.
        HideAllGestureDetector detector = new HideAllGestureDetector();
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        detector.onEvent(POINTER_DOWN, 2, 520f, 500f, 10L);
        detector.onEvent(POINTER_DOWN, 3, 540f, 500f, 20L);
        assertFalse(detector.onEvent(POINTER_UP, 3, 540f, 500f, 60L));
        assertTrue(detector.onEvent(UP, 1, 500f, 500f, 90L));
    }

    @Test
    public void aSecondGestureAfterAFiredOneStillFires() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        assertTrue(threeFingerTap(detector, 120L));
        assertTrue(threeFingerTap(detector, 120L));
    }

    @Test
    public void aMoveBeforeTheThirdFingerLandsDoesNotCancel() {
        HideAllGestureDetector detector = new HideAllGestureDetector();
        detector.onEvent(DOWN, 1, 500f, 500f, 0L);
        detector.onEvent(MOVE, 1, 900f, 500f, 5L);
        detector.onEvent(POINTER_DOWN, 2, 520f, 500f, 10L);
        detector.onEvent(POINTER_DOWN, 3, 540f, 500f, 20L);
        assertTrue(detector.onEvent(UP, 3, 540f, 500f, 120L));
    }
}
