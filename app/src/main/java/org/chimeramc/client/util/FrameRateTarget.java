package org.chimeramc.client.util;

/**
 * Maps a frame-rate cap to the arguments {@code Surface.setFrameRate} wants.
 *
 * <p>The launcher already picks a display mode for a cap through
 * {@link DisplayModePreference#selectModeAtOrBelow}, and that remains the path on API 28-29. From
 * API 30 there is a second, better mechanism: {@code Surface.setFrameRate} asks the system to
 * present the surface at a rate directly, without switching the panel's mode at all — so a cap
 * cannot change the resolution, and it works on a panel that exposes no matching mode.
 *
 * <p>The mapping is the part worth testing on its own: the compatibility flag and the change
 * strategy are what decide whether the system treats the request as a fixed content rate (correct
 * for a game) and whether it is allowed to switch at a non-seamless moment. This class holds that
 * decision as plain numbers so it can be asserted without a {@code Surface}.
 */
public final class FrameRateTarget {

    /** {@code Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE} (API 30): the content runs at a fixed rate. */
    public static final int COMPATIBILITY_FIXED_SOURCE = 1;

    /**
     * {@code Surface.CHANGE_FRAME_RATE_ALWAYS} (API 30): apply the rate even at a moment that may
     * produce a visible seam. A frame cap the player asked for must take effect; the alternative
     * ("only if seamless") can silently decline the request on a panel that never finds a seamless
     * moment.
     */
    public static final int CHANGE_FRAME_RATE_ALWAYS = 1;

    private FrameRateTarget() {
    }

    /**
     * The rate to request for a cap, or {@code 0} to clear a previously requested rate.
     *
     * <p>a cap of zero ({@code CAP_UNLIMITED}) becomes {@code 0}, which is the documented value that
     * resets the surface to its default. A negative cap is treated as unlimited rather than passed
     * through as a nonsense rate.
     */
    public static float rateForCap(int capHz) {
        return capHz <= 0 ? 0f : (float) capHz;
    }

    /** True when the cap asks the surface to be left at its own rate. */
    public static boolean clearsRate(int capHz) {
        return rateForCap(capHz) == 0f;
    }
}
