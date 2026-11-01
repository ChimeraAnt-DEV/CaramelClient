package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Pins the native-library load plan (item 301).
 *
 * <p>These are the rules that were previously interleaved inside the load loop: a library skipped
 * because the device cannot load it and one skipped because the caller asked look identical from
 * outside, so only a test that reads the plan itself can tell them apart.
 */
public class LibraryLoadPlanTest {

    private static final List<String> ALL = Arrays.asList(
            "libc++_shared.so", "libfmod.so", "libminecraftpe.so",
            "libPlayFabMultiplayer.so", "libmaesdk.so");
    private static final List<String> SYSTEM = Arrays.asList(
            "libPlayFabMultiplayer.so", "libmaesdk.so");

    @Test
    public void a64BitDeviceLoadsEveryLibrary() {
        List<LibraryLoadPlan.Step> plan = LibraryLoadPlan.plan(
                ALL, SYSTEM, Collections.<String>emptySet(), true);
        assertEquals(5, plan.size());
        assertEquals(1, plan.get(0).index);
        assertEquals(5, plan.get(0).total);
        assertEquals("libmaesdk.so", plan.get(4).name);
    }

    @Test
    public void a32BitDeviceDropsTheArm64OnlyPrebuilts() {
        List<LibraryLoadPlan.Step> plan = LibraryLoadPlan.plan(
                ALL, SYSTEM, Collections.<String>emptySet(), false);
        assertEquals(3, plan.size());
        assertFalse(plan.stream().anyMatch(s -> s.name.equals("libmaesdk.so")));
        assertFalse(plan.stream().anyMatch(s -> s.name.equals("libPlayFabMultiplayer.so")));
    }

    @Test
    public void anExcludedLibraryIsLeftOutOfThePlan() {
        Set<String> exclude = new HashSet<>(Collections.singletonList("HttpClient.Android"));
        List<String> libs = Arrays.asList("libc++_shared.so", "libHttpClient.Android.so");
        List<LibraryLoadPlan.Step> plan = LibraryLoadPlan.plan(libs, SYSTEM, exclude, true);
        assertEquals(1, plan.size());
        assertEquals("libc++_shared.so", plan.get(0).name);
    }

    @Test
    public void exclusionMatchesEitherNameForm() {
        Set<String> shortForm = new HashSet<>(Collections.singletonList("HttpClient.Android"));
        Set<String> fileForm = new HashSet<>(Collections.singletonList("libHttpClient.Android.so"));
        assertTrue(LibraryLoadPlan.isExcluded("libHttpClient.Android.so", shortForm));
        assertTrue(LibraryLoadPlan.isExcluded("HttpClient.Android", fileForm));
        assertTrue(LibraryLoadPlan.isExcluded("libHttpClient.Android.so", fileForm));
        assertFalse(LibraryLoadPlan.isExcluded("libfmod.so", shortForm));
    }

    @Test
    public void theSkipReasonNamesTheCause() {
        Map<String, String> reasons = LibraryLoadPlan.skipReasons(
                ALL, SYSTEM, Collections.<String>emptySet(), false, Collections.<String, String>emptyMap());
        assertEquals("arm64-only prebuilt, not bundled on this device",
                reasons.get("libmaesdk.so"));
        assertNull(reasons.get("libfmod.so"));
    }

    @Test
    public void aCallerSuppliedReasonWins() {
        Set<String> exclude = new HashSet<>(Collections.singletonList("fmod"));
        Map<String, String> custom = new HashMap<>();
        custom.put("fmod", "audio disabled by the user");
        Map<String, String> reasons = LibraryLoadPlan.skipReasons(
                ALL, SYSTEM, exclude, true, custom);
        assertEquals("audio disabled by the user", reasons.get("libfmod.so"));
    }

    @Test
    public void aDefaultReasonIsUsedWhenTheCallerGivesNone() {
        Set<String> exclude = new HashSet<>(Collections.singletonList("fmod"));
        Map<String, String> reasons = LibraryLoadPlan.skipReasons(
                ALL, SYSTEM, exclude, true, Collections.<String, String>emptyMap());
        assertEquals("not required in this launch path", reasons.get("libfmod.so"));
    }

    @Test
    public void progressSpansTheRangeWithoutOverflowing() {
        assertEquals(46, LibraryLoadPlan.progressFor(1, 4, 46, 74));
        assertEquals(53, LibraryLoadPlan.progressFor(2, 4, 46, 74));
        assertEquals(67, LibraryLoadPlan.progressFor(4, 4, 46, 74));
        // The last step stays short of the end: the next stage owns the end value.
        assertTrue(LibraryLoadPlan.progressFor(4, 4, 46, 74) < 74);
    }

    @Test
    public void aSingleLibraryPlanDoesNotDivideByZero() {
        assertEquals(46, LibraryLoadPlan.progressFor(1, 1, 46, 74));
    }

    @Test
    public void anOutOfRangeIndexIsClamped() {
        assertEquals(46, LibraryLoadPlan.progressFor(0, 4, 46, 74));
        assertEquals(67, LibraryLoadPlan.progressFor(99, 4, 46, 74));
    }
}
