package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A shared glass surface is layered, not a flat fill.
 *
 * <p>A single solid fill with a hairline stroke reads as a grey rectangle, which is what makes a UI
 * look unfinished. Glass reads as glass because of its light: a brighter top edge and a sheen that
 * falls off downward. These are asserted against the real drawable, because a build machine cannot
 * see a rendered surface.
 */
public class GlassSurfaceTest {

    @Test
    public void theGlassCardIsLayeredWithARimAndASheen() throws IOException {
        String xml = read("app/src/main/res/drawable/glass_card.xml");
        assertTrue("the card must be a layer-list, so it can stack a rim, a sheen and a fill",
                xml.contains("<layer-list"));
        assertTrue("the rim must be a gradient, brightest at the top, so the card catches light "
                        + "from above", xml.contains("@color/glass_rim_top")
                        && xml.contains("@color/glass_rim_bottom"));
        assertTrue("the sheen must fall off to transparent",
                xml.contains("@color/glass_sheen") && xml.contains("@color/glass_sheen_end"));
        assertTrue("the fill must remain the base panel colour",
                xml.contains("@color/glass_background"));
    }

    @Test
    public void theRimIsBrighterAtTheTopThanTheBottom() throws IOException {
        String colors = read("app/src/main/res/values/colors.xml");
        int top = argb(colors, "glass_rim_top");
        int bottom = argb(colors, "glass_rim_bottom");
        // Alpha is the top byte; a brighter top edge means a higher alpha than the bottom.
        assertTrue("the rim's top edge must be more opaque than its bottom edge. top="
                        + (top >>> 24) + " bottom=" + (bottom >>> 24),
                (top >>> 24) > (bottom >>> 24));
    }

    private static int argb(String colors, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("name=\"" + name + "\">#([0-9a-fA-F]{8})<").matcher(colors);
        assertTrue("colour " + name + " must be a full #AARRGGBB value", m.find());
        return (int) Long.parseLong(m.group(1), 16);
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(root().resolve(relativePath)), StandardCharsets.UTF_8);
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
