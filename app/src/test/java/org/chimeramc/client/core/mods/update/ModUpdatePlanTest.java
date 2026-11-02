package org.chimeramc.client.core.mods.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pins the mod-update rules: what counts as an update, and the announcement fingerprint. */
public class ModUpdatePlanTest {

    private static ModUpdatePlan.InstalledMod mod(String id, String name, String version) {
        return new ModUpdatePlan.InstalledMod(id, name, version);
    }

    private static Map<String, String> feed(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    @Test
    public void aNewerFeedVersionIsAnUpdate() {
        List<ModUpdatePlan.ModUpdate> updates = ModUpdatePlan.compute(
                Collections.singletonList(mod("a", "Alpha", "1.0.0")),
                feed("a", "1.1.0"));
        assertEquals(1, updates.size());
        assertEquals("a", updates.get(0).id);
        assertEquals("1.0.0", updates.get(0).installedVersion);
        assertEquals("1.1.0", updates.get(0).availableVersion);
    }

    @Test
    public void anEqualOrOlderFeedVersionIsNotAnUpdate() {
        assertTrue(ModUpdatePlan.compute(
                Collections.singletonList(mod("a", "Alpha", "1.1.0")), feed("a", "1.1.0")).isEmpty());
        assertTrue(ModUpdatePlan.compute(
                Collections.singletonList(mod("a", "Alpha", "1.1.0")), feed("a", "1.0.0")).isEmpty());
    }

    @Test
    public void aModWithNoFeedEntryIsSkipped() {
        assertTrue(ModUpdatePlan.compute(
                Collections.singletonList(mod("a", "Alpha", "1.0.0")), feed("b", "9.9.9")).isEmpty());
    }

    @Test
    public void aFeedEntryWithNoInstalledModIsIgnored() {
        // The feed describes what exists; an id nobody has installed is not the user's problem.
        assertTrue(ModUpdatePlan.compute(
                Collections.singletonList(mod("a", "Alpha", "1.0.0")), feed("ghost", "9.9.9")).isEmpty());
    }

    @Test
    public void aBlankInstalledVersionCountsAsUpdatable() {
        // "Unknown version" is not "up to date": the user should still be told.
        List<ModUpdatePlan.ModUpdate> updates = ModUpdatePlan.compute(
                Collections.singletonList(mod("a", "Alpha", "")), feed("a", "1.0.0"));
        assertEquals(1, updates.size());
        assertEquals("", updates.get(0).installedVersion);
    }

    @Test
    public void theFeedIdMatchIsCaseInsensitive() {
        List<ModUpdatePlan.ModUpdate> updates = ModUpdatePlan.compute(
                Collections.singletonList(mod("MyMod", "My Mod", "1.0.0")), feed("mymod", "2.0.0"));
        assertEquals(1, updates.size());
    }

    @Test
    public void aBlankFeedVersionIsSkipped() {
        assertTrue(ModUpdatePlan.compute(
                Collections.singletonList(mod("a", "Alpha", "1.0.0")), feed("a", "  ")).isEmpty());
    }

    @Test
    public void theInstalledOrderIsPreserved() {
        List<ModUpdatePlan.InstalledMod> installed = new ArrayList<>();
        installed.add(mod("c", "Charlie", "1.0"));
        installed.add(mod("a", "Alpha", "1.0"));
        installed.add(mod("b", "Bravo", "1.0"));
        List<ModUpdatePlan.ModUpdate> updates = ModUpdatePlan.compute(installed,
                feed("a", "2.0", "b", "2.0", "c", "2.0"));
        assertEquals(3, updates.size());
        assertEquals("c", updates.get(0).id);
        assertEquals("a", updates.get(1).id);
        assertEquals("b", updates.get(2).id);
    }

    @Test
    public void aMissingNameFallsBackToTheId() {
        List<ModUpdatePlan.ModUpdate> updates = ModUpdatePlan.compute(
                Collections.singletonList(mod("a", "", "1.0")), feed("a", "2.0"));
        assertEquals("a", updates.get(0).name);
    }

    @Test
    public void nullInputsProduceNoUpdates() {
        assertTrue(ModUpdatePlan.compute(null, feed("a", "2.0")).isEmpty());
        assertTrue(ModUpdatePlan.compute(Collections.singletonList(mod("a", "A", "1")), null).isEmpty());
        assertTrue(ModUpdatePlan.compute(null, null).isEmpty());
    }

    @Test
    public void theFingerprintIsOrderIndependentAndVersionSensitive() {
        List<ModUpdatePlan.ModUpdate> first = new ArrayList<>();
        first.add(new ModUpdatePlan.ModUpdate("a", "A", "1.0", "2.0"));
        first.add(new ModUpdatePlan.ModUpdate("b", "B", "1.0", "2.0"));
        List<ModUpdatePlan.ModUpdate> second = new ArrayList<>();
        second.add(new ModUpdatePlan.ModUpdate("b", "B", "1.0", "2.0"));
        second.add(new ModUpdatePlan.ModUpdate("a", "A", "1.0", "2.0"));
        assertEquals(ModUpdatePlan.fingerprint(first), ModUpdatePlan.fingerprint(second));

        List<ModUpdatePlan.ModUpdate> bumped = new ArrayList<>();
        bumped.add(new ModUpdatePlan.ModUpdate("a", "A", "1.0", "3.0"));
        bumped.add(new ModUpdatePlan.ModUpdate("b", "B", "1.0", "2.0"));
        assertFalse(ModUpdatePlan.fingerprint(first).equals(ModUpdatePlan.fingerprint(bumped)));
    }

    @Test
    public void anEmptyUpdateListHasAnEmptyFingerprint() {
        assertTrue(ModUpdatePlan.fingerprint(Collections.emptyList()).isEmpty());
        assertTrue(ModUpdatePlan.fingerprint(null).isEmpty());
    }

    @Test
    public void theFingerprintCarriesTheIdAndAvailableVersion() {
        List<ModUpdatePlan.ModUpdate> updates = Collections.singletonList(
                new ModUpdatePlan.ModUpdate("a", "A", "1.0", "2.0"));
        assertTrue(ModUpdatePlan.fingerprint(updates).contains("a@2.0"));
    }
}
