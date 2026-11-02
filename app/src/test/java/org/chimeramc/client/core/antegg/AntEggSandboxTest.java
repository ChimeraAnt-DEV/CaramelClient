package org.chimeramc.client.core.antegg;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * Pins the sandbox-root containment rule (item 454).
 *
 * Uses a real temporary directory rather than a mock, because the rule is about canonical paths and
 * a mock cannot answer {@link File#getCanonicalPath}. The containment case (a sibling whose name
 * merely starts with the root's) is the one a naive {@code startsWith} gets wrong.
 */
public class AntEggSandboxTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void aDirectoryInsideTheRootIsAccepted() throws Exception {
        File root = folder.newFolder("files");
        File sandbox = new File(root, "AntEggs");
        assertTrue(AntEggSandbox.isInside(root, sandbox));
        assertNull(AntEggSandbox.describeRefusal(sandbox, root));
    }

    @Test
    public void theRootItselfIsAccepted() throws Exception {
        File root = folder.newFolder("files");
        assertTrue(AntEggSandbox.isInside(root, root));
    }

    @Test
    public void aSiblingWithASharedPrefixIsRefused() throws Exception {
        File root = folder.newFolder("files");
        File sibling = folder.newFolder("files2");
        assertFalse(AntEggSandbox.isInside(root, sibling));
        assertFalse(AntEggSandbox.isInside(root, new File(sibling, "AntEggs")));
    }

    @Test
    public void aParentSegmentEscapeIsRefused() throws Exception {
        File root = folder.newFolder("files");
        File escaped = new File(root, "../AntEggs");
        assertFalse(AntEggSandbox.isInside(root, escaped));
    }

    @Test
    public void aRelativeRootIsRefused() {
        File relative = new File("AntEggs");
        assertFalse(AntEggSandbox.isInside(new File("/tmp"), relative));
        assertTrue(AntEggSandbox.describeRefusal(relative, new File("/tmp")).contains("absolute"));
    }

    @Test
    public void aNullIsRefused() {
        assertFalse(AntEggSandbox.isInside(null, new File("/tmp/x")));
        assertFalse(AntEggSandbox.isInside(new File("/tmp"), null));
        assertTrue(AntEggSandbox.describeRefusal(null, new File("/tmp")).contains("sandbox"));
    }

    @Test
    public void aRootOutsideTheAllowedDirectoryIsRefused() throws Exception {
        File allowed = folder.newFolder("files");
        File outside = folder.newFolder("outside");
        String reason = AntEggSandbox.describeRefusal(outside, allowed);
        assertTrue(reason, reason.contains("private storage"));
    }

    @Test
    public void aNotYetCreatedTargetDirectoryIsValidatedLexically() throws Exception {
        File root = folder.newFolder("files");
        File missing = new File(root, "AntEggs");
        assertFalse(missing.exists());
        assertTrue(AntEggSandbox.isInside(root, missing));
        assertNull(AntEggSandbox.describeRefusal(missing, root));
    }
}
