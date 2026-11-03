package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Gyroscope bias capture.
 *
 * <p>A gyro reports a small non-zero rate while the device is perfectly still, and the drift is
 * device-specific (temperature, mounting). Left uncorrected, that offset is integrated into the
 * look delta every frame and the camera creeps. Calibration records the average rate while the
 * player holds the device still and subtracts it afterwards.
 *
 * <p>Pure and Android-free so the sample/bias rules are unit-tested without a device. The overlay
 * owns the sensor and feeds samples in; this class only decides when enough have arrived and what
 * the bias is.
 */
public final class GyroCalibration {

    /** Samples required before a bias is considered stable. At SENSOR_DELAY_GAME that is ~0.3s. */
    public static final int MIN_SAMPLES = 20;
    /** Samples past which the oldest is dropped, so the average tracks the recent reading. */
    public static final int MAX_SAMPLES = 120;

    private double sumX;
    private double sumY;
    private double sumZ;
    private int samples;
    private boolean active;

    /** Begins (or restarts) a capture. Clears any previous accumulation. */
    public void begin() {
        sumX = 0;
        sumY = 0;
        sumZ = 0;
        samples = 0;
        active = true;
    }

    /** Stops the capture without discarding the samples already collected. */
    public void stop() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Feeds one raw gyro sample. A non-finite component is ignored rather than poisoning the mean
     * (a dropped sensor frame can read NaN on some devices).
     */
    public void add(float x, float y, float z) {
        if (!active) return;
        if (!isFinite(x) || !isFinite(y) || !isFinite(z)) return;
        sumX += x;
        sumY += y;
        sumZ += z;
        samples++;
    }

    public int sampleCount() {
        return samples;
    }

    /** True once enough samples have arrived for {@link #biasX()} and friends to be meaningful. */
    public boolean isReady() {
        return samples >= MIN_SAMPLES;
    }

    /** The mean X rate (rad/s); 0 until any sample has arrived. */
    public float biasX() {
        return samples == 0 ? 0f : (float) (sumX / samples);
    }

    public float biasY() {
        return samples == 0 ? 0f : (float) (sumY / samples);
    }

    public float biasZ() {
        return samples == 0 ? 0f : (float) (sumZ / samples);
    }

    /**
     * Whether the capture has gathered enough samples and can be committed. A caller commits with
     * {@link #stop()} and reads the biases; this is the guard so an early tap does not store a bias
     * from two samples.
     */
    public boolean canCommit() {
        return isReady();
    }

    /** How far through the capture the run is, 0..1, for a progress readout. */
    public float progress() {
        return Math.min(1f, samples / (float) MIN_SAMPLES);
    }

    private static boolean isFinite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }
}
