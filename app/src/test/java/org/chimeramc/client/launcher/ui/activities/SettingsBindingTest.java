package org.chimeramc.client.launcher.ui.activities;

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
 * The Settings screen binds its generated binding against the inflated settings root.
 *
 * <p>It used to bind against {@code android.R.id.content}, which is the activity's
 * {@code ContentFrameLayout} — not the layout's own root, a {@code LinearLayout}. The generated
 * {@code bind} casts to the layout's root type, so the cast threw a {@link ClassCastException} the
 * moment the screen opened, which is the crash testers hit on Settings, and the same mistake in the
 * cosmetic-sync lookup at the bottom of the file.
 *
 * <p>A build machine cannot open the screen, so the rule is asserted against the real source: the
 * binding is never bound against {@code android.R.id.content}, and the settings layout's root type
 * is what the binding expects.
 */
public class SettingsBindingTest {

    private static final Pattern BIND_AGAINST_CONTENT = Pattern.compile(
            "ActivitySettingsBinding\\.bind\\(\\s*findViewById\\(android\\.R\\.id\\.content\\)");

    @Test
    public void theBindingIsNeverBoundAgainstTheActivityContentFrame() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "SettingsActivity.java");
        assertFalse("binding against android.R.id.content casts a ContentFrameLayout to the "
                        + "layout's root type and crashes the screen",
                BIND_AGAINST_CONTENT.matcher(source).find());
    }

    @Test
    public void theSettingsLayoutRootMatchesWhatTheBindingExpects() throws IOException {
        String layout = read("app/src/main/res/layout/activity_settings.xml");
        // The generated binding casts to the layout's root type; assert that root is a LinearLayout
        // (what the code assumes) so a change to the layout's root cannot silently break the bind.
        Matcher m = Pattern.compile("<([A-Za-z0-9_.]+)\\s").matcher(layout);
        assertTrue(m.find());
        String root = m.group(1);
        assertEquals("the settings layout root type changed; the binding and this test must agree",
                "LinearLayout", root);
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
