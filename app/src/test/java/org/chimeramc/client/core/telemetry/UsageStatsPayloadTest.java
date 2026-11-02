package org.chimeramc.client.core.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

/** Pins exactly what the anonymous usage counter sends (item 281). */
public class UsageStatsPayloadTest {

    @Test
    public void theFieldsAreTheDocumentedOnesAndNoMore() {
        Map<String, String> fields = UsageStatsPayload.fields(
                "1.2.3", "4gb", "arm64-v8a", 10, 8, 1);
        assertEquals(7, fields.size());
        assertTrue(fields.containsKey("schema"));
        assertTrue(fields.containsKey("version"));
        assertTrue(fields.containsKey("device_class"));
        assertTrue(fields.containsKey("abi"));
        assertTrue(fields.containsKey("launches"));
        assertTrue(fields.containsKey("launch_successes"));
        assertTrue(fields.containsKey("crash_reports"));
    }

    @Test
    public void noIdentifyingFieldIsEverPresent() {
        // The whole point of the feature: these names must never appear, whatever the inputs.
        String json = UsageStatsPayload.toJson(UsageStatsPayload.fields(
                "1.0", "4gb", "arm64-v8a", 1, 1, 0)).toLowerCase();
        for (String banned : new String[]{"gamertag", "xuid", "account", "serial", "advertising",
                "server", "world", "mods", "email"}) {
            assertFalse("payload must not mention " + banned, json.contains(banned));
        }
    }

    @Test
    public void negativeCountsAreClampedToZero() {
        Map<String, String> fields = UsageStatsPayload.fields("1.0", "2gb", "arm64-v8a", -5, -1, -9);
        assertEquals("0", fields.get("launches"));
        assertEquals("0", fields.get("launch_successes"));
        assertEquals("0", fields.get("crash_reports"));
    }

    @Test
    public void theDeviceClassIsABucketNotANumber() {
        assertEquals("unknown", UsageStatsPayload.deviceClassFor(0));
        assertEquals("unknown", UsageStatsPayload.deviceClassFor(-1));
        assertEquals("2gb", UsageStatsPayload.deviceClassFor(2L * 1024 * 1024 * 1024));
        assertEquals("4gb", UsageStatsPayload.deviceClassFor(4L * 1024 * 1024 * 1024));
        assertEquals("8gb", UsageStatsPayload.deviceClassFor(8L * 1024 * 1024 * 1024));
        assertEquals("12gb+", UsageStatsPayload.deviceClassFor(16L * 1024 * 1024 * 1024));
    }

    @Test
    public void theBucketBoundariesAreExact() {
        // 2559 MB is still "2gb", 2560 MB is "4gb".
        assertEquals("2gb", UsageStatsPayload.deviceClassFor(2559L * 1024 * 1024));
        assertEquals("4gb", UsageStatsPayload.deviceClassFor(2560L * 1024 * 1024));
        assertEquals("4gb", UsageStatsPayload.deviceClassFor(5119L * 1024 * 1024));
        assertEquals("8gb", UsageStatsPayload.deviceClassFor(5120L * 1024 * 1024));
        assertEquals("8gb", UsageStatsPayload.deviceClassFor(9215L * 1024 * 1024));
        assertEquals("12gb+", UsageStatsPayload.deviceClassFor(9216L * 1024 * 1024));
    }

    @Test
    public void theJsonIsWellFormedAndCarriesTheSchema() {
        String json = UsageStatsPayload.toJson(UsageStatsPayload.fields(
                "9.9", "8gb", "arm64-v8a", 3, 2, 1));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("\"schema\":\"1\""));
        assertTrue(json.contains("\"launches\":\"3\""));
    }

    @Test
    public void nullsAndQuotesAreEscaped() {
        Map<String, String> fields = UsageStatsPayload.fields(
                "1.0\"evil", "4gb", "arm64-v8a", 1, 1, 0);
        String json = UsageStatsPayload.toJson(fields);
        assertTrue(json.contains("\\\""));
        assertFalse(json.contains("1.0\"evil"));
    }

    @Test
    public void aNullFieldMapProducesEmptyJson() {
        assertEquals("{}", UsageStatsPayload.toJson(null));
    }

    @Test
    public void thePreviewNamesEveryFieldAndStatesWhatIsAbsent() {
        String preview = UsageStatsPayload.describe(UsageStatsPayload.fields(
                "1.0", "4gb", "arm64-v8a", 5, 4, 1));
        assertTrue(preview.contains("launches"));
        assertTrue(preview.contains("device_class"));
        assertTrue(preview.contains("no account name"));
    }

    @Test
    public void theSchemaVersionIsPinned() {
        // A change to the payload shape must bump this, so a server can tell reports apart.
        assertEquals(1, UsageStatsPayload.SCHEMA_VERSION);
    }
}
