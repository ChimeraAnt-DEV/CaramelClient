package org.chimeramc.client.core.content;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The rule for ordering an instance's active resource packs (item 253).
 *
 * <p>Bedrock applies active packs bottom-up: the pack <em>last</em> in
 * {@code global_resource_packs.json} wins a conflict. The order in that array is therefore the
 * user-visible priority, and reordering it is what lets a player say "use this pack's textures
 * over that one" without editing JSON by hand.
 *
 * <p>Pure and Android-free so the reorder is unit-tested: a move that drops or duplicates an id
 * would silently change which pack wins, which is exactly the kind of bug that is invisible until
 * a texture looks wrong in game.
 */
public final class ResourcePackOrder {

    private ResourcePackOrder() {
    }

    /**
     * Moves the entry at {@code from} to {@code to}, returning the new order.
     *
     * <p>An out-of-range index returns the list unchanged rather than clamping: a stale drag index
     * from a list that changed under the user must not move an unrelated pack to the top.
     */
    public static List<String> move(List<String> uuids, int from, int to) {
        List<String> result = new ArrayList<>();
        if (uuids != null) result.addAll(uuids);
        if (from < 0 || to < 0 || from >= result.size() || to >= result.size() || from == to) {
            return result;
        }
        String moved = result.remove(from);
        result.add(to, moved);
        return result;
    }

    /**
     * Reorders the active ids to match {@code savedOrder}, appending any active id the saved order
     * does not mention.
     *
     * <p>This is what a persisted order is applied through. A saved order that predates a newly
     * installed pack must not hide that pack: an unmentioned id is kept, at the end, rather than
     * dropped. Duplicate ids in the saved order are collapsed so the same pack cannot appear twice
     * in the written list (the game would match it once and ignore the second entry).
     */
    public static List<String> apply(List<String> active, List<String> savedOrder) {
        List<String> result = new ArrayList<>();
        if (active == null || active.isEmpty()) return result;
        Set<String> activeSet = new LinkedHashSet<>();
        for (String id : active) {
            if (id != null && !id.trim().isEmpty()) activeSet.add(normalise(id));
        }
        Set<String> placed = new LinkedHashSet<>();
        if (savedOrder != null) {
            for (String id : savedOrder) {
                if (id == null) continue;
                String key = normalise(id);
                if (activeSet.contains(key) && placed.add(key)) {
                    result.add(key);
                }
            }
        }
        for (String id : activeSet) {
            if (placed.add(id)) result.add(id);
        }
        return result;
    }

    /** Normalises a uuid the same way the pack list writer does: trimmed and lower-cased. */
    public static String normalise(String id) {
        return id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
