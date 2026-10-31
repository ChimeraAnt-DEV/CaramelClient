package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import java.util.List;

/**
 * The HUD editor's safe area and grid snap (item 210).
 *
 * <p>Two rules live here so the editor cannot disagree with itself about where an overlay may sit:
 *
 * <ul>
 *   <li><b>Safe area.</b> The window is drawn edge-to-edge (the game activity does not reserve the
 *       status bar), so the top-left of the screen can be under a display cutout and the bottom can
 *       be under the gesture bar. An overlay dropped there is half invisible and, on a device with a
 *       notch, the notch covers it. The safe rectangle is the window minus the cutout and the system
 *       bars on each edge, and the editor clamps every drop into it.
 *   <li><b>Grid snap.</b> A free-floating overlay lands on whatever pixel the finger left it, so two
 *       overlays never line up. Snapping the final position to a grid makes a tidy row achievable
 *       with a drag instead of pixel-hunting.
 * </ul>
 *
 * <p>The geometry is pure so it is unit-tested without a device: the Android reads
 * ({@link #forView}) are a thin wrapper that feeds this class numbers.
 */
public final class OverlaySafeArea {

    /** The editor grid, in dp. Coarse enough to feel like a guide, fine enough to stay free. */
    public static final int DEFAULT_GRID_DP = 8;

    /** A rectangle in window pixels; {@code right}/{@code bottom} are exclusive edges. */
    public static final class Rect {
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;

        public Rect(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public int width() {
            return Math.max(0, right - left);
        }

        public int height() {
            return Math.max(0, bottom - top);
        }
    }

    /** A position in window pixels. */
    public static final class Position {
        public final int x;
        public final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * The grid in force while the HUD editor is open, in pixels; 0 outside the editor.
     *
     * <p>It is a static because every draggable overlay reaches the editor through the shared
     * {@link OverlayBounds#clampPosition} path, and threading the editor's state through each of the
     * six overlay classes would be six chances for one to miss the snap. The manager sets it on
     * enter and clears it on exit, and it is only ever non-zero inside the editor, so a gameplay
     * drag is unaffected. {@code volatile} because the set happens on the UI thread and a drag may
     * be read from a posted layout pass.
     */
    private static volatile int editorGridPx = 0;

    /** Sets the editor's snap grid in pixels; 0 disables snapping. */
    public static void setEditorGrid(int gridPx) {
        editorGridPx = Math.max(0, gridPx);
    }

    /** The editor's snap grid in pixels; 0 outside the HUD editor. */
    public static int editorGrid() {
        return editorGridPx;
    }

    private OverlaySafeArea() {
    }

    /**
     * The safe rectangle: the window minus the cutout and the system bars on each edge.
     *
     * <p>The inset on each edge is the larger of the system bar and the cutout, which is the same
     * union rule {@link org.chimeramc.client.ui.navigation.EdgeToEdgeInsets} uses for the launcher
     * chrome, so the two cannot describe different safe areas. The rectangle is never inverted: a
     * pathological pair of insets that would cross collapses to an empty box at the window centre
     * rather than producing a negative width the clamp would then treat as "anywhere".
     */
    public static Rect of(int windowWidth, int windowHeight, int insetLeft, int insetTop,
                          int insetRight, int insetBottom) {
        int w = Math.max(0, windowWidth);
        int h = Math.max(0, windowHeight);
        int left = Math.max(0, Math.min(insetLeft, w));
        int top = Math.max(0, Math.min(insetTop, h));
        int right = Math.max(left, w - Math.max(0, insetRight));
        int bottom = Math.max(top, h - Math.max(0, insetBottom));
        return new Rect(left, top, right, bottom);
    }

    /** Clamps a top-left position so a {@code w}x{@code h} overlay stays inside {@code safe}. */
    public static Position clamp(Rect safe, int x, int y, int w, int h) {
        int width = Math.max(1, w);
        int height = Math.max(1, h);
        // When the overlay is larger than the safe box the best available position is its top-left,
        // which keeps the part the user can see anchored rather than pushing the whole thing off.
        int maxX = Math.max(safe.left, safe.right - width);
        int maxY = Math.max(safe.top, safe.bottom - height);
        return new Position(clampInt(x, safe.left, maxX), clampInt(y, safe.top, maxY));
    }

    /** Rounds a coordinate to the nearest multiple of {@code gridPx}; a non-positive grid is a no-op. */
    public static int snap(int value, int gridPx) {
        if (gridPx <= 0) return value;
        return Math.round((float) value / gridPx) * gridPx;
    }

    /** Snaps then clamps: the order the editor uses, so a snap can never push a drop out of the safe area. */
    public static Position snapAndClamp(Rect safe, int x, int y, int w, int h, int gridPx) {
        return clamp(safe, snap(x, gridPx), snap(y, gridPx), w, h);
    }

    /**
     * The safe rectangle for a live window.
     *
     * <p>Reads the display cutout from the view's root window insets. Below API 28 there is no
     * cutout API at all; this project's {@code minSdk} is 28, so the cutout is always available, but
     * the read is still null-safe because a view that is not attached has no root insets yet.
     */
    public static Rect forView(View rootView, int windowWidth, int windowHeight) {
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        if (rootView != null) {
            WindowInsets insets = rootView.getRootWindowInsets();
            if (insets != null) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    left = cutout.getSafeInsetLeft();
                    top = cutout.getSafeInsetTop();
                    right = cutout.getSafeInsetRight();
                    bottom = cutout.getSafeInsetBottom();
                }
                // The deprecated getters are the ones that exist at minSdk 28;
                // getSystemWindowInsets() is API 30 and would class-init crash a 28 build.
                left = Math.max(left, insets.getSystemWindowInsetLeft());
                top = Math.max(top, insets.getSystemWindowInsetTop());
                right = Math.max(right, insets.getSystemWindowInsetRight());
                bottom = Math.max(bottom, insets.getSystemWindowInsetBottom());
            }
        }
        return of(windowWidth, windowHeight, left, top, right, bottom);
    }

    /** The cutout bounds, for drawing the notch on the safe-area frame. Empty when there is none. */
    public static List<android.graphics.Rect> cutoutBounds(View rootView) {
        if (rootView == null) return java.util.Collections.emptyList();
        WindowInsets insets = rootView.getRootWindowInsets();
        if (insets == null) return java.util.Collections.emptyList();
        DisplayCutout cutout = insets.getDisplayCutout();
        if (cutout == null) return java.util.Collections.emptyList();
        List<android.graphics.Rect> bounds = cutout.getBoundingRects();
        return bounds == null ? java.util.Collections.emptyList() : bounds;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
