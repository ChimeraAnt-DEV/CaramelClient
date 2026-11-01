package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * The module row's content comparison (item 202).
 *
 * <p>Pins that the RecyclerView diff sees every field the row draws from. The failure this guards
 * against is silent: a missed field means a row that never refreshes, so it keeps showing the old
 * name, the old toggle or a healthy look after the crash guard disabled it.
 */
public class ModMenuRowContentTest {

    private static final ModMenuRowState LOADED = ModMenuRowState.LOADED;
    private static final ModMenuRowState DISABLED = ModMenuRowState.DISABLED;

    private static boolean same(ModMenuRowState oldState, ModMenuRowState newState) {
        return ModMenuRowContent.sameContent("FPS", "FPS", "desc", "desc",
                true, true, false, false, true, true, oldState, newState, false, false);
    }

    @Test
    public void anUnchangedRowIsTheSameContent() {
        assertTrue(same(LOADED, LOADED));
    }

    @Test
    public void aRowStateChangeIsADifferentContent() {
        // The user's toggle did not change, but the crash guard disabling the module must still
        // rebind the row, or it keeps its healthy look.
        assertFalse(same(LOADED, ModMenuRowState.CRASHED));
        assertFalse(same(LOADED, ModMenuRowState.UNSUPPORTED));
    }

    @Test
    public void aToggleChangeIsADifferentContent() {
        assertFalse(ModMenuRowContent.sameContent("FPS", "FPS", "d", "d",
                true, true, false, false, false, true, DISABLED, LOADED, false, false));
    }

    @Test
    public void aNameOrDescriptionChangeIsADifferentContent() {
        assertFalse(ModMenuRowContent.sameContent("FPS", "FPS Counter", "d", "d",
                true, true, false, false, true, true, LOADED, LOADED, false, false));
        assertFalse(ModMenuRowContent.sameContent("FPS", "FPS", "old", "new",
                true, true, false, false, true, true, LOADED, LOADED, false, false));
    }

    @Test
    public void aConfigOrGroupLabelChangeIsADifferentContent() {
        assertFalse(ModMenuRowContent.sameContent("FPS", "FPS", "d", "d",
                false, true, false, false, true, true, LOADED, LOADED, false, false));
        assertFalse(ModMenuRowContent.sameContent("FPS", "FPS", "d", "d",
                true, true, false, true, true, true, LOADED, LOADED, false, false));
    }

    @Test
    public void aFavouriteChangeIsADifferentContent() {
        assertFalse(ModMenuRowContent.sameContent("FPS", "FPS", "d", "d",
                true, true, false, false, true, true, LOADED, LOADED, false, true));
    }

    @Test
    public void theResolvedStateFallsBackToAvailabilityAndEnabled() {
        Map<String, ModMenuRowState> empty = new HashMap<>();
        // No explicit state: registered + available + enabled reads LOADED, and off reads DISABLED.
        assertTrue(ModMenuRowContent.resolve(empty, "k", true, true) == LOADED);
        assertTrue(ModMenuRowContent.resolve(empty, "k", true, false) == DISABLED);
        // Unavailable reads UNSUPPORTED even with no explicit entry.
        assertTrue(ModMenuRowContent.resolve(empty, "k", false, true)
                == ModMenuRowState.UNSUPPORTED);
    }

    @Test
    public void anExplicitStateWinsOverTheFallback() {
        Map<String, ModMenuRowState> states = new HashMap<>();
        states.put("k", ModMenuRowState.CRASHED);
        // Registered and enabled, but the explicit crash state wins, exactly as the bind does.
        assertTrue(ModMenuRowContent.resolve(states, "k", true, true) == ModMenuRowState.CRASHED);
    }

    @Test
    public void aNullStateMapIsTreatedAsNoExplicitState() {
        assertTrue(ModMenuRowContent.resolve(null, "k", true, false) == DISABLED);
    }
}
