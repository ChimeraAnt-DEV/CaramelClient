package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Pins the supersample decision: only a software rasteriser is downgraded.
 *
 * <p>An unread or unknown family must get the hardware factor. The failure this guards is the
 * opposite one — downgrading a real GPU because the probe had not run yet would make the preview
 * visibly worse on every device that reports a family we do not name.
 */
public class PreviewQualityTest {

    @Test
    public void softwareIsDowngradedToNoSupersample() {
        assertEquals(PreviewQuality.SOFTWARE_SUPERSAMPLE,
                PreviewQuality.supersampleFor(GpuFamily.Kind.SOFTWARE));
    }

    @Test
    public void everyHardwareFamilyKeepsTheFullSupersample() {
        GpuFamily.Kind[] hardware = {
                GpuFamily.Kind.ADRENO, GpuFamily.Kind.MALI, GpuFamily.Kind.IMMORTALIS,
                GpuFamily.Kind.POWERVR, GpuFamily.Kind.XCLIPSE, GpuFamily.Kind.NVIDIA,
                GpuFamily.Kind.APPLE, GpuFamily.Kind.GENERIC, GpuFamily.Kind.UNKNOWN,
        };
        for (GpuFamily.Kind kind : hardware) {
            assertEquals("family " + kind, PreviewQuality.HARDWARE_SUPERSAMPLE,
                    PreviewQuality.supersampleFor(kind));
        }
    }

    @Test
    public void anUnreadNativeCodeIsTreatedAsHardwareNotSoftware() {
        // -1 is the native "not read" sentinel; it must not be mistaken for a software rasteriser.
        assertEquals(PreviewQuality.HARDWARE_SUPERSAMPLE,
                PreviewQuality.supersampleForNativeCode(-1));
        // The software code (8) is the only one downgraded.
        assertEquals(PreviewQuality.SOFTWARE_SUPERSAMPLE,
                PreviewQuality.supersampleForNativeCode(8));
        assertEquals(PreviewQuality.HARDWARE_SUPERSAMPLE,
                PreviewQuality.supersampleForNativeCode(1));
    }
}
