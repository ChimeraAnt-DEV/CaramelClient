package org.chimeramc.client.launcher.ui.activities;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The launch dock uses the UI pack's split LAUNCH GAME control, not a Material pill or a Minecraft
 * slab.
 *
 * <p>The pack's control is one green gradient face carrying the action and its build subtitle, with a
 * chevron segment that opens the launch options. The two halves are separate tap targets on one
 * surface, so the two things a launch control does — start a session, and change what will start —
 * cannot be confused.
 */
public class LaunchDockStyleTest {

    @Test
    public void theLaunchControlIsThePacksSplitFace() throws IOException {
        String dock = read("app/src/main/res/layout/include_launch_dock.xml");
        assertTrue("the launch face must use the pack's split drawable",
                dock.contains("android:background=\"@drawable/bg_split_launch\""));
        assertTrue("the chevron half must use its own drawable",
                dock.contains("android:background=\"@drawable/bg_split_launch_chevron\""));
        assertTrue("the two halves must be one row", dock.contains("android:orientation=\"horizontal\""));
        // The control must be spaced from the picker above, not adjacent to it.
        assertTrue("the picker and the launch control must be separated by a margin",
                dock.contains("android:layout_marginTop=\"@dimen/space_3\""));
    }

    @Test
    public void theLaunchFaceIsThePacksGreenAndTheChipIsStillStone() throws IOException {
        String face = read("app/src/main/res/drawable/bg_split_launch.xml");
        assertTrue("the launch face must be the pack's green gradient family",
                face.contains("@color/split_launch_top")
                        && face.contains("@color/split_launch_bottom")
                        && face.contains("@color/split_launch_pressed"));
        String chip = read("app/src/main/res/drawable/bg_dock_version_chip.xml");
        assertTrue("the version chip must stay the neutral stone slab",
                chip.contains("@color/mc_stone_face"));
        String dock = read("app/src/main/res/layout/include_launch_dock.xml");
        assertTrue("the launch label must use the dark text that reads on the green face",
                dock.contains("@color/split_launch_text"));
        assertTrue("the version chip label must use the stone text colour",
                dock.contains("@color/mc_stone_text"));
    }

    @Test
    public void theSplitHalvesAreSeparateAccessibleTargets() throws IOException {
        String dock = read("app/src/main/res/layout/include_launch_dock.xml");
        // Both halves are focusable and carry a description, so a screen reader and a controller can
        // reach and name each one; an icon-only segment with no description is unreachable.
        assertTrue("the chevron must be a named, focusable target",
                dock.contains("android:id=\"@+id/launch_chevron\"")
                        && dock.contains("android:contentDescription=\"@string/launch_menu_open\""));
        assertTrue("the face must be a named, focusable target",
                dock.contains("android:id=\"@+id/launch_button\"")
                        && dock.contains("android:contentDescription=\"@string/launch_button_label\""));
        // The subtitle is what tells the user which build the button will start.
        assertTrue("the face must carry the build subtitle",
                dock.contains("android:id=\"@+id/launch_button_subtitle\""));
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
