package org.chimeramc.client.core.mods.fps;

/**
 * The adaptive quality policy, mirroring BerryAPI's {@code PerformanceGovernor} (perf.h).
 *
 * <p>BerryAPI's governor is the engine-side implementation: it watches real frame time and steps a
 * quality budget. It has no JNI bridge, so the client cannot drive it directly; what the client
 * <em>can</em> do is apply the same control rules to the work it owns (its overlay tick rate and its
 * background polling). This class is that rule set, kept pure so it is unit-tested without a device.
 *
 * <p>The four rules, each taken from the governor's design:
 *
 * <ul>
 *   <li><b>Dead band.</b> Frame time is "too slow" above {@code target * (1 + slowMargin)} and "fast
 *       enough to restore" below {@code target * (1 - fastMargin)}. The gap absorbs jitter, so a
 *       single slow frame does not move anything.
 *   <li><b>Hysteresis.</b> Only after {@code framesToReact} consecutive frames outside the band does
 *       quality move, which stops the visible "breathing" a naive governor causes.
 *   <li><b>Thermal hold with dwell.</b> While the platform reports thermal pressure the ceiling is
 *       clamped, and restoration is blocked for {@code thermalDwellFrames} afterwards — a device that
 *       just throttled reports fast frames for a moment (it is doing less work), and restoring
 *       immediately would heat it straight back up.
 *   <li><b>Tail latency.</b> The p99 frame time must also be outside the band before reducing, so a
 *       smooth average cannot hide a spiky tail.
 * </ul>
 */
public final class AdaptiveFramePolicy {

    /** The tunables, mirroring {@code GovernorConfig}. */
    public static final class Config {
        public final int targetFps;
        public final float slowMargin;
        public final float fastMargin;
        public final int framesToReact;
        public final float stepSize;
        public final float thermalCeiling;
        public final float floorQuality;
        public final int thermalDwellFrames;
        public final float fairCeiling;
        public final boolean useTailLatency;

        public Config(int targetFps, float slowMargin, float fastMargin, int framesToReact,
                      float stepSize, float thermalCeiling, float floorQuality,
                      int thermalDwellFrames, float fairCeiling, boolean useTailLatency) {
            this.targetFps = targetFps;
            this.slowMargin = slowMargin;
            this.fastMargin = fastMargin;
            this.framesToReact = framesToReact;
            this.stepSize = stepSize;
            this.thermalCeiling = thermalCeiling;
            this.floorQuality = floorQuality;
            this.thermalDwellFrames = thermalDwellFrames;
            this.fairCeiling = fairCeiling;
            this.useTailLatency = useTailLatency;
        }

        /** The defaults BerryAPI ships. */
        public static Config defaults() {
            return new Config(60, 0.10f, 0.15f, 8, 0.08f, 0.4f, 0.25f, 120, 0.7f, true);
        }
    }

    /** What the governor did on an update, mirroring {@code GovernorAction}. */
    public enum Action {
        NONE,
        REDUCED,
        RESTORED,
        THERMAL_CLAMP,
    }

    /** Platform thermal pressure, mirroring {@code ThermalState}. */
    public enum Thermal {
        NOMINAL,
        FAIR,
        SERIOUS,
        CRITICAL,
        SHUTDOWN,
    }

    private final Config config;
    private float quality = 1f;
    private Thermal thermal = Thermal.NOMINAL;
    private int slowRun;
    private int fastRun;
    private int dwellFrames;
    /** The effective ceiling: 1 normally, the fair cap while warm, the thermal cap while limited. */
    private float effectiveCeiling = 1f;

    public AdaptiveFramePolicy(Config config) {
        this.config = config == null ? Config.defaults() : config;
    }

    public float quality() {
        return quality;
    }

    public Thermal thermal() {
        return thermal;
    }

    /** True while the platform reports real throttling. */
    public boolean thermallyLimited() {
        return thermal == Thermal.SERIOUS || thermal == Thermal.CRITICAL
                || thermal == Thermal.SHUTDOWN;
    }

    /**
     * Records one frame and returns the action taken.
     *
     * @param frameMs this frame's duration; a non-positive value is ignored
     * @param p99Ms the p99 over the window, used only when tail latency is on
     */
    public Action onFrame(float frameMs, float p99Ms) {
        if (frameMs <= 0f) return Action.NONE;

        // A dwell counts down first: no restoration is allowed until it expires.
        if (dwellFrames > 0) dwellFrames--;

        float target = 1000f / config.targetFps;
        float slowThreshold = target * (1f + config.slowMargin);
        float fastThreshold = target * (1f - config.fastMargin);

        boolean slow = frameMs > slowThreshold;
        // The tail must agree before reducing, so a spiky tail behind a smooth average still counts.
        if (slow && config.useTailLatency && p99Ms > 0f && p99Ms <= slowThreshold) {
            slow = false;
        }
        boolean fast = frameMs < fastThreshold;

        slowRun = slow ? slowRun + 1 : 0;
        fastRun = fast ? fastRun + 1 : 0;

        // Restoration is blocked while thermally limited or during the dwell.
        if (fastRun >= config.framesToReact) {
            if (thermallyLimited() || dwellFrames > 0) {
                return Action.NONE;
            }
            if (quality < ceiling()) {
                quality = Math.min(ceiling(), quality + config.stepSize);
                fastRun = 0;
                return Action.RESTORED;
            }
            return Action.NONE;
        }

        if (slowRun >= config.framesToReact) {
            if (quality > config.floorQuality) {
                quality = Math.max(config.floorQuality, quality - config.stepSize);
                slowRun = 0;
                return Action.REDUCED;
            }
        }
        return Action.NONE;
    }

    /**
     * Reports platform thermal pressure.
     *
     * <p>Serious or worse clamps to the thermal ceiling and starts the dwell; Fair holds at the fair
     * ceiling, which is "warm, not throttling" — reacting early keeps the device out of the Serious
     * band rather than waiting for it.
     */
    public Action onThermal(Thermal state) {
        thermal = state == null ? Thermal.NOMINAL : state;
        if (thermallyLimited()) {
            effectiveCeiling = config.thermalCeiling;
            dwellFrames = config.thermalDwellFrames;
            return clampTo(config.thermalCeiling) ? Action.THERMAL_CLAMP : Action.NONE;
        }
        if (thermal == Thermal.FAIR) {
            effectiveCeiling = config.fairCeiling;
            clampTo(config.fairCeiling);
            return Action.NONE;
        }
        effectiveCeiling = 1f;
        return Action.NONE;
    }

    /** The effective ceiling: the thermal or fair cap while it applies, else 1. */
    private float ceiling() {
        return effectiveCeiling;
    }

    private boolean clampTo(float cap) {
        if (quality > cap) {
            quality = cap;
            return true;
        }
        return false;
    }

    /** The launcher's overlay tick interval in ms for the current quality, bounded and honest. */
    public long overlayTickIntervalMs(long minMs, long maxMs) {
        float q = Math.max(0f, Math.min(1f, quality));
        return Math.round(minMs + (maxMs - minMs) * (1f - q));
    }
}
