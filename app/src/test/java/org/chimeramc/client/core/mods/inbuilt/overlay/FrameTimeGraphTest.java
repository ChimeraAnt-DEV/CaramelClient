package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The frame-time graph mapping.
 *
 * <p>Pins the direction (oldest on the left, newest on the right), the vertical scale (a longer
 * frame is higher), the floor that stops a steady cadence being pinned to the top, and the guards
 * for an empty history and a short scratch array.
 */
public class FrameTimeGraphTest {

    private static final long MS = 1_000_000L;
    private static final float W = 100f;
    private static final float H = 40f;

    private static FrameTimeHistory steady(int frames, long stepMs) {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        long t = 0;
        for (int i = 0; i < frames; i++) {
            t += stepMs * MS;
            history.record(t);
        }
        return history;
    }

    @Test
    public void anEmptyHistoryWritesNothing() {
        FrameTimeHistory history = new FrameTimeHistory();
        float[] xs = new float[FrameTimeHistory.CAPACITY];
        float[] ys = new float[FrameTimeHistory.CAPACITY];
        assertEquals(0, FrameTimeGraph.points(history, W, H, xs, ys));
        assertEquals(0, FrameTimeGraph.points(null, W, H, xs, ys));
    }

    @Test
    public void theOldestFrameIsOnTheLeftAndTheNewestOnTheRight() {
        FrameTimeHistory history = steady(5, 16);
        float[] xs = new float[FrameTimeHistory.CAPACITY];
        float[] ys = new float[FrameTimeHistory.CAPACITY];
        int n = FrameTimeGraph.points(history, W, H, xs, ys);
        assertEquals(5, n);
        assertEquals(0f, xs[0], 0.001f);
        assertEquals(W, xs[n - 1], 0.001f);
        // Strictly increasing.
        for (int i = 1; i < n; i++) {
            assertTrue("x must increase: " + xs[i - 1] + " -> " + xs[i], xs[i] > xs[i - 1]);
        }
    }

    @Test
    public void aLongerFrameIsDrawnHigher() {
        FrameTimeHistory history = new FrameTimeHistory();
        history.record(0L);
        history.record(16L * MS);
        history.record(100L * MS); // a hitch
        float[] xs = new float[FrameTimeHistory.CAPACITY];
        float[] ys = new float[FrameTimeHistory.CAPACITY];
        int n = FrameTimeGraph.points(history, W, H, xs, ys);
        assertEquals(2, n);
        // index 0 is the oldest (16 ms), index 1 the newest (100 ms): the hitch is higher, i.e.
        // a smaller y in screen coordinates.
        assertTrue("the hitch must be drawn higher than the steady frame", ys[1] < ys[0]);
    }

    @Test
    public void aSteadyCadenceIsNotPinnedToTheTop() {
        // A steady 60 fps with no hitch scales to the floor, so the line sits mid-box rather than
        // jammed against the top edge where every frame looks like a problem.
        FrameTimeHistory history = steady(20, 16);
        float[] xs = new float[FrameTimeHistory.CAPACITY];
        float[] ys = new float[FrameTimeHistory.CAPACITY];
        int n = FrameTimeGraph.points(history, W, H, xs, ys);
        for (int i = 0; i < n; i++) {
            assertTrue("a steady 16 ms cadence must sit below the top, y=" + ys[i],
                    ys[i] > 0.1f * H);
        }
    }

    @Test
    public void aShortScratchArrayIsNeverOverrun() {
        FrameTimeHistory history = steady(20, 16);
        float[] xs = new float[3];
        float[] ys = new float[3];
        int n = FrameTimeGraph.points(history, W, H, xs, ys);
        assertEquals(3, n);
    }

    @Test
    public void theScaleIsAtLeastTheFloor() {
        FrameTimeHistory history = steady(5, 16);
        assertEquals(FrameTimeGraph.MIN_SCALE_MS, FrameTimeGraph.scaleMs(history), 0.001f);
        assertEquals(FrameTimeGraph.MIN_SCALE_MS, FrameTimeGraph.scaleMs(null), 0.001f);
    }
}
