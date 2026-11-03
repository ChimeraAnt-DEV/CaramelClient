package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/**
 * An extraction must be refused before it starts when the disk cannot hold it.
 *
 * <p>Discovering the disk is full half-way through leaves a partial cache that the presence check
 * then retries on every launch, so the check has to happen before the first byte is written.
 */
public class ExtractionSpaceBudgetTest {

    @Test
    public void theNeedIsThePackageSizePlusHeadroom() {
        long one = 500L * 1024L * 1024L;
        assertEquals(one + ExtractionSpaceBudget.HEADROOM_BYTES,
                ExtractionSpaceBudget.neededBytes(Collections.singletonList(one)));
    }

    @Test
    public void everySplitPackageCounts() {
        long a = 300L * 1024L * 1024L;
        long b = 200L * 1024L * 1024L;
        assertEquals(a + b + ExtractionSpaceBudget.HEADROOM_BYTES,
                ExtractionSpaceBudget.neededBytes(Arrays.asList(a, b)));
    }

    @Test
    public void aMissingOrZeroSizePackageContributesNothing() {
        long a = 100L * 1024L * 1024L;
        assertEquals(a + ExtractionSpaceBudget.HEADROOM_BYTES,
                ExtractionSpaceBudget.neededBytes(Arrays.asList(a, null, 0L, -5L)));
    }

    @Test
    public void noPackagesAtAllStillNeedsTheHeadroom() {
        assertEquals(ExtractionSpaceBudget.HEADROOM_BYTES,
                ExtractionSpaceBudget.neededBytes(null));
    }

    @Test
    public void enoughSpacePasses() {
        assertEquals(ExtractionSpaceBudget.Verdict.OK,
                ExtractionSpaceBudget.check(8L * 1024L * 1024L * 1024L, 1L * 1024L * 1024L * 1024L));
    }

    @Test
    public void exactlyEnoughPasses() {
        long needed = 1L * 1024L * 1024L * 1024L;
        assertEquals(ExtractionSpaceBudget.Verdict.OK,
                ExtractionSpaceBudget.check(needed, needed));
    }

    @Test
    public void oneByteShortFails() {
        long needed = 1L * 1024L * 1024L * 1024L;
        assertEquals(ExtractionSpaceBudget.Verdict.INSUFFICIENT,
                ExtractionSpaceBudget.check(needed - 1L, needed));
    }

    /**
     * An unreadable free-space figure must never block a launch. The extraction's own write
     * failures already report themselves, and refusing here would break a launch that would work.
     */
    @Test
    public void anUnknownReadingNeverBlocks() {
        assertEquals(ExtractionSpaceBudget.Verdict.OK,
                ExtractionSpaceBudget.check(-1L, 5L * 1024L * 1024L * 1024L));
        assertEquals(ExtractionSpaceBudget.Verdict.OK,
                ExtractionSpaceBudget.check(ExtractionSpaceBudget.HEADROOM_BYTES * -1L, 1L));
    }

    @Test
    public void sizesAreFormattedForPeople() {
        assertEquals("unknown", ExtractionSpaceBudget.formatBytes(-1L));
        assertEquals("2.0 GB", ExtractionSpaceBudget.formatBytes(2L * 1024L * 1024L * 1024L));
        assertEquals("500 MB", ExtractionSpaceBudget.formatBytes(500L * 1024L * 1024L));
        assertEquals("64 KB", ExtractionSpaceBudget.formatBytes(64L * 1024L));
        assertTrue(ExtractionSpaceBudget.formatBytes(1024L).endsWith("KB"));
    }
}
