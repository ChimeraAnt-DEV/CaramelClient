package org.chimeramc.client.core.antegg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins the pre-download preview of a linked package (item 233). */
public class AntEggLinkPreviewTest {

    @Test
    public void anUnknownSizeIsNotReportedAsZero() {
        assertEquals("unknown size", AntEggLinkPreview.formatSize(-1));
    }

    @Test
    public void sizesAreHumanReadable() {
        assertEquals("0 B", AntEggLinkPreview.formatSize(0));
        assertEquals("512 B", AntEggLinkPreview.formatSize(512));
        assertEquals("1.0 KB", AntEggLinkPreview.formatSize(1024));
        assertEquals("1.0 MB", AntEggLinkPreview.formatSize(1024 * 1024));
        assertEquals("2.5 MB", AntEggLinkPreview.formatSize((long) (2.5 * 1024 * 1024)));
    }

    @Test
    public void theDisplayNameIsTheFileNameTheLinkImplies() {
        assertEquals("mod.antegg", AntEggLinkPreview.displayName("https://example.com/mod.antegg"));
    }

    @Test
    public void theDescriptionNamesTheFileSizeAndSource() {
        String described = AntEggLinkPreview.describe(
                "https://example.com/mod.antegg", 2048);
        assertTrue(described.contains("mod.antegg"));
        assertTrue(described.contains("2.0 KB"));
        assertTrue(described.contains("https://example.com/mod.antegg"));
    }

    @Test
    public void anUnknownSizeReadsAsUnknownNotZero() {
        String described = AntEggLinkPreview.describe("https://example.com/mod.antegg", -1);
        assertTrue(described.contains("unknown size"));
    }

    @Test
    public void theDigestLineIsBlankWhenThereIsNoDigest() {
        assertEquals("", AntEggLinkPreview.describeDigest(null));
        assertEquals("", AntEggLinkPreview.describeDigest(""));
        assertEquals("SHA-256: abc", AntEggLinkPreview.describeDigest("abc"));
    }
}
