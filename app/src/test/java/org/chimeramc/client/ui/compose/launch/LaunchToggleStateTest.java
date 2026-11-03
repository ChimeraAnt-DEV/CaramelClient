package org.chimeramc.client.ui.compose.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.List;

/**
 * The Launch tab's quick-toggle row, pinned without a device.
 *
 * <p>The visibility rule is the part that can silently regress: guest mode hides the Mods (AntEgg)
 * and Voice cards, and Replays always stays because it is a local capture rather than a social
 * surface.
 */
public class LaunchToggleStateTest {

    private static List<LaunchToggleState.Toggle> toggles(boolean guest) {
        return LaunchToggleState.visible(guest,
                "Mod packs", "3 packs", "Voice", "LAN", "Replays", "2 clips");
    }

    @Test
    public void theDefaultRowShowsAllThreeInOrder() {
        List<LaunchToggleState.Toggle> row = toggles(false);
        assertEquals(3, row.size());
        assertEquals(LaunchToggleState.Kind.ANTEGG, row.get(0).kind);
        assertEquals(LaunchToggleState.Kind.VOICE, row.get(1).kind);
        assertEquals(LaunchToggleState.Kind.REPLAY, row.get(2).kind);
    }

    @Test
    public void guestModeHidesModsAndVoiceButKeepsReplays() {
        List<LaunchToggleState.Toggle> row = toggles(true);
        assertEquals(1, row.size());
        assertEquals(LaunchToggleState.Kind.REPLAY, row.get(0).kind);
        for (LaunchToggleState.Toggle t : row) {
            assertFalse("guest mode must not show the Mods or Voice card",
                    t.kind == LaunchToggleState.Kind.ANTEGG || t.kind == LaunchToggleState.Kind.VOICE);
        }
    }

    @Test
    public void theStatusesAreCarriedThroughUnchanged() {
        List<LaunchToggleState.Toggle> row = toggles(false);
        assertEquals("3 packs", row.get(0).status);
        assertEquals("LAN", row.get(1).status);
        assertEquals("2 clips", row.get(2).status);
        assertEquals("Mod packs", row.get(0).label);
    }
}
