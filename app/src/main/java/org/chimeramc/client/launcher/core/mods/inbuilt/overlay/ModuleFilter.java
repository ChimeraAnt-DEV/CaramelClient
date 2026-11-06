package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The section filters the Mod Menu offers, and that callers outside it may ask for.
 *
 * <p>Extracted from {@code ModMenuOverlay} as a top-level type so the menu's filter set is a named,
 * independently referenceable contract rather than a nested detail of a 3,500-line class. The radial
 * wheel's sectors and the menu's filters must name the same set — {@code RadialWheelRoutingTest} pins
 * that — and sharing one type is what makes that structural instead of a convention.
 *
 * <p>The package is {@code ...core.mods.inbuilt.overlay} (not the {@code ...launcher.core...} path
 * this file sits under) because that is the package the 157 sibling sources in this directory
 * declare. A file placed by path with the path-derived package is not a package-mate of them and
 * cannot see them.
 */
public enum ModuleFilter {
    ALL,
    FAVORITES,
    ENABLED,
    INBUILT,
    EXTERNAL,
    PVP,
    // The category split the VIP window used to own. With one menu (U4) they are filters of this
    // menu, and their predicates come from the tested ModCategory rather than a second
    // implementation.
    VISUALS,
    MISC,
    // The HUD section. Its predicate is ModIds.isHudModule, so the radial wheel's HUD sector and
    // this filter name the same set and cannot drift apart.
    HUD
}
