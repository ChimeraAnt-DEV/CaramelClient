package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The hold-to-scrub drag rules. */
public class ReplayScrubGestureTest {

    @Test
    public void aHorizontalDragPastSlopScrubs() {
        assertTrue(ReplayScrubGesture.isScrub(0f, 0f, 40f, 4f, 8));
    }

    @Test
    public void aVerticalDragIsTheListScrolling() {
        assertFalse(ReplayScrubGesture.isScrub(0f, 0f, 4f, 40f, 8));
    }

    @Test
    public void aDragInsideSlopDoesNotScrub() {
        assertFalse(ReplayScrubGesture.isScrub(0f, 0f, 5f, 0f, 8));
    }

    @Test
    public void aDiagonalDragPrefersItsLargerAxis() {
        assertTrue(ReplayScrubGesture.isScrub(0f, 0f, 30f, 20f, 8));
        assertFalse(ReplayScrubGesture.isScrub(0f, 0f, 20f, 30f, 8));
    }

    @Test
    public void theFractionIsClampedToTheView() {
        assertEquals(0f, ReplayScrubGesture.fraction(-10f, 100), 0.0001f);
        assertEquals(0.5f, ReplayScrubGesture.fraction(50f, 100), 0.0001f);
        assertEquals(1f, ReplayScrubGesture.fraction(250f, 100), 0.0001f);
    }

    @Test
    public void aZeroWidthViewReadsAsTheStart() {
        assertEquals(0f, ReplayScrubGesture.fraction(50f, 0), 0.0001f);
    }
}
