package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The notification stack's bounded-queue rule.
 *
 * <p>Extracted from {@link ModNotificationManager} so the "how many must go" decision is pure and
 * unit-tested without an Activity: the manager owns the windows, this owns the arithmetic. A burst
 * of events must never leave more windows alive than the cap, and the ones dropped must be the
 * oldest, so the notification the player just caused is always the one still on screen.
 */
public final class ModNotificationQueue {

    private ModNotificationQueue() {
    }

    /**
     * How many entries must be dismissed to bring {@code currentSize} down to {@code maxSize}.
     *
     * <p>Never negative: a queue already at or under the cap needs no eviction. A non-positive cap
     * is treated as "evict everything", because a cap of zero is only reachable by a programming
     * error and leaving an unbounded stack is the worse failure.
     */
    public static int overflowCount(int currentSize, int maxSize) {
        if (currentSize <= 0) return 0;
        if (maxSize <= 0) return currentSize;
        return Math.max(0, currentSize - maxSize);
    }

    /**
     * The indices to drop, oldest first, from a stack ordered newest-first.
     *
     * <p>The stack is newest-first (index 0 is the newest), so the oldest entries are at the tail;
     * the dropped indices are therefore the last {@code overflowCount} of the list.
     */
    public static int[] evictionIndices(int currentSize, int maxSize) {
        int count = overflowCount(currentSize, maxSize);
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            out[i] = currentSize - 1 - i;
        }
        return out;
    }
}
