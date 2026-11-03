package org.chimeramc.client.ui.activities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The setup wizard is the 5-step checkbox-gated flow, and its licence is truthful.
 *
 * <p>A build machine cannot walk the wizard, so the parts that would silently regress are asserted
 * against the real files: the layout carries the licence checkbox and the five steps, the licence
 * text states the Google Play limitation (it used to claim support the client does not have), and the
 * EULA dialog and the launch-tab Discord card are gone (the licence and the invite now live in the
 * wizard).
 */
public class SetupWizardResourcesTest {

    @Test
    public void theLayoutCarriesTheLicenceCheckboxAndTheFiveStepControls() throws IOException {
        String xml = read("app/src/main/res/layout/activity_setup_wizard.xml");
        assertTrue("the licence step needs an acceptance checkbox",
                xml.contains("@+id/setup_license_check")
                        && xml.contains("MaterialCheckBox"));
        assertTrue("the wizard needs a scrollable terms view", xml.contains("@+id/setup_terms_scroll"));
        assertTrue("the wizard needs Next, Back and Skip", xml.contains("@+id/setup_primary")
                && xml.contains("@+id/setup_back") && xml.contains("@+id/setup_skip"));
        assertTrue("step 5 needs the changelog and Discord actions",
                xml.contains("@+id/setup_changelog") && xml.contains("@+id/setup_discord"));
    }

    @Test
    public void theLicenceStatesTheGooglePlayLimitation() throws IOException {
        String strings = read("app/src/main/res/values/strings.xml");
        int start = strings.indexOf("name=\"setup_license_terms\"");
        assertTrue("the licence terms must exist", start >= 0);
        int end = strings.indexOf("</string>", start);
        String terms = strings.substring(start, end);
        assertTrue("the licence must say the Google Play version is not fully supported",
                terms.contains("Google Play"));
        assertTrue("the licence must say it is unofficial",
                terms.contains("unofficial") || terms.contains("not affiliated"));
        assertTrue("the licence must require a genuine copy",
                terms.contains("genuine") || terms.contains("licensed"));
    }

    @Test
    public void theOldEulaDialogAndTheLaunchTabDiscordCardAreGone() throws IOException {
        String main = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java");
        assertFalse("the Accept/Decline EULA dialog must be gone (the wizard owns the licence)",
                main.contains("showEulaDialog"));
        String launch = read("app/src/main/res/layout/activity_main.xml");
        assertFalse("the Discord card must not be on the launch tab",
                launch.contains("@string/discord_join"));
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
