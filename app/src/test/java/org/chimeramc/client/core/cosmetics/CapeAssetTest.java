package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Every authored cape is the vanilla 64x32 layout.
 *
 * <p>A cape texture is drawn at the vanilla atlas size, so a file at another size would be sampled
 * with the wrong UVs and show the wrong slice of art. This pins the size for every id the catalogue
 * claims has an authored texture, and that the file for each id actually exists.
 */
public class CapeAssetTest {

    @Test
    public void everyAuthoredCapeIsTheVanillaLayout() throws IOException {
        Path dir = root().resolve("resources/cosmetics/capes");
        List<String> offenders = new ArrayList<>();
        for (String id : AuthoredCapeTextures.IDS) {
            Path f = dir.resolve(id + ".png");
            if (!Files.exists(f)) {
                offenders.add(id + " (missing file)");
                continue;
            }
            byte[] b = Files.readAllBytes(f);
            // PNG: 8-byte signature, then an IHDR chunk whose width/height are two big-endian ints.
            int w = ((b[16] & 0xFF) << 24) | ((b[17] & 0xFF) << 16) | ((b[18] & 0xFF) << 8)
                    | (b[19] & 0xFF);
            int h = ((b[20] & 0xFF) << 24) | ((b[21] & 0xFF) << 16) | ((b[22] & 0xFF) << 8)
                    | (b[23] & 0xFF);
            if (w != 64 || h != 32) offenders.add(id + " is " + w + "x" + h);
        }
        assertTrue("an authored cape must be the vanilla 64x32 layout: " + offenders,
                offenders.isEmpty());
    }

    @Test
    public void theCatalogueCarriesEnoughArtToChooseFrom() {
        assertTrue("the closet needs a real choice of authored capes, not a handful",
                AuthoredCapeTextures.IDS.size() >= 12);
    }

    @Test
    public void everyAuthoredCapeHasRealDepthNotFlatBlocks() throws Exception {
        // The first authored set was 2-3 flat colour blocks per panel, which reads as a template
        // rather than cloth. Each cape must now carry shading: a top highlight, a bottom shadow, a
        // weave and an outline, so the visible panel holds far more than a handful of colours.
        Path dir = root().resolve("resources/cosmetics/capes");
        java.util.List<String> flat = new java.util.ArrayList<>();
        for (String id : AuthoredCapeTextures.IDS) {
            Path f = dir.resolve(id + ".png");
            if (!Files.exists(f)) continue;
            int[] px = decodeRgba(Files.readAllBytes(f), 64, 32);
            java.util.Set<Integer> colours = new java.util.HashSet<>();
            for (int y = 1; y < 17; y++) {
                for (int x = 1; x < 11; x++) {
                    int i = (y * 64 + x) * 4;
                    if (px[i + 3] == 0) continue;
                    colours.add((px[i] << 16) | (px[i + 1] << 8) | px[i + 2]);
                }
            }
            if (colours.size() < 24) flat.add(id + " (" + colours.size() + " colours)");
        }
        assertTrue("every authored cape must carry shading, not flat blocks: " + flat,
                flat.isEmpty());
    }

    /** Decodes a non-interlaced 8-bit RGBA PNG into row-major RGBA bytes. */
    private static int[] decodeRgba(byte[] png, int w, int h) throws Exception {
        int pos = 8;
        java.io.ByteArrayOutputStream idat = new java.io.ByteArrayOutputStream();
        while (pos < png.length) {
            int len = readBe32(png, pos);
            String type = new String(png, pos + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if ("IDAT".equals(type)) {
                idat.write(png, pos + 8, len);
            } else if ("IEND".equals(type)) {
                break;
            }
            pos += 12 + len;
        }
        byte[] raw = inflate(idat.toByteArray());
        int stride = w * 4;
        int[] out = new int[h * stride];
        byte[] prev = new byte[stride];
        int p = 0;
        for (int y = 0; y < h; y++) {
            int filter = raw[p++] & 0xFF;
            byte[] line = new byte[stride];
            System.arraycopy(raw, p, line, 0, stride);
            p += stride;
            for (int i = 0; i < stride; i++) {
                int a = i >= 4 ? line[i - 4] & 0xFF : 0;
                int b = prev[i] & 0xFF;
                int c = i >= 4 ? prev[i - 4] & 0xFF : 0;
                int add;
                switch (filter) {
                    case 1: add = a; break;
                    case 2: add = b; break;
                    case 3: add = (a + b) >> 1; break;
                    case 4:
                        int pp = a + b - c;
                        int pa = Math.abs(pp - a), pb = Math.abs(pp - b), pc = Math.abs(pp - c);
                        add = (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
                        break;
                    default: add = 0; break;
                }
                line[i] = (byte) ((line[i] + add) & 0xFF);
            }
            for (int i = 0; i < stride; i++) out[y * stride + i] = line[i] & 0xFF;
            prev = line;
        }
        return out;
    }

    private static byte[] inflate(byte[] data) throws Exception {
        java.util.zip.Inflater inflater = new java.util.zip.Inflater();
        inflater.setInput(data);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!inflater.finished()) {
            int n = inflater.inflate(buf);
            if (n == 0 && inflater.needsInput()) break;
            out.write(buf, 0, n);
        }
        inflater.end();
        return out.toByteArray();
    }

    private static int readBe32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }

    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
