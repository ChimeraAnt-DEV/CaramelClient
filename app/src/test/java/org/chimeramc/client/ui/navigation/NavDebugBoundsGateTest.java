package org.chimeramc.client.ui.navigation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The nav bounds overlay is opt-in, never drawn just because this is a debug build.
 *
 * <p>The overlay paints a translucent green outline around every nav view (red on one it thinks is
 * clipped). It used to be installed whenever {@code BuildConfig.DEBUG} was true, so every debug APK
 * a tester installed showed green rectangles over the navigation and read as a rendering bug. A
 * diagnostic must be asked for, so it is gated on a setting that defaults off.
 */
public class NavDebugBoundsGateTest {

    @Test
    public void theOverlayIsGatedOnASettingNotOnTheBuildType() throws IOException {
        String base = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "BaseActivity.java");
        int i = base.indexOf("private void installNavDebugBounds()");
        assertTrue("installNavDebugBounds must exist", i >= 0);
        // The first few lines of the method decide whether it runs.
        String head = base.substring(i, Math.min(base.length(), i + 700));
        assertFalse("the overlay must not be installed merely because this is a debug build",
                head.contains("if (!org.chimeramc.client.BuildConfig.DEBUG"));
        assertTrue("the overlay must be gated on the debug-bounds setting",
                head.contains("isNavDebugBoundsEnabled"));
    }

    @Test
    public void theSettingExistsAndDefaultsOff() throws IOException {
        String settings = read("app/src/main/java/org/chimeramc/client/launcher/settings/FeatureSettings.java");
        assertTrue("FeatureSettings must carry the switch",
                settings.contains("isNavDebugBoundsEnabled")
                        && settings.contains("setNavDebugBoundsEnabled"));
        assertTrue("the switch must default off",
                settings.contains("private boolean navDebugBoundsEnabled = false;"));
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
