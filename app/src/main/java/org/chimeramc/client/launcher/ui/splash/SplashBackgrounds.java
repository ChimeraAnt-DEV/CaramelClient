package org.chimeramc.client.launcher.ui.splash;

import java.util.List;
import java.util.Random;

/**
 * Picks the splash screen's background.
 *
 * <p>The splash used to be a 3D scene — a floating island, a pickaxe swinging at an ore block and a
 * block-break animation. That is a lot of per-frame drawing on the cold-start path, for a screen the
 * user looks at for a couple of seconds. The replacement is a random still background per launch plus
 * a small rotating loader, so the splash costs one decode and one rotating drawable.
 *
 * <p>The selection rule is pure, so "a launch picks one of the backgrounds, and two launches in a row
 * are not guaranteed to repeat" is unit-tested without a device.
 */
public final class SplashBackgrounds {

    private SplashBackgrounds() {
    }

    /**
     * Picks an index in {@code [0, count)} for a launch.
     *
     * <p>Uniform over the whole set. {@code previous} is avoided when there is more than one
     * candidate, so a user who relaunches does not see the same picture twice in a row; with a single
     * candidate it is returned regardless, because there is nothing else to show.
     *
     * <p>The rejection loop draws from the full range and redraws on a collision. Drawing from
     * {@code count - 1} and shifting past the previous index looks equivalent but is not: it can never
     * yield the last index, so one background would silently never appear.
     *
     * @param count    the number of candidates; 0 or fewer yields -1 (nothing to show)
     * @param previous the index shown last launch, or -1 when unknown
     * @param random   the source of randomness, injected so the choice is testable
     */
    public static int pick(int count, int previous, Random random) {
        if (count <= 0) return -1;
        if (count == 1) return 0;
        final boolean avoid = previous >= 0 && previous < count;
        if (random == null) {
            // No source of randomness: we cannot make a new choice, so keep the previous one (or
            // start at the first). This path exists only so the caller need not null-check.
            return avoid ? previous : 0;
        }
        // A collision is 1-in-count, so this settles in one or two draws in practice; the bound only
        // exists so a pathological Random cannot spin here.
        for (int attempt = 0; attempt < kMaxDraws; attempt++) {
            final int choice = random.nextInt(count);
            if (!avoid || choice != previous) return choice;
        }
        return avoid ? previous : 0;
    }

    /** The draw bound for the rejection loop; a collision is 1-in-count, so this is never reached. */
    private static final int kMaxDraws = 64;

    /**
     * Picks an entry from a list, returning null for an empty list.
     *
     * @param previous the entry shown last launch, or null when unknown
     */
    public static <T> T pick(List<T> candidates, T previous, Random random) {
        if (candidates == null || candidates.isEmpty()) return null;
        int previousIndex = previous == null ? -1 : candidates.indexOf(previous);
        int index = pick(candidates.size(), previousIndex, random);
        return index < 0 ? null : candidates.get(index);
    }
}
