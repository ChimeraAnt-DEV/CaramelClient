package org.chimeramc.client.ui.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The logcat overlay's level-filter chips and pass rule (item 209). */
public class LogcatLevelFilterTest {

    @Test
    public void theChipOrderMatchesTheLevels() {
        // Chip 0 is ALL, then one chip per level in Android's own severity order.
        assertEquals(LogcatLevelFilter.ALL, LogcatLevelFilter.levelAt(0));
        assertEquals('V', LogcatLevelFilter.levelAt(1));
        assertEquals('F', LogcatLevelFilter.levelAt(6));
        assertEquals(LogcatLevelFilter.ALL, LogcatLevelFilter.levelAt(7));
        assertEquals(LogcatLevelFilter.ALL, LogcatLevelFilter.levelAt(-1));
    }

    @Test
    public void theChipIndexRoundTrips() {
        for (int i = 0; i <= LogcatLevelFilter.LEVELS.length; i++) {
            assertEquals(i, LogcatLevelFilter.indexOf(LogcatLevelFilter.levelAt(i)));
        }
        assertEquals(0, LogcatLevelFilter.indexOf('Z'));
        assertEquals(0, LogcatLevelFilter.indexOf(LogcatLevelFilter.ALL));
    }

    @Test
    public void allLetsEveryLevelThrough() {
        assertTrue(LogcatLevelFilter.passes(LogcatLevelFilter.ALL, 'V'));
        assertTrue(LogcatLevelFilter.passes(LogcatLevelFilter.ALL, 'E'));
        assertTrue(LogcatLevelFilter.passes(LogcatLevelFilter.ALL, LogcatLevelFilter.UNKNOWN));
    }

    @Test
    public void aSpecificLevelOnlyLetsItsOwnLinesThrough() {
        assertTrue(LogcatLevelFilter.passes('E', 'E'));
        assertFalse(LogcatLevelFilter.passes('E', 'W'));
    }

    @Test
    public void anUnclassifiedLineIsHiddenByASpecificFilter() {
        // A line the parser could not classify must not be claimed to be the level asked for.
        assertFalse(LogcatLevelFilter.passes('E', LogcatLevelFilter.UNKNOWN));
    }
}
