package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * Pins {@link SkinCacheStore}: the round-trip through a slot, the key sanitisation, and the
 * byte-budgeted LRU eviction that keeps a long-lived install bounded.
 */
public class SkinCacheStoreTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static LocalSkinCache.Entry entry(int seed) {
        int[] argb = new int[64 * 64];
        java.util.Arrays.fill(argb, 0xFF000000 | seed);
        return new LocalSkinCache.Entry(64, 64, SkinCaptureValidator.ArmModel.WIDE, "",
                seed, "id" + seed, argb);
    }

    @Test
    public void aWrittenSlotRoundTrips() {
        File root = temp.getRoot();
        assertTrue(SkinCacheStore.put(root, "abc", entry(1), 0L, 100L));
        assertTrue(SkinCacheStore.contains(root, "abc"));
        LocalSkinCache.Entry read = SkinCacheStore.get(root, "abc", 200L);
        assertNotNull(read);
        assertEquals(64, read.width);
        assertEquals(1, read.argb[0] & 0xFF);
    }

    @Test
    public void aMissingSlotReadsAsNull() {
        assertNull(SkinCacheStore.get(temp.getRoot(), "nope", 0L));
        assertFalse(SkinCacheStore.contains(temp.getRoot(), "nope"));
    }

    @Test
    public void aKeyWithPathSeparatorsCannotEscapeTheRoot() {
        File root = temp.getRoot();
        File slot = SkinCacheStore.slotDir(root, "../../evil");
        // The slot must sit directly under the store root, never above it.
        assertEquals(SkinCacheStore.root(root).getAbsolutePath(), slot.getParentFile().getAbsolutePath());
        // The sanitised name is a single segment: it can contain dots, but no separator.
        assertFalse(slot.getName().contains("/"));
        assertFalse(slot.getName().contains(File.separator));
    }

    @Test
    public void aDotOrDotDotKeyBecomesTheDefaultSlot() {
        assertEquals("default", SkinCacheStore.sanitizeKey("."));
        assertEquals("default", SkinCacheStore.sanitizeKey(".."));
        assertEquals("default", SkinCacheStore.sanitizeKey(""));
        assertEquals("default", SkinCacheStore.sanitizeKey(null));
    }

    @Test
    public void anOverlongKeyIsTruncatedToASingleSegment() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) sb.append('a');
        String key = SkinCacheStore.sanitizeKey(sb.toString());
        assertTrue(key.length() <= 96);
    }

    @Test
    public void theBudgetEvictsTheLeastRecentlyUsedSlotAndKeepsTheNewOne() {
        File root = temp.getRoot();
        // Two slots written at t=1 and t=2; each is 64*64*4 + header bytes.
        SkinCacheStore.put(root, "old", entry(1), 0L, 1L);
        SkinCacheStore.put(root, "new", entry(2), 0L, 2L);
        long oneSlot = SkinCacheStore.totalBytes(root) / 2;

        // A budget that fits only one slot must evict the older one.
        SkinCacheStore.enforceBudget(root, oneSlot, 3L, "new");
        assertTrue("the new slot must survive", SkinCacheStore.contains(root, "new"));
        assertFalse("the old slot must be evicted", SkinCacheStore.contains(root, "old"));
    }

    @Test
    public void aZeroBudgetMeansUnboundedAndEvictsNothing() {
        File root = temp.getRoot();
        SkinCacheStore.put(root, "a", entry(1), 0L, 1L);
        SkinCacheStore.put(root, "b", entry(2), 0L, 2L);
        SkinCacheStore.enforceBudget(root, 0L, 3L, null);
        assertTrue(SkinCacheStore.contains(root, "a"));
        assertTrue(SkinCacheStore.contains(root, "b"));
    }

    @Test
    public void aReadingTouchesTheSlotSoItIsNotEvictedNext() {
        File root = temp.getRoot();
        SkinCacheStore.put(root, "a", entry(1), 0L, 1L);
        SkinCacheStore.put(root, "b", entry(2), 0L, 2L);
        // Read "a" last so it becomes the most recently used.
        SkinCacheStore.get(root, "a", 5_000L);
        long oneSlot = SkinCacheStore.totalBytes(root) / 2;
        SkinCacheStore.enforceBudget(root, oneSlot, 6_000L, null);
        assertTrue("the recently read slot must survive", SkinCacheStore.contains(root, "a"));
    }

    @Test
    public void clearRemovesEverySlot() {
        File root = temp.getRoot();
        SkinCacheStore.put(root, "a", entry(1), 0L, 1L);
        SkinCacheStore.put(root, "b", entry(2), 0L, 2L);
        SkinCacheStore.clear(root);
        assertFalse(SkinCacheStore.contains(root, "a"));
        assertFalse(SkinCacheStore.contains(root, "b"));
        assertEquals(0L, SkinCacheStore.totalBytes(root));
    }

    @Test
    public void composeKeyJoinsWithTheSeparator() {
        assertEquals("x__y", SkinCacheStore.composeKey("x", "y"));
        assertEquals("x", SkinCacheStore.composeKey("x"));
        assertEquals("", SkinCacheStore.composeKey());
    }
}
