package org.chimeramc.client.ui.activities;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The wizard must survive a configuration change.
 *
 * <p>The customise step's theme radios call {@code ThemeManager.setThemeMode}, which recreates the
 * activity so the new theme is visible at once. The activity ignored {@code savedInstanceState}, so
 * that recreation dropped the user back to the first step - the reported "the last two steps show
 * nothing, then jump somewhere else". Saving and restoring the step (and the two bits of progress a
 * recreate would otherwise lose) is what keeps the user on the page they were reading.
 */
public class SetupWizardStateTest {

    @Test
    public void theStepIsSavedAndRestored() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SetupWizardActivity.java");
        assertTrue("the wizard must persist its current step across a recreate",
                activity.contains("onSaveInstanceState"));
        assertTrue("the step must be written to the saved state",
                activity.contains("KEY_STEP"));
        assertTrue("the step must be read back from the saved state",
                activity.contains("savedInstanceState.getString(KEY_STEP)")
                        || activity.contains("savedInstanceState.getString(SetupWizardActivity.KEY_STEP)"));
    }

    @Test
    public void progressAndLicenceAreSavedToo() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SetupWizardActivity.java");
        assertTrue("the accepted licence must survive a recreate",
                activity.contains("KEY_LICENSE_ACCEPTED"));
        assertTrue("the per-run import count must survive a recreate",
                activity.contains("KEY_IMPORTED"));
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
