package org.chimeramc.client.launcher.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Mod Menu's navigation bar sits at the bottom and lays out horizontally.
 *
 * <p>It used to be a vertical icon rail down the left edge. The bar is now the last child of the
 * menu's vertical column, so it renders at the bottom, and its container is horizontal so the tiles
 * run left-to-right. Both are asserted against the real layout and source, because a build machine
 * cannot see where a view lands on screen.
 */
public class ModMenuBottomNavTest {

    @Test
    public void theTabBarIsTheLastChildAndIsHorizontal() throws IOException {
        String xml = read("app/src/main/res/layout/overlay_mod_menu.xml");
        int contentHost = xml.indexOf("@+id/mod_menu_content_host");
        int tabHost = xml.indexOf("@+id/mod_menu_tab_host");
        int tabBar = xml.indexOf("@+id/mod_menu_tab_bar");
        assertTrue("the content host and the tab host must both exist",
                contentHost >= 0 && tabHost >= 0 && tabBar >= 0);
        assertTrue("the tab host must come after the content host, so the bar is at the bottom",
                tabHost > contentHost);
        assertTrue("the tab bar must come after the content host", tabBar > contentHost);
        assertTrue("the tab host must be a HorizontalScrollView for a bottom bar",
                xml.substring(xml.lastIndexOf('<', tabHost), tabHost).contains("HorizontalScrollView"));
        assertTrue("the tab bar must be horizontal",
                xml.substring(tabBar, tabBar + 400).contains("android:orientation=\"horizontal\""));
    }

    @Test
    public void theRailSpacesTilesOnItsOwnAxis() throws IOException {
        String rail = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/"
                + "overlay/ModMenuRail.java");
        assertTrue("the rail must detect a horizontal bar",
                rail.contains("getOrientation() == LinearLayout.HORIZONTAL"));
        assertTrue("a horizontal bar must space tiles with a right margin",
                rail.contains("lp.rightMargin = gap"));
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
