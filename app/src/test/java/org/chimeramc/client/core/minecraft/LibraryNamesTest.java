package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the library-name rule (item 301).
 *
 * <p>The extraction cache, the loader and the repair path all normalise through this class. A drift
 * here is silent: the cache check would stop finding its own files and re-extract on every launch,
 * or the loader would double-prefix an already-complete name.
 */
public class LibraryNamesTest {

    @Test
    public void aShortNameBecomesAFileName() {
        assertEquals("libc++_shared.so", LibraryNames.fileName("c++_shared"));
        assertEquals("libminecraftpe.so", LibraryNames.fileName("minecraftpe"));
        assertEquals("libfmod.so", LibraryNames.fileName("fmod"));
    }

    @Test
    public void anAlreadyCompleteNameIsUnchanged() {
        assertEquals("libc++_shared.so", LibraryNames.fileName("libc++_shared.so"));
        assertEquals("libmaesdk.so", LibraryNames.fileName("libmaesdk.so"));
    }

    @Test
    public void normaliseStripsTheWrapper() {
        assertEquals("c++_shared", LibraryNames.normalize("libc++_shared.so"));
        assertEquals("minecraftpe", LibraryNames.normalize("libminecraftpe.so"));
    }

    @Test
    public void normaliseOfAShortNameIsTheNameItself() {
        assertEquals("c++_shared", LibraryNames.normalize("c++_shared"));
    }

    @Test
    public void theRoundTripIsStable() {
        for (String name : new String[]{"c++_shared", "minecraftpe", "fmod", "maesdk"}) {
            assertEquals(name, LibraryNames.normalize(LibraryNames.fileName(name)));
        }
    }

    @Test
    public void completeFileNameDetection() {
        assertTrue(LibraryNames.isCompleteFileName("libc++_shared.so"));
        assertFalse(LibraryNames.isCompleteFileName("c++_shared"));
        assertFalse(LibraryNames.isCompleteFileName("libc++_shared"));
        assertFalse(LibraryNames.isCompleteFileName(null));
    }

    @Test
    public void nullAndEmptyAreSafe() {
        assertEquals("lib.so", LibraryNames.fileName(null));
        assertEquals("lib.so", LibraryNames.fileName(""));
        assertEquals("", LibraryNames.normalize(null));
    }
}
