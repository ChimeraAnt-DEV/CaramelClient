package org.chimeramc.client.launcher.ui.activities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The launch dock uses the Minecraft-style slab controls, not Material pills.
 *
 * <p>The Play action and the instance dropdown are two standalone controls with space between them,
 * not one merged bar. The Play face is the light blue the game itself uses, and the dropdown is the
 * neutral stone slab, so the two read as siblings rather than a single joined surface.
 */
public class LaunchDockStyleTest {

    @Test
    public void thePlayButtonAndDropdownAreSeparateSpacedSlabs() throws IOException {
        String dock = read("app/src/main/res/layout/include_launch_dock.xml");
        // Both controls exist with their own backgrounds.
        assertTrue("the Play button must use the Minecraft slab drawable",
                dock.contains("android:background=\"@drawable/bg_dock_play\""));
        assertTrue("the dropdown must use its own slab drawable",
                dock.contains("android:background=\"@drawable/bg_dock_version_chip\""));
        // The two must be spaced apart, not adjacent.
        assertTrue("the dropdown and Play must be separated by a margin",
                dock.contains("android:layout_marginTop=\"@dimen/space_3\""));
    }

    @Test
    public void thePlayFaceIsLightBlueAndTheDropdownIsStone() throws IOException {
        String play = read("app/src/main/res/drawable/bg_dock_play.xml");
        assertTrue("the Play face must be the light-blue Minecraft family",
                play.contains("@color/mc_button_face")
                        && play.contains("@color/mc_button_top")
                        && play.contains("@color/mc_button_edge"));
        String chip = read("app/src/main/res/drawable/bg_dock_version_chip.xml");
        assertTrue("the dropdown must be the neutral stone slab",
                chip.contains("@color/mc_stone_face"));
        String dock = read("app/src/main/res/layout/include_launch_dock.xml");
        assertTrue("the Play label must use the dark text that reads on the light face",
                dock.contains("@color/mc_button_text"));
        assertTrue("the dropdown label must use the stone text colour",
                dock.contains("@color/mc_stone_text"));
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
