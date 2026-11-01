package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

/** Pins the private-directory rule for loading native code (item 512). */
public class NativeLoadPolicyTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void aLibraryInsideTheRootIsAllowed() throws IOException {
        File root = folder.newFolder("private");
        File lib = new File(root, "libmod.so");
        assertTrue(NativeLoadPolicy.isWithinAllowedRoots(lib, root));
        assertNull(NativeLoadPolicy.describeRefusal(lib, root));
    }

    @Test
    public void aLibraryOutsideEveryRootIsRefused() throws IOException {
        File root = folder.newFolder("private");
        File elsewhere = folder.newFolder("shared");
        File lib = new File(elsewhere, "libmod.so");
        assertFalse(NativeLoadPolicy.isWithinAllowedRoots(lib, root));
        assertNotNull(NativeLoadPolicy.describeRefusal(lib, root));
    }

    @Test
    public void aSiblingDirectorySharingAPrefixIsRefused() throws IOException {
        File root = folder.newFolder("private");
        // "private-other" starts with "private" as a string but is not inside it; the separator
        // check is what stops that false positive.
        File sibling = folder.newFolder("private-other");
        File lib = new File(sibling, "libmod.so");
        assertFalse(NativeLoadPolicy.isWithinAllowedRoots(lib, root));
    }

    @Test
    public void aDotDotEscapeIsRefused() throws IOException {
        File root = folder.newFolder("private");
        File lib = new File(root, "../outside.so");
        assertFalse(NativeLoadPolicy.isWithinAllowedRoots(lib, root));
    }

    @Test
    public void aNullPathIsRefused() throws IOException {
        File root = folder.newFolder("private");
        assertFalse(NativeLoadPolicy.isWithinAllowedRoots(null, root));
        assertNotNull(NativeLoadPolicy.describeRefusal(null, root));
    }

    @Test
    public void noRootsIsRefused() throws IOException {
        File lib = folder.newFile("libmod.so");
        assertFalse(NativeLoadPolicy.isWithinAllowedRoots(lib));
        assertNotNull(NativeLoadPolicy.describeRefusal(lib));
    }

    @Test
    public void anyOfSeveralRootsIsEnough() throws IOException {
        File internal = folder.newFolder("internal");
        File external = folder.newFolder("external");
        File lib = new File(external, "mods/libmod.so");
        assertTrue(NativeLoadPolicy.isWithinAllowedRoots(lib, internal, external));
    }
}
