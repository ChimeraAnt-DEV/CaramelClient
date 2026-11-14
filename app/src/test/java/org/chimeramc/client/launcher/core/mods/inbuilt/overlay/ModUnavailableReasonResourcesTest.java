package org.chimeramc.client.launcher.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Every reason a module can be unavailable has its own message, and the surface names it.
 *
 * <p>A single "needs game data" line for every cause told the user the wrong thing: a module waiting
 * for an engine feed, a module a server decides, a module waiting for a backend capability and a
 * module waiting for a present hook are four different situations with four different answers. This
 * pins that the four messages exist, that the launcher-side catalogue builder words each reason id
 * with the matching resource, and that the native menu renders the wording it is handed rather than
 * a raw id.
 *
 * <p>A build machine has no {@code R} class, so this reads the real {@code strings.xml} and source
 * rather than a resource id, the same way the layout guards do.
 */
public class ModUnavailableReasonResourcesTest {

    private static final String[] REASON_STRINGS = {
            "mod_unavailable_reason",
            "mod_unavailable_reason_server",
            "mod_unavailable_reason_backend",
            "mod_unavailable_reason_hook",
    };

    /** The one Java surface that words an unavailable reason for the native menu. */
    private static final String SURFACE =
            "app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/overlay/"
                    + "InGameMenuController.java";

    @Test
    public void everyReasonHasItsOwnMessage() throws IOException {
        String strings = read("app/src/main/res/values/strings.xml");
        for (String name : REASON_STRINGS) {
            assertTrue("values/strings.xml must define " + name,
                    strings.contains("name=\"" + name + "\""));
        }
    }

    @Test
    public void theCatalogueBuilderNamesEveryReason() throws IOException {
        String source = read(SURFACE);
        for (String name : REASON_STRINGS) {
            assertTrue(SURFACE + " must reference R.string." + name
                            + " so the reason is shown with the right wording",
                    source.contains("R.string." + name));
        }
    }

    @Test
    public void theNativeMenuRendersTheWordingItIsHandedNotARawId() throws IOException {
        // The reason crosses as the already-worded message; the native side draws it as a status
        // label, so a raw id can never reach the screen.
        String render = read("app/src/main/cpp/sweetloader/src/pl/menu/MenuRender.cpp");
        assertTrue("the native menu must draw the module's unavailable wording",
                render.contains("m.unavailableReason"));
        assertFalse("the menu must not render a raw reason id",
                render.contains("\"no_game_data\"") || render.contains("\"server_decided\"")
                        || render.contains("\"no_backend\"") || render.contains("\"no_hook\""));
    }

    private static Path resolve(String relativePath) {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
