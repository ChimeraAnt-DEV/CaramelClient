package org.chimeramc.client.core.replay;

/**
 * Caps how often a hold-to-scrub drag decodes a preview frame (item 206).
 *
 * <p>A drag reports a fraction on every touch move - sixty or more a second on a fast swipe - and
 * each one would otherwise queue a {@code MediaMetadataRetriever} decode. Decoding cannot keep up,
 * so the queue grows and the preview lags the finger instead of following it. This admits at most
 * one decode per {@link #MIN_INTERVAL_MS} and skips a request that lands in the same one-second
 * bucket as the last one, because the frame cache keys by whole second and would return the same
 * bitmap anyway.
 *
 * <p>Pure and clock-injected, so the rate rule is unit-tested without a device.
 */
public final class ReplayScrubThrottle {

    /** The shortest gap between two decodes; about eight per second, which reads as continuous. */
    public static final long MIN_INTERVAL_MS = 120L;

    private boolean decoded;
    private long lastDecodeMs;
    private int lastBucket = Integer.MIN_VALUE;

    /** Clears the state at the start of a drag, so the first move always decodes. */
    public void reset() {
        decoded = false;
        lastDecodeMs = 0L;
        lastBucket = Integer.MIN_VALUE;
    }

    /**
     * Whether a decode should be started for this drag position.
     *
     * @param nowMs      the current uptime in milliseconds
     * @param fraction   0..1 through the clip; NaN and out-of-range values are clamped
     * @param durationMs the clip length, used to bucket the position
     */
    public boolean shouldDecode(long nowMs, float fraction, long durationMs) {
        int bucket = bucketOf(fraction, durationMs);
        // Same one-second bucket as the last decode: the frame cache would return the same bitmap.
        if (bucket == lastBucket) return false;
        // Too soon after the last decode: let the previous one land rather than queue another.
        if (decoded && nowMs - lastDecodeMs < MIN_INTERVAL_MS) return false;
        decoded = true;
        lastDecodeMs = nowMs;
        lastBucket = bucket;
        return true;
    }

    /**
     * The whole-second bucket a drag position falls in.
     *
     * <p>Mirrors {@link ReplayThumbnails#frameAt}: the position is clamped to one millisecond
     * inside the clip, so a drag at the very end and the last decoded frame share a bucket rather
     * than asking for a frame the cache never stored.
     */
    public static int bucketOf(float fraction, long durationMs) {
        if (durationMs <= 0L) return 0;
        float clamped = Float.isNaN(fraction) ? 0f : Math.max(0f, Math.min(1f, fraction));
        long positionMs = (long) (clamped * durationMs);
        positionMs = Math.max(0L, Math.min(positionMs, durationMs - 1L));
        return (int) (positionMs / 1000L);
    }
}
