package org.chimeramc.client.core.minecraft;

/**
 * Defaults for the dynamic-render-distance governor.
 *
 * <p>These are the bounds the launcher pushes to the preloader; the hysteresis itself lives in the
 * preloader's {@code OptifineGovernor} so the step rule is unit-tested once.
 */
public final class RenderDistanceDefaults {

    /** Never step below this many chunks. */
    public static final int MIN_CHUNKS = 4;

    /** Start at this many chunks and never step above it. */
    public static final int MAX_CHUNKS = 12;

    /** Step down when the frame rate sits below this for the sustain window. */
    public static final int FPS_THRESHOLD = 45;

    /** Chunks moved per accepted change. */
    public static final int STEP_SIZE = 1;

    /**
     * Minimum ms between two changes. A drop and a restore that land on consecutive
     * sustain windows would otherwise read as flicker; the dwell lets each change settle first.
     */
    public static final long MIN_DWELL_MS = 4000L;

    private RenderDistanceDefaults() {
    }
}
