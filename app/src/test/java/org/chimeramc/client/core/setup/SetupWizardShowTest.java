package org.chimeramc.client.core.setup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.launcher.core.setup.SetupWizard;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The setup wizard is shown until the user has completed it.
 *
 * <p>It used to require <em>no installed instance</em> as well, so anyone who already had one never
 * saw the wizard at all — which is what testers hit. That condition made sense when the wizard was
 * only "import a game, pick a version"; now the wizard owns the licence, the account, the theme and
 * the changelog, so an existing user still has to go through it. The rule is now one thing: the
 * wizard has not been completed.
 *
 * <p>A build machine cannot show an Activity, so the rule is a pure function and the Activity's use
 * of it is asserted against the real source.
 */
public class SetupWizardShowTest {

    @Test
    public void theWizardIsShownUntilItIsCompleted() {
        assertTrue("a fresh install must see the wizard", SetupWizard.shouldShow(false));
        assertFalse("a completed wizard must not be shown again", SetupWizard.shouldShow(true));
    }

    @Test
    public void theDecisionDoesNotDependOnAnInstalledInstance() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SetupWizardActivity.java");
        int i = source.indexOf("public static boolean shouldShow(");
        assertTrue("shouldShow must exist", i >= 0);
        int end = source.indexOf("}", source.indexOf("return", i));
        String body = source.substring(i, Math.min(source.length(), end + 1));
        assertFalse("shouldShow must not require an empty instance list: an existing user still "
                        + "has to accept the licence and pick a theme",
                body.contains("getInstalledVersions"));
        assertTrue("shouldShow must ask the step machine", body.contains("shouldShow("));
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
