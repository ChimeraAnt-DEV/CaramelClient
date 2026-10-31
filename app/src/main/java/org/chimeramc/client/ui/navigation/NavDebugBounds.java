package org.chimeramc.client.ui.navigation;

/**
 * The navigation's real laid-out geometry, formatted for a debug log (item 80).
 *
 * <p>The nav bugs in this file's history - a rail row padded by the rail's own width, a header that
 * centred a pair instead of stacking, a wordmark shown on a bar with no room for it - all share one
 * shape: a view whose declared size and whose actual laid-out bounds disagree, or a visible view
 * that ends up with no width at all. None of them is visible on a build machine and all of them are
 * obvious the moment the real bounds are printed, so this class turns the bounds into one line per
 * entry plus a flag for the two conditions worth noticing.
 *
 * <p>Pure: it takes the numbers a {@code View} reports and returns text. That keeps the formatting
 * unit-tested and out of the activity, and means the toggle can log without a Context.
 */
public final class NavDebugBounds {

    private NavDebugBounds() {
    }

    /** One entry's laid-out geometry, as a {@code View} reports it after layout. */
    public static final class Entry {
        public final String name;
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;
        public final int measuredWidth;
        public final boolean visible;

        public Entry(String name, int left, int top, int right, int bottom,
                     int measuredWidth, boolean visible) {
            this.name = name;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.measuredWidth = measuredWidth;
            this.visible = visible;
        }

        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }
    }

    /**
     * A visible entry laid out with no width, or with a width its own measured size says is too
     * small. Either is the clipping bug: something is being drawn off the view.
     */
    public static boolean looksClipped(Entry entry) {
        if (entry == null || !entry.visible) return false;
        if (entry.width() <= 0) return true;
        // A view narrower than the width it asked for has been squeezed by its parent, which is
        // how a padding-as-width bug and a colliding constraint both present.
        return entry.measuredWidth > 0 && entry.width() < entry.measuredWidth;
    }

    /** One line per entry, with the clipping verdict appended to the ones that need attention. */
    public static String describe(Entry[] entries) {
        if (entries == null || entries.length == 0) return "(no nav entries)";
        StringBuilder out = new StringBuilder();
        for (Entry entry : entries) {
            if (entry == null) continue;
            if (out.length() > 0) out.append('\n');
            out.append(entry.name)
                    .append(entry.visible ? "" : " [gone]")
                    .append(" x=").append(entry.left)
                    .append("..").append(entry.right)
                    .append(" (").append(entry.width()).append("px")
                    .append(" of ").append(entry.measuredWidth).append(")")
                    .append(" y=").append(entry.top).append("..").append(entry.bottom);
            if (looksClipped(entry)) out.append("  <-- CLIPPED");
        }
        return out.toString();
    }

    /** The entries that {@link #looksClipped} flags, so the caller can log a count or a warning. */
    public static int clippedCount(Entry[] entries) {
        if (entries == null) return 0;
        int count = 0;
        for (Entry entry : entries) {
            if (looksClipped(entry)) count++;
        }
        return count;
    }
}
