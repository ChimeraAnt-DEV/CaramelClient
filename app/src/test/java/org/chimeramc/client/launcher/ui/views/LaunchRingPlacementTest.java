package org.chimeramc.client.launcher.ui.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The launch ring sits in the bottom-right, the status stays centred.
 *
 * <p>The ring and the status line were stacked in the centred lockup, so the ring pushed the text
 * off centre and read as part of the message. The ring is now its own view pinned to the
 * bottom-right corner, and the status keeps the centre to itself.
 */
public class LaunchRingPlacementTest {

    @Test
    public void theRingIsPinnedToTheBottomRight() throws IOException {
        String xml = read("app/src/main/res/layout/activity_minecraft_loading.xml");
        int idAt = xml.indexOf("@+id/minecraftLaunchRing");
        assertTrue("the ring must exist", idAt > 0);
        int openAt = xml.lastIndexOf("<", idAt);
        int closeAt = xml.indexOf(">", idAt);
        String attrs = xml.substring(openAt, closeAt);
        assertTrue("the ring must be pinned to the bottom-right. attrs: " + attrs,
                attrs.contains("layout_gravity=\"bottom|end\"")
                        || attrs.contains("layout_gravity=\"end|bottom\""));
    }

    @Test
    public void theStatusKeepsTheCentre() throws IOException {
        String xml = read("app/src/main/res/layout/activity_minecraft_loading.xml");
        // The centred lockup holds the logo and the status, and no longer holds the ring.
        int centerAt = xml.indexOf("@+id/minecraftLaunchCenter");
        int centerEnd = xml.indexOf("</LinearLayout>", centerAt);
        String center = xml.substring(centerAt, centerEnd);
        assertTrue("the status must stay in the centred lockup", center.contains("@+id/minecraftLaunchStatus"));
        assertFalse("the ring must not be inside the centred lockup",
                center.contains("@+id/minecraftLaunchRing"));
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
