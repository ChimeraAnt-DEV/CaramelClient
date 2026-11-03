package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Stores an overlay's position as a fraction of the surface rather than in pixels.
 *
 * <p>A pixel position is only meaningful for the surface it was measured on. Rotate the device, or
 * change the resolution scale, and a widget saved at "x = 1200" on a 1920-wide surface lands in a
 * different place - or off-screen - on a 1280-wide one. Storing the fraction keeps a widget where
 * the user put it relative to the screen, which is what "the same place" means across a rotation.
 *
 * <p>The maths is pure and clamped to 0..1, so a corrupt preference (a negative, a value above one,
 * or a NaN) can never place a widget outside the surface.
 */
public final class OverlayPositionStore {

    private OverlayPositionStore() {
    }

    /** A pixel offset on a surface of {@code surfacePx}, as a 0..1 fraction. */
    public static float fraction(int pixel, int surfacePx) {
        if (surfacePx <= 0) return 0f;
        return clamp01((float) pixel / (float) surfacePx);
    }

    /** The pixel offset a 0..1 fraction names on a surface of {@code surfacePx}. */
    public static int pixel(float fraction, int surfacePx) {
        if (surfacePx <= 0 || Float.isNaN(fraction)) return 0;
        return Math.round(clamp01(fraction) * surfacePx);
    }

    /** True when the fraction is finite and inside 0..1, i.e. safe to use as a position. */
    public static boolean isValid(float fraction) {
        return !Float.isNaN(fraction) && !Float.isInfinite(fraction)
                && fraction >= 0f && fraction <= 1f;
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) return 0f;
        return Math.max(0f, Math.min(1f, value));
    }
}
