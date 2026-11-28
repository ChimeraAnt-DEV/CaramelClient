package org.chimeramc.client.ui.compose.caramel.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The HoneyClient announcement and the in-app browser it opens.
 *
 * <p>Two things the defect asked for that a device test would otherwise own: a third news post that
 * says HoneyClient is coming, whose yellow button opens {@code honeyclient.github.io} inside the
 * launcher rather than handing the user to the system browser. Without an emulator these cases pin
 * the post, its link, the yellow tone, and the WebView modal that hosts the page.
 */
public class CaramelHoneyNewsTest {

    private static final Path ROOT = findRoot();

    @Test
    public void theAnnouncementIsAddedToTheNewsList() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/core/changelog/BundledNews.java");
        int at = activity.indexOf("caramel_news_honey_title");
        assertTrue("the HoneyClient announcement is not built into the news list", at >= 0);
        String call = activity.substring(at, Math.min(activity.length(), at + 600));
        assertTrue("the announcement must carry the HoneyClient link",
                call.contains("https://honeyclient.github.io/"));
        assertTrue("the announcement must supply a button label",
                call.contains("caramel_news_honey_button"));
    }

    @Test
    public void thePostSaysWhatTheUserAskedItToSay() throws IOException {
        String strings = read("app/src/main/res/values/strings.xml");
        assertTrue(stringContains(strings, "caramel_news_honey_title", "HoneyClient coming soon.."));
        // The label used to read "Click me for more information!", which read as machine-written
        // filler. A short, conventional action label is what a shipped launcher would use.
        assertTrue(stringContains(strings, "caramel_news_honey_button", "Learn more"));
    }

    @Test
    public void theActionButtonIsProfessionalAndOpensTheInAppBrowser() throws IOException {
        String modals = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelModals.kt");
        int at = modals.indexOf("post.actionLabel");
        assertTrue("the news modal has no action button", at >= 0);
        String region = modals.substring(at, Math.min(modals.length(), at + 500));
        assertFalse("the button must not use the loud yellow promo tone",
                region.contains("tone = Tone.Yellow"));
        assertTrue("the button must carry an action icon", region.contains("icon = Icons.Rounded.OpenInNew"));
        assertTrue("the button must set the in-app browser's URL", region.contains("state.webUrl = post.actionUrl"));

        String controls = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelControls.kt");
        assertTrue("Tone.Yellow must exist", controls.contains("Yellow("));

        String home = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelHome.kt");
        assertTrue("the Home news card must offer the action too",
                home.contains("state.webUrl = post.actionUrl"));
        assertFalse("the Home card must not use the loud yellow promo tone",
                home.contains("tone = Tone.Yellow"));
        assertTrue("the Home card must wear the same action icon as the modal",
                home.contains("icon = Icons.Rounded.OpenInNew"));
    }

    @Test
    public void theBrowserIsAWebViewInsideTheLauncher() throws IOException {
        String web = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelWebModal.kt");
        assertTrue("the page must be a real WebView", web.contains("WebView(context)"));
        assertTrue("the page must not leave for the system browser", web.contains("shouldOverrideUrlLoading"));
        assertTrue("the modal must close", web.contains("state.webUrl = \"\""));

        String screen = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelLauncherScreen.kt");
        assertTrue("the browser is not rendered, so the button opens nothing",
                screen.contains("CaramelWebModal("));

        // The only place a link may leave the app is the system browser; the announcement must not
        // use an ACTION_VIEW intent.
        assertFalse("the announcement must not fire a system-browser intent",
                web.contains("Intent.ACTION_VIEW"));
    }

    @Test
    public void everyLocaleCarriesTheAnnouncement() throws IOException {
        for (String locale : new String[]{"es", "fr", "hi", "id", "ja", "pt", "ru", "tr", "vi",
                "zh-rCN"}) {
            String strings = read("app/src/main/res/values-" + locale + "/strings.xml");
            for (String key : new String[]{"caramel_news_honey_title", "caramel_news_honey_body",
                    "caramel_news_honey_meta", "caramel_news_honey_button"}) {
                assertTrue(key + " is not translated for " + locale,
                        strings.contains("name=\"" + key + "\""));
            }
        }
    }

    private static boolean stringContains(String strings, String key, String text) {
        Matcher m = Pattern.compile("<string name=\"" + key + "\"[^>]*>(.*?)</string>",
                Pattern.DOTALL).matcher(strings);
        assertEquals("no string named " + key, true, m.find());
        return m.group(1).trim().equals(text);
    }

    private static Path findRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.isRegularFile(dir.resolve("app/src/main/AndroidManifest.xml"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("cannot locate the repository root");
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(ROOT.resolve(relative)), StandardCharsets.UTF_8);
    }
}
