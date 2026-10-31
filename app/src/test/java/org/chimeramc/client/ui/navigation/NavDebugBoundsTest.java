package org.chimeramc.client.ui.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the debug layout readout (item 80).
 *
 * <p>Every navigation defect fixed so far - the rail row padded by the rail's own width, the header
 * centring a pair, the wordmark on a bar with no room for it - presents the same way: a view that is
 * visible, asked for a size, and laid out with less than that (or none at all). This is the check
 * that names it, so it is asserted directly rather than trusted to the drawing.
 */
public class NavDebugBoundsTest {

    @Test
    public void aVisibleEntryWithNoWidthIsFlagged() {
        NavDebugBounds.Entry entry = new NavDebugBounds.Entry("tab", 0, 0, 0, 48, 72, true);
        assertTrue("a visible row laid out at zero width is the clipping bug", 
                NavDebugBounds.looksClipped(entry));
    }

    @Test
    public void anEntryNarrowerThanItMeasuredIsFlagged() {
        // This is exactly the rail padding bug: the view asked for 72dp and the parent gave it less
        // than it needed, so its icon and label drew outside it.
        NavDebugBounds.Entry entry = new NavDebugBounds.Entry("tab", 0, 0, 40, 48, 72, true);
        assertTrue(NavDebugBounds.looksClipped(entry));
    }

    @Test
    public void anEntryThatGotItsWidthIsNotFlagged() {
        NavDebugBounds.Entry entry = new NavDebugBounds.Entry("tab", 0, 0, 72, 48, 72, true);
        assertFalse(NavDebugBounds.looksClipped(entry));
        // Wider than measured (a match_parent row) is also fine.
        assertFalse(NavDebugBounds.looksClipped(
                new NavDebugBounds.Entry("row", 0, 0, 200, 48, 72, true)));
    }

    @Test
    public void aGoneEntryIsNeverFlagged() {
        // A hidden wordmark has no width by design; flagging it would train the reader to ignore
        // the flag.
        assertFalse(NavDebugBounds.looksClipped(
                new NavDebugBounds.Entry("nav_app_name", 0, 0, 0, 0, 0, false)));
    }

    @Test
    public void anEntryWithNoMeasurementIsOnlyFlaggedForZeroWidth() {
        // Before the first layout a view has neither bounds nor a measurement; only a genuinely
        // zero-width visible entry is suspicious.
        assertFalse(NavDebugBounds.looksClipped(
                new NavDebugBounds.Entry("tab", 0, 0, 30, 48, 0, true)));
        assertTrue(NavDebugBounds.looksClipped(
                new NavDebugBounds.Entry("tab", 0, 0, 0, 48, 0, true)));
    }

    @Test
    public void nullIsHandled() {
        assertFalse(NavDebugBounds.looksClipped(null));
        assertEquals(0, NavDebugBounds.clippedCount(null));
        assertEquals("(no nav entries)", NavDebugBounds.describe(null));
        assertEquals("(no nav entries)", NavDebugBounds.describe(new NavDebugBounds.Entry[0]));
    }

    @Test
    public void theDescriptionMarksOnlyTheClippedEntry() {
        NavDebugBounds.Entry[] entries = new NavDebugBounds.Entry[]{
                new NavDebugBounds.Entry("nav_tab_launch", 0, 0, 72, 48, 72, true),
                new NavDebugBounds.Entry("nav_app_name", 80, 0, 80, 48, 120, true),
                new NavDebugBounds.Entry("nav_logo", 200, 0, 228, 28, 28, true),
        };
        String text = NavDebugBounds.describe(entries);
        String[] lines = text.split("\n");
        assertEquals(3, lines.length);
        assertFalse(lines[0].contains("CLIPPED"));
        assertTrue("the squeezed wordmark must be the flagged line", lines[1].contains("CLIPPED"));
        assertFalse(lines[2].contains("CLIPPED"));
        assertEquals(1, NavDebugBounds.clippedCount(entries));
        // The width is reported both as laid out and as measured, so a report says what happened.
        assertTrue(lines[1].contains("(0px of 120)"));
    }

    @Test
    public void aHiddenEntryIsMarkedInTheDescriptionButNotAsClipped() {
        String text = NavDebugBounds.describe(new NavDebugBounds.Entry[]{
                new NavDebugBounds.Entry("nav_app_name", 0, 0, 0, 0, 0, false)});
        assertTrue(text.contains("[gone]"));
        assertFalse(text.contains("CLIPPED"));
    }
}
