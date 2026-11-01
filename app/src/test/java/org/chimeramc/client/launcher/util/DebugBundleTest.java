package org.chimeramc.client.launcher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins the debug-bundle tailing and redaction rules (items 262, 263, 517). */
public class DebugBundleTest {

    @Test
    public void tailKeepsOnlyTheMostRecentLines() {
        StringBuilder log = new StringBuilder();
        for (int i = 1; i <= 500; i++) {
            log.append("line").append(i).append('\n');
        }
        String tail = DebugBundle.tail(log.toString(), 200);
        String[] lines = tail.split("\n");
        assertEquals(200, lines.length);
        assertEquals("line301", lines[0]);
        assertEquals("line500", lines[199]);
    }

    @Test
    public void aTrailingNewlineDoesNotConsumeABudgetLine() {
        String tail = DebugBundle.tail("a\nb\nc\n", 3);
        assertEquals("a\nb\nc", tail);
    }

    @Test
    public void anEmptyOrNullLogTailsToEmptyNotNull() {
        assertEquals("", DebugBundle.tail(null, 10));
        assertEquals("", DebugBundle.tail("", 10));
        assertEquals("", DebugBundle.tail("x", 0));
    }

    @Test
    public void aShorterLogIsReturnedWhole() {
        assertEquals("one\ntwo", DebugBundle.tail("one\ntwo", 200));
    }

    @Test
    public void aBearerTokenIsRedacted() {
        String redacted = DebugBundle.redact("Authorization: Bearer abcdef123456");
        assertFalse(redacted.contains("abcdef123456"));
        assertTrue(redacted.contains("<redacted>"));
    }

    @Test
    public void anAccessTokenQueryValueIsRedacted() {
        String redacted = DebugBundle.redact("GET /x?access_token=deadbeefcafe&y=1");
        assertFalse(redacted.contains("deadbeefcafe"));
        assertTrue(redacted.contains("y=1"));
    }

    @Test
    public void aJwtIsRedacted() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abcdefghijk";
        String redacted = DebugBundle.redact("token is " + jwt);
        assertFalse(redacted.contains(jwt));
    }

    @Test
    public void ordinaryTextIsLeftAlone() {
        String text = "Launch finished in 1200 ms; library libc++_shared.so loaded";
        assertEquals(text, DebugBundle.redact(text));
    }

    @Test
    public void identityIsRedactedWhenNamed() {
        String redacted = DebugBundle.redactIdentity("player Notch joined", "Notch");
        assertFalse(redacted.contains("Notch"));
        assertTrue(redacted.contains("<redacted>"));
    }

    @Test
    public void aBlankIdentityIsIgnoredSoNothingIsWiped() {
        String text = "player Notch joined";
        assertEquals(text, DebugBundle.redactIdentity(text, "", "   ", null));
    }

    @Test
    public void composeIncludesTheReportAndASaysWhenNoLogExists() {
        String bundle = DebugBundle.compose("REPORT", null);
        assertTrue(bundle.startsWith("REPORT"));
        assertTrue(bundle.contains("(no log available)"));
    }

    @Test
    public void composeIncludesTheRedactedTail() {
        String bundle = DebugBundle.compose("REPORT", "Authorization: Bearer secretvalue\n");
        assertFalse(bundle.contains("secretvalue"));
        assertTrue(bundle.contains("REPORT"));
    }

    @Test
    public void streamingSafeComposeAlsoMasksCoordinatesAndAddresses() {
        // The report and the log both carry a world position and a server host; a streamer who
        // copies the bundle must not leak either (item 277).
        String report = "Position: X: 12.5 Y: 64.0 Z: -30.2\nServer: play.example.net:19132";
        String plain = DebugBundle.compose(report, null, false);
        assertTrue(plain.contains("12.5"));
        assertTrue(plain.contains("play.example.net:19132"));

        String safe = DebugBundle.compose(report, null, true);
        assertFalse(safe.contains("12.5"));
        assertFalse(safe.contains("play.example.net:19132"));
    }

    @Test
    public void streamingSafeComposeStillKeepsTheReport() {
        String safe = DebugBundle.compose("REPORT", "hello world\n", true);
        assertTrue(safe.contains("REPORT"));
        assertTrue(safe.contains("hello world"));
    }
}
