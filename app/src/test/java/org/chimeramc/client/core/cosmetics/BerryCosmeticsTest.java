package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * The launcher-side rule that maps the equipped set into BerryAPI's cosmetics loadout.
 *
 * <p>The native call cannot run off-device, so what this pins is the pure filtering rule and the
 * fail-closed contract: an empty selection equips nothing, and without the library the sync is a
 * no-op that never throws.
 */
public class BerryCosmeticsTest {

    @Test
    public void onlyEquippedPiecesArePushedInOrder() {
        List<String> ids = BerryCosmetics.equipIds("cape_glow", "hat_crown", "pet_fox");
        assertEquals(Arrays.asList("cape_glow", "hat_crown", "pet_fox"), ids);
    }

    @Test
    public void anEmptyOrNoneSelectionIsNotEquipped() {
        assertEquals(0, BerryCosmetics.equipIds(null, null, null).size());
        assertEquals(0, BerryCosmetics.equipIds("", "", "").size());
        assertEquals(0, BerryCosmetics.equipIds(CosmeticCatalog.NONE, CosmeticCatalog.NONE,
                CosmeticCatalog.NONE).size());
    }

    @Test
    public void aPartialSelectionKeepsOnlyTheChosenPieces() {
        assertEquals(Arrays.asList("cape_glow"),
                BerryCosmetics.equipIds("cape_glow", CosmeticCatalog.NONE, null));
        assertEquals(Arrays.asList("pet_fox"),
                BerryCosmetics.equipIds(CosmeticCatalog.NONE, "", "pet_fox"));
    }

    @Test
    public void theSurfaceIsNotAvailableWithoutTheLibrary() {
        // Off-device the native call throws UnsatisfiedLinkError; available() must swallow it.
        assertFalse(BerryCosmetics.available());
    }

    @Test
    public void syncingWithoutTheLibraryIsANoOpThatNeverThrows() {
        BerryCosmetics.syncEquipped("cape_glow", "hat_crown", "pet_fox");
    }

    @Test
    public void theSummaryIsNeverEmpty() {
        String summary = BerryCosmetics.summary();
        assertTrue(summary != null && !summary.isEmpty());
        assertTrue(summary.toLowerCase().contains("berryapi"));
    }
}
