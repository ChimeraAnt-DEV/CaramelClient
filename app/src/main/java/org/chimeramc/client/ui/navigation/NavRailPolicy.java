package org.chimeramc.client.ui.navigation;

/**
 * Whether the navigation rail shows its labels, and how wide it is, given the window mode and the
 * user's collapse choice.
 *
 * <p>The rail's width comes from a resource qualifier ({@code values-sw840dp}), which is a
 * compile-time decision and cannot express "the user folded the rail on this tablet". This class is
 * the runtime answer: the expanded rail shows labels unless the user collapsed it, and a collapsed
 * rail takes the icon-only width so the content gains the difference. Pure and Android-free, so the
 * rule is unit-tested without a device.
 */
public final class NavRailPolicy {

    /** The icon-only rail width in dp, matching {@code values/dimens.xml}'s {@code nav_rail_width}. */
    public static final int COLLAPSED_WIDTH_DP = 72;
    /** The labelled rail width in dp, matching {@code values-sw840dp/dimens.xml}. */
    public static final int EXPANDED_WIDTH_DP = 184;

    private NavRailPolicy() {
    }

    /**
     * Whether the rail at {@code mode} shows its labels.
     *
     * <p>A bar never shows all labels (it is horizontal), so this is false for {@link
     * NavLayoutMode#BOTTOM_NAV}. The icon-only rail never shows labels. Only the expanded rail can,
     * and the user can fold it.
     */
    public static boolean showsLabels(NavLayoutMode mode, boolean collapsed) {
        if (mode != NavLayoutMode.EXPANDED_RAIL) return false;
        return !collapsed;
    }

    /**
     * The rail's width in dp for {@code mode} and {@code collapsed}.
     *
     * <p>A collapsed rail always takes the icon-only width; a bar has no rail width (returns the
     * collapsed value as a harmless default the caller ignores).
     */
    public static int railWidthDp(NavLayoutMode mode, boolean collapsed) {
        if (mode == NavLayoutMode.EXPANDED_RAIL && !collapsed) return EXPANDED_WIDTH_DP;
        return COLLAPSED_WIDTH_DP;
    }

    /**
     * Whether a collapse/expand control should be offered at all.
     *
     * <p>Only the expanded rail has labels to fold; offering the control on a bar or an icon-only
     * rail would be a button that does nothing.
     */
    public static boolean canCollapse(NavLayoutMode mode) {
        return mode == NavLayoutMode.EXPANDED_RAIL;
    }
}
