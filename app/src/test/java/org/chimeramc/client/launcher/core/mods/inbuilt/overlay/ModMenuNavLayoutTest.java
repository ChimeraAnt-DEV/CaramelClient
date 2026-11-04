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
 * The mod menu's bottom bar is a valid horizontal scroller.
 *
 * <p>The bar was moved from a vertical {@code ScrollView} to a {@code HorizontalScrollView}. The
 * original kept {@code fillViewport="true"} with {@code layout_height="match_parent"}, which is
 * meaningful for a vertical scroller. On a horizontal one with {@code wrap_content} height the flag
 * asks the scroller to stretch its child to a viewport it has no measured height for, which is a
 * measure-time fault; and a scroller whose content cannot scroll is dead weight.
 *
 * <p>A build machine cannot open the menu, so the rule is asserted against the real layout: the
 * horizontal bar must not carry a vertical-only flag, and its height must be a real value.
 */
public class ModMenuNavLayoutTest {

    @Test
    public void theHorizontalBarIsNotAskedToFillAVerticalViewport() throws IOException {
        String xml = read("app/src/main/res/layout/overlay_mod_menu.xml");
        // The tab host's own element: from its <HorizontalScrollView ...> opening tag to its id line.
        // The file has a second HorizontalScrollView earlier, so anchor on the id itself.
        int idAt = xml.indexOf("@+id/mod_menu_tab_host");
        assertTrue("the bottom bar must exist", idAt > 0);
        int openAt = xml.lastIndexOf("<HorizontalScrollView", idAt);
        int closeAt = xml.indexOf(">", idAt);
        assertTrue("the tab host must be a HorizontalScrollView", openAt > 0 && openAt < idAt);
        String attrs = xml.substring(openAt, closeAt);
        assertFalse("a horizontal bar with wrap_content height must not set fillViewport: there is "
                        + "no measured height for it to fill. attrs: " + attrs,
                attrs.contains("fillViewport"));
        assertFalse("the bar must not keep the vertical scroller's match_parent height. attrs: "
                        + attrs,
                attrs.contains("layout_height=\"match_parent\""));
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
