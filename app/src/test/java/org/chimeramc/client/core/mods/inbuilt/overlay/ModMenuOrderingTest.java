package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pins the drag-to-reorder rule (M9).
 *
 * <p>The important property is not the happy path: it is that a saved arrangement survives an
 * update that adds or removes a module, because that is the case a player actually hits.
 */
public class ModMenuOrderingTest {

    private static final List<String> SHIPPED = Arrays.asList("a", "b", "c", "d");

    @Test
    public void withNoSavedOrderTheShippedOrderIsKept() {
        assertEquals(SHIPPED, ModMenuOrdering.apply(SHIPPED, null));
        assertEquals(SHIPPED, ModMenuOrdering.apply(SHIPPED, Collections.emptyList()));
    }

    @Test
    public void aSavedOrderRearrangesTheSection() {
        assertEquals(Arrays.asList("c", "a", "d", "b"),
                ModMenuOrdering.apply(SHIPPED, Arrays.asList("c", "a", "d", "b")));
    }

    @Test
    public void aModuleTheSavedOrderDoesNotMentionGoesLastInsteadOfJumpingToTheFront() {
        // "c" is new (added by an update). It must not jump ahead of the modules the player
        // deliberately arranged; appending it is the predictable outcome.
        assertEquals(Arrays.asList("b", "a", "d", "c"),
                ModMenuOrdering.apply(SHIPPED, Arrays.asList("b", "a", "d")));
    }

    @Test
    public void aKeyThatNoLongerExistsIsDropped() {
        assertEquals(Arrays.asList("b", "a", "d", "c"),
                ModMenuOrdering.apply(SHIPPED, Arrays.asList("gone", "b", "a", "d")));
    }

    @Test
    public void everyShippedKeyAppearsExactlyOnce() {
        List<String> result = ModMenuOrdering.apply(SHIPPED,
                Arrays.asList("d", "d", "b", "missing"));
        assertEquals(SHIPPED.size(), result.size());
        for (String key : SHIPPED) {
            assertEquals(key + " appears once", 1, Collections.frequency(result, key));
        }
    }

    @Test
    public void aDuplicateKeyInTheSavedOrderDoesNotDuplicateARow() {
        List<String> result = ModMenuOrdering.apply(SHIPPED, Arrays.asList("a", "a", "a", "b"));
        assertEquals(4, result.size());
        assertEquals(1, Collections.frequency(result, "a"));
    }

    @Test
    public void anEmptySectionStaysEmpty() {
        assertTrue(ModMenuOrdering.apply(Collections.emptyList(), Arrays.asList("a")).isEmpty());
        assertTrue(ModMenuOrdering.apply(null, Arrays.asList("a")).isEmpty());
    }

    @Test
    public void aMoveWithinTheSectionApplies() {
        assertEquals(Arrays.asList("b", "c", "a", "d"),
                ModMenuOrdering.move(SHIPPED, 0, 2));
    }

    @Test
    public void anOutOfRangeMoveLeavesTheListUnchanged() {
        assertEquals(SHIPPED, ModMenuOrdering.move(SHIPPED, -1, 2));
        assertEquals(SHIPPED, ModMenuOrdering.move(SHIPPED, 0, 99));
        assertEquals(SHIPPED, ModMenuOrdering.move(SHIPPED, 99, 0));
    }

    @Test
    public void aNullListMovesToEmptyRatherThanThrowing() {
        assertTrue(ModMenuOrdering.move(null, 0, 1).isEmpty());
    }

    @Test
    public void applyingAMoveThenReapplyingItIsStable() {
        List<String> once = ModMenuOrdering.move(SHIPPED, 0, 3);
        assertEquals(once, ModMenuOrdering.apply(once, once));
    }

    @Test
    public void modulesAreAlphabeticalByDisplayName() {
        assertEquals(Arrays.asList("Apple", "Banana", "cherry"),
                ModMenuOrdering.alphabetically(Arrays.asList("cherry", "Banana", "Apple"), s -> s));
    }

    @Test
    public void alphabeticalIgnoresCase() {
        assertEquals(Arrays.asList("alpha", "Beta", "gamma"),
                ModMenuOrdering.alphabetically(Arrays.asList("gamma", "Beta", "alpha"), s -> s));
    }

    @Test
    public void aNameThatIsMissingSortsAfterARealOneRatherThanThrowing() {
        java.util.List<String> result =
                ModMenuOrdering.alphabetically(Arrays.asList("b", "a"), s -> s.equals("a") ? null : s);
        assertEquals(Arrays.asList("b", "a"), result);
    }

    @Test
    public void alphabeticalDropsDuplicatesAndNulls() {
        assertEquals(Arrays.asList("a", "b"),
                ModMenuOrdering.alphabetically(Arrays.asList("b", null, "a", "b"), s -> s));
    }

    @Test
    public void aSavedOrderStillWinsOverAlphabetical() {
        java.util.List<String> alpha = ModMenuOrdering.alphabetically(SHIPPED, s -> s);
        assertEquals(SHIPPED, alpha);
        // A saved order (here the reverse) rearranges the alphabetical base.
        assertEquals(Arrays.asList("d", "c", "b", "a"),
                ModMenuOrdering.apply(alpha, Arrays.asList("d", "c", "b", "a")));
    }
}
