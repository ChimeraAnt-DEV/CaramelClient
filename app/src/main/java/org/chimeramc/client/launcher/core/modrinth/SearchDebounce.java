package org.chimeramc.client.core.modrinth;

/**
 * Decides when a keystroke's search should actually run (item 149).
 *
 * <p>Searching on every character means a request per keystroke: typing "shaders" fires seven
 * requests, six of which are already stale by the time they return, and the list flickers through
 * six intermediate result sets. Waiting for a short pause collapses that to one request and one
 * update, without making the field feel slow.
 *
 * <p>Pure and clock-injected so the timing rule is unit-tested rather than tuned by feel: a clock
 * that jumps backwards is treated as "due", because one wasted request beats a search that never
 * runs.
 */
public final class SearchDebounce {

    /** How long the field must be quiet before the search runs. */
    public static final int DEBOUNCE_MS = 300;

    private SearchDebounce() {
    }

    /**
     * Whether a search should run now.
     *
     * @param lastInputMs when the text last changed, in the caller's clock
     * @param nowMs the current time in the same clock
     */
    public static boolean isDue(long lastInputMs, long nowMs) {
        if (nowMs < lastInputMs) {
            // A backwards clock (uptime wrap) would otherwise suppress the search indefinitely.
            return true;
        }
        return nowMs - lastInputMs >= DEBOUNCE_MS;
    }

    /**
     * Whether a completed response should be applied.
     *
     * <p>Even with the debounce, a slow request can return after the user has typed more, and
     * applying it would replace the newer result with an older one. Comparing the generation the
     * request was issued with to the current one is what makes that impossible.
     */
    public static boolean isCurrent(int issuedGeneration, int currentGeneration) {
        return issuedGeneration == currentGeneration;
    }
}
