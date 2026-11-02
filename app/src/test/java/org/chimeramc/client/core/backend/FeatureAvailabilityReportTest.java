package org.chimeramc.client.core.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the preloader feature-availability report parsing (item 453).
 *
 * The report is what the Diagnostics screen prints and what a bug report quotes, so its shape, its
 * fail-closed behaviour on a bad blob and its summary wording are all fixed here. The class is
 * Android-free, so this runs on the JVM with no device.
 */
public class FeatureAvailabilityReportTest {

    @Test
    public void parsesEveryFeatureSortedByName() {
        String blob = "{\"overlay_hooks\":{\"state\":\"available\"},"
                + "\"native_cosmetics\":{\"state\":\"unavailable\","
                + "\"reason\":\"render slot did not resolve\"}}";
        FeatureAvailabilityReport report = FeatureAvailabilityReport.parse(blob);

        assertEquals(2, report.entries.size());
        // Sorted, because org.json's JSONObject is a hash map and its iteration order is not the
        // document order; the display order must be stable between runs.
        assertEquals("native_cosmetics", report.entries.get(0).feature);
        assertTrue(report.entries.get(0).isUnavailable());
        assertEquals("render slot did not resolve", report.entries.get(0).reason);
        assertEquals("overlay_hooks", report.entries.get(1).feature);
        assertTrue(report.entries.get(1).isAvailable());
    }

    @Test
    public void aMissingReasonIsAnEmptyStringNotAnError() {
        FeatureAvailabilityReport report = FeatureAvailabilityReport.parse(
                "{\"join_packet_seam\":{\"state\":\"available\"}}");
        assertEquals(1, report.entries.size());
        assertEquals("", report.entries.get(0).reason);
    }

    @Test
    public void aDisabledFeatureIsNeitherAvailableNorUnavailable() {
        FeatureAvailabilityReport report = FeatureAvailabilityReport.parse(
                "{\"texture_bind_seam\":{\"state\":\"disabled\"}}");
        assertEquals(1, report.entries.size());
        assertFalse(report.entries.get(0).isAvailable());
        assertFalse(report.entries.get(0).isUnavailable());
        // Disabled is not a failure, so it must not appear in the unavailable list.
        assertTrue(report.unavailable().isEmpty());
    }

    @Test
    public void aNullBlobReadsAsEmpty() {
        assertTrue(FeatureAvailabilityReport.parse(null).isEmpty());
        assertTrue(FeatureAvailabilityReport.parse("").isEmpty());
        assertTrue(FeatureAvailabilityReport.parse("   ").isEmpty());
    }

    @Test
    public void aMalformedBlobReadsAsEmptyNotAThrow() {
        assertTrue(FeatureAvailabilityReport.parse("{not json").isEmpty());
        assertTrue(FeatureAvailabilityReport.parse("[1,2,3]").isEmpty());
        assertTrue(FeatureAvailabilityReport.parse("\"a string\"").isEmpty());
    }

    @Test
    public void aNonObjectValueIsSkipped() {
        FeatureAvailabilityReport report = FeatureAvailabilityReport.parse(
                "{\"good\":{\"state\":\"available\"},\"bad\":42}");
        assertEquals(1, report.entries.size());
        assertEquals("good", report.entries.get(0).feature);
    }

    @Test
    public void anEmptyReportSaysThePreloaderDidNotAnswer() {
        assertEquals("no report from the preloader", FeatureAvailabilityReport.empty().summary());
    }

    @Test
    public void allAvailableSummarisesWithTheCount() {
        String blob = "{\"a\":{\"state\":\"available\"},\"b\":{\"state\":\"available\"}}";
        assertEquals("2/2 features available", FeatureAvailabilityReport.parse(blob).summary());
    }

    @Test
    public void anUnavailableFeatureIsNamedWithItsReason() {
        String blob = "{\"a\":{\"state\":\"available\"},"
                + "\"b\":{\"state\":\"unavailable\",\"reason\":\"slot did not resolve\"}}";
        String summary = FeatureAvailabilityReport.parse(blob).summary();
        assertTrue(summary, summary.startsWith("1/2 features available; unavailable: "));
        assertTrue(summary, summary.contains("b=unavailable (slot did not resolve)"));
    }

    @Test
    public void describeAllListsEveryEntry() {
        String blob = "{\"a\":{\"state\":\"available\"},\"b\":{\"state\":\"failed\","
                + "\"reason\":\"hook call failed\"}}";
        String text = FeatureAvailabilityReport.parse(blob).describeAll();
        assertTrue(text, text.contains("a=available"));
        assertTrue(text, text.contains("b=failed (hook call failed)"));
        assertFalse("no trailing newline", text.endsWith("\n"));
    }

    @Test
    public void describeAllOnAnEmptyReportSaysSo() {
        assertEquals("  (no report from the preloader)",
                FeatureAvailabilityReport.empty().describeAll());
    }
}
