package org.chimeramc.client.launcher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins the About screen's build identity and native status lines (item 177). */
public class BuildSummaryTest {

    @Test
    public void namesVersionCommitAndType() {
        String line = BuildSummary.describe("BETA3", 12345L, "abc1234", "release", false);
        assertTrue(line.contains("BETA3"));
        assertTrue(line.contains("12345"));
        assertTrue(line.contains("abc1234"));
        assertTrue(line.contains("release"));
        assertFalse(line.contains("beta"));
    }

    @Test
    public void marksTheBetaChannel() {
        String line = BuildSummary.describe("BETA3", 1L, "abc1234", "beta", true);
        assertTrue(line.contains("beta"));
    }

    @Test
    public void anUnknownCommitOrTypeReadsAsUnknownNotNull() {
        String line = BuildSummary.describe("BETA3", 1L, null, "  ", false);
        assertTrue(line.contains("unknown"));
        assertFalse(line.contains("null"));
    }

    @Test
    public void anEmptyVersionNameReadsAsUnknown() {
        String line = BuildSummary.describe("", 1L, "abc", "debug", false);
        assertTrue(line.startsWith("unknown"));
    }

    @Test
    public void aMatchingAbiSaysSo() {
        assertEquals("native: ABI 1 (matches)", BuildSummary.nativeStatus(1, 1));
    }

    @Test
    public void aMissingLibraryIsDistinctFromAMismatch() {
        String missing = BuildSummary.nativeStatus(1, 0);
        String mismatch = BuildSummary.nativeStatus(1, 2);
        assertTrue(missing.contains("unavailable"));
        assertTrue(mismatch.contains("client expects 1"));
        assertFalse(missing.equals(mismatch));
    }
}
