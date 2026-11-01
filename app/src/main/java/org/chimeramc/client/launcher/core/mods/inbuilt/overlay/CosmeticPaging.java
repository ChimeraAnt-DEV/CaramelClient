package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The page arithmetic for the Closet's card grid (item 205).
 *
 * <p>The catalogue is hundreds of entries per category, and the grid is a {@code ScrollView} of
 * wrapping rows, so building every card up front costs a bitmap render and a view per cosmetic
 * before the panel is even seen. This splits a category into fixed-size pages so only the first page
 * is built on open and the rest are appended as the player scrolls. Pure and Android-free so the
 * boundaries are unit-tested without a device.
 */
public final class CosmeticPaging {

    /** How many cards are built per page; a few rows, so the first paint stays cheap. */
    public static final int PAGE_SIZE = 24;

    private CosmeticPaging() {
    }

    /** The number of pages needed for {@code total} items, at least 1 (an empty grid is one page). */
    public static int pageCount(int total, int pageSize) {
        if (pageSize <= 0) return 1;
        if (total <= 0) return 1;
        return (total + pageSize - 1) / pageSize;
    }

    /** The first index of page {@code page} (0-based), clamped to a valid page. */
    public static int pageStart(int page, int total, int pageSize) {
        if (pageSize <= 0) return 0;
        int clamped = clampPage(page, total, pageSize);
        return clamped * pageSize;
    }

    /**
     * The exclusive end index of page {@code page}, clamped to {@code total}. Together with
     * {@link #pageStart} this is the half-open range the page renders.
     */
    public static int pageEnd(int page, int total, int pageSize) {
        if (pageSize <= 0) return Math.max(0, total);
        int clamped = clampPage(page, total, pageSize);
        return Math.min(total, (clamped + 1) * pageSize);
    }

    /** Whether there is another page after {@code page}. */
    public static boolean hasNextPage(int page, int total, int pageSize) {
        if (pageSize <= 0) return false;
        return page + 1 < pageCount(total, pageSize);
    }

    private static int clampPage(int page, int total, int pageSize) {
        if (page < 0) return 0;
        int last = pageCount(total, pageSize) - 1;
        return Math.min(page, last);
    }
}
