package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pins the pause-menu button row's entries (item 224).
 *
 * <p>The row and the menu it appears in must agree about what the actions are, and each must have a
 * label and an icon or it renders as a blank button.
 */
public class PauseMenuActionsTest {

    @Test
    public void theRowHasTheRequestedActions() {
        List<PauseMenuActions.Entry> entries = PauseMenuActions.all();
        assertEquals(5, entries.size());
        assertNotNull(PauseMenuActions.byId(PauseMenuActions.ACTION_SCREENSHOT));
        assertNotNull(PauseMenuActions.byId(PauseMenuActions.ACTION_HIDE_HUD));
        assertNotNull(PauseMenuActions.byId(PauseMenuActions.ACTION_SWITCH_LAYOUT));
        assertNotNull(PauseMenuActions.byId(PauseMenuActions.ACTION_OPEN_LOGS));
        assertNotNull(PauseMenuActions.byId(PauseMenuActions.ACTION_QUICK_SETTINGS));
    }

    @Test
    public void everyEntryHasALabelAndAnIcon() {
        for (PauseMenuActions.Entry entry : PauseMenuActions.all()) {
            assertTrue(entry.getId() + " has a label", entry.getLabelRes() != 0);
            assertTrue(entry.getId() + " has an icon", entry.getIconRes() != 0);
        }
    }

    @Test
    public void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (PauseMenuActions.Entry entry : PauseMenuActions.all()) {
            assertTrue("duplicate pause-menu id " + entry.getId(), ids.add(entry.getId()));
        }
    }

    @Test
    public void anUnknownIdResolvesToNullRatherThanThrowing() {
        assertNull(PauseMenuActions.byId(null));
        assertNull(PauseMenuActions.byId(""));
        assertNull(PauseMenuActions.byId("not_an_action"));
    }

    @Test
    public void theListIsUnmodifiableSoARowCannotReorderItself() {
        List<PauseMenuActions.Entry> entries = PauseMenuActions.all();
        assertFalse(entries.isEmpty());
        try {
            entries.add(entries.get(0));
            assertTrue("the entry list must be unmodifiable", false);
        } catch (UnsupportedOperationException expected) {
            // Expected: the row only reads it.
        }
    }
}
