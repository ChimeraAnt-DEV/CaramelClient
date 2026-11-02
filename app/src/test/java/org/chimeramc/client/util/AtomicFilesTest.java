package org.chimeramc.client.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * A write must never leave a half-written file behind (item 302).
 *
 * <p>These drive the real helper against a real filesystem - no mocks - because the property being
 * tested is a filesystem property: a reader either sees the whole old file or the whole new one.
 */
public class AtomicFilesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void replacesExistingContentWholesale() throws IOException {
        File target = folder.newFile("manifest");
        Files.write(target.toPath(), "OLD".getBytes(StandardCharsets.UTF_8));

        assertTrue(AtomicFiles.writeText(target, "NEW-CONTENT"));

        assertEquals("NEW-CONTENT", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void createsTheFileAndItsParentDirectory() {
        File target = new File(new File(folder.getRoot(), "a/b/c"), "marker");

        assertTrue(AtomicFiles.writeText(target, "v7"));

        assertTrue(target.isFile());
    }

    @Test
    public void aFailedWriteLeavesThePreviousContentIntact() throws IOException {
        File target = folder.newFile("settings.json");
        Files.write(target.toPath(), "GOOD".getBytes(StandardCharsets.UTF_8));

        // Block the temp file: a non-empty directory cannot be deleted, so the helper fails
        // before it ever touches the target. This is the whole point of the helper - a
        // truncate-then-write would have destroyed GOOD before discovering the same failure.
        File blocker = new File(target.getAbsolutePath() + AtomicFiles.TEMP_SUFFIX);
        assertTrue(blocker.mkdirs());
        assertTrue(new File(blocker, "occupied").createNewFile());

        assertFalse(AtomicFiles.writeText(target, "NEW"));

        assertEquals("GOOD", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void leavesNoTempFileBehindOnSuccess() throws IOException {
        File target = folder.newFile("clean");
        AtomicFiles.writeText(target, "x");

        String[] names = folder.getRoot().list();
        assertTrue("unexpected files: " + Arrays.toString(names), names != null);
        for (String name : names) {
            assertFalse("temp file survived a successful write: " + name,
                    name.endsWith(AtomicFiles.TEMP_SUFFIX));
        }
    }

    @Test
    public void writesUtf8BytesNotPlatformDefault() throws IOException {
        File target = folder.newFile("utf8");
        String content = "caf\u00e9 \u2013 na\u00efve";
        AtomicFiles.writeText(target, content);

        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.toPath()));
    }

    @Test
    public void nullContentWritesAnEmptyFileRatherThanFailing() throws IOException {
        File target = folder.newFile("empty");
        assertTrue(AtomicFiles.writeText(target, null));
        assertEquals(0L, target.length());
    }

    @Test
    public void nullTargetIsRefusedNotThrown() {
        assertFalse(AtomicFiles.writeText(null, "x"));
    }
}
