package org.chimeramc.client.core.content;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pins the resource-pack size/resolution readout (item 253). The PNG header parse is tested from
 * real bytes, and the directory walk from real files on disk.
 */
public class PackAssetSummaryTest {

    /** Builds a minimal but valid PNG header for the given dimensions. */
    private static byte[] pngHeader(int width, int height) {
        byte[] b = new byte[24];
        b[0] = (byte) 0x89; b[1] = 'P'; b[2] = 'N'; b[3] = 'G';
        b[4] = 0x0D; b[5] = 0x0A; b[6] = 0x1A; b[7] = 0x0A;
        // IHDR chunk length (13) then the type.
        b[11] = 13;
        b[12] = 'I'; b[13] = 'H'; b[14] = 'D'; b[15] = 'R';
        writeInt(b, 16, width);
        writeInt(b, 20, height);
        return b;
    }

    private static void writeInt(byte[] b, int off, int value) {
        b[off] = (byte) (value >>> 24);
        b[off + 1] = (byte) (value >>> 16);
        b[off + 2] = (byte) (value >>> 8);
        b[off + 3] = (byte) value;
    }

    @Test
    public void readsDimensionsFromAHeader() {
        int[] size = PackAssetSummary.readPngSize(pngHeader(256, 128));
        assertNotNull(size);
        assertEquals(256, size[0]);
        assertEquals(128, size[1]);
    }

    @Test
    public void rejectsANonPngHeader() {
        byte[] notPng = new byte[24];
        assertNull(PackAssetSummary.readPngSize(notPng));
        assertNull(PackAssetSummary.readPngSize(new byte[10]));
        assertNull(PackAssetSummary.readPngSize((byte[]) null));
    }

    @Test
    public void rejectsAHeaderWithoutIhdr() {
        byte[] b = pngHeader(16, 16);
        b[12] = 'X';
        assertNull(PackAssetSummary.readPngSize(b));
    }

    @Test
    public void rejectsAZeroSizedImage() {
        assertNull(PackAssetSummary.readPngSize(pngHeader(0, 16)));
    }

    @Test
    public void scansARealPackDirectory() throws Exception {
        File root = Files.createTempDirectory("pack-assets").toFile();
        root.deleteOnExit();
        File textures = new File(root, "textures/blocks");
        assertTrue(textures.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(textures, "a.png"))) {
            out.write(pngHeader(64, 64));
        }
        try (FileOutputStream out = new FileOutputStream(new File(textures, "b.png"))) {
            out.write(pngHeader(512, 256));
        }
        // A non-PNG file still counts toward the byte size but not the resolution.
        try (FileOutputStream out = new FileOutputStream(new File(root, "notes.txt"))) {
            out.write("hello".getBytes("UTF-8"));
        }

        PackAssetSummary.Result result = PackAssetSummary.scan(root);

        assertEquals(2, result.textureCount);
        assertEquals(512, result.maxTextureWidth);
        assertEquals(256, result.maxTextureHeight);
        assertEquals(512, result.maxTextureEdge());
        assertEquals("512x256", result.resolutionLabel());
        assertTrue(result.totalBytes >= 5);
    }

    @Test
    public void aPackWithNoReadableTextureReportsAnEmDash() throws Exception {
        File root = Files.createTempDirectory("pack-empty").toFile();
        root.deleteOnExit();
        PackAssetSummary.Result result = PackAssetSummary.scan(root);
        assertEquals(0, result.textureCount);
        assertEquals("\u2014", result.resolutionLabel());
    }

    @Test
    public void aMissingDirectoryReadsAsAnEmptyPack() {
        PackAssetSummary.Result result = PackAssetSummary.scan(new File("/no/such/pack/here"));
        assertEquals(0, result.textureCount);
        assertEquals(0L, result.totalBytes);
    }
}
