package org.chimeramc.client.ui.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins the rail's fold rule. */
public class NavRailPolicyTest {

    @Test
    public void theExpandedRailShowsLabelsUntilFolded() {
        assertTrue(NavRailPolicy.showsLabels(NavLayoutMode.EXPANDED_RAIL, false));
        assertFalse(NavRailPolicy.showsLabels(NavLayoutMode.EXPANDED_RAIL, true));
    }

    @Test
    public void theIconOnlyRailNeverShowsLabels() {
        assertFalse(NavRailPolicy.showsLabels(NavLayoutMode.NAV_RAIL, false));
        assertFalse(NavRailPolicy.showsLabels(NavLayoutMode.NAV_RAIL, true));
    }

    @Test
    public void theBarNeverShowsRailLabels() {
        assertFalse(NavRailPolicy.showsLabels(NavLayoutMode.BOTTOM_NAV, false));
        assertFalse(NavRailPolicy.showsLabels(NavLayoutMode.BOTTOM_NAV, true));
    }

    @Test
    public void foldingAnExpandedRailNarrowsItToTheIconOnlyWidth() {
        assertEquals(NavRailPolicy.EXPANDED_WIDTH_DP,
                NavRailPolicy.railWidthDp(NavLayoutMode.EXPANDED_RAIL, false));
        assertEquals(NavRailPolicy.COLLAPSED_WIDTH_DP,
                NavRailPolicy.railWidthDp(NavLayoutMode.EXPANDED_RAIL, true));
    }

    @Test
    public void theFoldControlIsOfferedOnlyWhereItDoesSomething() {
        assertTrue(NavRailPolicy.canCollapse(NavLayoutMode.EXPANDED_RAIL));
        assertFalse(NavRailPolicy.canCollapse(NavLayoutMode.NAV_RAIL));
        assertFalse(NavRailPolicy.canCollapse(NavLayoutMode.BOTTOM_NAV));
    }
}
