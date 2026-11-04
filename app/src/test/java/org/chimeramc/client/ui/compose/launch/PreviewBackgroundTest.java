package org.chimeramc.client.ui.compose.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * The launch-tab background catalogue, pinned without a device.
 *
 * <p>The rule that matters is that a stored id always resolves: an unknown or absent id must fall
 * back to the default rather than crash or show nothing, and cycling must wrap. Ids are stable so a
 * reorder cannot point a saved choice at a different image.
 */
public class PreviewBackgroundTest {

    @Test
    public void everyEntryHasAUniqueStableIdAndADrawable() {
        Set<String> ids = new HashSet<>();
        for (PreviewBackground.Entry e : PreviewBackground.all()) {
            assertNotNull(e.id);
            assertFalse(e.id.isEmpty());
            assertTrue("duplicate background id: " + e.id, ids.add(e.id));
            assertTrue(e.id + " has no drawable", e.drawableRes != 0);
            assertNotNull(e.label);
        }
        assertTrue("the catalogue must not be empty", !ids.isEmpty());
    }

    @Test
    public void anUnknownOrAbsentIdFallsBackToTheDefault() {
        assertEquals(PreviewBackground.DEFAULT_ID, PreviewBackground.resolve(null).id);
        assertEquals(PreviewBackground.DEFAULT_ID, PreviewBackground.resolve("").id);
        assertEquals(PreviewBackground.DEFAULT_ID, PreviewBackground.resolve("nope").id);
        assertEquals(0, PreviewBackground.indexOf(null));
        assertEquals(0, PreviewBackground.indexOf("nope"));
    }

    @Test
    public void aKnownIdResolvesToItself() {
        for (PreviewBackground.Entry e : PreviewBackground.all()) {
            assertEquals(e, PreviewBackground.resolve(e.id));
            assertEquals(PreviewBackground.all().indexOf(e), PreviewBackground.indexOf(e.id));
        }
    }

    @Test
    public void cyclingWrapsThroughEveryBackground() {
        String id = PreviewBackground.DEFAULT_ID;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < PreviewBackground.all().size(); i++) {
            assertTrue("a cycle must not repeat before it wraps", seen.add(id));
            id = PreviewBackground.next(id);
        }
        assertEquals("a full cycle must return to the start", PreviewBackground.DEFAULT_ID, id);
        // An unknown id cycles from the default.
        assertEquals(PreviewBackground.next(PreviewBackground.DEFAULT_ID),
                PreviewBackground.next("nope"));
    }
}
