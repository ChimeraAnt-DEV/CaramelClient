package org.chimeramc.client.preloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The launcher-side contract of BerryAPI's cosmetics surface.
 *
 * <p>The JNI half cannot run off-device (there is no sweetloader), so what this pins is the two
 * things a caller relies on and that a silent regression would break: the enum values that must
 * match BerryAPI's C ABI, and the fail-closed answers every entry point gives when the library or
 * its cosmetic ABI is absent. It also exercises the record/counter decoding against the exact
 * shapes the native layer produces, so a field shifted in the marshalling is caught here rather
 * than on a device.
 */
public class BerryCosmeticApiTest {

    @Test
    public void theSlotValuesMatchTheCAbi() {
        // BerryCosmeticSlotC: None=0, Head=1, Back=2, Shoulders=3, Body=4, Trail=5, Effect=6,
        // Emote=7. A drift here would equip into the wrong slot.
        assertEquals(0, BerryApi.SLOT_NONE);
        assertEquals(1, BerryApi.SLOT_HEAD);
        assertEquals(2, BerryApi.SLOT_BACK);
        assertEquals(3, BerryApi.SLOT_SHOULDERS);
        assertEquals(4, BerryApi.SLOT_BODY);
        assertEquals(5, BerryApi.SLOT_TRAIL);
        assertEquals(6, BerryApi.SLOT_EFFECT);
        assertEquals(7, BerryApi.SLOT_EMOTE);
    }

    @Test
    public void theStatusValuesMatchTheCAbi() {
        assertEquals(0, BerryApi.COSMETIC_OK);
        assertEquals(1, BerryApi.COSMETIC_NOT_ATTACHED);
        assertEquals(2, BerryApi.COSMETIC_BAD_ARGUMENT);
        assertEquals(3, BerryApi.COSMETIC_UNKNOWN_ASSET);
        assertEquals(4, BerryApi.COSMETIC_NOT_OWNED);
        assertEquals(5, BerryApi.COSMETIC_BAD_SLOT);
        assertEquals(6, BerryApi.COSMETIC_BAD_FORMAT);
        assertEquals(7, BerryApi.COSMETIC_UNKNOWN_PRESET);
        assertEquals(8, BerryApi.COSMETIC_BUFFER_TOO_SMALL);
        assertEquals(9, BerryApi.COSMETIC_NO_HOOK_BACKEND);
        assertEquals(10, BerryApi.COSMETIC_HOOK_REFUSED);
        assertEquals(11, BerryApi.COSMETIC_ALREADY_HOOKED);
        assertEquals(12, BerryApi.COSMETIC_BAD_READER);
    }

    @Test
    public void everyReadAnswersTheFailClosedDefaultWithoutTheLibrary() {
        // Off-device the native call throws UnsatisfiedLinkError; each entry point must swallow it
        // and return a safe default so the cosmetics screen shows "not available" instead of
        // crashing the launcher.
        assertFalse(BerryApi.cosmeticAvailable());
        assertEquals(0, BerryApi.cosmeticRegistryCount());
        assertTrue(BerryApi.cosmeticCatalog().isEmpty());
        assertEquals("", BerryApi.cosmeticEquipped(BerryApi.SLOT_HEAD));
        assertEquals(0, BerryApi.cosmeticPresetNames().length);
        assertNull(BerryApi.cosmeticStats());
        assertEquals(-1, BerryApi.cosmeticHasPlayerModelHook());
        assertEquals(-1, BerryApi.cosmeticLocalOnly());
        assertEquals(-1, BerryApi.cosmeticHasPreset("any"));
    }

    @Test
    public void everyWriteAnswersNotAttachedWithoutTheLibrary() {
        assertEquals(BerryApi.COSMETIC_NOT_ATTACHED, BerryApi.cosmeticEquip("cape_glow"));
        assertEquals(BerryApi.COSMETIC_NOT_ATTACHED, BerryApi.cosmeticUnequip(BerryApi.SLOT_BACK));
        assertEquals(BerryApi.COSMETIC_NOT_ATTACHED, BerryApi.cosmeticClearLoadout());
        assertEquals(BerryApi.COSMETIC_NOT_ATTACHED, BerryApi.cosmeticSavePreset("p"));
        assertEquals(BerryApi.COSMETIC_NOT_ATTACHED, BerryApi.cosmeticApplyPreset("p"));
        assertEquals(BerryApi.COSMETIC_NOT_ATTACHED, BerryApi.cosmeticSetLocalOnly(true));
        assertEquals(0, BerryApi.cosmeticReload(null, null));
    }

    @Test
    public void aCatalogRecordDecodesEveryField() {
        // The native catalog marshals each asset as String[20] in this exact order.
        String[] row = {
                "cape_glow", "2", "3", "100", "200",
                "0.5", "-1.25", "0", "10", "20", "30", "1.5",
                "0.9", "0.8", "0.7", "0.25", "sway", "1.2", "summer_pack", "Lynxifyy"};
        BerryApi.CosmeticRecord r = new BerryApi.CosmeticRecord(row);
        assertEquals("cape_glow", r.id);
        assertEquals(2, r.slot);
        assertEquals(3, r.bone);
        assertEquals(100L, r.geometry);
        assertEquals(200L, r.texture);
        assertEquals(0.5f, r.offsetX, 1e-6f);
        assertEquals(-1.25f, r.offsetY, 1e-6f);
        assertEquals(0f, r.offsetZ, 1e-6f);
        assertEquals(10f, r.pitch, 1e-6f);
        assertEquals(20f, r.yaw, 1e-6f);
        assertEquals(30f, r.roll, 1e-6f);
        assertEquals(1.5f, r.scale, 1e-6f);
        assertEquals(0.9f, r.tintR, 1e-6f);
        assertEquals(0.8f, r.tintG, 1e-6f);
        assertEquals(0.7f, r.tintB, 1e-6f);
        assertEquals(0.25f, r.trail, 1e-6f);
        assertEquals("sway", r.animation);
        assertEquals(1.2f, r.animationSpeed, 1e-6f);
        assertEquals("summer_pack", r.pack);
        assertEquals("Lynxifyy", r.author);
    }

    @Test
    public void aCatalogRecordFailsClosedOnMalformedNumbers() {
        String[] row = {"id", "", "x", "nan", "1", "", "", "", "", "", "", "", "", "", "", "",
                "", "", "", ""};
        BerryApi.CosmeticRecord r = new BerryApi.CosmeticRecord(row);
        assertEquals("id", r.id);
        assertEquals(0, r.slot);
        assertEquals(0, r.bone);
        assertEquals(0L, r.geometry);
        assertEquals(1L, r.texture);
    }

    @Test
    public void theCountersDecodeInOrder() {
        BerryApi.CosmeticCounters c = new BerryApi.CosmeticCounters(
                new long[] {11, 22, 33, 44, 55, 66, 7, 8});
        assertEquals(11L, c.frames);
        assertEquals(22L, c.submitted);
        assertEquals(33L, c.skippedNoBone);
        assertEquals(44L, c.skippedNotOwned);
        assertEquals(55L, c.cacheHits);
        assertEquals(66L, c.cacheMisses);
        assertEquals(7L, c.registryCount);
        assertEquals(8L, c.presetCount);
    }

    @Test
    public void theCatalogDecodesTheNativeRows() {
        // cosmeticCatalog() must be non-null even when empty, so a caller can iterate it without a
        // null check; off-device that is the empty list.
        List<BerryApi.CosmeticRecord> catalog = BerryApi.cosmeticCatalog();
        assertNotNull(catalog);
    }
}
