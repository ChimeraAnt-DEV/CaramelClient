package org.chimeramc.client.core.versions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pins the per-instance note rules (item 278). */
public class InstanceNotesTest {

    @Test
    public void aPlainNoteIsKeptTrimmed() {
        assertEquals("PvP build", InstanceNotes.normalize("  PvP build  "));
    }

    @Test
    public void blankBecomesNullNotAnEmptyString() {
        assertNull(InstanceNotes.normalize(""));
        assertNull(InstanceNotes.normalize("   "));
        assertNull(InstanceNotes.normalize("\n\t\r"));
        assertNull(InstanceNotes.normalize(null));
    }

    @Test
    public void newlinesAndTabsBecomeSingleSpaces() {
        assertEquals("keep shaders off", InstanceNotes.normalize("keep\nshaders\toff"));
    }

    @Test
    public void controlCharactersAreDropped() {
        assertEquals("ab", InstanceNotes.normalize("a\u0000\u0007b"));
    }

    @Test
    public void theLengthIsCapped() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < InstanceNotes.MAX_LENGTH + 100; i++) sb.append('x');
        String out = InstanceNotes.normalize(sb.toString());
        assertEquals(InstanceNotes.MAX_LENGTH, out.length());
    }

    @Test
    public void aCappedNoteIsStillTrimmed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < InstanceNotes.MAX_LENGTH + 50; i++) sb.append('x');
        sb.append("   ");
        String out = InstanceNotes.normalize(sb.toString());
        assertFalse(out.endsWith(" "));
    }

    @Test
    public void presenceFollowsNormalisation() {
        assertTrue(InstanceNotes.isPresent("note"));
        assertFalse(InstanceNotes.isPresent("   "));
        assertFalse(InstanceNotes.isPresent(null));
    }

    @Test
    public void normalisationIsIdempotent() {
        String once = InstanceNotes.normalize("  a\nb  ");
        assertEquals(once, InstanceNotes.normalize(once));
    }
}
