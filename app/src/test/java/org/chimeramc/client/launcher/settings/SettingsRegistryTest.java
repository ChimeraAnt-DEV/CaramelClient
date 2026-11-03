package org.chimeramc.client.launcher.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.settings.FeatureSettings;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The settings registry, pinned without a device.
 *
 * <p>The registry is the one description of a setting, so the rules that matter are: every id is
 * unique, every setting carries a title and a description resource, the getter and setter agree with
 * the store's own default, search finds a setting by what it does, and reset returns exactly the
 * changed settings to their defaults.
 */
public class SettingsRegistryTest {

    /** A text lookup that returns the ids, so the search test needs no resources. */
    private static final SettingsRegistry.TextLookup IDS = new SettingsRegistry.TextLookup() {
        @Override public String title(SettingsRegistry.Setting s) { return s.id; }
        @Override public String description(SettingsRegistry.Setting s) { return ""; }
    };

    @Test
    public void everyIdIsUniqueAndEverySettingIsDescribed() {
        Set<String> ids = new HashSet<>();
        for (SettingsRegistry.Setting s : SettingsRegistry.all()) {
            assertTrue("duplicate id: " + s.id, ids.add(s.id));
            assertNotNull(s.id);
            assertFalse("blank id", s.id.isEmpty());
            assertTrue(s.id + " has no title resource", s.titleRes != 0);
            assertTrue(s.id + " has no description resource", s.descriptionRes != 0);
            assertNotNull(s.id + " has no keywords", s.keywords);
            if (s.kind != SettingsRegistry.Kind.ACTION) {
                assertNotNull(s.id + " has no getter", s.getter);
                assertNotNull(s.id + " has no setter", s.setter);
            }
        }
        assertFalse("the registry must not be empty", ids.isEmpty());
    }

    @Test
    public void theSectionsAndOrderAreStable() {
        List<SettingsRegistry.Setting> basic = SettingsRegistry.inSection(
                SettingsRegistry.Section.BASIC);
        assertTrue("BASIC must have settings", !basic.isEmpty());
        for (SettingsRegistry.Setting s : basic) {
            assertEquals(SettingsRegistry.Section.BASIC, s.section);
        }
        // inSection returns registration order, and find resolves any registered id.
        for (SettingsRegistry.Setting s : SettingsRegistry.all()) {
            assertEquals(s, SettingsRegistry.find(s.id));
        }
        assertNull(SettingsRegistry.find("no_such_setting"));
        assertNull(SettingsRegistry.find(null));
    }

    @Test
    public void aGetterAndSetterAgreeWithTheStoresDefault() {
        // The registry's declared default must be what a fresh store actually reads, or reset would
        // flip a setting to a value the store does not use.
        FeatureSettings fresh = new FeatureSettings();
        for (SettingsRegistry.Setting s : SettingsRegistry.all()) {
            if (s.kind == SettingsRegistry.Kind.ACTION) continue;
            assertEquals("default drifted for " + s.id, s.defaultValue, s.value(fresh));
            assertTrue("a fresh store must read every setting as its default: " + s.id,
                    s.isDefault(fresh));
        }
    }

    @Test
    public void applyWritesThroughAndResetReturnsToDefault() {
        FeatureSettings settings = new FeatureSettings();
        SettingsRegistry.Setting lowDelay = SettingsRegistry.find("low_input_delay");
        assertNotNull(lowDelay);
        boolean original = lowDelay.value(settings);

        lowDelay.apply(settings, !original);
        assertEquals(!original, lowDelay.value(settings));
        assertFalse("the changed setting must not read as default", lowDelay.isDefault(settings));
        assertTrue("the changed id must be listed",
                SettingsRegistry.nonDefaultIds(settings).contains("low_input_delay"));

        int changed = SettingsRegistry.resetToDefaults(settings);
        assertTrue("reset must report at least the one change", changed >= 1);
        assertEquals(original, lowDelay.value(settings));
        assertTrue("everything must be at default after a reset",
                SettingsRegistry.nonDefaultIds(settings).isEmpty());
    }

    @Test
    public void searchMatchesIdKeywordsAndText() {
        // By id.
        assertTrue(contains(SettingsRegistry.search("guest", IDS), "guest_mode"));
        // By a keyword that is not in the id.
        assertTrue("a keyword must be searchable",
                contains(SettingsRegistry.search("latency", IDS), "low_input_delay")
                        || contains(SettingsRegistry.search("latency", IDS),
                                "reduce_network_latency"));
        // By resolved text.
        SettingsRegistry.TextLookup text = new SettingsRegistry.TextLookup() {
            @Override public String title(SettingsRegistry.Setting s) { return "Zebra"; }
            @Override public String description(SettingsRegistry.Setting s) { return ""; }
        };
        assertEquals(SettingsRegistry.all().size(),
                SettingsRegistry.search("zebra", text).size());
        // An empty query matches everything; a miss matches nothing.
        assertEquals(SettingsRegistry.all().size(), SettingsRegistry.search("", IDS).size());
        assertEquals(SettingsRegistry.all().size(), SettingsRegistry.search(null, IDS).size());
        assertTrue(SettingsRegistry.search("zzzznothing", IDS).isEmpty());
    }

    private static boolean contains(List<SettingsRegistry.Setting> list, String id) {
        for (SettingsRegistry.Setting s : list) {
            if (s.id.equals(id)) return true;
        }
        return false;
    }
}
