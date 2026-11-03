package org.chimeramc.client.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The settings serialisation contract.
 *
 * <p>{@link FeatureSettings} is one Gson object per key in a single JSON blob, so the two failure
 * modes that matter are a <em>missing</em> field and an <em>extra</em> field. A field the JSON
 * omits must keep the value its declaration initialises it to - an older settings file, written
 * before a switch existed, has to load with that switch's documented default rather than a JVM
 * zero, or a new behaviour-changing toggle would silently come on (or off) for every existing
 * user. A field the JSON carries that this build does not know must be ignored, so a file written
 * by a newer build still loads.
 *
 * <p>The rule is pinned here without a device: {@link SettingsStorage#toJson} and
 * {@link SettingsStorage#fromJson} are pure, so the round trip and both edge cases are exercised on
 * the JVM with no {@code Context}.
 */
public class FeatureSettingsTest {

    @Test
    public void aRoundTripPreservesEveryValue() {
        FeatureSettings original = new FeatureSettings();
        original.setLowInputDelayEnabled(true);
        original.setFpsOptimizerEnabled(true);
        original.setReduceNetworkLatencyEnabled(false);
        original.setGuestModeEnabled(true);
        original.setStreamingSafeModeEnabled(true);
        original.setNavRailCollapsed(true);
        original.setLastNavTab("MODS");
        original.setOptifineModeEnabled(true);
        original.setOptifineItemEnabled("entity_culling", true);
        original.setOptifineCrashCount("particle_culling", 3);

        FeatureSettings restored = SettingsStorage.fromJson(SettingsStorage.toJson(original));
        assertNotNull(restored);
        assertTrue(restored.isLowInputDelayEnabled());
        assertTrue(restored.isFpsOptimizerEnabled());
        assertFalse(restored.isReduceNetworkLatencyEnabled());
        assertTrue(restored.isGuestModeEnabled());
        assertTrue(restored.isStreamingSafeModeEnabled());
        assertTrue(restored.isNavRailCollapsed());
        assertEquals("MODS", restored.getLastNavTab());
        assertTrue(restored.isOptifineModeEnabled());
        assertTrue(restored.isOptifineItemEnabled("entity_culling"));
        assertEquals(3, restored.getOptifineCrashCount("particle_culling"));
    }

    @Test
    public void aMissingFieldKeepsItsDeclaredDefault() {
        // A file written before these keys existed carries neither, so the switches that default
        // on must stay on and the ones that default off must stay off.
        String legacy = "{\"lowInputDelayEnabled\":true}";
        FeatureSettings restored = SettingsStorage.fromJson(legacy);
        assertNotNull(restored);
        assertTrue("an explicit value must load", restored.isLowInputDelayEnabled());
        assertTrue("optifineAllocatorEnabled defaults to true and must not become false",
                restored.isOptifineItemEnabled("allocator"));
        assertFalse("a switch that defaults off must not turn on", restored.isGuestModeEnabled());
        assertNull("lastNavTab defaults to null", restored.getLastNavTab());
    }

    @Test
    public void anExtraFieldIsIgnoredNotFatal() {
        String fromNewerBuild = "{\"guestModeEnabled\":true,\"aFieldThisBuildDoesNotKnow\":42}";
        FeatureSettings restored = SettingsStorage.fromJson(fromNewerBuild);
        assertNotNull("a newer build's file must still load", restored);
        assertTrue(restored.isGuestModeEnabled());
    }

    @Test
    public void malformedJsonReadsAsNoSettings() {
        assertNull(SettingsStorage.fromJson("{ not json"));
        assertNull(SettingsStorage.fromJson(null));
    }

    @Test
    public void anEmptyObjectLoadsWithEveryDefault() {
        FeatureSettings restored = SettingsStorage.fromJson("{}");
        assertNotNull(restored);
        assertFalse(restored.isLowInputDelayEnabled());
        assertFalse(restored.isFpsOptimizerEnabled());
        assertTrue("the allocator item defaults on", restored.isOptifineItemEnabled("allocator"));
        assertEquals("a missing crash count reads as zero", 0,
                restored.getOptifineCrashCount("entity_culling"));
    }
}
