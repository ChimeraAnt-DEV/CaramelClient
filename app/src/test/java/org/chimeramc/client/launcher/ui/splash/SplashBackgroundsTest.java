package org.chimeramc.client.launcher.ui.splash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The splash background selection rule.
 *
 * <p>The point of these checks is the two properties a user notices: a launch shows one of the
 * backgrounds, and relaunching does not show the same one twice in a row.
 */
public class SplashBackgroundsTest {

    @Test
    public void noCandidatesYieldsNothing() {
        assertEquals(-1, SplashBackgrounds.pick(0, -1, new Random(1)));
        assertEquals(-1, SplashBackgrounds.pick(-3, -1, new Random(1)));
    }

    @Test
    public void aSingleCandidateIsAlwaysChosen() {
        for (int i = 0; i < 20; i++) {
            assertEquals(0, SplashBackgrounds.pick(1, -1, new Random(i)));
        }
        // Even when it was the previous one: there is nothing else to show.
        assertEquals(0, SplashBackgrounds.pick(1, 0, new Random(1)));
    }

    @Test
    public void theChoiceIsInRange() {
        Random random = new Random(42);
        for (int i = 0; i < 2000; i++) {
            int index = SplashBackgrounds.pick(5, -1, random);
            assertTrue("index " + index + " out of range", index >= 0 && index < 5);
        }
    }

    @Test
    public void theSameBackgroundIsNotShownTwiceInARow() {
        // Every possible previous index, over many draws: the pick must never equal it.
        for (int previous = 0; previous < 5; previous++) {
            Random random = new Random(previous * 31L + 7);
            for (int i = 0; i < 2000; i++) {
                int index = SplashBackgrounds.pick(5, previous, random);
                assertNotEquals("repeated the previous background", previous, index);
            }
        }
    }

    @Test
    public void everyBackgroundCanBeChosen() {
        // A rule that always returned 0 (or skipped one entry) would pass the range check but be
        // obviously wrong to a user. Over enough draws each index must appear.
        boolean[] seen = new boolean[4];
        Random random = new Random(99);
        for (int i = 0; i < 4000; i++) {
            seen[SplashBackgrounds.pick(4, -1, random)] = true;
        }
        for (int i = 0; i < 4; i++) {
            assertTrue("index " + i + " was never chosen", seen[i]);
        }
    }

    @Test
    public void anUnknownPreviousDoesNotRestrictTheChoice() {
        Random random = new Random(5);
        boolean[] seen = new boolean[3];
        for (int i = 0; i < 3000; i++) {
            seen[SplashBackgrounds.pick(3, -1, random)] = true;
        }
        assertTrue(seen[0] && seen[1] && seen[2]);
    }

    @Test
    public void aNullRandomFallsBackToThePreviousOrTheFirst() {
        assertEquals(2, SplashBackgrounds.pick(5, 2, null));
        assertEquals(0, SplashBackgrounds.pick(5, -1, null));
        assertEquals(0, SplashBackgrounds.pick(5, 9, null));
    }

    @Test
    public void listPickReturnsNullForAnEmptyList() {
        assertNull(SplashBackgrounds.pick(Collections.<String>emptyList(), null, new Random(1)));
        assertNull(SplashBackgrounds.pick(null, null, new Random(1)));
    }

    @Test
    public void listPickReturnsAnEntryAndAvoidsThePrevious() {
        List<String> candidates = Arrays.asList("a", "b", "c");
        Random random = new Random(11);
        for (int i = 0; i < 500; i++) {
            String picked = SplashBackgrounds.pick(candidates, "b", random);
            assertTrue(candidates.contains(picked));
            assertNotEquals("b", picked);
        }
    }

    @Test
    public void listPickHandlesAPreviousThatIsNoLongerInTheList() {
        // The background set can shrink between releases; a stale remembered value must not throw.
        List<String> candidates = Arrays.asList("a", "b");
        String picked = SplashBackgrounds.pick(candidates, "gone", new Random(3));
        assertTrue(candidates.contains(picked));
    }
}
