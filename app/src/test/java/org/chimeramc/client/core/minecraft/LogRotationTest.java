package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Pins the log-rotation rule (item 309).
 *
 * <p>The launch log is flushed per line, so it must be capped or a long session can fill storage.
 * The cap has to keep the <em>newest</em> lines: after a crash, the end of the log is what a debug
 * bundle is read for.
 */
public class LogRotationTest {

    @Test
    public void aFileInsideTheBudgetIsNeverTruncated() {
        assertFalse(LogRotation.shouldRotate(0L));
        assertFalse(LogRotation.shouldRotate(1024L));
        assertFalse(LogRotation.shouldRotate(LogRotation.MAX_BYTES));
        assertEquals(0L, LogRotation.truncateFrom(LogRotation.MAX_BYTES));
        assertEquals(0L, LogRotation.truncateFrom(1234L));
    }

    @Test
    public void aFileOverTheBudgetKeepsTheNewestHalf() {
        long size = LogRotation.MAX_BYTES * 3;
        assertTrue(LogRotation.shouldRotate(size));
        long cut = LogRotation.truncateFrom(size);
        // The kept part is half the file, and it is the tail, so the cut is half-way in.
        assertEquals(size / 2, cut);
        assertEquals(size - cut, size / 2);
    }

    @Test
    public void theCutNeverExceedsTheFile() {
        long size = LogRotation.MAX_BYTES + 1;
        long cut = LogRotation.truncateFrom(size);
        assertTrue(cut >= 0L);
        assertTrue(cut < size);
    }

    @Test
    public void aNegativeSizeIsTreatedAsEmpty() {
        assertEquals(0L, LogRotation.truncateFrom(-1L));
    }

    @Test
    public void theFirstLineBoundaryIsFound() {
        byte[] window = "abc\ndef".getBytes(StandardCharsets.UTF_8);
        assertEquals(4, LogRotation.firstLineBoundary(window));
    }

    @Test
    public void aWindowWithNoNewlineReportsNone() {
        byte[] window = "abcdef".getBytes(StandardCharsets.UTF_8);
        assertEquals(-1, LogRotation.firstLineBoundary(window));
        assertEquals(-1, LogRotation.firstLineBoundary(null));
        assertEquals(-1, LogRotation.firstLineBoundary(new byte[0]));
    }

    @Test
    public void aNewlineOnTheFirstByteIsABoundaryOfOne() {
        byte[] window = "\nrest".getBytes(StandardCharsets.UTF_8);
        assertEquals(1, LogRotation.firstLineBoundary(window));
    }

    @Test
    public void theTailOffsetSkipsThePartialLine() {
        long size = LogRotation.MAX_BYTES * 2;
        long rawCut = LogRotation.truncateFrom(size);
        byte[] window = "half-line\nfull-line".getBytes(StandardCharsets.UTF_8);
        long tail = LogRotation.tailOffset(size, window);
        assertEquals(rawCut + "half-line\n".length(), tail);
    }

    @Test
    public void theTailOffsetFallsBackToTheRawCutWithNoNewline() {
        long size = LogRotation.MAX_BYTES * 2;
        long rawCut = LogRotation.truncateFrom(size);
        byte[] window = "no newline here".getBytes(StandardCharsets.UTF_8);
        assertEquals(rawCut, LogRotation.tailOffset(size, window));
    }

    @Test
    public void theTailOffsetNeverExceedsTheFile() {
        long size = LogRotation.MAX_BYTES + 10;
        byte[] window = new byte[4096];
        java.util.Arrays.fill(window, (byte) 'x');
        window[0] = '\n';
        long tail = LogRotation.tailOffset(size, window);
        assertTrue(tail >= 0L);
        assertTrue(tail <= size);
    }

    @Test
    public void aSmallFileKeepsEverything() {
        assertEquals(0L, LogRotation.tailOffset(10L, new byte[0]));
    }
}
