package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Applies the zoom scroll sensitivity and smoothing to a raw scroll delta.
 *
 * <p>Pure and Android-free so the two knobs are unit-tested without a device. Sensitivity is a
 * straight multiplier on the notch; smoothing is a one-pole low-pass that keeps {@code smoothing/100}
 * of the previous output, so a coarse wheel notch eases into the zoom instead of stepping. A
 * smoothing of 0 passes the value through unchanged.
 *
 * <p>The filter is stateful and must be used from one thread (the input path), which is where the
 * overlay calls it. It allocates nothing.
 */
public final class ZoomScrollFilter {

    /** The largest smoothing the filter accepts, so the output can always reach its target. */
    public static final int MAX_SMOOTHING = 90;

    private float previous;

    /**
     * The filtered delta for one scroll notch.
     *
     * @param delta the raw scroll notch (positive scrolls up)
     * @param sensitivityPercent 100 is the default; clamped to a positive value
     * @param smoothingPercent 0 passes through; clamped to 0..{@link #MAX_SMOOTHING}
     */
    public float apply(float delta, int sensitivityPercent, int smoothingPercent) {
        float scaled = delta * (sensitivityPercent / 100f);
        int smoothing = Math.max(0, Math.min(MAX_SMOOTHING, smoothingPercent));
        if (smoothing == 0) {
            previous = scaled;
            return scaled;
        }
        float keep = smoothing / 100f;
        float value = previous * keep + scaled * (1f - keep);
        previous = value;
        return value;
    }

    /** Forgets the previous output, so a new zoom gesture does not inherit the last one's easing. */
    public void reset() {
        previous = 0f;
    }

    /** The last filtered value, for a test or a readout. */
    public float current() {
        return previous;
    }
}
