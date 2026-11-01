package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/** Item 221: the HUD editor's corner resize handles. */
public class OverlayResizeHandleTest {

    private static final float DENSITY = 2f;

    @Test
    public void aPressOnEachCornerPicksThatCorner() {
        int size = 200;
        assertEquals(OverlayResizeHandle.Corner.TOP_LEFT,
                OverlayResizeHandle.cornerAt(2f, 2f, size, size, DENSITY));
        assertEquals(OverlayResizeHandle.Corner.TOP_RIGHT,
                OverlayResizeHandle.cornerAt(size - 2f, 2f, size, size, DENSITY));
        assertEquals(OverlayResizeHandle.Corner.BOTTOM_LEFT,
                OverlayResizeHandle.cornerAt(2f, size - 2f, size, size, DENSITY));
        assertEquals(OverlayResizeHandle.Corner.BOTTOM_RIGHT,
                OverlayResizeHandle.cornerAt(size - 2f, size - 2f, size, size, DENSITY));
    }

    @Test
    public void theCentreOfAWidgetIsNotAHandle() {
        assertNull(OverlayResizeHandle.cornerAt(100f, 100f, 200, 200, DENSITY));
    }

    @Test
    public void aWidgetTooSmallToHoldTwoHandlesOnAnAxisOnlyOffersTheBottomRight() {
        float hit = OverlayResizeHandle.HIT_DP * DENSITY;
        int size = (int) (hit * 1.5f);
        assertEquals(OverlayResizeHandle.Corner.BOTTOM_RIGHT,
                OverlayResizeHandle.cornerAt(size - 1f, size - 1f, size, size, DENSITY));
        // The left/top corners are suppressed because both handles cannot fit on that axis.
        assertNull(OverlayResizeHandle.cornerAt(0f, 0f, size, size, DENSITY));
    }

    @Test
    public void draggingTheBottomRightGrowsDownAndRightAndAnchorsTheTopLeft() {
        OverlayResizeHandle.Geometry start =
                new OverlayResizeHandle.Geometry(40, 50, 100, 100);
        OverlayResizeHandle.Geometry next = OverlayResizeHandle.resize(
                OverlayResizeHandle.Corner.BOTTOM_RIGHT, start, 40, 40, 1f, 1f, DENSITY);
        assertEquals(40, next.x);
        assertEquals(50, next.y);
        assertEquals(140, next.width);
        assertEquals(140, next.height);
    }

    @Test
    public void draggingTheTopLeftMovesTheOriginAndAnchorsTheBottomRight() {
        OverlayResizeHandle.Geometry start =
                new OverlayResizeHandle.Geometry(40, 50, 100, 100);
        OverlayResizeHandle.Geometry next = OverlayResizeHandle.resize(
                OverlayResizeHandle.Corner.TOP_LEFT, start, -20, -20, 1f, 1f, DENSITY);
        assertEquals(140, next.right());
        assertEquals(150, next.bottom());
        assertEquals(120, next.width);
        assertEquals(120, next.height);
    }

    @Test
    public void theSizeTakesTheLargerAxisDemandSoTheWidgetReachesThePointer() {
        OverlayResizeHandle.Geometry start =
                new OverlayResizeHandle.Geometry(0, 0, 100, 100);
        OverlayResizeHandle.Geometry next = OverlayResizeHandle.resize(
                OverlayResizeHandle.Corner.BOTTOM_RIGHT, start, 60, 0, 1f, 1f, DENSITY);
        assertEquals(160, next.width);
        assertEquals(160, next.height);
    }

    @Test
    public void aNonSquareWidgetKeepsItsAspectRatio() {
        OverlayResizeHandle.Geometry start =
                new OverlayResizeHandle.Geometry(0, 0, 100, 50);
        OverlayResizeHandle.Geometry next = OverlayResizeHandle.resize(
                OverlayResizeHandle.Corner.BOTTOM_RIGHT, start, 100, 0, 2f, 1f, DENSITY);
        assertEquals(2f, next.width / (float) next.height, 0.05f);
    }

    @Test
    public void theSizeIsClampedToTheSliderRange() {
        OverlayResizeHandle.Geometry start =
                new OverlayResizeHandle.Geometry(0, 0, 100, 100);
        OverlayResizeHandle.Geometry tooSmall = OverlayResizeHandle.resize(
                OverlayResizeHandle.Corner.BOTTOM_RIGHT, start, -500, -500, 1f, 1f, DENSITY);
        assertEquals(OverlayResizeHandle.MIN_SIZE_DP * DENSITY, tooSmall.width, 0.01f);

        OverlayResizeHandle.Geometry tooBig = OverlayResizeHandle.resize(
                OverlayResizeHandle.Corner.BOTTOM_RIGHT, start, 5000, 5000, 1f, 1f, DENSITY);
        assertEquals(OverlayResizeHandle.MAX_SIZE_DP * DENSITY, tooBig.width, 0.01f);
    }

    @Test
    public void theCommittedDpSizeMatchesWhatTheDragSettledOn() {
        int sizeDp = OverlayResizeHandle.sizeDpFor(112, 112, 1f, 1f, DENSITY);
        assertEquals(56, sizeDp);
    }

    @Test
    public void theCommittedSizeIsClampedTheSameWayTheSliderIs() {
        assertEquals(OverlayResizeHandle.MAX_SIZE_DP,
                OverlayResizeHandle.sizeDpFor(100000, 100000, 1f, 1f, DENSITY));
        assertEquals(OverlayResizeHandle.MIN_SIZE_DP,
                OverlayResizeHandle.sizeDpFor(1, 1, 1f, 1f, DENSITY));
    }

    @Test
    public void aNullCornerLeavesTheGeometryUntouched() {
        OverlayResizeHandle.Geometry start =
                new OverlayResizeHandle.Geometry(10, 20, 30, 40);
        assertSame(start, OverlayResizeHandle.resize(null, start, 50, 50, 1f, 1f, DENSITY));
    }
}
