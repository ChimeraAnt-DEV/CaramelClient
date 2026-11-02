package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the native-code mapping and the labels for the GPU family.
 *
 * <p>The native enum's numeric values are a wire contract: the diagnostics array carries the raw
 * code, so a renumbering here without the native side would silently mislabel every device. The
 * out-of-range cases matter too, because -1 is what the native side sends when it has not read the
 * strings yet.
 */
public class GpuFamilyTest {

    @Test
    public void everyNativeCodeMapsToItsFamily() {
        assertEquals(GpuFamily.Kind.UNKNOWN, GpuFamily.fromNativeCode(0));
        assertEquals(GpuFamily.Kind.ADRENO, GpuFamily.fromNativeCode(1));
        assertEquals(GpuFamily.Kind.MALI, GpuFamily.fromNativeCode(2));
        assertEquals(GpuFamily.Kind.IMMORTALIS, GpuFamily.fromNativeCode(3));
        assertEquals(GpuFamily.Kind.POWERVR, GpuFamily.fromNativeCode(4));
        assertEquals(GpuFamily.Kind.XCLIPSE, GpuFamily.fromNativeCode(5));
        assertEquals(GpuFamily.Kind.NVIDIA, GpuFamily.fromNativeCode(6));
        assertEquals(GpuFamily.Kind.APPLE, GpuFamily.fromNativeCode(7));
        assertEquals(GpuFamily.Kind.SOFTWARE, GpuFamily.fromNativeCode(8));
        assertEquals(GpuFamily.Kind.GENERIC, GpuFamily.fromNativeCode(9));
    }

    @Test
    public void anUnreadOrUnknownCodeReadsAsUnknown() {
        // -1 is the native "not read yet" sentinel; a future value must not be mistaken for one we
        // know, or the report would name the wrong GPU.
        assertEquals(GpuFamily.Kind.UNKNOWN, GpuFamily.fromNativeCode(-1));
        assertEquals(GpuFamily.Kind.UNKNOWN, GpuFamily.fromNativeCode(10));
        assertEquals(GpuFamily.Kind.UNKNOWN, GpuFamily.fromNativeCode(Integer.MIN_VALUE));
    }

    @Test
    public void onlySoftwareIsSoftware() {
        assertTrue(GpuFamily.isSoftware(GpuFamily.Kind.SOFTWARE));
        assertFalse(GpuFamily.isSoftware(GpuFamily.Kind.ADRENO));
        assertFalse(GpuFamily.isSoftware(GpuFamily.Kind.UNKNOWN));
    }

    @Test
    public void everyFamilyHasADistinctNonEmptyLabel() {
        GpuFamily.Kind[] all = GpuFamily.Kind.values();
        for (int i = 0; i < all.length; i++) {
            String label = GpuFamily.label(all[i]);
            assertTrue("label for " + all[i], label != null && !label.isEmpty());
            for (int j = i + 1; j < all.length; j++) {
                assertFalse("labels collide: " + all[i] + "/" + all[j],
                        label.equals(GpuFamily.label(all[j])));
            }
        }
    }
}
