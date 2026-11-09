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
 * The setup wizard is a self-sizing popup, and its last two steps render their own content.
 *
 * <p>A build machine cannot show the wizard, so the parts that would silently regress are asserted
 * against the real files: the activity's window is translucent (so the launcher shows behind it),
 * the card is {@code wrap_content} (so it is only as tall as the current page), the last two steps'
 * blocks live in this layout rather than on another screen, and the wizard is opened as an overlay
 * from the launcher instead of being a splash destination.
 */
public class SetupWizardPopupTest {

    @Test
    public void theWindowIsATranslucentPopupOverTheLauncher() throws IOException {
        String themes = read("app/src/main/res/values/theme.xml");
        int start = themes.indexOf("name=\"SetupWizardTheme\"");
        assertTrue("the wizard theme must exist", start >= 0);
        String block = themes.substring(start, themes.indexOf("</style>", start));
        assertTrue("the popup window must be translucent so the launcher shows behind it",
                block.contains("windowIsTranslucent\">true"));
        assertTrue("the popup window must have a transparent background",
                block.contains("windowBackground\">@android:color/transparent"));
        assertTrue("the popup must not play an activity transition over the card animation",
                block.contains("windowAnimationStyle\">@null"));

        // API 31+ shows a system splash for every activity; the popup must not flash one.
        String v31 = read("app/src/main/res/values-v31/theme.xml");
        assertTrue("the API 31+ popup theme must blank the system splash",
                v31.contains("name=\"SetupWizardTheme\"")
                        && v31.contains("windowSplashScreenAnimatedIcon\">@android:color/transparent"));

        String manifest = read("app/src/main/AndroidManifest.xml");
        assertTrue("the wizard must use the popup theme",
                manifest.contains("SetupWizardActivity")
                        && manifest.contains("android:theme=\"@style/SetupWizardTheme\""));
    }

    @Test
    public void theCardSizesItselfToThePage() throws IOException {
        String xml = read("app/src/main/res/layout/activity_setup_wizard.xml");
        assertTrue("the popup needs a dim overlay", xml.contains("@+id/setup_overlay"));
        int cardAt = xml.indexOf("@+id/setup_card");
        assertTrue("the popup needs a card", cardAt >= 0);
        int blockStart = xml.lastIndexOf("<LinearLayout", cardAt);
        String card = xml.substring(blockStart, xml.indexOf("</LinearLayout>", cardAt));
        assertTrue("the card must wrap its content so it is only as tall as the page",
                card.contains("android:layout_height=\"wrap_content\""));
        assertFalse("the card must not stretch to fill the screen",
                card.contains("android:layout_height=\"match_parent\""));
    }

    @Test
    public void theLastTwoStepsRenderTheirOwnContent() throws IOException {
        String xml = read("app/src/main/res/layout/activity_setup_wizard.xml");
        assertTrue("the customise step needs a theme picker block",
                xml.contains("@+id/setup_theme_section") && xml.contains("@+id/setup_theme_group"));
        assertTrue("the finish step needs a changelog block",
                xml.contains("@+id/setup_changelog_section")
                        && xml.contains("@+id/setup_changelog_text"));

        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SetupWizardActivity.java");
        assertTrue("the customise step must show its inline block",
                activity.contains("themeSection.setVisibility(customize ? View.VISIBLE : View.GONE)"));
        assertTrue("the finish step must show its inline changelog block",
                activity.contains("changelogSection.setVisibility(finish ? View.VISIBLE : View.GONE)"));
        assertTrue("the finish step must load the bundled changelog",
                activity.contains("loadChangelogInline()"));
        assertFalse("the customise step must not navigate to CustomizeActivity (its content is inline)",
                activity.contains("startActivity(new Intent(this, CustomizeActivity.class))"));
        assertTrue("the card must pop in on a step change",
                activity.contains("DynamicAnim.overshootScaleIn(card"));
    }

    @Test
    public void theLauncherOpensTheWizardAsAnOverlay() throws IOException {
        String main = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "MainActivity.java");
        assertTrue("the launcher must own opening the wizard overlay",
                main.contains("showSetupWizardIfNeeded"));
        assertTrue("the overlay must be opened once per process",
                main.contains("private static boolean sWizardOverlayLaunched"));

        String splash = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SplashActivity.java");
        assertFalse("the splash must not route to the wizard as a full-screen destination",
                splash.contains("destination = org.chimeramc.client.ui.activities.SetupWizardActivity.class"));
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
