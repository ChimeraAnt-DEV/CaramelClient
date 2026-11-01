package org.chimeramc.client.core.crash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** Pins the crash-report consent preview (item 518). */
public class CrashReportPreviewTest {

    @Test
    public void theHeadersNameOnlyTheAppVersionAndTheReport() {
        List<CrashReportPreview.Header> headers =
                CrashReportPreview.headers("1.2.3", "tombstone_0.txt", true);
        assertEquals(3, headers.size());
        assertEquals("X-Crash-App-Version", headers.get(0).name);
        assertEquals("1.2.3", headers.get(0).value);
        assertEquals("X-Crash-Report-Name", headers.get(1).name);
        assertEquals("tombstone_0.txt", headers.get(1).value);
        assertEquals("X-Crash-Report-Truncated", headers.get(2).name);
        assertEquals("true", headers.get(2).value);
    }

    @Test
    public void aMissingReportSaysSoRatherThanShowingNothing() {
        String body = CrashReportPreview.body(null);
        assertTrue(body.contains("no crash report"));
        assertFalse(body.isEmpty());
    }

    @Test
    public void aShortReportIsShownWhole() {
        assertEquals("hello", CrashReportPreview.body("hello"));
    }

    @Test
    public void aLongReportIsTruncatedAndSaysSo() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < CrashReportPreview.PREVIEW_CHARS + 500; i++) big.append('x');
        String body = CrashReportPreview.body(big.toString());
        assertTrue(body.startsWith(big.substring(0, CrashReportPreview.PREVIEW_CHARS)));
        assertTrue(body.contains("truncated"));
    }

    @Test
    public void theDescriptionNamesTheEndpointWhenOneExists() {
        String described = CrashReportPreview.describe(
                "https://collector.example/crash", "1.2.3", "tombstone_0.txt", false, "stack");
        assertTrue(described.contains("https://collector.example/crash"));
        assertTrue(described.contains("X-Crash-App-Version: 1.2.3"));
        assertTrue(described.contains("stack"));
    }

    @Test
    public void theDescriptionSaysNothingCanBeSentWithNoEndpoint() {
        String described = CrashReportPreview.describe("", "1.2.3", "", false, null);
        assertTrue(described.contains("No collector is configured"));
    }

    @Test
    public void nullEndpointIsTreatedAsUnconfigured() {
        String described = CrashReportPreview.describe(null, "1.2.3", "", false, null);
        assertTrue(described.contains("No collector is configured"));
    }
}
