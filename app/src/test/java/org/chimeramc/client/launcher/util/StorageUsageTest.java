package org.chimeramc.client.launcher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

/** Pins the disk-usage walk and formatting (item 265). */
public class StorageUsageTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private void write(File file, int bytes) throws Exception {
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(new byte[bytes]);
        }
    }

    @Test
    public void aMissingRootIsZero() {
        assertEquals(0L, StorageUsage.sizeOf((File) null));
        assertEquals(0L, StorageUsage.sizeOf(new File(temp.getRoot(), "nope")));
    }

    @Test
    public void aSingleFileCountsItsLength() throws Exception {
        File f = temp.newFile("a.bin");
        write(f, 100);
        assertEquals(100L, StorageUsage.sizeOf(f));
    }

    @Test
    public void nestedFilesAreSummed() throws Exception {
        File root = temp.newFolder("instance");
        write(new File(root, "one.bin"), 10);
        write(new File(root, "sub/two.bin"), 20);
        write(new File(root, "sub/deep/three.bin"), 30);
        assertEquals(60L, StorageUsage.sizeOf(root));
    }

    @Test
    public void severalRootsAreCombinedAndNullsSkipped() throws Exception {
        File a = temp.newFolder("a");
        File b = temp.newFolder("b");
        write(new File(a, "x"), 5);
        write(new File(b, "y"), 7);
        assertEquals(12L, StorageUsage.sizeOf(a, null, b));
    }

    @Test
    public void anEmptyDirectoryIsZero() throws Exception {
        assertEquals(0L, StorageUsage.sizeOf(temp.newFolder("empty")));
    }

    @Test
    public void formattingPicksASensibleUnit() {
        assertEquals("512 B", StorageUsage.format(512));
        assertEquals("1 KB", StorageUsage.format(1024));
        assertEquals("1.5 MB", StorageUsage.format(1024L * 1024L * 3 / 2));
        assertTrue(StorageUsage.format(1024L * 1024L * 1024L * 2).endsWith("GB"));
    }

    @Test
    public void formattingNeverGoesNegative() {
        assertEquals("0 B", StorageUsage.format(0));
    }

    @Test
    public void aLargeTreeDoesNotBlowTheStack() throws Exception {
        File root = temp.newFolder("deep");
        File current = root;
        for (int i = 0; i < 400; i++) {
            current = new File(current, "d");
        }
        current.mkdirs();
        write(new File(current, "leaf.bin"), 42);
        assertEquals(42L, StorageUsage.sizeOf(root));
    }

    @Test
    public void theMeasurementIsDeterministic() throws Exception {
        File root = temp.newFolder("stable");
        write(new File(root, "a"), 11);
        long first = StorageUsage.sizeOf(root);
        long second = StorageUsage.sizeOf(root);
        assertEquals(first, second);
        assertNotEquals(0L, first);
    }
}
