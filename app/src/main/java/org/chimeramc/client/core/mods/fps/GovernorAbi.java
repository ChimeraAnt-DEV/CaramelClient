package org.chimeramc.client.core.mods.fps;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The client's mirror of BerryAPI's performance-governor C ABI.
 *
 * <p>BerryAPI's {@code PerformanceGovernor} ({@code perf.h}) is engine-side and reached, like the
 * replay service, through a C struct of function pointers. The client cannot include that header, so
 * this mirrors the two enums it must agree on — the quality knobs and the thermal states — and parses
 * the governor's status JSON. Pure, so a drift fails a unit test rather than a device.
 *
 * <p>The knob order is the ABI: {@code QualityKnob} is a C enum, so the client's index for
 * {@code Particles} must be the library's. Reordering one side without the other would move a value
 * onto the wrong knob, which is why the order is asserted here rather than assumed.
 */
public final class GovernorAbi {

    private GovernorAbi() {
    }

    /** The {@code BERRY_GOVERNOR_ABI_VERSION} this mirror targets. */
    public static final int ABI_VERSION = 1;

    /**
     * The quality knobs, in the library's enum order.
     *
     * <p>Cheapest to give up first: particles, then entity distance, animation distance, entity
     * culling, lighting, and finally render distance. The order is the contract.
     */
    public enum Knob {
        PARTICLES,
        ENTITY_DISTANCE,
        ANIMATION_DISTANCE,
        ENTITY_CULLING,
        LIGHTING_UPDATES,
        RENDER_DISTANCE,
    }

    /** The thermal states, mirroring {@code ThermalState}. */
    public enum Thermal {
        NOMINAL,
        FAIR,
        SERIOUS,
        CRITICAL,
        SHUTDOWN,
    }

    /** The knob at an index, or null when the index is out of range. */
    public static Knob knobAt(int index) {
        Knob[] all = Knob.values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    /** The thermal state at an index, or null when out of range. */
    public static Thermal thermalAt(int index) {
        Thermal[] all = Thermal.values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    /** Whether the platform is actually throttling (Serious or worse). */
    public static boolean isThrottling(Thermal state) {
        return state == Thermal.SERIOUS || state == Thermal.CRITICAL
                || state == Thermal.SHUTDOWN;
    }

    /** Whether a library's reported governor ABI is usable by this mirror. */
    public static boolean isCompatible(int reportedAbiVersion) {
        return reportedAbiVersion == ABI_VERSION;
    }

    /** A parsed governor status: the quality per knob and the thermal state. */
    public static final class Status {
        /** Quality per knob, in {@link Knob} order, each 0..1. */
        public final float[] quality;
        public final Thermal thermal;
        /** The effective settings, or null when not reported. */
        public final Settings settings;
        public final String raw;

        Status(float[] quality, Thermal thermal, Settings settings, String raw) {
            this.quality = quality;
            this.thermal = thermal;
            this.settings = settings;
            this.raw = raw;
        }

        /** The mean quality across the knobs, 0..1. */
        public float meanQuality() {
            if (quality.length == 0) return 1f;
            float sum = 0f;
            for (float q : quality) sum += q;
            return sum / quality.length;
        }

        /** Whether the platform is throttling. */
        public boolean throttling() {
            return isThrottling(thermal);
        }
    }

    /** The concrete engine settings the governor drives, mirroring {@code RenderSettings}. */
    public static final class Settings {
        public final int renderDistanceChunks;
        public final int maxParticles;
        public final float entityDistance;
        public final float animationDistance;
        public final boolean cullFarEntities;
        public final int lightingUpdatesPerTick;
        public final int targetFps;

        Settings(int renderDistanceChunks, int maxParticles, float entityDistance,
                 float animationDistance, boolean cullFarEntities, int lightingUpdatesPerTick,
                 int targetFps) {
            this.renderDistanceChunks = renderDistanceChunks;
            this.maxParticles = maxParticles;
            this.entityDistance = entityDistance;
            this.animationDistance = animationDistance;
            this.cullFarEntities = cullFarEntities;
            this.lightingUpdatesPerTick = lightingUpdatesPerTick;
            this.targetFps = targetFps;
        }
    }

    /**
     * Parses the governor's status JSON.
     *
     * <p>Never throws: a body that does not parse reads as a full-quality, nominal governor, which is
     * the "no data" default the library documents — never a fabricated reading. The raw text is kept
     * so the diagnostics screen can show what actually arrived.
     */
    public static Status parseStatus(String json) {
        float[] full = new float[Knob.values().length];
        for (int i = 0; i < full.length; i++) full[i] = 1f;
        if (json == null || json.isEmpty()) {
            return new Status(full, Thermal.NOMINAL, null, "");
        }
        try {
            JSONObject o = new JSONObject(json);
            float[] q = new float[Knob.values().length];
            org.json.JSONArray arr = o.optJSONArray("quality");
            for (int i = 0; i < q.length; i++) {
                q[i] = arr != null && i < arr.length() ? (float) arr.optDouble(i, 1.0) : 1f;
                if (q[i] < 0f) q[i] = 0f;
                if (q[i] > 1f) q[i] = 1f;
            }
            Thermal thermal = thermalAt(o.optInt("thermal", 0));
            Settings settings = null;
            JSONObject s = o.optJSONObject("settings");
            if (s != null) {
                settings = new Settings(
                        s.optInt("render_distance_chunks", 12),
                        s.optInt("max_particles", 4000),
                        (float) s.optDouble("entity_distance", 64.0),
                        (float) s.optDouble("animation_distance", 32.0),
                        s.optInt("cull_far_entities", 0) == 1,
                        s.optInt("lighting_updates_per_tick", 8),
                        s.optInt("target_fps", 60));
            }
            return new Status(q, thermal == null ? Thermal.NOMINAL : thermal, settings, json);
        } catch (JSONException e) {
            return new Status(full, Thermal.NOMINAL, null, json);
        }
    }

    /** The marker shown when the governor ABI is absent or too old. */
    public static String unavailableReason(boolean present, int reportedAbiVersion) {
        if (!present) return "BerryAPI's performance governor is not present.";
        if (!isCompatible(reportedAbiVersion)) {
            return "BerryAPI reports governor ABI v" + reportedAbiVersion
                    + "; this client expects v" + ABI_VERSION + ".";
        }
        return "";
    }
}
