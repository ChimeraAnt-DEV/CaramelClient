package org.chimeramc.client.core.minecraft;

import java.util.Collection;

/**
 * How much free storage an extraction needs, and whether there is enough (item 304).
 *
 * <p>Extraction writes every `.so` a package ships for one ABI. Those files are stored compressed
 * inside the APK, so the extracted set is at least as large as the bytes it came from and can be
 * larger. The APK's own size is therefore a sound floor for the estimate, and the headroom covers
 * the manifest, the hash record and the filesystem's own slack.
 *
 * <p>The alternative - discovering the disk is full part-way through - is the worst outcome, because
 * it leaves a half-extracted cache that the presence check then treats as a cache miss and retries
 * on every launch. Failing before the first byte is written is what keeps that from happening.
 *
 * <p>Android-free and pure, so the arithmetic and the verdict are unit-tested with no device.
 */
public final class ExtractionSpaceBudget {

    private ExtractionSpaceBudget() {
    }

    /** Headroom on top of the package size: the manifest, the hash record, and filesystem slack. */
    public static final long HEADROOM_BYTES = 64L * 1024L * 1024L;

    /** The result of the check. */
    public enum Verdict {
        /** Enough room, or the free-space reading is unknown so nothing can be proven. */
        OK,
        /** The volume is demonstrably too small for this extraction. */
        INSUFFICIENT,
    }

    /**
     * The bytes an extraction of these packages needs.
     *
     * <p>Summed over every package because a split install's libraries can live in any of them, and
     * a zero-length or missing entry contributes nothing rather than a negative.
     */
    public static long neededBytes(Collection<Long> apkSizes) {
        long total = HEADROOM_BYTES;
        if (apkSizes != null) {
            for (Long size : apkSizes) {
                if (size != null && size > 0L) total += size;
            }
        }
        return total;
    }

    /**
     * Whether {@code freeBytes} can hold {@code neededBytes}.
     *
     * <p>An unknown reading ({@code freeBytes < 0}) is {@link Verdict#OK}: refusing a launch because
     * a {@code StatFs} call failed would block a launch that would have worked, and the extraction's
     * own write failures already report themselves.
     */
    public static Verdict check(long freeBytes, long neededBytes) {
        if (freeBytes < 0L) return Verdict.OK;
        if (neededBytes <= 0L) return Verdict.OK;
        return freeBytes >= neededBytes ? Verdict.OK : Verdict.INSUFFICIENT;
    }

    /** A short human-readable size, for the message the user sees. */
    public static String formatBytes(long bytes) {
        if (bytes < 0L) return "unknown";
        if (bytes >= 1024L * 1024L * 1024L) {
            return String.format(java.util.Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
        if (bytes >= 1024L * 1024L) {
            return String.format(java.util.Locale.US, "%.0f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0);
    }
}
