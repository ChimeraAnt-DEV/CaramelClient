package org.chimeramc.client.launcher.ui.views;

/**
 * The launch ring's colour as a function of the launch's progress.
 *
 * <p>The ring under the logo rotates continuously and its colour reports the launch state without a
 * second label: a calm navy-to-snow sweep while preparing, a warm amber while extracting, a green
 * settle when the game is ready, a red hold on failure. Keeping the mapping here, as pure integer
 * maths, means the rule is unit-tested and the view only draws.
 */
public final class LaunchRingPalette {

    /** The three launch phases the ring reports. */
    public enum Phase {
        PREPARING,
        WORKING,
        READY,
        FAILED,
    }

    private LaunchRingPalette() {
    }

    private static final int PREPARING_NEAR = 0xFF22304F;   // deep navy
    private static final int PREPARING_FAR = 0xFFB8D8F5;    // snow blue
    private static final int WORKING_NEAR = 0xFF3A2C12;     // deep amber
    private static final int WORKING_FAR = 0xFFFBBF5C;      // amber
    private static final int READY_NEAR = 0xFF14331F;       // deep green
    private static final int READY_FAR = 0xFF4ADE80;        // success
    private static final int FAILED_NEAR = 0xFF3A1717;      // deep red
    private static final int FAILED_FAR = 0xFFFF6B6B;       // danger

    /** The phase for a status, given the current progress (0..100). */
    public static Phase phaseFor(boolean failed, boolean ready, int progress) {
        if (failed) return Phase.FAILED;
        if (ready) return Phase.READY;
        // Any real progress past the first few percent means extraction/copy work is happening;
        // before that the launcher is still resolving the package.
        return progress > 3 ? Phase.WORKING : Phase.PREPARING;
    }

    /** The ring's inner (trailing) colour for a phase. */
    public static int nearColor(Phase phase) {
        switch (phase) {
            case WORKING: return WORKING_NEAR;
            case READY: return READY_NEAR;
            case FAILED: return FAILED_NEAR;
            default: return PREPARING_NEAR;
        }
    }

    /** The ring's outer (leading) colour for a phase. */
    public static int farColor(Phase phase) {
        switch (phase) {
            case WORKING: return WORKING_FAR;
            case READY: return READY_FAR;
            case FAILED: return FAILED_FAR;
            default: return PREPARING_FAR;
        }
    }

    /**
     * The colour at a point on the ring, blended from near to far.
     *
     * @param t 0 at the trailing end, 1 at the leading end; clamped.
     */
    public static int colorAt(Phase phase, float t) {
        float a = Math.max(0f, Math.min(1f, t));
        return mix(nearColor(phase), farColor(phase), a);
    }

    /** The sweep angle in degrees for an elapsed time, so the ring turns at a steady rate. */
    public static float sweepDegrees(long elapsedMs, float periodMs) {
        if (periodMs <= 0f) return 0f;
        return (elapsedMs % (long) periodMs) / periodMs * 360f;
    }

    /** Pure ARGB blend, so this class needs no android.graphics.Color. */
    public static int mix(int a, int b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * k);
        int g = (int) (ag + (bg - ag) * k);
        int bl = (int) (ab + (bb - ab) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
