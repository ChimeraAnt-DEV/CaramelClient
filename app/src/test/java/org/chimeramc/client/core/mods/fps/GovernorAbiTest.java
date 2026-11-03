package org.chimeramc.client.core.mods.fps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The client's mirror of BerryAPI's governor C ABI, pinned without the native library.
 *
 * <p>The knob order is the contract: it is a C enum on the library side, so a reorder on one side
 * would move a value onto the wrong knob. That is asserted here, not assumed.
 */
public class GovernorAbiTest {

    @Test
    public void theKnobOrderMatchesTheLibraryEnum() {
        // Cheapest to give up first, exactly as perf.h declares QualityKnob.
        assertEquals(GovernorAbi.Knob.PARTICLES, GovernorAbi.knobAt(0));
        assertEquals(GovernorAbi.Knob.ENTITY_DISTANCE, GovernorAbi.knobAt(1));
        assertEquals(GovernorAbi.Knob.ANIMATION_DISTANCE, GovernorAbi.knobAt(2));
        assertEquals(GovernorAbi.Knob.ENTITY_CULLING, GovernorAbi.knobAt(3));
        assertEquals(GovernorAbi.Knob.LIGHTING_UPDATES, GovernorAbi.knobAt(4));
        assertEquals(GovernorAbi.Knob.RENDER_DISTANCE, GovernorAbi.knobAt(5));
        assertNull(GovernorAbi.knobAt(6));
        assertNull(GovernorAbi.knobAt(-1));
        assertEquals(6, GovernorAbi.Knob.values().length);
    }

    @Test
    public void theThermalOrderMatchesAndThrottlingIsSeriousOrWorse() {
        assertEquals(GovernorAbi.Thermal.NOMINAL, GovernorAbi.thermalAt(0));
        assertEquals(GovernorAbi.Thermal.SHUTDOWN, GovernorAbi.thermalAt(4));
        assertNull(GovernorAbi.thermalAt(5));
        assertFalse(GovernorAbi.isThrottling(GovernorAbi.Thermal.NOMINAL));
        assertFalse("Fair is warm, not throttling", GovernorAbi.isThrottling(GovernorAbi.Thermal.FAIR));
        assertTrue(GovernorAbi.isThrottling(GovernorAbi.Thermal.SERIOUS));
        assertTrue(GovernorAbi.isThrottling(GovernorAbi.Thermal.CRITICAL));
    }

    @Test
    public void theVersionGuardRefusesADrift() {
        assertTrue(GovernorAbi.isCompatible(GovernorAbi.ABI_VERSION));
        assertFalse(GovernorAbi.isCompatible(GovernorAbi.ABI_VERSION + 1));
        assertTrue(GovernorAbi.unavailableReason(false, 0).contains("not present"));
        assertTrue(GovernorAbi.unavailableReason(true, 2).contains("expects v1"));
        assertEquals("", GovernorAbi.unavailableReason(true, GovernorAbi.ABI_VERSION));
    }

    @Test
    public void theStatusParsesQualityThermalAndSettings() {
        GovernorAbi.Status s = GovernorAbi.parseStatus("{\"quality\":[0.5,0.6,0.7,0.8,0.9,1.0],"
                + "\"thermal\":2,\"settings\":{\"render_distance_chunks\":8,\"max_particles\":2000,"
                + "\"entity_distance\":40.0,\"animation_distance\":24.0,\"cull_far_entities\":1,"
                + "\"lighting_updates_per_tick\":4,\"target_fps\":60}}");
        assertEquals(0.5f, s.quality[0], 1e-4f);
        assertEquals(GovernorAbi.Thermal.SERIOUS, s.thermal);
        assertTrue(s.throttling());
        assertTrue(s.meanQuality() > 0.5f && s.meanQuality() < 1f);
        assertEquals(8, s.settings.renderDistanceChunks);
        assertEquals(2000, s.settings.maxParticles);
        assertTrue(s.settings.cullFarEntities);
        assertEquals(4, s.settings.lightingUpdatesPerTick);
    }

    @Test
    public void anAbsentOrMalformedStatusReadsAsFullQualityNominal() {
        GovernorAbi.Status s = GovernorAbi.parseStatus(null);
        assertEquals(1f, s.quality[0], 1e-4f);
        assertEquals(GovernorAbi.Thermal.NOMINAL, s.thermal);
        assertFalse(s.throttling());
        assertNull(s.settings);

        GovernorAbi.Status bad = GovernorAbi.parseStatus("nope");
        assertEquals(1f, bad.quality[0], 1e-4f);
        assertFalse(bad.throttling());
        assertEquals("nope", bad.raw);
    }

    @Test
    public void qualityIsClampedToZeroOne() {
        GovernorAbi.Status s = GovernorAbi.parseStatus(
                "{\"quality\":[1.5,-0.5],\"thermal\":0}");
        assertEquals(1f, s.quality[0], 1e-4f);
        assertEquals(0f, s.quality[1], 1e-4f);
        // Knobs the body omits read as full quality, not zero.
        assertEquals(1f, s.quality[5], 1e-4f);
    }
}
