package org.chimeramc.client.core.cosmetics;

/**
 * The wait between cloud-relay polls, backing off while the relay is unreachable (item 244).
 *
 * <p>The cloud poll used a fixed one-second interval, so a device whose relay is down (no network,
 * the public relay rate-limiting us, a wrong topic) hammered the endpoint once a second for the
 * whole session. Backing off keeps a working relay at its one-second freshness while a broken one
 * costs a request a minute instead of sixty.
 *
 * <p>Pure and clock-free: the caller owns the loop and asks for the next delay. Backoff is
 * deliberately capped rather than growing without bound, so a relay that recovers is noticed within
 * {@link #MAX_DELAY_MS} rather than after an ever-longer wait.
 */
public final class CloudPollBackoff {

    /** The delay while polls are succeeding: the freshness bound the fixed interval used to give. */
    public static final long BASE_DELAY_MS = 1_000L;

    /** The longest wait, so a recovered relay is noticed within a minute. */
    public static final long MAX_DELAY_MS = 60_000L;

    /** Consecutive failures below this keep the base delay: a one-off blip is not an outage. */
    private static final int FREE_FAILURES = 1;

    private int consecutiveFailures;

    /** Records a successful poll and resets the delay to {@link #BASE_DELAY_MS}. */
    public void onSuccess() {
        consecutiveFailures = 0;
    }

    /** Returns to the base delay; used when the route restarts, so a stale count is not carried in. */
    public void reset() {
        consecutiveFailures = 0;
    }

    /** Records a failed poll; the next delay grows towards {@link #MAX_DELAY_MS}. */
    public void onFailure() {
        if (consecutiveFailures < Integer.MAX_VALUE) {
            consecutiveFailures++;
        }
    }

    /** The delay to wait before the next poll. */
    public long nextDelayMs() {
        if (consecutiveFailures <= FREE_FAILURES) return BASE_DELAY_MS;
        int steps = consecutiveFailures - FREE_FAILURES;
        // Doubling, capped: 2s, 4s, 8s ... 60s. Shift beyond 63 would be undefined, so it is
        // clamped before the shift rather than trusting the failure count to stay small.
        long delay = steps >= 32 ? MAX_DELAY_MS : BASE_DELAY_MS << Math.min(steps, 6);
        return Math.min(delay, MAX_DELAY_MS);
    }

    /** How many consecutive polls have failed. */
    public int consecutiveFailures() {
        return consecutiveFailures;
    }
}
