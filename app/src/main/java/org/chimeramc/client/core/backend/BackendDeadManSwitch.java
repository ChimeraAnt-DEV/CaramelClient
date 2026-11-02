package org.chimeramc.client.core.backend;

/**
 * Releases injected input when the backend stops responding (item 490).
 *
 * <p>A backend that hangs mid-session is worse than one that never loaded: a key it injected stays
 * held down in the game, so the player runs forward forever with no way to stop. The dead-man switch
 * notices a backend that has stopped acknowledging calls for a sustained window and reports that the
 * caller must release everything and tell the user, rather than leaving the input stuck.
 *
 * <p>The window is deliberately generous. A single failed call is normal (a feature the running game
 * build does not expose), and releasing input on one failure would make a working session drop keys
 * at random. What this detects is a backend that has stopped answering <em>entirely</em>.
 *
 * <p>Pure and clock-injected, so the timing rule is unit-tested without sleeping.
 */
public final class BackendDeadManSwitch {

    /** How long the backend may go without a single successful call before input is released. */
    public static final long SILENCE_WINDOW_MS = 5_000L;

    /** How many consecutive failed calls are needed before the silence window even starts counting. */
    public static final int FAILURES_BEFORE_ARMING = 3;

    private int consecutiveFailures;
    /**
     * When the current failure streak began.
     *
     * <p>Sentinel is {@code -1}, not {@code 0}: a monotonic or wall clock can legitimately read 0 at
     * the very start of a process, and using 0 as "unset" made the anchor reset on every failure at
     * the start of a session, so the window never elapsed.
     */
    private long firstFailureMs = -1L;
    private boolean tripped;

    /**
     * Records the outcome of one backend call.
     *
     * @param success whether the call was accepted by the backend.
     * @param nowMs   the wall-clock time of the call.
     * @return true when the switch has just tripped, i.e. this is the moment to release all input
     *     and warn. It trips at most once per silence, so a caller cannot be spammed.
     */
    public boolean record(boolean success, long nowMs) {
        if (success) {
            // Any answered call proves the backend is alive: the streak and the trip both reset, so a
            // recovered backend is trusted immediately rather than after another full window.
            consecutiveFailures = 0;
            firstFailureMs = -1L;
            tripped = false;
            return false;
        }
        consecutiveFailures++;
        if (firstFailureMs < 0L) {
            firstFailureMs = nowMs;
        }
        if (tripped) {
            return false;
        }
        if (consecutiveFailures < FAILURES_BEFORE_ARMING) {
            return false;
        }
        if (nowMs - firstFailureMs < SILENCE_WINDOW_MS) {
            return false;
        }
        tripped = true;
        return true;
    }

    /** Whether the switch is currently tripped. */
    public boolean isTripped() {
        return tripped;
    }

    /** The number of consecutive failures seen since the last successful call. */
    public int consecutiveFailures() {
        return consecutiveFailures;
    }

    /** Clears the switch, so a backend swap starts from a clean state. */
    public void reset() {
        consecutiveFailures = 0;
        firstFailureMs = -1L;
        tripped = false;
    }
}
