package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Every surface that shows the player's skin must hear about a skin that arrives late.
 *
 * <p>A sign-in schedules the fetch and the first resolve shows the fallback, so a surface that only
 * resolves on creation shows the default skin until it is recreated. The radial wheel was in that
 * state: it refreshed on attach but never registered a listener, so the real skin did not appear
 * until the wheel was created again — part of the "I signed in and got a default skin" report.
 */
public class SkinListenerCoverageTest {

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relative)),
                StandardCharsets.UTF_8);
    }

    /**
     * Walks up from the test working directory to the repository root.
     *
     * <p>Gradle runs unit tests from the module directory, not the repository root, so a
     * repo-relative path does not resolve directly. Same helper the file-size budget test uses, so
     * the two cannot disagree about where the root is.
     */
    private static java.nio.file.Path projectRoot() {
        java.nio.file.Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }

    @Test
    public void theRadialWheelRegistersForSkinUpdates() throws IOException {
        String wheel = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "inbuilt/overlay/RadialMenuView.java");
        assertTrue("the wheel must register a skin listener so a late skin reaches it",
                wheel.contains("AccountSkinService.addListener("));
        assertTrue("and it must unregister on detach, so a discarded view is never woken",
                wheel.contains("AccountSkinService.removeListener("));
    }

    @Test
    public void theCapePreviewRegistersForSkinUpdates() throws IOException {
        String preview = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "inbuilt/overlay/CapePreviewView.java");
        assertTrue(preview.contains("AccountSkinService.addListener("));
        assertTrue(preview.contains("AccountSkinService.removeListener("));
    }

    @Test
    public void theFetchIsGuardedPerAccountNotGlobally() throws IOException {
        String service = read("app/src/main/java/org/chimeramc/client/core/cosmetics/"
                + "AccountSkinService.java");
        assertTrue("the fetch guard must be keyed by account, or a new sign-in is dropped "
                        + "while the previous account's fetch is still running",
                service.contains("SkinFetchGuard"));
    }
}
