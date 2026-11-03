package org.chimeramc.client.core.minecraft;

/**
 * The rule for a launch that never reaches a drawn frame.
 *
 * <p>A launch can stall without failing: the extraction completes, the loader reports success, and
 * then the game process never draws. From the loading screen that is indistinguishable from a slow
 * start, so the user waits indefinitely with no way out. This class turns "how long has it been
 * since the last sign of progress" into a decision the screen can act on, and offers the two actions
 * that actually help - open the logs, or retry without mods - rather than a bare "it is taking a
 * while".
 *
 * <p>The escalation is deliberately two-stage. A long-but-progressing launch must never be
 * interrupted (some versions genuinely take minutes to unpack), so only a launch with <em>no</em>
 * progress for {@link #STALLED_MS} is called stalled, and only after {@link #DEAD_MS} with no frame
 * does it offer the retry-without-mods action. Progress resets the clock, so the timeout measures
 * silence, not total time.
 *
 * <p>Pure: the clock is passed in, so every boundary is unit-tested without a device or a game.
 */
public final class LaunchWatchdog {

    /** No progress for this long is a stall worth telling the user about. */
    public static final long STALLED_MS = 90_000L;

    /** No frame and no progress for this long offers the restart-without-mods escape. */
    public static final long DEAD_MS = 180_000L;

    /** What the loading screen should show, if anything. */
    public enum State {
        /** Still progressing; show nothing. */
        OK,
        /** No progress for a while; tell the user it is slow and offer the logs. */
        STALLED,
        /** No frame at all for a long time; offer the restart-without-mods action. */
        DEAD
    }

    private LaunchWatchdog() {
    }

    /**
     * Classifies a launch.
     *
     * @param startedAtMs     when the launch began
     * @param lastProgressMs  the last progress event (may equal {@code startedAtMs})
     * @param firstFrameSeen  whether any frame has been drawn since the launch began
     * @param nowMs           the current time
     */
    public static State evaluate(long startedAtMs, long lastProgressMs, boolean firstFrameSeen,
                                 long nowMs) {
        if (firstFrameSeen) return State.OK;
        long sinceProgress = nowMs - Math.max(startedAtMs, lastProgressMs);
        if (sinceProgress >= DEAD_MS) return State.DEAD;
        if (sinceProgress >= STALLED_MS) return State.STALLED;
        return State.OK;
    }

    /**
     * True when the state warrants a visible message.
     *
     * <p>A convenience so the caller does not test against a hard-coded state, which is how a new
     * state would silently become invisible.
     */
    public static boolean isActionable(State state) {
        return state == State.STALLED || state == State.DEAD;
    }

    /**
     * The next moment {@link #evaluate} could change its answer, so the caller can schedule one
     * wake-up instead of polling every frame.
     *
     * <p>Returns the earlier of the two thresholds that is still in the future; when both are
     * already past (the caller is late) it returns {@code nowMs}, which fires immediately.
     */
    public static long nextCheckAt(long startedAtMs, long lastProgressMs, boolean firstFrameSeen,
                                   long nowMs) {
        if (firstFrameSeen) return Long.MAX_VALUE;
        long anchor = Math.max(startedAtMs, lastProgressMs);
        long stalledAt = anchor + STALLED_MS;
        long deadAt = anchor + DEAD_MS;
        if (nowMs < stalledAt) return stalledAt;
        if (nowMs < deadAt) return deadAt;
        return nowMs;
    }
}
