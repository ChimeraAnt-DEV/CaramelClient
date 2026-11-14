package org.chimeramc.client.launcher.ui.activities;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The launch control is the UI pack's split LAUNCH GAME face, not a Material pill or a Minecraft slab.
 *
 * <p>The pack's control is one green gradient face carrying the action and its build subtitle, with a
 * chevron segment that opens the launch options. The two halves are separate tap targets on one
 * surface, so the two things a launch control does — start a session, and change what will start —
 * cannot be confused. The screen is Compose now, so the check reads the real source: a build machine
 * cannot tap the control.
 */
public class LaunchDockStyleTest {

    private static final String SCREEN =
            "app/src/main/java/org/chimeramc/client/ui/compose/caramel/launch/CaramelLaunchScreen.kt";

    @Test
    public void theLaunchControlIsThePacksSplitFace() throws IOException {
        String screen = read(SCREEN);
        assertTrue("the launch face must use the pack's green action tone",
                screen.contains("C.GreenLight") && screen.contains("C.GreenDark"));
        assertTrue("the two halves must be one row",
                screen.contains("verticalAlignment = Alignment.CenterVertically"));
        assertTrue("the face must carry the launch label",
                screen.contains("\"LAUNCH GAME\""));
        assertTrue("the face must carry the build subtitle",
                screen.contains("state.buildLine.ifEmpty { state.versionId }"));
    }

    @Test
    public void theSplitHalvesAreSeparateAccessibleTargets() throws IOException {
        String screen = read(SCREEN);
        assertTrue("the face must be its own tap target, gated on the launch state",
                screen.contains(".clickable(enabled = state.launchEnabled && !state.launching, "
                        + "onClick = onLaunch)"));
        assertTrue("the chevron must be its own tap target that opens the picker",
                screen.contains(".clickable { open = !open }"));
        // The subtitle is what tells the user which build the button will start.
        assertTrue("the chevron must be reachable from a controller as well as a finger",
                screen.contains("Icon("));
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
