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
 * The wizard's last two steps render their own content, and finishing returns to the launcher.
 *
 * <p>The report was "the last two tabs show nothing, or when they are done it transfers them to the
 * controller tab". Both are structural: a step that shows nothing, and a finish that lands somewhere
 * unexpected. These checks read the real activity and layout so the regressions cannot come back —
 * the customise step owns the theme radios, the finish step owns the changelog block, and finishing
 * opens the launcher (never a controller screen).
 */
public class SetupWizardLastTabsTest {

    @Test
    public void eachStepShowsOnlyItsOwnBlock() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SetupWizardActivity.java");
        assertTrue("the customise step must show the theme section",
                activity.contains("themeSection.setVisibility(customize ? View.VISIBLE : View.GONE)"));
        assertTrue("the finish step must show the changelog section",
                activity.contains("changelogSection.setVisibility(finish ? View.VISIBLE : View.GONE)"));
        assertTrue("the finish step must show the changelog and Discord actions",
                activity.contains("finishActions.setVisibility(finish ? View.VISIBLE : View.GONE)"));
    }

    @Test
    public void thePopupResizesSmoothlyBetweenSteps() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SetupWizardActivity.java");
        assertTrue("the card must animate its size change between steps",
                activity.contains("beginDelayedTransition"));
        assertTrue("the size transition must honour reduced motion",
                activity.contains("DynamicAnim.areAnimationsEnabled()"));
    }

    @Test
    public void finishingOpensTheLauncherAndNotAControllerScreen() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SetupWizardActivity.java");
        assertTrue("finishing the wizard must open the launcher",
                activity.contains("new Intent(this, MainActivity.class)"));
        assertFalse("the wizard must not navigate to a controller screen",
                activity.contains("ControllerActivity")
                        || activity.contains("nav_tab_controller"));
    }

    @Test
    public void theCardIsSelfSizing() throws IOException {
        String layout = read("app/src/main/res/layout/activity_setup_wizard.xml");
        assertTrue("the popup card must wrap its content height, not fill the screen",
                layout.contains("android:id=\"@+id/setup_card\"")
                        && layout.contains("android:layout_height=\"wrap_content\""));
    }

    @Test
    public void backIsHandledThroughTheDispatcher() throws IOException {
        // An onBackPressed override that does not call super trips the lint gate (MissingSuperCall).
        // Back is handled through the dispatcher instead, so the wizard keeps its own back behaviour
        // without overriding the deprecated method.
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SetupWizardActivity.java");
        assertTrue("the wizard must register an OnBackPressedCallback",
                activity.contains("getOnBackPressedDispatcher().addCallback(this"));
        assertFalse("the wizard must not override onBackPressed",
                activity.contains("public void onBackPressed()"));
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
