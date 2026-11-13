package org.chimeramc.client.core.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the hook self-test report parsing.
 *
 * The report is what the Diagnostics screen prints and what a bug report quotes, so its shape, its
 * fail-closed behaviour on a bad blob and its summary wording are fixed here. The class is
 * Android-free, so this runs on the JVM with no device.
 */
public class HookSelfTestReportTest {

    @Test
    public void parsesTheControlAndEverySite() {
        String blob = "{\"backend_ok\":true,\"passed\":1,\"total\":2,\"sites\":["
                + "{\"name\":\"b\",\"state\":\"unresolved\"},"
                + "{\"name\":\"a\",\"state\":\"ok\"}]}";
        HookSelfTestReport report = HookSelfTestReport.parse(blob);

        assertTrue(report.backendOk);
        assertEquals(1, report.reportedPassed);
        assertEquals(2, report.reportedTotal);
        assertEquals(2, report.sites.size());
        // Sorted by name, because the sweetloader's render order is not something the client should
        // depend on for a stable display.
        assertEquals("a", report.sites.get(0).name);
        assertTrue(report.sites.get(0).isOk());
        assertEquals("b", report.sites.get(1).name);
        assertTrue(report.sites.get(1).isUnresolved());
    }

    @Test
    public void aFailedSiteIsReportedWithItsDetail() {
        HookSelfTestReport report = HookSelfTestReport.parse(
                "{\"backend_ok\":true,\"passed\":0,\"total\":1,\"sites\":["
                        + "{\"name\":\"render\",\"state\":\"not-executable\","
                        + "\"detail\":\"not executable code\"}]}");
        assertEquals(1, report.failed().size());
        assertEquals("not-executable", report.failed().get(0).state);
        assertEquals("not executable code", report.failed().get(0).detail);
    }

    @Test
    public void anUnresolvedSiteIsNotAFailure() {
        HookSelfTestReport report = HookSelfTestReport.parse(
                "{\"backend_ok\":true,\"passed\":1,\"total\":1,\"sites\":["
                        + "{\"name\":\"render\",\"state\":\"unresolved\"}]}");
        assertTrue(report.failed().isEmpty());
    }

    @Test
    public void aNullBlobReadsAsEmpty() {
        assertTrue(HookSelfTestReport.parse(null).isEmpty());
        assertTrue(HookSelfTestReport.parse("").isEmpty());
        assertTrue(HookSelfTestReport.parse("   ").isEmpty());
    }

    @Test
    public void aMalformedBlobReadsAsEmptyNotAThrow() {
        assertTrue(HookSelfTestReport.parse("{not json").isEmpty());
        assertTrue(HookSelfTestReport.parse("[1,2,3]").isEmpty());
    }

    @Test
    public void aBrokenBackendDominatesTheSummary() {
        HookSelfTestReport report = HookSelfTestReport.parse(
                "{\"backend_ok\":false,\"passed\":0,\"total\":1,\"sites\":["
                        + "{\"name\":\"a\",\"state\":\"failed\"}]}");
        assertTrue(report.summary().contains("backend self-test FAILED"));
    }

    @Test
    public void allPassingSummarisesWithTheCount() {
        HookSelfTestReport report = HookSelfTestReport.parse(
                "{\"backend_ok\":true,\"passed\":2,\"total\":2,\"sites\":["
                        + "{\"name\":\"a\",\"state\":\"ok\"},{\"name\":\"b\",\"state\":\"ok\"}]}");
        assertEquals("2/2 hook sites round-tripped", report.summary());
    }

    @Test
    public void aFailedSiteIsNamedInTheSummary() {
        HookSelfTestReport report = HookSelfTestReport.parse(
                "{\"backend_ok\":true,\"passed\":1,\"total\":2,\"sites\":["
                        + "{\"name\":\"a\",\"state\":\"ok\"},"
                        + "{\"name\":\"b\",\"state\":\"failed\",\"detail\":\"refused\"}]}");
        String summary = report.summary();
        assertTrue(summary, summary.startsWith("1/2 hook sites round-tripped; failed: "));
        assertTrue(summary, summary.contains("b=failed (refused)"));
    }

    @Test
    public void anEmptyReportSaysTheSweetLoaderDidNotAnswer() {
        assertEquals("no self-test from the sweetloader", HookSelfTestReport.empty().summary());
    }

    @Test
    public void describeAllListsTheBackendAndEverySite() {
        HookSelfTestReport report = HookSelfTestReport.parse(
                "{\"backend_ok\":true,\"passed\":1,\"total\":1,\"sites\":["
                        + "{\"name\":\"a\",\"state\":\"ok\"}]}");
        String text = report.describeAll();
        assertTrue(text, text.contains("backend: ok"));
        assertTrue(text, text.contains("a=ok"));
        assertFalse("no trailing newline", text.endsWith("\n"));
    }
}
