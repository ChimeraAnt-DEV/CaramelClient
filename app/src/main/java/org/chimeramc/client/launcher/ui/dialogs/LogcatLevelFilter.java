package org.chimeramc.client.ui.dialogs;

/**
 * The logcat overlay's level filter, as a pure rule (item 209).
 *
 * <p>The overlay offered its levels through a {@link android.widget.ListPopupWindow} anchored to a
 * text view, which is a spinner in all but name: it hides the options until tapped and, being a
 * second window, outlives the host view. The filter is now a row of chips, and this class owns the
 * chip order and the pass rule so the chips and the filter cannot disagree about which lines show.
 *
 * <p>{@link #ALL} is the "no filter" sentinel. A line whose level could not be parsed reads as
 * {@link #UNKNOWN} and is hidden by any specific filter, which is the honest behaviour: a line the
 * parser cannot classify must not be claimed to be an error.
 */
public final class LogcatLevelFilter {

    /** The chip order, most severe last, matching Android's own level ordering. */
    public static final char[] LEVELS = {'V', 'D', 'I', 'W', 'E', 'F'};

    /** No level filter: every line passes. */
    public static final char ALL = 0;

    /** A line whose level could not be determined. */
    public static final char UNKNOWN = 0;

    private LogcatLevelFilter() {
    }

    /** The filter a chip at {@code index} selects; index 0 is the "ALL" chip. */
    public static char levelAt(int index) {
        if (index <= 0 || index > LEVELS.length) return ALL;
        return LEVELS[index - 1];
    }

    /** The chip index that selects {@code level}, or 0 for ALL and anything unrecognised. */
    public static int indexOf(char level) {
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i] == level) return i + 1;
        }
        return 0;
    }

    /** Whether a line at {@code lineLevel} passes {@code filter}. */
    public static boolean passes(char filter, char lineLevel) {
        if (filter == ALL) return true;
        // An unclassified line is not evidence of the level the user asked for.
        if (lineLevel == UNKNOWN) return false;
        return lineLevel == filter;
    }
}
