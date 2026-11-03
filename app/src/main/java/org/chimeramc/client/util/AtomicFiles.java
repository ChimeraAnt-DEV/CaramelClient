package org.chimeramc.client.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Writes a file so a crash or a kill can never leave a half-written one behind.
 *
 * <p>A plain {@code writeText} truncates the target and then streams the new content into it. If
 * the process dies between those two moments - and a launcher writing its extraction manifest
 * immediately before starting a game is exactly the situation where that happens - the file on
 * disk is the new length with the old bytes, or nothing at all. For the extraction manifest and
 * the settings files the reader compares content for equality, so a torn write is not a degraded
 * state, it is a corrupt one: the next launch treats a perfectly good cache as stale, or worse,
 * trusts a manifest that describes libraries that are not there.
 *
 * <p>The fix is the standard one: write a sibling temp file, flush it to the platter, then rename
 * over the target. A rename within one directory is atomic on every filesystem Android uses, so a
 * reader sees either the whole old file or the whole new one and never a mixture.
 *
 * <p>Pure {@code java.io}, no Android types, so the torn-write behaviour is unit-testable on the
 * JVM.
 */
public final class AtomicFiles {

    private AtomicFiles() {
    }

    /** Suffix for the sibling temp file. Kept in one place so cleanup can find it. */
    public static final String TEMP_SUFFIX = ".atomic.tmp";

    /**
     * Replaces {@code target} with {@code content}, atomically.
     *
     * @return true when the new content is in place; false when the write could not be completed,
     *         in which case the previous file (if any) is left untouched rather than truncated.
     */
    public static boolean writeText(File target, String content) {
        if (target == null) return false;
        try {
            writeOrThrow(target, content);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /**
     * The same write, but reporting the failure instead of returning false.
     *
     * <p>Exists so a caller that has somewhere to put a reason (the diagnostics report, the launch
     * log) does not have to re-implement the temp-file dance to get one.
     */
    public static void writeOrThrow(File target, String content) throws IOException {
        if (target == null) throw new IOException("No target file for an atomic write");
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
        }

        File temp = new File(target.getAbsolutePath() + TEMP_SUFFIX);
        // A leftover from an interrupted write is meaningless; start clean so a stale temp can
        // never be mistaken for this write's output.
        if (temp.exists() && !temp.delete()) {
            throw new IOException("Failed to clear stale temp file: " + temp.getAbsolutePath());
        }

        try (FileOutputStream fos = new FileOutputStream(temp, false);
             Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content != null ? content : "");
            writer.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            temp.delete();
            throw e;
        }

        // renameTo fails on some filesystems when the target exists (and Android's external
        // storage is one of them). Deleting first is not atomic, but the window is one
        // filesystem operation wide instead of one whole-file-write wide, and the temp file
        // already holds the complete new content - so a crash here loses the old file, never
        // leaves a mixture of the two.
        if (!temp.renameTo(target)) {
            if (target.exists() && !target.delete()) {
                temp.delete();
                throw new IOException("Failed to replace: " + target.getAbsolutePath());
            }
            if (!temp.renameTo(target)) {
                temp.delete();
                throw new IOException("Failed to rename temp into place: " + target.getAbsolutePath());
            }
        }
    }
}
