package org.chimeramc.client.core.content;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pins the resource-pack ordering rules (item 253): a move never drops or duplicates an id, and a
 * saved order never hides a pack it does not mention.
 */
public class ResourcePackOrderTest {

    @Test
    public void moveRelocatesTheEntry() {
        List<String> moved = ResourcePackOrder.move(Arrays.asList("a", "b", "c"), 0, 2);
        assertEquals(Arrays.asList("b", "c", "a"), moved);
    }

    @Test
    public void moveOfTheSameIndexIsANoOp() {
        assertEquals(Arrays.asList("a", "b", "c"),
                ResourcePackOrder.move(Arrays.asList("a", "b", "c"), 1, 1));
    }

    @Test
    public void anOutOfRangeMoveLeavesTheListUnchanged() {
        List<String> original = Arrays.asList("a", "b", "c");
        assertEquals(original, ResourcePackOrder.move(original, -1, 2));
        assertEquals(original, ResourcePackOrder.move(original, 0, 9));
        assertEquals(original, ResourcePackOrder.move(original, 5, 0));
    }

    @Test
    public void applyingASavedOrderFollowsIt() {
        List<String> ordered = ResourcePackOrder.apply(
                Arrays.asList("a", "b", "c"), Arrays.asList("c", "a", "b"));
        assertEquals(Arrays.asList("c", "a", "b"), ordered);
    }

    @Test
    public void anUnmentionedActivePackIsKeptAtTheEnd() {
        // "d" was installed after the order was saved; it must not disappear.
        List<String> ordered = ResourcePackOrder.apply(
                Arrays.asList("a", "b", "d"), Arrays.asList("b", "a"));
        assertEquals(Arrays.asList("b", "a", "d"), ordered);
    }

    @Test
    public void theSavedOrderCannotIntroduceAPackThatIsNotActive() {
        List<String> ordered = ResourcePackOrder.apply(
                Arrays.asList("a"), Arrays.asList("a", "ghost"));
        assertEquals(Collections.singletonList("a"), ordered);
    }

    @Test
    public void duplicateSavedEntriesAreCollapsed() {
        List<String> ordered = ResourcePackOrder.apply(
                Arrays.asList("a", "b"), Arrays.asList("a", "a", "b"));
        assertEquals(Arrays.asList("a", "b"), ordered);
    }

    @Test
    public void uuidsAreCaseAndWhitespaceInsensitive() {
        List<String> ordered = ResourcePackOrder.apply(
                Arrays.asList("AAAA-0001", "bbbb-0002"), Arrays.asList(" bbbb-0002 ", "aaaa-0001"));
        assertEquals(Arrays.asList("bbbb-0002", "aaaa-0001"), ordered);
    }

    @Test
    public void anEmptyActiveSetYieldsAnEmptyOrder() {
        assertTrue(ResourcePackOrder.apply(Collections.emptyList(), Arrays.asList("a")).isEmpty());
        assertTrue(ResourcePackOrder.apply(null, Arrays.asList("a")).isEmpty());
    }
}
