package org.chimeramc.client.launcher.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The per-device profile key. Pure, so the identity rules are pinned without a pad.
 */
public class ControllerDeviceKeyTest {

    private static final int XBOX_VENDOR = 0x045E;
    private static final int XBOX_PRODUCT = 0x02FD;
    private static final int OTHER_PRODUCT = 0x02E0;

    @Test
    public void aDeviceWithBothIdsGetsAKey() {
        ControllerDeviceKey key = ControllerDeviceKey.of(XBOX_VENDOR, XBOX_PRODUCT);
        assertEquals(XBOX_VENDOR, key.vendorId());
        assertEquals(XBOX_PRODUCT, key.productId());
    }

    @Test
    public void aMissingVendorIdYieldsNoKey() {
        // Collapsing every unidentified pad onto one binding would let a user edit one pad's
        // profile and silently change another's, so an unknown id gets no key at all.
        assertNull(ControllerDeviceKey.of(ControllerDeviceKey.UNKNOWN, XBOX_PRODUCT));
        assertNull(ControllerDeviceKey.of(-5, XBOX_PRODUCT));
    }

    @Test
    public void aMissingProductIdYieldsNoKey() {
        assertNull(ControllerDeviceKey.of(XBOX_VENDOR, ControllerDeviceKey.UNKNOWN));
        assertNull(ControllerDeviceKey.of(XBOX_VENDOR, -5));
    }

    @Test
    public void twoModelsOfOneVendorGetDifferentKeys() {
        ControllerDeviceKey a = ControllerDeviceKey.of(XBOX_VENDOR, XBOX_PRODUCT);
        ControllerDeviceKey b = ControllerDeviceKey.of(XBOX_VENDOR, OTHER_PRODUCT);
        assertNotEquals(a, b);
        assertNotEquals(a.storageKey(), b.storageKey());
    }

    @Test
    public void theSameModelGetsAnEqualKey() {
        assertEquals(ControllerDeviceKey.of(XBOX_VENDOR, XBOX_PRODUCT),
                ControllerDeviceKey.of(XBOX_VENDOR, XBOX_PRODUCT));
        assertEquals(ControllerDeviceKey.of(XBOX_VENDOR, XBOX_PRODUCT).hashCode(),
                ControllerDeviceKey.of(XBOX_VENDOR, XBOX_PRODUCT).hashCode());
    }

    @Test
    public void theStorageKeyIsStableAndSeparatorFree() {
        ControllerDeviceKey key = ControllerDeviceKey.of(0x054C, 0x05C4);
        assertEquals("54c_5c4", key.storageKey());
        assertEquals(key.storageKey(), ControllerDeviceKey.of(0x054C, 0x05C4).storageKey());
    }

    @Test
    public void theStorageKeyDoesNotCollideAcrossVendorProductSwaps() {
        // Without a separator "1" + "23" and "12" + "3" would produce the same token.
        assertNotEquals(ControllerDeviceKey.of(0x1, 0x23).storageKey(),
                ControllerDeviceKey.of(0x12, 0x3).storageKey());
    }

    @Test
    public void aKeyIsNotEqualToNullOrAnotherType() {
        ControllerDeviceKey key = ControllerDeviceKey.of(XBOX_VENDOR, XBOX_PRODUCT);
        assertFalse(key.equals(null));
        assertFalse(key.equals("045e_2fd"));
        assertTrue(key.equals(key));
    }
}
