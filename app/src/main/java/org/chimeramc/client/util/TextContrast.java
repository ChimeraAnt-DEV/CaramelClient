package org.chimeramc.client.util;

/**
 * Picks a legible label colour for an arbitrary fill.
 *
 * <p>A button whose background is tinted with the user's accent has no fixed label colour: on the
 * theme's white it disappears into a light accent (the launch screen's "Back button has no letters"
 * report) and on black it disappears into a dark one. This answers the only question that matters —
 * is the fill light or dark — and returns the label that clears the WCAG AA contrast threshold on
 * it, so a caller can set the tint and the label together instead of trusting the theme's default.
 */
public final class TextContrast {

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    /** The WCAG AA contrast threshold for normal text. */
    private static final double AA = 4.5;

    private TextContrast() {
    }

    /**
     * The label colour to draw on {@code fill}: black or white, whichever reads better.
     *
     * <p>Whichever of the two has the higher contrast ratio against the fill wins, so a mid-tone
     * accent still gets the more legible of the pair rather than an arbitrary side of a threshold.
     * Only the fill's RGB is considered; its alpha is ignored, because the label sits over whatever
     * the fill composites onto.
     */
    public static int on(int fill) {
        return contrastRatio(fill, WHITE) >= contrastRatio(fill, BLACK) ? WHITE : BLACK;
    }

    /** The WCAG relative luminance of a colour. */
    public static double luminance(int color) {
        return 0.2126 * channel((color >> 16) & 0xFF)
                + 0.7152 * channel((color >> 8) & 0xFF)
                + 0.0722 * channel(color & 0xFF);
    }

    /** The WCAG contrast ratio between two opaque colours (1.0 to 21.0). */
    public static double contrastRatio(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double channel(int value) {
        double v = value / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
