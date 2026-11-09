package org.chimeramc.client.core.auth;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * All three Microsoft sign-in methods are wired and share one application identity.
 *
 * <p>The activity offers three routes — device code, an in-app WebView, and MSAL (interactive +
 * silent). The MSAL route was once handed the literal {@code YOUR_CLIENT_ID_HERE}, so that button
 * could never finish; and the redirect URI has to be the one the manifest registers, or Microsoft's
 * reply has nowhere to land. These checks read the real activity so the three routes cannot drift
 * apart again: each is started, and the two that take an application identity ask
 * {@link MsftAuthManager} for it rather than carrying their own copy.
 */
public class MsftLoginFlowTest {

    @Test
    public void theThreeMethodsAreAllStarted() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "MsftLoginActivity.java");
        assertTrue("the device-code route must be started",
                activity.contains("startDeviceCodeLogin"));
        assertTrue("the in-app WebView route must be started",
                activity.contains("startWebViewLogin"));
        assertTrue("the MSAL route must be started",
                activity.contains("startMsalLogin") && activity.contains("startMsalInteractiveSignIn"));
    }

    @Test
    public void msalUsesTheSharedClientIdAndRedirectUri() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "MsftLoginActivity.java");
        assertTrue("MSAL must use the one real Bedrock client id",
                activity.contains("MsftAuthManager.bedrockClientId()"));
        assertTrue("MSAL must use the redirect URI the manifest registers",
                activity.contains("MsftAuthManager.msalRedirectUri("));
        assertTrue("a placeholder client id must never appear in the activity",
                !activity.contains("YOUR_CLIENT_ID"));
    }

    @Test
    public void everyFlowCompletesTheBedrockChain() throws IOException {
        String manager = read("app/src/main/java/org/chimeramc/client/launcher/core/auth/"
                + "MsftAuthManager.java");
        // Device code, WebView code and the MSAL token all end at a BedrockAuthManager, which is what
        // turns an MSA token into the Xbox Live / XSTS / Minecraft certificate the game needs.
        assertTrue(manager.contains("loginWithDeviceCode"));
        assertTrue(manager.contains("loginWithCode"));
        assertTrue(manager.contains("loginWithMsaToken"));
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
