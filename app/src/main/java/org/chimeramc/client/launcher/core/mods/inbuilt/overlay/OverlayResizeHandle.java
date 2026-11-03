package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Corner resize handles for the HUD editor.
 *
 * <p>The editor already offered a size slider, but a slider is a number, not a gesture: to make a
 * button match the one beside it the player had to drag the slider and watch. A handle on the
 * selected overlay is the direct manipulation the request asked for — grab the corner, drag, and the
 * button is the size the finger says.
 *
 * <p>Geometry only, in window pixels, so the rules are unit-tested without a device. Two decisions
 * are load-bearing:
 *
 * <ul>
 *   <li><b>The opposite corner is anchored.</b> Dragging the bottom-right grows the widget to the
 *       bottom-right and leaves the top-left exactly where it was, which is what makes a resize feel
 *       like a resize rather than a move. A drag of the top-left corner moves the origin while the
 *       bottom-right stays put.
 *   <li><b>One size drives both axes.</b> An overlay stores a single dp size and derives its width
 *       and height from that size times a per-mod scale, so the aspect is fixed. The handle
 *       therefore takes the larger of the two axes' demands, which grows the widget to fit the
 *       pointer instead of fighting it on the axis the player is not thinking about.
 * </ul>
 */
public final class OverlayResizeHandle {

    /** Which corner the player grabbed. */
    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    /** The size range the slider already offers, so the handle and the slider cannot disagree. */
    public static final int MIN_SIZE_DP = 20;
    public static final int MAX_SIZE_DP = 100;

    /** The touch area of a handle, in dp. Comfortably past the 48dp rule once padded by the edge. */
    public static final int HIT_DP = 32;

    /** A widget's box in window pixels. */
    public static final class Geometry {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public Geometry(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    private OverlayResizeHandle() {
    }

    /**
     * The corner whose handle region contains a touch, or null.
     *
     * <p>Coordinates are relative to the widget's own top-left, which is what a touch listener on the
     * widget receives. Corners are tested in the order the editor prefers to grab them
     * (bottom-right first, since that is the handle the view paints most prominently), and a widget
     * too small to hold two handles at once only offers the bottom-right one.
     */
    public static Corner cornerAt(float localX, float localY, int width, int height, float density) {
        float hit = HIT_DP * density;
        boolean nearLeft = localX <= hit;
        boolean nearRight = localX >= width - hit;
        boolean nearTop = localY <= hit;
        boolean nearBottom = localY >= height - hit;

        // A widget narrower or shorter than two hit areas cannot offer both corners on that axis;
        // the bottom-right is the one the view draws, so it wins.
        boolean canOfferLeft = width >= hit * 2f;
        boolean canOfferTop = height >= hit * 2f;

        if (nearRight && nearBottom) return Corner.BOTTOM_RIGHT;
        if (nearLeft && canOfferLeft && nearBottom) return Corner.BOTTOM_LEFT;
        if (nearRight && nearTop && canOfferTop) return Corner.TOP_RIGHT;
        if (nearLeft && canOfferLeft && nearTop && canOfferTop) return Corner.TOP_LEFT;
        return null;
    }

    /**
     * The geometry after dragging one corner by a delta.
     *
     * @param corner   the grabbed corner
     * @param start    the geometry when the drag began
     * @param dx       horizontal drag, in pixels
     * @param dy       vertical drag, in pixels
     * @param widthScale  the mod's width multiplier over its base size (1.0 for a square widget)
     * @param heightScale the mod's height multiplier over its base size
     * @param density  display density, for the size clamp
     */
    public static Geometry resize(Corner corner, Geometry start, int dx, int dy,
                                  float widthScale, float heightScale, float density) {
        if (corner == null) return start;
        float wScale = widthScale <= 0f ? 1f : widthScale;
        float hScale = heightScale <= 0f ? 1f : heightScale;

        int requestedWidth = start.width + (isRight(corner) ? dx : -dx);
        int requestedHeight = start.height + (isBottom(corner) ? dy : -dy);

        // The overlay has one size and a fixed aspect, so take the larger demand: the widget then
        // reaches the pointer on both axes rather than lagging on the one being ignored.
        int sizePx = Math.max(Math.round(requestedWidth / wScale), Math.round(requestedHeight / hScale));
        sizePx = clampSizePx(sizePx, density);

        int width = Math.max(1, Math.round(sizePx * wScale));
        int height = Math.max(1, Math.round(sizePx * hScale));

        int x = isLeft(corner) ? start.right() - width : start.x;
        int y = isTop(corner) ? start.bottom() - height : start.y;
        return new Geometry(x, y, width, height);
    }

    /** The dp size a pixel size corresponds to, clamped to the slider's range. */
    public static int sizeDpFor(int widthPx, int heightPx, float widthScale, float heightScale,
                                float density) {
        float wScale = widthScale <= 0f ? 1f : widthScale;
        float hScale = heightScale <= 0f ? 1f : heightScale;
        float safeDensity = density <= 0f ? 1f : density;
        int sizePx = Math.max(Math.round(widthPx / wScale), Math.round(heightPx / hScale));
        return Math.round(clampSizePx(sizePx, safeDensity) / safeDensity);
    }

    private static int clampSizePx(int sizePx, float density) {
        float safeDensity = density <= 0f ? 1f : density;
        int min = Math.round(MIN_SIZE_DP * safeDensity);
        int max = Math.round(MAX_SIZE_DP * safeDensity);
        if (sizePx < min) return min;
        if (sizePx > max) return max;
        return sizePx;
    }

    private static boolean isLeft(Corner corner) {
        return corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT;
    }

    private static boolean isRight(Corner corner) {
        return corner == Corner.TOP_RIGHT || corner == Corner.BOTTOM_RIGHT;
    }

    private static boolean isTop(Corner corner) {
        return corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT;
    }

    private static boolean isBottom(Corner corner) {
        return corner == Corner.BOTTOM_LEFT || corner == Corner.BOTTOM_RIGHT;
    }
}
