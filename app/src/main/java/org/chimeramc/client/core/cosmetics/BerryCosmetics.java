package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.preloader.BerryApi;
import org.chimeramc.client.util.AppLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridges the launcher's equipped set into BerryAPI's cosmetics service.
 *
 * <p>BerryAPI owns the cosmetics runtime (the catalog, the local loadout, the wardrobe presets).
 * This is the one place the launcher pushes its selection across: it clears the local loadout and
 * equips whatever the player has chosen, so the two stores cannot drift. The {@code equip} call
 * carries an asset id only — BerryAPI's registry resolves the slot — so a launcher catalogue id
 * that BerryAPI's packs do not register is refused by name rather than equipped into a guessed
 * slot.
 *
 * <p><b>Fail-closed.</b> When the library or its cosmetic ABI is absent every call answers the safe
 * default and this class does nothing; the existing native render path and the preview are
 * unaffected. Nothing here throws.
 */
public final class BerryCosmetics {

    private BerryCosmetics() {
    }

    /** True when BerryAPI's cosmetics surface resolved and passed its ABI check. */
    public static boolean available() {
        return BerryApi.cosmeticAvailable();
    }

    /**
     * The asset ids to equip for a selection, in equip order, skipping the empty selection.
     *
     * <p>Pure, so the filtering rule (a {@code null} or {@link CosmeticCatalog#NONE} piece is not
     * equipped) is unit-tested rather than buried in the native call.
     */
    public static List<String> equipIds(String capeId, String accessoryId, String petId) {
        List<String> ids = new ArrayList<>(3);
        addIfEquipped(ids, capeId);
        addIfEquipped(ids, accessoryId);
        addIfEquipped(ids, petId);
        return ids;
    }

    private static void addIfEquipped(List<String> ids, String id) {
        if (id != null && !id.isEmpty() && !CosmeticCatalog.NONE.equals(id)) ids.add(id);
    }

    /**
     * Pushes the player's equipped set into BerryAPI.
     *
     * <p>Clears the local loadout first, then equips each chosen piece, so a piece the player
     * removed does not linger. Idempotent and safe to call on every equip and at session start.
     */
    public static void syncEquipped(CosmeticStore store) {
        if (store == null) return;
        syncEquipped(store.getEquippedCapeId(), store.getEquippedAccessoryId(),
                store.getEquippedPetId());
    }

    /** As {@link #syncEquipped(CosmeticStore)}, from the raw ids. */
    public static void syncEquipped(String capeId, String accessoryId, String petId) {
        if (!available()) return;
        try {
            BerryApi.cosmeticClearLoadout();
            for (String id : equipIds(capeId, accessoryId, petId)) {
                int status = BerryApi.cosmeticEquip(id);
                if (status != BerryApi.COSMETIC_OK) {
                    AppLog.warnOnce("GlowberryClient", "berry:equip:" + id,
                            "BerryAPI refused cosmetic '" + id + "' (status " + status + ")", null);
                }
            }
        } catch (Throwable t) {
            AppLog.warnOnce("GlowberryClient", "berry:cosmetic-sync",
                    "BerryAPI cosmetics sync failed", t);
        }
    }

    /** A one-line status for the diagnostics page; never empty. */
    public static String summary() {
        if (!available()) return "BerryAPI cosmetics: not available";
        int count = BerryApi.cosmeticRegistryCount();
        BerryApi.CosmeticCounters stats = BerryApi.cosmeticStats();
        String presets = stats != null ? (" presets " + stats.presetCount) : "";
        return "BerryAPI cosmetics: " + count + " asset(s)" + presets
                + (BerryApi.cosmeticHasPlayerModelHook() == 1 ? ", bone hook live" : ", no bone hook");
    }
}
