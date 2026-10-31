package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The Closet grid's page arithmetic (item 205).
 *
 * <p>Pins the boundaries a paged grid gets wrong: an exact multiple of the page size, a partial last
 * page, an empty category (one empty page, not zero), a negative page, and a page past the end.
 */
public class CosmeticPagingTest {

    @Test
    public void thePageCountRoundsUp() {
        assertEquals(1, CosmeticPaging.pageCount(0, 24));
        assertEquals(1, CosmeticPaging.pageCount(1, 24));
        assertEquals(1, CosmeticPaging.pageCount(24, 24));
        assertEquals(2, CosmeticPaging.pageCount(25, 24));
        assertEquals(3, CosmeticPaging.pageCount(60, 24));
    }

    @Test
    public void anEmptyCategoryStillHasOnePage() {
        assertEquals(1, CosmeticPaging.pageCount(0, CosmeticPaging.PAGE_SIZE));
        assertEquals(0, CosmeticPaging.pageStart(0, 0, CosmeticPaging.PAGE_SIZE));
        assertEquals(0, CosmeticPaging.pageEnd(0, 0, CosmeticPaging.PAGE_SIZE));
    }

    @Test
    public void aPageRangeIsHalfOpenAndClampedToTheTotal() {
        // Page 0 of 60 items: 0..24.
        assertEquals(0, CosmeticPaging.pageStart(0, 60, 24));
        assertEquals(24, CosmeticPaging.pageEnd(0, 60, 24));
        // The last page is clamped to the total, not a full page past it.
        assertEquals(48, CosmeticPaging.pageStart(2, 60, 24));
        assertEquals(60, CosmeticPaging.pageEnd(2, 60, 24));
    }

    @Test
    public void aPartialLastPageEndsAtTheTotal() {
        // 50 items at 24 per page is three pages: 0..24, 24..48, 48..50.
        assertEquals(3, CosmeticPaging.pageCount(50, 24));
        assertEquals(48, CosmeticPaging.pageStart(2, 50, 24));
        assertEquals(50, CosmeticPaging.pageEnd(2, 50, 24));
    }

    @Test
    public void aNegativeOrPastTheEndPageClamps() {
        assertEquals(0, CosmeticPaging.pageStart(-5, 60, 24));
        // Page 99 of 3 pages clamps to the last page, so a scroll callback cannot over-run.
        assertEquals(48, CosmeticPaging.pageStart(99, 60, 24));
        assertEquals(60, CosmeticPaging.pageEnd(99, 60, 24));
    }

    @Test
    public void hasNextPageStopsAtTheLastPage() {
        assertTrue(CosmeticPaging.hasNextPage(0, 60, 24));
        assertTrue(CosmeticPaging.hasNextPage(1, 60, 24));
        assertFalse(CosmeticPaging.hasNextPage(2, 60, 24));
        assertFalse(CosmeticPaging.hasNextPage(0, 0, 24));
    }

    @Test
    public void aNonPositivePageSizeIsSafe() {
        assertEquals(1, CosmeticPaging.pageCount(10, 0));
        assertEquals(0, CosmeticPaging.pageStart(0, 10, 0));
        assertEquals(10, CosmeticPaging.pageEnd(0, 10, 0));
        assertFalse(CosmeticPaging.hasNextPage(0, 10, 0));
    }
}
