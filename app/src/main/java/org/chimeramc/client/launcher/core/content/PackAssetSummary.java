package org.chimeramc.client.core.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * The size and texture-resolution readout for a resource pack (item 253).
 *
 * <p>Bedrock draws a pack's textures at whatever resolution they are authored at, so the largest
 * texture is the number that decides how much GPU memory the pack costs and whether it will look
 * sharp or blurry on a given device. The pack's own manifest does not state it, so it is read from
 * the largest PNG the pack ships.
 *
 * <p>The PNG width/height live in the IHDR chunk, which is always the first chunk, so the size is
 * read from the first 24 bytes of each file rather than by decoding it. That keeps the scan cheap
 * enough to run over a pack with hundreds of textures, and keeps the class Android-free so the
 * parse is unit-tested against real PNG headers.
 *
 * <p><b>Bounded.</b> The walk stops after {@link #MAX_TEXTURES_SCANNED} files and only descends
 * {@link #MAX_DEPTH} levels, so a pack with an accidental symlink loop or a huge tree cannot turn
 * a list refresh into an unbounded disk walk.
 */
public final class PackAssetSummary {

    /** The walk stops counting after this many texture files. */
    public static final int MAX_TEXTURES_SCANNED = 4000;
    /** The walk does not descend past this depth below the pack root. */
    public static final int MAX_DEPTH = 8;

    private PackAssetSummary() {
    }

    /** The largest texture's dimensions and the pack's total byte size. */
    public static final class Result {
        public final long totalBytes;
        public final int maxTextureWidth;
        public final int maxTextureHeight;
        public final int textureCount;

        Result(long totalBytes, int maxTextureWidth, int maxTextureHeight, int textureCount) {
            this.totalBytes = totalBytes;
            this.maxTextureWidth = maxTextureWidth;
            this.maxTextureHeight = maxTextureHeight;
            this.textureCount = textureCount;
        }

        /** The largest texture's long edge, or 0 when the pack ships no readable PNG. */
        public int maxTextureEdge() {
            return Math.max(maxTextureWidth, maxTextureHeight);
        }

        /**
         * A short resolution label such as {@code 256x256}, or an em dash when the pack ships no
         * readable texture.
         *
         * <p>An em dash rather than {@code 0x0}: a pack with no textures is not a 0-resolution pack,
         * and showing a number would read as a measurement.
         */
        public String resolutionLabel() {
            if (maxTextureWidth <= 0 || maxTextureHeight <= 0) return "\u2014";
            return maxTextureWidth + "x" + maxTextureHeight;
        }
    }

    /** Scans a pack directory. Never throws; a missing directory reads as an empty pack. */
    public static Result scan(File packDir) {
        Accumulator acc = new Accumulator();
        walk(packDir, 0, acc);
        return new Result(acc.totalBytes, acc.maxWidth, acc.maxHeight, acc.textureCount);
    }

    private static final class Accumulator {
        long totalBytes;
        int maxWidth;
        int maxHeight;
        int textureCount;
    }

    private static void walk(File file, int depth, Accumulator acc) {
        if (file == null || depth > MAX_DEPTH || acc.textureCount >= MAX_TEXTURES_SCANNED) return;
        if (file.isFile()) {
            acc.totalBytes += file.length();
            if (isPng(file)) {
                int[] size = readPngSize(file);
                if (size != null) {
                    acc.textureCount++;
                    if (size[0] > acc.maxWidth) acc.maxWidth = size[0];
                    if (size[1] > acc.maxHeight) acc.maxHeight = size[1];
                }
            }
            return;
        }
        File[] children = file.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (acc.textureCount >= MAX_TEXTURES_SCANNED) return;
            walk(child, depth + 1, acc);
        }
    }

    private static boolean isPng(File file) {
        String name = file.getName().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".png");
    }

    /**
     * Reads a PNG's width and height from its IHDR chunk, or null when the file is not a PNG.
     *
     * <p>Layout: an 8-byte signature, then the IHDR chunk's 4-byte length, 4-byte type
     * ({@code "IHDR"}), then width and height as 4-byte big-endian values. Only the first 24 bytes
     * are read, so the cost does not depend on the image size.
     */
    public static int[] readPngSize(File file) {
        byte[] header = new byte[24];
        try (InputStream in = Files.newInputStream(file.toPath())) {
            int read = 0;
            while (read < header.length) {
                int n = in.read(header, read, header.length - read);
                if (n < 0) break;
                read += n;
            }
            if (read < header.length) return null;
        } catch (IOException e) {
            return null;
        }
        return readPngSize(header);
    }

    /**
     * The pure parse half: reads width/height from a 24-byte PNG header array.
     *
     * <p>Separated so the byte layout is unit-tested without touching the disk.
     */
    public static int[] readPngSize(byte[] header) {
        if (header == null || header.length < 24) return null;
        if ((header[0] & 0xFF) != 0x89 || header[1] != 'P' || header[2] != 'N'
                || header[3] != 'G') {
            return null;
        }
        if (header[12] != 'I' || header[13] != 'H' || header[14] != 'D' || header[15] != 'R') {
            return null;
        }
        int width = readInt(header, 16);
        int height = readInt(header, 20);
        if (width <= 0 || height <= 0) return null;
        return new int[]{width, height};
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }
}
