package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Decides whether a hotbar slot's state needs pushing to the backend (item 196).
 *
 * <p>The slot's state is pushed from several paths — a geometry change, a press, a visibility change
 * and a config change — and those can fire back to back for one visible state. Re-sending the same
 * numbers is wasted work on the input path, so the state is fingerprinted and a repeat is dropped.
 *
 * <p>Pure and Android-free so the rule is unit-tested: two calls that describe the same state push
 * once, and any change to a field pushes again.
 */
public final class HotbarSlotSync {

    private String lastKey;

    /**
     * Records {@code key} and reports whether it differs from the previously recorded one.
     *
     * @return true when the state changed (and should be pushed), false for a repeat.
     */
    public boolean shouldPush(String key) {
        if (key == null || key.equals(lastKey)) {
            return false;
        }
        lastKey = key;
        return true;
    }

    /** Forgets the last state, so the next {@link #shouldPush} always returns true. */
    public void reset() {
        lastKey = null;
    }

    /** True when nothing has been pushed yet or the last push was cleared. */
    public boolean isEmpty() {
        return lastKey == null;
    }

    /** Builds the fingerprint of one slot state; the field order is fixed so it is comparable. */
    public static String key(int x, int y, int width, int height,
                             int surfaceWidth, int surfaceHeight,
                             float opacity, boolean visible, boolean pressed) {
        return x + "," + y + "," + width + "," + height
                + "," + surfaceWidth + "," + surfaceHeight
                + "," + opacity + "," + visible + "," + pressed;
    }
}
