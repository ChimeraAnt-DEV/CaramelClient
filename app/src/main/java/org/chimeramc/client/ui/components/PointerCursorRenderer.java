package org.chimeramc.client.ui.components;

import android.graphics.Path;

/**
 * Draws the macOS-style pointer without allocating on the draw path (Phase 12).
 *
 * <p>{@link UiDraw#pointerCursor} builds a fresh {@code float[]} and {@code Path} on every call, and
 * the three cursor hosts ({@code MenuCursorOverlay}, {@code RadialMenuView},
 * {@code LauncherCursorOverlay}) draw the pointer inside {@code onDraw}. That is two allocations per
 * frame on the input-to-photon path for a shape whose outline never changes — only its position
 * does. This class owns one {@link Path} and one scratch {@code float[]}, rebuilds the path only
 * when the cursor actually moves, and otherwise reuses it, so a held-still cursor allocates nothing.
 *
 * <p>The geometry itself stays in {@link PointerCursorGeometry}; this class only caches the
 * transform, so the shape is still unit-testable through the pure class.
 */
public final class PointerCursorRenderer {

    /** The pointer's height in dp, matching {@link UiDraw#pointerCursor}. */
    public static final float HEIGHT_DP = 13f;

    private final Path path = new Path();
    private final float[] scratch = new float[PointerCursorGeometry.POINT_COUNT * 2];

    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float lastHeight = Float.NaN;

    /**
     * Rebuilds the outline at {@code (x, y)} for the given height, or leaves it as-is when nothing
     * moved.
     *
     * <p>Rebuilding a {@link Path} means clearing and re-adding every vertex, so skipping it when the
     * position and height are unchanged is the whole point of the cache. A draw that follows the
     * same point every frame (a resting cursor, or a preview) then touches no allocation at all.
     *
     * @param x      hotspot x, in pixels
     * @param y      hotspot y, in pixels
     * @param height the pointer's height in pixels
     */
    public void prepare(float x, float y, float height) {
        if (x == lastX && y == lastY && height == lastHeight) return;
        lastX = x;
        lastY = y;
        lastHeight = height;
        PointerCursorGeometry.points(x, y, height, scratch);
        path.reset();
        path.moveTo(scratch[0], scratch[1]);
        for (int i = 2; i < scratch.length; i += 2) {
            path.lineTo(scratch[i], scratch[i + 1]);
        }
        path.close();
    }

    /** The cached outline, for a caller that wants to draw it with its own paints. */
    public Path path() {
        return path;
    }

    /** Forgets the last position so the next {@link #prepare} rebuilds unconditionally. */
    public void reset() {
        lastX = Float.NaN;
        lastY = Float.NaN;
        lastHeight = Float.NaN;
    }
}
