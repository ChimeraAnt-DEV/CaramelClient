package org.chimeramc.pojavcontrols;

/**
 * Pointer-speed calibration (item 223).
 *
 * <p>The touch look speed is a fixed function of the screen height today, which assumes every
 * player moves their thumb the same distance for the same camera turn. They do not. This class
 * turns one measured drag into a multiplier the look path applies on top of the base sensitivity,
 * and it is pure arithmetic so the rule can be tested without a device.
 *
 * <p>The measurement is the honest part. A calibration target of a known length is drawn, the
 * player drags across it, and the distance the finger actually travelled is compared with the
 * distance the target asked for: a player who only needs to move half as far to cross it is twice
 * as sensitive as the default. The multiplier is clamped, so a mis-measured drag (a tap, a drag
 * that leaves the target) cannot produce a value that makes the game unplayable, and the caller
 * can always reset to {@link #DEFAULT_MULTIPLIER}.
 */
public final class PointerCalibration {

    /** No adjustment; the base sensitivity is used as authored. */
    public static final float DEFAULT_MULTIPLIER = 1f;

    /** The lowest multiplier a calibration may produce, so look never becomes unusably slow. */
    public static final float MIN_MULTIPLIER = 0.4f;

    /** The highest multiplier a calibration may produce, so look never becomes twitchy. */
    public static final float MAX_MULTIPLIER = 2.5f;

    /** A drag shorter than this fraction of the target is treated as a miss, not a measurement. */
    public static final float MIN_MEASURED_FRACTION = 0.25f;

    private PointerCalibration() {
    }

    /**
     * The multiplier one drag implies, or {@link #DEFAULT_MULTIPLIER} when the drag is not a
     * usable measurement.
     *
     * <p>A drag that covers only a quarter of the target is refused rather than scaled up: it is
     * far more likely to be a tap or an accidental swipe than a player whose thumb is four times
     * too short, and accepting it would silently multiply the look speed by the maximum.
     */
    public static float multiplierFor(float targetDistancePx, float measuredDistancePx) {
        if (!isUsable(targetDistancePx, measuredDistancePx)) return DEFAULT_MULTIPLIER;
        return clamp(targetDistancePx / measuredDistancePx);
    }

    /** True when a drag is long enough, on both sides, to be a real measurement. */
    public static boolean isUsable(float targetDistancePx, float measuredDistancePx) {
        if (!(targetDistancePx > 0f) || !(measuredDistancePx > 0f)) return false;
        if (!Float.isFinite(targetDistancePx) || !Float.isFinite(measuredDistancePx)) return false;
        return measuredDistancePx >= targetDistancePx * MIN_MEASURED_FRACTION;
    }

    /** Bounds a multiplier to the playable range. */
    public static float clamp(float multiplier) {
        if (!Float.isFinite(multiplier)) return DEFAULT_MULTIPLIER;
        return Math.max(MIN_MULTIPLIER, Math.min(multiplier, MAX_MULTIPLIER));
    }

    /** Applies a stored multiplier to the base sensitivity, tolerating a nonsense stored value. */
    public static float apply(float baseSensitivity, float multiplier) {
        if (!(baseSensitivity > 0f) || !Float.isFinite(baseSensitivity)) return baseSensitivity;
        return baseSensitivity * clamp(multiplier);
    }
}
