package org.chimeramc.client.core.minecraft;

/**
 * The rule for capping an append-only log file (item 309).
 *
 * <p>The launch log is opened in append mode and flushed on every line so it survives a kill. That
 * is the right durability trade, but it means the file grows without bound: a long session with a
 * retry loop can write hundreds of megabytes and nothing stops it until the device runs out of
 * space.
 *
 * <p>The policy is <em>truncate-to-tail</em>, not rename-and-restart. The launcher reads this file
 * back to build a debug bundle, and the useful content after a crash is the <em>end</em> of the log,
 * so keeping the newest lines in the same file is what a report needs. A rotate that renamed the
 * file would leave the reader looking at the oldest lines.
 *
 * <p>Pure and Android-free, so {@code LogRotationTest} pins the arithmetic: the size threshold, the
 * keep fraction, and the byte offset a truncate starts from.
 */
public final class LogRotation {

    /** Rotate once the file passes this. 4 MiB is tens of thousands of lines, far more than a report needs. */
    public static final long MAX_BYTES = 4L * 1024L * 1024L;

    /** Of a file that has just passed the threshold, keep this fraction (the newest part). */
    public static final double KEEP_FRACTION = 0.5d;

    private LogRotation() {
    }

    /** True when a file of {@code sizeBytes} should be rotated before the next append. */
    public static boolean shouldRotate(long sizeBytes) {
        return sizeBytes > MAX_BYTES;
    }

    /**
     * The raw byte offset a truncate should start from, keeping the newest {@link #KEEP_FRACTION}.
     *
     * <p>Returns 0 for a file that is already inside the budget, so a caller that calls this
     * unconditionally never truncates a small file.
     */
    public static long truncateFrom(long sizeBytes) {
        if (sizeBytes <= 0L) return 0L;
        if (!shouldRotate(sizeBytes)) return 0L;
        long keep = (long) (sizeBytes * KEEP_FRACTION);
        return Math.max(0L, sizeBytes - keep);
    }

    /**
     * The index just past the first newline in {@code window}, or -1 when the window holds none.
     *
     * <p>Truncating at a raw offset can land mid-line, and a report whose first line is half a
     * timestamp reads as corruption. The caller reads a bounded window from the cut and uses this to
     * step past the partial line. -1 means "no boundary in the window"; the caller then keeps the
     * raw offset rather than dropping content, because losing the tail is worse than one odd first
     * line.
     */
    public static int firstLineBoundary(byte[] window) {
        if (window == null) return -1;
        for (int i = 0; i < window.length; i++) {
            if (window[i] == '\n') return i + 1;
        }
        return -1;
    }

    /**
     * The final byte offset to keep from, given the raw cut and the window read from it.
     *
     * <p>Combines {@link #truncateFrom} and {@link #firstLineBoundary}: 0 keeps the whole file, and
     * the result is always within {@code [0, sizeBytes]}.
     */
    public static long tailOffset(long sizeBytes, byte[] windowFromCut) {
        long cut = truncateFrom(sizeBytes);
        if (cut <= 0L) return 0L;
        int boundary = firstLineBoundary(windowFromCut);
        if (boundary < 0) return cut;
        long adjusted = cut + boundary;
        return Math.min(adjusted, sizeBytes);
    }
}
