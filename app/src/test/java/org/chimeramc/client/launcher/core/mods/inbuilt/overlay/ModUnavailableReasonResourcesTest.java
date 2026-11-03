package org.chimeramc.client.launcher.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Every reason a module can be unavailable has its own message, and the UI names it.
 *
 * <p>A single "needs game data" line for every cause told the user the wrong thing: a module waiting
 * for an engine feed, a module a server decides, and a module waiting for a backend capability are
 * three different situations with three different answers. This pins that the three messages exist
 * and that both surfaces that show a reason (the Mod Menu list and the overlay) reference all three,
 * so a new cause cannot be added with the wrong wording or no wording at all.
 *
 * <p>A build machine has no {@code R} class, so this reads the real {@code strings.xml} and source
 * rather than a resource id, the same way the layout guards do.
 */
public class ModUnavailableReasonResourcesTest {

    private static final String[] REASON_STRINGS = {
            "mod_unavailable_reason",
            "mod_unavailable_reason_server",
            "mod_unavailable_reason_backend",
    };

    private static final String[] SURFACES = {
            "app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/overlay/ModMenuAdapter.java",
            "app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/overlay/ModMenuOverlay.java",
    };

    @Test
    public void everyReasonHasItsOwnMessage() throws IOException {
        String strings = read("app/src/main/res/values/strings.xml");
        for (String name : REASON_STRINGS) {
            assertTrue("values/strings.xml must define " + name,
                    strings.contains("name=\"" + name + "\""));
        }
    }

    @Test
    public void bothSurfacesNameEveryReason() throws IOException {
        for (String surface : SURFACES) {
            String source = read(surface);
            for (String name : REASON_STRINGS) {
                assertTrue(surface + " must reference R.string." + name
                                + " so the reason is shown with the right wording",
                        source.contains("R.string." + name));
            }
        }
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
