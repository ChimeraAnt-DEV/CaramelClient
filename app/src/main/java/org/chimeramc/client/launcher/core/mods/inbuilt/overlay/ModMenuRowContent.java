package org.chimeramc.client.core.mods.inbuilt.overlay;

import java.util.Map;

/**
 * Whether two versions of a module row draw the same thing (item 202).
 *
 * <p>The RecyclerView diff decides, per row, whether to rebind it. That decision has to see
 * <em>everything</em> the row renders from, or a change goes unnoticed and the row stays stale on
 * screen. The three things that bit: a module the crash guard disabled (the user's toggle is
 * unchanged, so a toggle-only comparison says "same"), a module whose availability changed with the
 * build, and the favourite star.
 *
 * <p>Extracted from {@link ModMenuAdapter} so the rule is a pure function with a unit test rather
 * than a branch inside an Android adapter, where it could only be exercised on a device. The adapter
 * still owns the two lists and the diff; this owns the "same content?" answer.
 */
public final class ModMenuRowContent {

    private ModMenuRowContent() {
    }

    /**
     * The state a row is drawn with, resolved from an explicit map and the availability/enabled
     * fallback.
     *
     * @param states     the row-state map in force for the side being compared (the map the row was
     *                   bound with on the old side, the incoming map on the new side)
     * @param stableKey  the module's stable key
     * @param available  whether the module is interactive on this build
     * @param enabled    whether the user has it on
     */
    public static ModMenuRowState resolve(Map<String, ModMenuRowState> states, String stableKey,
                                          boolean available, boolean enabled) {
        ModMenuRowState explicit = states != null ? states.get(stableKey) : null;
        return explicit != null ? explicit : ModMenuRowState.of(true, available, false, enabled);
    }

    /**
     * Whether an old and a new version of a module row render identically.
     *
     * <p>Every field the card draws from is compared: the name, the caption, whether it has a
     * settings button, the compact-mode group label, the toggle, the resolved row state and the
     * favourite star. Missing any one of them is a row that does not refresh.
     */
    public static boolean sameContent(String oldName, String newName,
                                      String oldDescription, String newDescription,
                                      boolean oldHasConfig, boolean newHasConfig,
                                      boolean oldShowGroupLabel, boolean newShowGroupLabel,
                                      boolean oldEnabled, boolean newEnabled,
                                      ModMenuRowState oldState, ModMenuRowState newState,
                                      boolean oldFavorite, boolean newFavorite) {
        return java.util.Objects.equals(oldName, newName)
                && java.util.Objects.equals(oldDescription, newDescription)
                && oldHasConfig == newHasConfig
                && oldShowGroupLabel == newShowGroupLabel
                && oldEnabled == newEnabled
                && oldState == newState
                && oldFavorite == newFavorite;
    }
}
