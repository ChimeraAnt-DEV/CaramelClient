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

    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
