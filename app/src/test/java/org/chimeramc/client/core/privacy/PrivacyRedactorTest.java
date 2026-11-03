package org.chimeramc.client.core.privacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The streaming-safe masks, tested without a device.
 *
 * <p>The rules must mask what is identifying and leave everything else alone: over-masking a log
 * hides the bug being reported, and under-masking leaks what the mode exists to protect.
 */
public class PrivacyRedactorTest {

    @Test
    public void masksALabelledCoordinateLine() {
        String masked = PrivacyRedactor.coordinates("X: 12.5 Y: 64.0 Z: -30.2");
        assertFalse(masked.contains("12.5"));
        assertFalse(masked.contains("64.0"));
        assertFalse(masked.contains("-30.2"));
        assertTrue(masked.contains("X: " + PrivacyRedactor.MASK));
    }

    @Test
    public void masksABareCommaSeparatedTriple() {
        String masked = PrivacyRedactor.coordinates("pos 100, 70, -200 end");
        assertFalse(masked.contains("100"));
        assertFalse(masked.contains("-200"));
        assertTrue(masked.contains(PrivacyRedactor.MASK));
    }

    @Test
    public void leavesNonCoordinatesAlone() {
        // The failure this pins: "Memory: 512" contains "y: 512", which an unanchored axis match
        // masked. A pair of numbers must also survive, so only a real triple is masked.
        String text = "FPS: 60 Memory: 512 / 1024 MB";
        assertEquals(text, PrivacyRedactor.coordinates(text));
    }

    @Test
    public void masksASlashSeparatedTriple() {
        String masked = PrivacyRedactor.coordinates("XYZ: 128.50 / 64.00 / -302.25");
        assertFalse(masked.contains("128.50"));
        assertFalse(masked.contains("302.25"));
        assertTrue(masked.contains(PrivacyRedactor.MASK));
    }

    @Test
    public void masksAHostWithPort() {
        String masked = PrivacyRedactor.serverAddress("play.example.com:19132");
        assertEquals(PrivacyRedactor.ADDRESS_MASK, masked);
    }

    @Test
    public void masksAnIpv4WithPort() {
        String masked = PrivacyRedactor.serverAddress("192.168.1.20:19132");
        assertEquals(PrivacyRedactor.ADDRESS_MASK, masked);
    }

    @Test
    public void leavesAHostWithoutAPortAlone() {
        // "example.com" with no port is more likely prose than a server row.
        assertEquals("example.com", PrivacyRedactor.serverAddress("example.com"));
    }

    @Test
    public void masksAnIdentityWhereverItAppears() {
        String masked = PrivacyRedactor.identity(
                "signed in as Notch (Notch)", "Notch");
        assertFalse(masked.contains("Notch"));
        assertTrue(masked.contains("<redacted>"));
    }

    @Test
    public void everythingAppliesAllThreeMasks() {
        String masked = PrivacyRedactor.everything(
                "Notch at X: 1.0 Y: 2.0 Z: 3.0 on play.example.com:19132", "Notch");
        assertFalse(masked.contains("Notch"));
        assertFalse(masked.contains("1.0"));
        assertFalse(masked.contains("play.example.com"));
    }

    @Test
    public void nullAndEmptyAreReturnedUnchanged() {
        assertEquals(null, PrivacyRedactor.coordinates(null));
        assertEquals("", PrivacyRedactor.coordinates(""));
        assertEquals(null, PrivacyRedactor.serverAddress(null));
        assertEquals(null, PrivacyRedactor.everything(null, "Notch"));
    }
}
