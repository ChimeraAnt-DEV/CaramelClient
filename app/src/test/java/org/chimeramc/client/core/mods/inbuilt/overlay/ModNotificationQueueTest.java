package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The notification stack's bounded-queue rule (item 201).
 *
 * <p>Pins that a burst never exceeds the cap and that the entries dropped are the oldest, so the
 * notification the player just triggered is always the one still shown.
 */
public class ModNotificationQueueTest {

    @Test
    public void aQueueUnderTheCapEvictsNothing() {
        assertEquals(0, ModNotificationQueue.overflowCount(0, 3));
        assertEquals(0, ModNotificationQueue.overflowCount(3, 3));
    }

    @Test
    public void aQueueOverTheCapEvictsTheExcess() {
        assertEquals(1, ModNotificationQueue.overflowCount(4, 3));
        assertEquals(5, ModNotificationQueue.overflowCount(8, 3));
    }

    @Test
    public void aNonPositiveCapEvictsEverything() {
        assertEquals(4, ModNotificationQueue.overflowCount(4, 0));
        assertEquals(4, ModNotificationQueue.overflowCount(4, -1));
    }

    @Test
    public void theEvictedIndicesAreTheOldestFirst() {
        // The stack is newest-first, so the tail entries are the oldest and are dropped first.
        assertArrayEquals(new int[] {3}, ModNotificationQueue.evictionIndices(4, 3));
        assertArrayEquals(new int[] {4, 3}, ModNotificationQueue.evictionIndices(5, 3));
    }

    @Test
    public void nothingIsEvictedWhenThereIsNoOverflow() {
        assertArrayEquals(new int[0], ModNotificationQueue.evictionIndices(2, 3));
    }
}
