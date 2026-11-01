package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * The allocation-free pointer path (Phase 12).
 *
 * <p>{@link PointerCursorGeometry#points(float, float, float, float[])} and
 * {@link PointerCursorRenderer} exist so the cursor can be drawn every frame without allocating. The
 * one property that matters is that the buffer overload produces exactly the same coordinates as the
 * allocating one — a cache that draws a subtly different shape is worse than the allocation it saves.
 */
public class PointerCursorGeometryBufferTest {

    @Test
    public void theBufferOverloadMatchesTheAllocatingOne() {
        float[] expected = PointerCursorGeometry.points(120f, 40f, 13f);
        float[] buffer = new float[PointerCursorGeometry.POINT_COUNT * 2];
        float[] returned = PointerCursorGeometry.points(120f, 40f, 13f, buffer);

        assertSame("the buffer overload must fill the caller's buffer, not allocate", buffer,
                returned);
        assertArrayEquals(expected, buffer, 1e-6f);
    }

    @Test
    public void aShortBufferIsFilledWithoutOverrunning() {
        float[] tooSmall = new float[3];
        float[] expected = PointerCursorGeometry.points(0f, 0f, 10f);
        PointerCursorGeometry.points(0f, 0f, 10f, tooSmall);

        // Only the first three entries fit; the rest of the buffer is untouched and nothing throws.
        assertEquals(expected[0], tooSmall[0], 1e-6f);
        assertEquals(expected[1], tooSmall[1], 1e-6f);
        assertEquals(expected[2], tooSmall[2], 1e-6f);
    }

    @Test
    public void theBufferOverloadKeepsTheTipAsTheHotspot() {
        float[] buffer = new float[PointerCursorGeometry.POINT_COUNT * 2];
        PointerCursorGeometry.points(77f, 33f, 20f, buffer);
        assertEquals("the tip must land exactly on the hotspot", 77f, buffer[0], 1e-6f);
        assertEquals(33f, buffer[1], 1e-6f);
    }
}
