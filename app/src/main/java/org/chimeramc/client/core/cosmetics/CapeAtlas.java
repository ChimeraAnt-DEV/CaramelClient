package org.chimeramc.client.core.cosmetics;

/**
 * Samples a cape's 64x32 sheet at a point on the cloth panel.
 *
 * <p>The preview drew the cape with the procedural weave only, so an authored cape — the whole point
 * of the hand-designed set — never appeared in the preview: the player picked artwork and saw a
 * template. This maps normalised cloth coordinates onto the visible panel of the sheet, so the
 * preview and the in-game texture show the same art. The panel is the box "front" face at
 * {@code x=1..10, y=1..16} (see {@link CapeTexturePainter}), which is the face the player sees once
 * the vanilla cape bone's 180-degree turn is applied.
 *
 * <p>Pure: it takes an int[] ARGB atlas and returns a colour, so the mapping is unit-tested without a
 * device.
 */
public final class CapeAtlas {

    /** The visible panel's left column in the sheet. */
    public static final int PANEL_X = 1;
    /** The visible panel's top row in the sheet. */
    public static final int PANEL_Y = 1;
    /** The panel's width in pixels (the vanilla 10-px cape). */
    public static final int PANEL_WIDTH = 10;
    /** The panel's height in pixels (the vanilla 16-px cape). */
    public static final int PANEL_HEIGHT = 16;

    private CapeAtlas() {
    }

    /**
     * The colour at a normalised cloth point.
     *
     * @param atlas   the sheet as ARGB, row-major, {@code width * height} long
     * @param width   the sheet width (64 for a vanilla cape)
     * @param height  the sheet height (32 for a vanilla cape)
     * @param u       0..1 across the cloth, left to right (clamped)
     * @param v       0..1 down the cloth, top to bottom (clamped)
     * @return the ARGB colour, or 0 (transparent) when the atlas is missing or too small
     */
    public static int sample(int[] atlas, int width, int height, float u, float v) {
        if (atlas == null || width <= 0 || height <= 0) return 0;
        if (atlas.length < width * height) return 0;
        float cu = clamp01(u);
        float cv = clamp01(v);
        int px = PANEL_X + (int) (cu * (PANEL_WIDTH - 1));
        int py = PANEL_Y + (int) (cv * (PANEL_HEIGHT - 1));
        if (px < 0) px = 0;
        if (py < 0) py = 0;
        if (px >= width) px = width - 1;
        if (py >= height) py = height - 1;
        return atlas[py * width + px];
    }

    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        return value > 1f ? 1f : value;
    }
}
