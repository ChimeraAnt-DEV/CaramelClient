package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

/** Pins the per-instance disk breakdown. */
public class InstanceDiskUsageTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private void write(File file, int bytes) throws Exception {
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(new byte[bytes]);
        }
    }

    @Test
    public void measureSplitsTheDirectories() throws Exception {
        File data = temp.newFolder("data");
        File mods = temp.newFolder("mods");
        File runtime = temp.newFolder("runtime");
        File cache = temp.newFolder("cache");
        write(new File(data, "world"), 1000);
        write(new File(mods, "mod.zip"), 200);
        write(new File(runtime, "libminecraftpe.so"), 300);
        write(new File(cache, "scratch"), 40);

        InstanceDiskUsage usage = InstanceDiskUsage.measure(data, mods, runtime, cache);
        assertEquals(1000L, usage.gameDataBytes);
        assertEquals(200L, usage.modsBytes);
        assertEquals(300L, usage.runtimeBytes);
        assertEquals(40L, usage.cacheBytes);
        assertEquals(1540L, usage.totalBytes());
    }

    @Test
    public void missingDirectoriesCountAsZero() {
        InstanceDiskUsage usage = InstanceDiskUsage.measure(null, null, null, null);
        assertEquals(0L, usage.totalBytes());
        assertFalse(usage.hasClearableCache());
    }

    @Test
    public void clearableCacheTracksTheCacheSize() throws Exception {
        File cache = temp.newFolder("cache");
        InstanceDiskUsage empty = InstanceDiskUsage.measure(null, null, null, cache);
        assertFalse(empty.hasClearableCache());

        write(new File(cache, "f"), 1);
        InstanceDiskUsage withCache = InstanceDiskUsage.measure(null, null, null, cache);
        assertTrue(withCache.hasClearableCache());
    }

    @Test
    public void negativeInputsClampToZero() {
        InstanceDiskUsage usage = new InstanceDiskUsage(-5L, -1L, -100L, -7L);
        assertEquals(0L, usage.totalBytes());
        assertFalse(usage.hasClearableCache());
    }

    @Test
    public void totalIsTheSumOfTheParts() {
        InstanceDiskUsage usage = new InstanceDiskUsage(10L, 20L, 30L, 40L);
        assertEquals(100L, usage.totalBytes());
    }
}
