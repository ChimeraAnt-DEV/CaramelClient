package org.chimeramc.client.ui.compose.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Compose screens draw the same glass material as the View drawable.
 *
 * <p>The shared card used to be a flat fill with a hairline, which reads as a grey rectangle. The
 * View drawable was fixed first ({@code glass_card.xml}); this pins that the Compose surfaces were
 * brought onto the same material rather than left flat, and that the two share one set of colours so
 * a Compose card and a View card cannot drift apart.
 *
 * <p>A build machine cannot see a rendered surface, so the checks are on the real palette values, the
 * real drawable colours and the Compose source that must use the shared component.
 */
public class ComposeGlassSurfaceTest {

    @Test
    public void theComposeGlassTokensMatchTheViewDrawable() throws IOException {
        String colors = read("app/src/main/res/values/colors.xml");
        assertEquals("GlowPalette.GLASS_RIM_TOP must equal @color/glass_rim_top",
                argb(colors, "glass_rim_top"), GlowPalette.GLASS_RIM_TOP);
        assertEquals("GlowPalette.GLASS_RIM_BOTTOM must equal @color/glass_rim_bottom",
                argb(colors, "glass_rim_bottom"), GlowPalette.GLASS_RIM_BOTTOM);
        assertEquals("GlowPalette.GLASS_SHEEN must equal @color/glass_sheen",
                argb(colors, "glass_sheen"), GlowPalette.GLASS_SHEEN);
        assertEquals("GlowPalette.GLASS_SHEEN_END must equal @color/glass_sheen_end",
                argb(colors, "glass_sheen_end"), GlowPalette.GLASS_SHEEN_END);
    }

    @Test
    public void theRimIsBrighterAtTheTopThanTheBottom() {
        // Alpha is the top byte; a brighter top edge means a higher alpha than the bottom.
        assertTrue("the rim's top edge must be more opaque than its bottom edge",
                (GlowPalette.GLASS_RIM_TOP >>> 24) > (GlowPalette.GLASS_RIM_BOTTOM >>> 24));
    }

    @Test
    public void theSheenFallsOffToFullyTransparent() {
        assertTrue("the sheen must start visible", (GlowPalette.GLASS_SHEEN >>> 24) > 0);
        assertEquals("the sheen must end fully transparent so it falls off downward",
                0, GlowPalette.GLASS_SHEEN_END >>> 24);
    }

    @Test
    public void theSharedGlassComponentDrawsAFillAndASheen() throws IOException {
        String glass = read("app/src/main/java/org/chimeramc/client/ui/compose/theme/GlowGlass.kt");
        assertTrue("glowGlass must draw the base fill", glass.contains(".background(base)"));
        assertTrue("glowGlass must draw a vertical sheen gradient",
                glass.contains("Brush.verticalGradient"));
        assertTrue("GlassSurface must exist as the shared card component",
                glass.contains("fun GlassSurface("));
    }

    @Test
    public void theComposeSurfacesUseTheSharedGlassMaterial() throws IOException {
        // The Caramel launch screen draws every card with the shared GlassPanel, so the launch tab,
        // the mod rows and the content cards cannot drift onto a flat fill. The Caramel glass is the
        // UI pack's material (CaramelGlass.kt); GlassSurface remains the material for the older
        // Compose screens (the account and connect sections).
        String screen = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/launch/CaramelLaunchScreen.kt");
        assertTrue("the launch screen must draw its cards with the shared GlassPanel",
                screen.contains("GlassPanel("));
        String[] surfaces = {
                "app/src/main/java/org/chimeramc/client/ui/compose/account/AccountSection.kt",
                "app/src/main/java/org/chimeramc/client/ui/compose/connect/ConnectionStatus.kt",
        };
        for (String path : surfaces) {
            String src = read(path);
            assertTrue(path + " must draw its cards with the shared GlassSurface, not a flat fill",
                    src.contains("GlassSurface("));
        }
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
