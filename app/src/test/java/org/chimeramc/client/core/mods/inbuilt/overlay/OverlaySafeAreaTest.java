package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The HUD editor's safe-area and grid-snap geometry. */
public class OverlaySafeAreaTest {

    @Test
    public void withNoInsetsTheSafeAreaIsTheWholeWindow() {
        OverlaySafeArea.Rect safe = OverlaySafeArea.of(1920, 1080, 0, 0, 0, 0);
        assertEquals(0, safe.left);
        assertEquals(0, safe.top);
        assertEquals(1920, safe.right);
        assertEquals(1080, safe.bottom);
        assertEquals(1920, safe.width());
        assertEquals(1080, safe.height());
    }

    @Test
    public void aCutoutShrinksTheSafeArea() {
        // A landscape notch on the left and the gesture bar on the bottom.
        OverlaySafeArea.Rect safe = OverlaySafeArea.of(1920, 1080, 80, 0, 0, 48);
        assertEquals(80, safe.left);
        assertEquals(0, safe.top);
        assertEquals(1920, safe.right);
        assertEquals(1032, safe.bottom);
    }

    @Test
    public void insetsLargerThanTheWindowDoNotInvertTheBox() {
        OverlaySafeArea.Rect safe = OverlaySafeArea.of(200, 100, 500, 500, 500, 500);
        assertTrue("a crossed pair must not produce a negative width", safe.width() >= 0);
        assertTrue("a crossed pair must not produce a negative height", safe.height() >= 0);
        assertEquals(200, safe.left);
        assertEquals(100, safe.top);
    }

    @Test
    public void clampKeepsTheOverlayInsideTheSafeArea() {
        OverlaySafeArea.Rect safe = OverlaySafeArea.of(1000, 500, 100, 0, 0, 50);
        // Dropped past the left cutout.
        OverlaySafeArea.Position left = OverlaySafeArea.clamp(safe, -400, 0, 80, 40);
        assertEquals(100, left.x);
        // Dropped past the right and bottom edges.
        OverlaySafeArea.Position bottomRight = OverlaySafeArea.clamp(safe, 5000, 5000, 80, 40);
        assertEquals(920, bottomRight.x);
        assertEquals(410, bottomRight.y);
    }

    @Test
    public void anOverlayLargerThanTheSafeAreaAnchorsToItsTopLeft() {
        OverlaySafeArea.Rect safe = OverlaySafeArea.of(100, 100, 10, 10, 10, 10);
        OverlaySafeArea.Position p = OverlaySafeArea.clamp(safe, 0, 0, 500, 500);
        assertEquals(safe.left, p.x);
        assertEquals(safe.top, p.y);
    }

    @Test
    public void snapRoundsToTheNearestGridLine() {
        assertEquals(0, OverlaySafeArea.snap(3, 8));
        assertEquals(8, OverlaySafeArea.snap(5, 8));
        assertEquals(16, OverlaySafeArea.snap(15, 8));
        assertEquals(24, OverlaySafeArea.snap(24, 8));
    }

    @Test
    public void aNonPositiveGridLeavesTheValueAlone() {
        assertEquals(37, OverlaySafeArea.snap(37, 0));
        assertEquals(37, OverlaySafeArea.snap(37, -8));
    }

    @Test
    public void snapThenClampNeverPushesAnOverlayOutOfTheSafeArea() {
        OverlaySafeArea.Rect safe = OverlaySafeArea.of(100, 100, 10, 10, 10, 10);
        // Snapping 89 up to 96 would be outside the safe right edge (90) without the clamp.
        OverlaySafeArea.Position p = OverlaySafeArea.snapAndClamp(safe, 89, 89, 8, 8, 8);
        assertTrue(p.x >= safe.left && p.x + 8 <= safe.right);
        assertTrue(p.y >= safe.top && p.y + 8 <= safe.bottom);
    }

    @Test
    public void theEditorGridIsZeroUntilItIsSetAndIsNeverNegative() {
        OverlaySafeArea.setEditorGrid(0);
        assertEquals(0, OverlaySafeArea.editorGrid());
        OverlaySafeArea.setEditorGrid(-16);
        assertEquals(0, OverlaySafeArea.editorGrid());
        OverlaySafeArea.setEditorGrid(24);
        assertEquals(24, OverlaySafeArea.editorGrid());
        OverlaySafeArea.setEditorGrid(0);
    }
}
