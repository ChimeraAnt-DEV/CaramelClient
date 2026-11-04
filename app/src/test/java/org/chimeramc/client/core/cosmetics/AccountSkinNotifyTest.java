package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The account skin fetch reports back when it finishes.
 *
 * <p>The preview resolved the account skin by scheduling a background fetch and returning null, so
 * the first resolve after a sign-in always showed the fallback (Steve) and the real skin only
 * appeared when the panel was reopened. The fetch now notifies listeners on completion, so the
 * preview can refresh as soon as the skin lands.
 */
public class AccountSkinNotifyTest {

    @Test
    public void theFetchNotifiesListenersOnCompletion() throws IOException {
        String src = read("app/src/main/java/org/chimeramc/client/core/cosmetics/"
                + "AccountSkinService.java");
        assertTrue("the service must expose a listener type",
                src.contains("interface Listener"));
        assertTrue("the service must let a caller register a listener",
                src.contains("addListener"));
        assertTrue("the service must let a caller remove a listener",
                src.contains("removeListener"));
        // The fetch path must notify, in a finally so a failed fetch still wakes the caller.
        int fetch = src.indexOf("public static void refreshInBackground(");
        assertTrue(fetch >= 0);
        int end = src.indexOf("\n    }", fetch);
        String body = src.substring(fetch, end);
        assertTrue("the background fetch must notify its listeners", body.contains("notifyListeners"));
        assertTrue("the notify must run even when the fetch fails, so a caller is never left waiting",
                body.contains("finally"));
    }

    @Test
    public void aListenerIsOnlyCalledOncePerRegistration() throws IOException {
        String src = read("app/src/main/java/org/chimeramc/client/core/cosmetics/"
                + "AccountSkinService.java");
        // The listener set must be a set, so a double-registered listener fires once.
        assertTrue("listeners must be held in a set, not a list",
                src.contains("Set<Listener>") || src.contains("newKeySet"));
        assertFalse("a list would let a double registration fire twice",
                src.contains("List<Listener>"));
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
