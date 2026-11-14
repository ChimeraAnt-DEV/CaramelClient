package org.chimeramc.client.ui.compose.caramel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.ui.components.UiTokens;
import org.chimeramc.client.ui.compose.theme.GlowPalette;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Caramel pack palette has three carriers that must not drift apart: the Compose port
 * ({@code ui/compose/caramel/CaramelTheme.kt}), the shared View/Compose scale ({@link UiTokens} /
 * {@link GlowPalette}) and the in-game menu's ImGui twin
 * ({@code sweetloader/src/pl/menu/MenuTheme.hpp}).
 *
 * <p>The Compose port is the pack's own {@code theme/Theme.kt} value for value, including the extra
 * accents the launch screen uses (the caramel amber, the green family, the darker blue) that the
 * shared scale never needed. The overlap — navy, blue, text, the three status colours — must equal
 * {@code UiTokens}, and the ImGui header must equal both, or a launcher card and the in-game menu end
 * up two shades apart. This reads the real files, because the drift is between source files a
 * reviewer cannot diff by eye.
 */
public class CaramelPaletteParityTest {

    private static final Pattern COMPOSE_COLOR =
            Pattern.compile("val (\\w+) = Color\\(0x([0-9A-Fa-f]{8})\\)");
    private static final Pattern IMGUI_COLOR =
            Pattern.compile("inline ImU32 (\\w+)\\(\\) \\{ return IM_COL32\\((\\d+), (\\d+), (\\d+), (\\d+)\\); \\}");

    private static Map<String, Integer> compose;
    private static Map<String, Integer> imgui;

    @BeforeClass
    public static void loadPalettes() throws IOException {
        compose = parseCompose(read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelTheme.kt"));
        imgui = parseImgui(read("app/src/main/cpp/sweetloader/src/pl/menu/MenuTheme.hpp"));
    }

    @Test
    public void theOverlapEqualsTheSharedScale() {
        // The window is the pack's Abyss; the shared scale keeps it slightly translucent (0xF5), so
        // only the RGB must match.
        assertEquals(UiTokens.BG_WINDOW & 0x00FFFFFF, ((int) compose.get("Abyss")) & 0x00FFFFFF);
        assertEquals(UiTokens.BG_SURFACE, (int) compose.get("Navy"));
        assertEquals(UiTokens.BG_SURFACE_HOVER, (int) compose.get("DeepBlue"));
        assertEquals(UiTokens.ACCENT_500, (int) compose.get("Blue"));
        assertEquals(UiTokens.ACCENT_400, (int) compose.get("BlueLight"));
        assertEquals(UiTokens.TEXT_PRIMARY, (int) compose.get("Text"));
        assertEquals(UiTokens.TEXT_SECONDARY, (int) compose.get("Text2"));
        assertEquals(UiTokens.TEXT_DISABLED, (int) compose.get("Text3"));
        assertEquals(UiTokens.SUCCESS, (int) compose.get("Green"));
        assertEquals(UiTokens.WARNING, (int) compose.get("Orange"));
        assertEquals(UiTokens.DANGER, (int) compose.get("Red"));
        // The deeper blue step the pack adds beyond UiTokens is still the shared snow blue's family.
        assertEquals(GlowPalette.SNOW_BLUE_DEEP, (int) compose.get("BlueDark"));
    }

    @Test
    public void theImguiTwinEqualsTheComposePort() {
        // Every colour the ImGui menu names must be the Compose port's value, so the native menu and
        // the launcher are one palette. The header uses IM_COL32(r, g, b, a); the port uses 0xAARRGGBB.
        String[] names = {"Abyss", "Navy", "Blue", "BlueLight", "BlueDark", "Caramel", "Green",
                "GreenLight", "GreenDark", "Red", "Orange", "Text", "Text2", "Text3", "OnGreen"};
        for (String name : names) {
            assertTrue("MenuTheme.hpp must define " + name, imgui.containsKey(name));
            assertTrue("CaramelTheme.kt must define " + name, compose.containsKey(name));
            assertEquals("MenuTheme.hpp." + name + " must equal CaramelTheme.kt." + name,
                    (int) compose.get(name), (int) imgui.get(name));
        }
    }

    @Test
    public void thePortKeepsThePacksExtraAccents() {
        // The colours the launch screen needs and the shared scale never carried. If these are folded
        // away, the launch screen loses the pack's amber nav indicator and its green Play face.
        assertEquals(0xFFF5A742, (int) compose.get("Caramel"));
        assertEquals(0xFFDE8624, (int) compose.get("CaramelDark"));
        assertEquals(0xFF18A050, (int) compose.get("GreenDark"));
        assertEquals(0xFF062412, (int) compose.get("OnGreen"));
    }

    private static Map<String, Integer> parseCompose(String source) {
        Map<String, Integer> out = new HashMap<>();
        Matcher m = COMPOSE_COLOR.matcher(source);
        while (m.find()) {
            out.put(m.group(1), (int) Long.parseLong(m.group(2), 16));
        }
        return out;
    }

    private static Map<String, Integer> parseImgui(String source) {
        Map<String, Integer> out = new HashMap<>();
        Matcher m = IMGUI_COLOR.matcher(source);
        while (m.find()) {
            int r = Integer.parseInt(m.group(2));
            int g = Integer.parseInt(m.group(3));
            int b = Integer.parseInt(m.group(4));
            int a = Integer.parseInt(m.group(5));
            out.put(m.group(1), (a << 24) | (r << 16) | (g << 8) | b);
        }
        return out;
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
