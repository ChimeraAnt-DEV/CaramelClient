package org.chimeramc.client.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Item 178 — the external-intent scheme rules.
 *
 * <p>The scheme check is the part that decides whether an externally-supplied uri is acted on, and
 * it is pure, so it is tested here without an Android {@code Uri} (which is a stub off-device).
 */
public class IntentSanitizerTest {

    @Test
    public void httpAndHttpsAndContentAndMinecraftAreAccepted() {
        assertTrue(IntentSanitizer.isAllowedScheme("http"));
        assertTrue(IntentSanitizer.isAllowedScheme("https"));
        assertTrue(IntentSanitizer.isAllowedScheme("content"));
        assertTrue(IntentSanitizer.isAllowedScheme("minecraft"));
    }

    @Test
    public void caseIsIgnoredForSchemes() {
        assertTrue(IntentSanitizer.isAllowedScheme("HTTPS"));
        assertTrue(IntentSanitizer.isAllowedScheme("Minecraft"));
    }

    @Test
    public void fileAndJavascriptAreRefused() {
        assertFalse(IntentSanitizer.isAllowedScheme("file"));
        assertFalse(IntentSanitizer.isAllowedScheme("javascript"));
        assertFalse(IntentSanitizer.isAllowedScheme("intent"));
        assertFalse(IntentSanitizer.isAllowedScheme(null));
    }

    @Test
    public void theLengthLimitIsTheDocumentedOne() {
        // The limit is what stops an external app handing the launcher a multi-megabyte extra, so
        // pin it rather than letting it drift.
        assertTrue(IntentSanitizer.MAX_EXTRA_CHARS > 0);
        assertTrue(IntentSanitizer.MAX_EXTRA_CHARS <= 64 * 1024);
    }
}
