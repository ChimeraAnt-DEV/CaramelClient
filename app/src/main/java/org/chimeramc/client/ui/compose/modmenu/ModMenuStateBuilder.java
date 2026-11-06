package org.chimeramc.client.ui.compose.modmenu;

import org.chimeramc.client.core.mods.inbuilt.UnifiedMod;
import org.chimeramc.client.core.mods.inbuilt.model.ModAvailability;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps the launcher's mod model onto the Compose Mod Menu's immutable state.
 *
 * <p>The overlay owns the {@link UnifiedMod} list and the persistence; this is the one place that
 * knows how a mod becomes a row, so the Compose UI never touches a {@code UnifiedMod} and the
 * mapping (including the availability badge and the favorite flag) is unit-testable on the JVM.
 */
public final class ModMenuStateBuilder {

    private ModMenuStateBuilder() {
    }

    /** Builds one row from a mod. Pure: it reads the mod and the favorites set, nothing else. */
    public static ModRow toRow(UnifiedMod mod, boolean favorite) {
        List<ModConfigRow> configs = new ArrayList<>();
        for (UnifiedMod.ConfigEntry entry : mod.getConfigEntries()) {
            configs.add(new ModConfigRow(
                    entry.key,
                    entry.displayName == null ? entry.key : entry.displayName,
                    mod.getConfigValue(entry.key, entry.defaultValue == null ? "" : entry.defaultValue),
                    entry.type == UnifiedMod.ConfigType.TOGGLE));
        }
        return new ModRow(
                mod.getStableKey(),
                mod.getName() == null ? "" : mod.getName(),
                mod.getDescription() == null ? "" : mod.getDescription(),
                mod.getGroupName() == null ? "" : mod.getGroupName(),
                mod.isEnabled(),
                favorite,
                mod.hasConfig(),
                ModAvailability.unavailableReason(mod.getId()),
                configs);
    }

    /**
     * Builds the whole state from the mod list and the pinned keys.
     *
     * @param mods         the mods to show, in the order the list should show them
     * @param favoriteKeys the pinned stable keys
     * @param destination  the destination to open on
     */
    public static ModMenuState build(List<UnifiedMod> mods,
                                     java.util.Set<String> favoriteKeys,
                                     ModMenuDestination destination) {
        List<ModRow> rows = new ArrayList<>();
        if (mods != null) {
            for (UnifiedMod mod : mods) {
                if (mod == null) continue;
                boolean favorite = favoriteKeys != null && favoriteKeys.contains(mod.getStableKey());
                rows.add(toRow(mod, favorite));
            }
        }
        return new ModMenuState(
                destination,
                "",
                rows,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                "",
                false,
                0,
                25);
    }

    /**
     * Flips a module's enabled state and returns the new value, so the caller can persist it.
     *
     * <p>The overlay keeps its own persistence, so this only drives the model; a module that cannot
     * run in this build is refused here as well as in the UI, so a caller that bypasses the switch
     * still cannot enable something that would do nothing.
     */
    public static boolean toggle(UnifiedMod mod, boolean enabled) {
        if (mod == null) return false;
        if (enabled && ModAvailability.isUnavailable(mod.getId())) {
            return mod.isEnabled();
        }
        mod.applyEnabled(enabled);
        return enabled;
    }

    /** The row key for a module, for a lookup by the stable key the UI passes back. */
    public static boolean matchesKey(UnifiedMod mod, String stableKey) {
        return mod != null && stableKey != null
                && stableKey.equalsIgnoreCase(mod.getStableKey());
    }

    /** A lowercase search key for a mod, matching what the Compose filter searches. */
    public static String searchKey(UnifiedMod mod) {
        if (mod == null) return "";
        return (mod.getName() + " " + mod.getDescription() + " " + mod.getGroupName())
                .toLowerCase(Locale.ROOT);
    }
}
