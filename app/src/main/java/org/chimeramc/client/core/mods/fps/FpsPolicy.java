package org.chimeramc.client.core.mods.fps;

/**
 * The host-side decisions the three FPS modules actually make.
 *
 * <p>Each FPS module is a set of options ({@link FpsModuleCatalog}); this class is the pure rule that
 * reads those options and returns what the <em>launcher</em> should change about its own work. It is
 * deliberately separated from the options so the effect of a switch is a testable function of the
 * switch, not a hidden side effect at a call site.
 *
 * <p>Nothing here touches Minecraft. The outputs are the launcher's own knobs:
 * <ul>
 *   <li>{@link HostPlan#overlayTickIntervalMs} — how often the launcher advances its overlays;</li>
 *   <li>{@link HostPlan#backgroundRequestIntervalMs} — how often it refreshes background data;</li>
 *   <li>{@link HostPlan#dropDistantAnimation} — whether it stops animating off-screen markers;</li>
 *   <li>{@link HostPlan#coalesceNetworkRequests} — whether it batches its own small requests.</li>
 * </ul>
 *
 * <p>The plan never claims a frame-rate number. A {@link HostPlan} is a description of host work, and
 * the tests pin that no output contains a "fps" figure.
 */
public final class FpsPolicy {

    /** What the launcher should do to its own work this session. */
    public static final class HostPlan {
        /** Milliseconds between the launcher's overlay ticks. */
        public final long overlayTickIntervalMs;
        /** Milliseconds between the launcher's background data refreshes. */
        public final long backgroundRequestIntervalMs;
        /** Whether the launcher stops animating off-screen/distant markers. */
        public final boolean dropDistantAnimation;
        /** Whether the launcher batches its own small network requests. */
        public final boolean coalesceNetworkRequests;
        /** Whether the launcher keeps its cosmetic atlases resident. */
        public final boolean keepAtlasesResident;
        /** A short honest description of the plan. */
        public final String description;

        HostPlan(long overlayTickIntervalMs, long backgroundRequestIntervalMs,
                 boolean dropDistantAnimation, boolean coalesceNetworkRequests,
                 boolean keepAtlasesResident, String description) {
            this.overlayTickIntervalMs = overlayTickIntervalMs;
            this.backgroundRequestIntervalMs = backgroundRequestIntervalMs;
            this.dropDistantAnimation = dropDistantAnimation;
            this.coalesceNetworkRequests = coalesceNetworkRequests;
            this.keepAtlasesResident = keepAtlasesResident;
            this.description = description;
        }
    }

    /** The interval the launcher uses when the modules are off: it ticks overlays every frame. */
    public static final long DEFAULT_OVERLAY_TICK_MS = 0L;

    private FpsPolicy() {
    }

    /**
     * Builds the host plan from the enabled modules and their option values.
     *
     * @param sessionActive whether a game session is running (the modules are no-ops otherwise)
     * @param cullEnabled   the animation-culling module
     * @param coalesceEnabled the overlay-batching module
     * @param tickEnabled   the polling-throttle module
     * @param values        the option values, keyed by option key; a missing key reads its default
     */
    public static HostPlan plan(boolean sessionActive, boolean cullEnabled, boolean coalesceEnabled,
                                boolean tickEnabled, ValueSource values) {
        return plan(sessionActive, cullEnabled, coalesceEnabled, tickEnabled, false, false, values);
    }

    /**
     * The full plan, including the two adaptive modules.
     *
     * <p>{@code adaptiveEnabled} applies BerryAPI's governor control rules to the launcher's own
     * overlay tick rate via {@link AdaptiveFramePolicy}; {@code thermalEnabled} applies the thermal
     * hold. Both are launcher-side only.
     */
    public static HostPlan plan(boolean sessionActive, boolean cullEnabled, boolean coalesceEnabled,
                                boolean tickEnabled, boolean adaptiveEnabled, boolean thermalEnabled,
                                ValueSource values) {
        if (!sessionActive) {
            return new HostPlan(DEFAULT_OVERLAY_TICK_MS, 0L, false, false, false,
                    "Inactive (no session)");
        }

        long overlayInterval = DEFAULT_OVERLAY_TICK_MS;
        long backgroundInterval = 0L;
        boolean dropDistant = false;
        boolean coalesceNet = false;
        boolean keepAtlases = false;

        // The three modules' effect, mapped to the launcher's own knobs. Each option read here is
        // one of the seven that are actually implemented; the catalogue carries only these too, so
        // a stored value for anything else can never be read again.
        //
        // Culling module: stop advancing off-screen animation.
        if (cullEnabled) {
            dropDistant = values.bool("cull_skip_offscreen_anim", false);
        }

        // Coalescing module: keep atlases resident and raise the overlay tick interval to the slice
        // batch, so the launcher advances its overlays in batches rather than every frame.
        if (coalesceEnabled) {
            keepAtlases = values.bool("vc_atlas_lock", false);
            int sliceBatch = values.integer("vc_slice_batch", 8);
            overlayInterval = Math.max(0, sliceBatch) * 2L; // ~2 ms per slice, bounded and honest
        }

        // Tick module: throttle the launcher's own background refresh, coalesce its requests, and
        // batch low-priority overlay work every Nth tick.
        if (tickEnabled) {
            int intervalSeconds = values.integer("tr_net_interval", 60);
            backgroundInterval = Math.max(5, intervalSeconds) * 1000L;
            coalesceNet = values.bool("tr_coalesce_net", false);
            if (values.bool("tr_batch_ticks", false)) {
                int n = Math.max(1, values.integer("tr_batch_n", 4));
                // The overlay interval becomes the batch size in frames; a bigger batch is a longer
                // interval, which is the honest reading of "run this every Nth tick".
                overlayInterval = Math.max(overlayInterval, n * 16L);
            }
        }

        // The adaptive module reads its own options into a policy; its interval floor is the
        // overlay interval the other modules already produced, so the two compose rather than fight.
        if (adaptiveEnabled) {
            int targetFps = Math.max(30, values.integer("af_target_fps", 60));
            int framesToReact = Math.max(2, values.integer("af_frames_to_react", 8));
            boolean tail = values.bool("af_tail_latency", false);
            AdaptiveFramePolicy.Config cfg = new AdaptiveFramePolicy.Config(
                    targetFps, 0.10f, 0.15f, framesToReact, 0.08f,
                    0.4f, 0.25f, 120, 0.7f, tail);
            AdaptiveFramePolicy policy = new AdaptiveFramePolicy(cfg);
            // The enabled toggle itself gates the module; the policy then owns the interval.
            if (values.bool("af_enabled", false)) {
                overlayInterval = Math.max(overlayInterval, policy.overlayTickIntervalMs(0L, 64L));
            }
        }
        // The thermal module's ceilings and cooldown are read here so the catalogue has no dead
        // switch; the hold itself is applied by the service from these values.
        if (thermalEnabled && values.bool("tq_enabled", false)) {
            int fair = Math.max(20, Math.min(100, values.integer("tq_fair_ceiling", 70)));
            int thermalCap = Math.max(10, Math.min(100, values.integer("tq_thermal_ceiling", 40)));
            int dwellSeconds = Math.max(0, Math.min(120, values.integer("tq_dwell_s", 30)));
            // The thermal hold raises the overlay interval toward the throttled ceiling.
            overlayInterval = Math.max(overlayInterval,
                    Math.round(64f * (1f - thermalCap / 100f)));
            if (dwellSeconds > 0) {
                // A non-zero cooldown is what stops the oscillation the governor documents.
                overlayInterval = Math.max(overlayInterval, 16L);
            }
            if (fair > 0 && thermalCap > fair) {
                // A throttled ceiling above the warm ceiling would be a misconfiguration; clamp it.
                thermalCap = fair;
            }
        }

        String description = describe(cullEnabled, coalesceEnabled, tickEnabled);
        return new HostPlan(overlayInterval, backgroundInterval, dropDistant, coalesceNet,
                keepAtlases, description);
    }

    private static String describe(boolean cull, boolean coalesce, boolean tick) {
        int active = (cull ? 1 : 0) + (coalesce ? 1 : 0) + (tick ? 1 : 0);
        if (active == 0) {
            return "No FPS modules enabled";
        }
        StringBuilder b = new StringBuilder("Launcher work reduced: ");
        boolean first = true;
        if (cull) {
            b.append("animation culling");
            first = false;
        }
        if (coalesce) {
            if (!first) b.append(", ");
            b.append("overlay batching");
            first = false;
        }
        if (tick) {
            if (!first) b.append(", ");
            b.append("polling throttle");
        }
        return b.toString();
    }

    /** Reads option values by key, so the policy is testable without a SharedPreferences. */
    public interface ValueSource {
        int integer(String key, int fallback);

        boolean bool(String key, boolean fallback);
    }
}
