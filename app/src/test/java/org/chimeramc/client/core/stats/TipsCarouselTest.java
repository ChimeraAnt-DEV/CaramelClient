package org.chimeramc.client.core.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the tip carousel's two rules (item 283): the carousel retires after a fixed number of
 * launches, and a rotation never re-shows the tip that is already on screen.
 */
public class TipsCarouselTest {

    @Test
    public void theCarouselIsShownUntilTheLaunchBudgetRunsOut() {
        assertTrue(TipsCarousel.shouldShow(0));
        assertTrue(TipsCarousel.shouldShow(TipsCarousel.MAX_LAUNCHES - 1));
        assertFalse(TipsCarousel.shouldShow(TipsCarousel.MAX_LAUNCHES));
        assertFalse(TipsCarousel.shouldShow(TipsCarousel.MAX_LAUNCHES + 1));
    }

    @Test
    public void anEmptyTipListHasNoIndex() {
        assertEquals(-1, TipsCarousel.nextIndex(0, -1, 0));
        assertEquals(-1, TipsCarousel.nextIndex(-3, 2, 7));
    }

    @Test
    public void aSingleTipAlwaysResolvesToItself() {
        assertEquals(0, TipsCarousel.nextIndex(1, -1, 0));
        assertEquals(0, TipsCarousel.nextIndex(1, 0, 99));
    }

    @Test
    public void theFirstCallStartsAtTheTop() {
        assertEquals(0, TipsCarousel.nextIndex(5, -1, 3));
    }

    @Test
    public void rotationNeverRepeatsTheCurrentTip() {
        int count = 6;
        for (int current = 0; current < count; current++) {
            for (int roll = 0; roll < 50; roll++) {
                int next = TipsCarousel.nextIndex(count, current, roll);
                assertTrue("index in range", next >= 0 && next < count);
                assertNotEquals("must advance", current, next);
            }
        }
    }

    @Test
    public void everyOtherTipIsReachable() {
        int count = 5;
        int current = 2;
        boolean[] seen = new boolean[count];
        for (int roll = 0; roll < count - 1; roll++) {
            seen[TipsCarousel.nextIndex(count, current, roll)] = true;
        }
        for (int i = 0; i < count; i++) {
            if (i == current) continue;
            assertTrue("tip " + i + " should be reachable", seen[i]);
        }
    }

    @Test
    public void aNegativeRollIsTreatedAsZero() {
        assertEquals(TipsCarousel.nextIndex(4, 1, 0), TipsCarousel.nextIndex(4, 1, -8));
    }
}
