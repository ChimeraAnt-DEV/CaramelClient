package org.chimeramc.client.core.modrinth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the search debounce rule (item 149).
 *
 * <p>Two behaviours matter and neither is visible from the call site: the field must wait for a
 * pause, and a stale response must never replace a newer one.
 */
public class SearchDebounceTest {

    private static final long T0 = 5_000L;

    @Test
    public void aKeystrokeImmediatelyAfterTheLastIsNotDue() {
        assertFalse(SearchDebounce.isDue(T0, T0));
    }

    @Test
    public void justBeforeTheWindowIsNotDue() {
        assertFalse(SearchDebounce.isDue(T0, T0 + SearchDebounce.DEBOUNCE_MS - 1));
    }

    @Test
    public void atTheWindowIsDue() {
        assertTrue(SearchDebounce.isDue(T0, T0 + SearchDebounce.DEBOUNCE_MS));
    }

    @Test
    public void wellPastTheWindowIsDue() {
        assertTrue(SearchDebounce.isDue(T0, T0 + 5_000L));
    }

    @Test
    public void aBackwardsClockIsDueRatherThanStuck() {
        // Uptime wrap: one wasted request beats a search that never runs.
        assertTrue(SearchDebounce.isDue(T0, T0 - 10_000L));
    }

    @Test
    public void theCurrentGenerationIsApplied() {
        assertTrue(SearchDebounce.isCurrent(3, 3));
    }

    @Test
    public void aStaleGenerationIsDropped() {
        assertFalse(SearchDebounce.isCurrent(2, 3));
    }

    @Test
    public void aNewerGenerationIsAlsoDropped() {
        // Cannot happen in practice, but the rule is equality, not ordering.
        assertFalse(SearchDebounce.isCurrent(4, 3));
    }

    @Test
    public void theWindowIsShortEnoughToFeelImmediate() {
        assertEquals(300, SearchDebounce.DEBOUNCE_MS);
    }
}
