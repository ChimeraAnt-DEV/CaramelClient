package org.chimeramc.client.core.mods.inbuilt.overlay;

import org.chimeramc.client.core.mods.inbuilt.UnifiedMod;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a module list into favorites and the rest, each sorted by name.
 *
 * <p>Extracted from {@code ModMenuOverlay} as a top-level type: the grouping rule is pure (it reads
 * only a mod's favorite flag and name) and had no reason to be a nested detail of a 3,400-line
 * class. Kept package-private because only the menu uses it.
 *
 * <p>The package is {@code ...core.mods.inbuilt.overlay} to match the sibling sources in this
 * directory, not the {@code ...launcher.core...} path the file sits under.
 */
final class GroupedMods {

    private final List<UnifiedMod> favorites = new ArrayList<>();
    private final List<UnifiedMod> others = new ArrayList<>();

    void add(UnifiedMod mod, boolean favorite) {
        if (favorite) {
            favorites.add(mod);
        } else {
            others.add(mod);
        }
    }

    void appendTo(List<UnifiedMod> target) {
        sort(favorites);
        sort(others);
        target.addAll(favorites);
        target.addAll(others);
    }

    private void sort(List<UnifiedMod> mods) {
        mods.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
    }
}
