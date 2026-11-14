package org.chimeramc.client.launcher.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The mod menu's tab bar is drawn natively, from one ordered list, and the old Android layout is
 * gone for good.
 *
 * <p>The menu used to be an Android {@code overlay_mod_menu.xml} whose bottom bar was a
 * {@code HorizontalScrollView}. It kept {@code fillViewport="true"} with a {@code wrap_content}
 * height, which asks a horizontal scroller to stretch its child to a viewport it has no measured
 * height for — a measure-time fault — and the bar could not scroll anyway. The menu is Dear ImGui on
 * the game's GL context now, so the layout is deleted; this pins that it stays deleted and that the
 * native header's tab bar keeps one source of truth for its labels and icons.
 */
public class ModMenuNavLayoutTest {

    private static final String RENDER =
            "app/src/main/cpp/sweetloader/src/pl/menu/MenuRender.cpp";

    @Test
    public void theOldAndroidMenuLayoutIsGone() {
        assertFalse("the Android menu layout must stay deleted; the menu is native now",
                Files.exists(root().resolve("app/src/main/res/layout/overlay_mod_menu.xml")));
        assertFalse("the Android radial menu layout must stay deleted",
                Files.exists(root().resolve("app/src/main/res/layout/overlay_radial_menu.xml")));
    }

    @Test
    public void theNativeTabBarHasOneSourceForItsLabelsAndIcons() throws IOException {
        String source = read(RENDER);
        // The header declares the labels and the icons as parallel arrays and passes their shared
        // count to the tab bar, so a tab cannot be added to one and forgotten in the other.
        assertTrue("the header must declare the tab labels",
                source.contains("static const char *labels[] = {"));
        assertTrue("the header must declare the tab icons",
                source.contains("static const Icon icons[] = {"));
        assertTrue("the tab bar must be driven by the label array's count",
                source.contains("ui::TabBar(\"tabs\", labels, icons,"));
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
