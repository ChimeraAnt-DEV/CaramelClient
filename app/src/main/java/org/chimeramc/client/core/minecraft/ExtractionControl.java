package org.chimeramc.client.core.minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The cancellation signal an extraction checks between files.
 *
 * <p>Extraction of a Bedrock package copies hundreds of megabytes. Until now a launch that the user
 * had given up on - backing out of the loading screen, or the launcher being closed - kept copying
 * to completion, because nothing in the path could be told to stop. The work is not wasted in a
 * harmful way (the manifest is written only after a complete extraction, so an abandoned one is
 * treated as a cache miss next launch and simply redone), but it is work the user asked to stop.
 *
 * <p>The check is deliberately coarse: it is consulted between files and between the extraction's
 * own stages, never in the middle of writing one library. A library is written through a temp file
 * and renamed into place, so stopping between files always leaves the cache either missing the file
 * (a miss next launch) or holding a complete one - never a torn file the hash record would then have
 * to catch.
 *
 * <p>Pure and Android-free, so the flag's semantics are unit-tested with no device.
 */
public final class ExtractionControl {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /** Signals that the extraction should stop at the next safe boundary. */
    public void cancel() {
        cancelled.set(true);
    }

    /** Whether a cancellation has been signalled. */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Throws when a cancellation has been signalled.
     *
     * <p>Throwing rather than returning a boolean is what makes the signal impossible to ignore:
     * every copy loop in the extraction already has a {@code try}/{@code catch} for a failed file,
     * and a returned {@code false} would be dropped into one of those and read as "this file failed,
     * carry on with the next".
     */
    public void throwIfCancelled() throws ExtractionCancelledException {
        if (cancelled.get()) throw new ExtractionCancelledException();
    }

    /** The exception a cancelled extraction stops with. Distinct so a caller can tell it apart. */
    public static final class ExtractionCancelledException extends Exception {
        public ExtractionCancelledException() {
            super("Extraction cancelled");
        }
    }
}
