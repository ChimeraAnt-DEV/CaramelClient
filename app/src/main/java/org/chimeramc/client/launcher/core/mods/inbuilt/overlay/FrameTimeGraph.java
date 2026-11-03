package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Maps a {@link FrameTimeHistory} onto screen points for the FPS overlay's graph mode.
 *
 * <p>Pure and Android-free so the mapping — oldest frame on the left, newest on the right, a bigger
 * frame interval drawn higher because a longer frame is worse — is a unit test rather than something
 * a player has to eyeball. The view supplies the scratch arrays and only strokes the result, so the
 * per-frame path allocates nothing.
 *
 * <p>The vertical scale has a floor ({@link #MIN_SCALE_MS}). Scaling to the window's own maximum
 * would draw a perfectly steady 60 fps as a line pinned to the top of the box the instant one frame
 * ran long, which reads as a permanent problem; a floor keeps a steady cadence mid-box and lets a
 * real hitch stand out above it.
 */
public final class FrameTimeGraph {

    /** The smallest interval the graph scales to, so a steady cadence is not pinned to the top. */
    public static final float MIN_SCALE_MS = 33f;

    private FrameTimeGraph() {
    }

    /**
     * Fills {@code xs}/{@code ys} with one point per recorded frame, oldest first, and returns how
     * many points were written.
     *
     * <p>The arrays must hold at least {@link FrameTimeHistory#size()} entries; a shorter array is
     * filled only up to its length so a caller cannot be overrun. An empty history writes nothing
     * and returns 0, so the caller can skip the stroke entirely.
     */
    public static int points(FrameTimeHistory history, float width, float height,
                             float[] xs, float[] ys) {
        if (history == null || history.isEmpty() || xs == null || ys == null) return 0;
        int n = history.size();
        int limit = Math.min(n, Math.min(xs.length, ys.length));
        if (limit <= 0) return 0;
        float scale = scaleMs(history);
        float safeWidth = Math.max(0f, width);
        float safeHeight = Math.max(0f, height);
        for (int i = 0; i < limit; i++) {
            // i = 0 is the oldest retained frame, drawn at the left; the newest is on the right.
            int historyIndex = n - 1 - i;
            float v = history.intervalMs(historyIndex);
            if (v < 0f) v = 0f;
            float norm = v / scale;
            if (norm > 1f) norm = 1f;
            xs[i] = n == 1 ? 0f : safeWidth * i / (float) (n - 1);
            ys[i] = safeHeight - norm * safeHeight;
        }
        return limit;
    }

    /** The interval the graph scales to: the window maximum, but never below {@link #MIN_SCALE_MS}. */
    public static float scaleMs(FrameTimeHistory history) {
        if (history == null) return MIN_SCALE_MS;
        return Math.max(MIN_SCALE_MS, history.maxIntervalMs());
    }
}
