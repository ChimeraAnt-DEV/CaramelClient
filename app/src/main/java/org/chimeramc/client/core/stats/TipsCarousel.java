package org.chimeramc.client.core.stats;

/**
 * The loading-screen tip carousel's rules.
 *
 * <p>A tip is shown on the launch screen and rotates on a timer, but the carousel retires itself
 * after {@link #MAX_LAUNCHES} launches: a player on their hundredth start does not need to be told
 * where the version dropdown is. Both rules — "is the carousel still worth showing" and "which tip
 * comes next" — live here, Android-free, so the retirement and the no-repeat rotation are
 * unit-tested rather than being two counters spread through an Activity.
 *
 * <p><b>Rotation never repeats the tip that is showing.</b> Picking at random (the original code)
 * re-shows the same tip often enough to read as a bug — the panel looks frozen. {@link #nextIndex}
 * takes the current index and picks a different one, using the caller's roll so the choice stays
 * testable: given a roll it is a pure function, and only the caller owns the randomness.
 */
public final class TipsCarousel {

    /** After this many launches the carousel stops being shown. */
    public static final int MAX_LAUNCHES = 10;

    private TipsCarousel() {
    }

    /**
     * Whether the tip panel should be shown at all, given how many launches have happened.
     *
     * <p>A count of zero (a first start that has not been recorded yet) still shows tips: the
     * counter is incremented on the launch path, so gating on {@code >= 1} would hide the panel on
     * the very first run, which is when a tip is most useful.
     */
    public static boolean shouldShow(int launchCount) {
        return launchCount < MAX_LAUNCHES;
    }

    /**
     * The next tip index, never equal to {@code current} when there is more than one tip.
     *
     * @param count   the number of tips; {@code <= 0} has no valid index and returns -1
     * @param current the index currently shown, or -1 for "none yet"
     * @param roll    a non-negative random value the caller supplies; only its value modulo the
     *                remaining choices is used
     * @return the next index, or -1 when there are no tips
     *
     * <p>The first tip (index 0) is chosen when {@code current} is -1, so a caller that has nothing
     * on screen starts at the top of the list rather than at a random offset.
     */
    public static int nextIndex(int count, int current, int roll) {
        if (count <= 0) return -1;
        if (count == 1) return 0;
        if (current < 0) return 0;
        int safeRoll = roll < 0 ? 0 : roll;
        // Pick uniformly from the count-1 tips that are not `current`, then map that choice onto
        // the full list by skipping over `current`. This keeps the distribution uniform rather
        // than biasing toward the tips after `current`.
        int choice = safeRoll % (count - 1);
        return choice >= current ? choice + 1 : choice;
    }
}
