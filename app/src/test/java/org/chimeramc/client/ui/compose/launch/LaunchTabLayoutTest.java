package org.chimeramc.client.ui.compose.launch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The launch tab renders the Compose hero, and the preview sits to the right of the identity.
 *
 * <p>A build machine cannot rasterise a Compose screen, so the two things that would silently regress
 * are asserted against the real files: the activity layout hosts the Compose hero (not the deleted XML
 * block), and the hero places the identity column before the preview in source order — which, in a
 * {@code Row}, is left-to-right, so the preview is on the right.
 */
public class LaunchTabLayoutTest {

    private static final String SCREEN =
            "app/src/main/java/org/chimeramc/client/ui/compose/caramel/launch/CaramelLaunchScreen.kt";

    @Test
    public void theLaunchTabIsOneComposeScreenAndTheOldLayoutIsGone() throws IOException {
        String main = read("app/src/main/res/layout/activity_main.xml");
        assertTrue("the launch tab must host the Caramel Compose screen",
                main.contains("androidx.compose.ui.platform.ComposeView")
                        && main.contains("@+id/launch_content"));
        assertFalse("the old hero include must be gone", main.contains("@layout/include_hero_panel"));
        assertFalse("include_hero_panel.xml is the old hero and must not exist",
                Files.exists(root().resolve("app/src/main/res/layout/include_hero_panel.xml")));
        assertFalse("the old dock include must be gone",
                main.contains("@layout/include_launch_dock"));
        assertFalse("include_launch_dock.xml is the old dock and must not exist",
                Files.exists(root().resolve("app/src/main/res/layout/include_launch_dock.xml")));
    }

    @Test
    public void theIdentityColumnPrecedesThePreviewInTheRow() throws IOException {
        String hero = read(SCREEN);
        int identity = hero.indexOf("Left: identity and the launch control");
        int preview = hero.indexOf("Right: the live 3D character preview");
        assertTrue("the hero must label its two halves", identity >= 0 && preview >= 0);
        assertTrue("the identity column must come before the preview, so the preview is on the right",
                identity < preview);
    }

    @Test
    public void thePreviewUsesTheRealCapePreviewView() throws IOException {
        String hero = read(SCREEN);
        assertTrue("the preview must be the real CapePreviewView, not a reimplementation",
                hero.contains("org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView"));
        assertTrue("the preview must be released when the composition leaves",
                hero.contains("onRelease") && hero.contains("view.release()"));
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
