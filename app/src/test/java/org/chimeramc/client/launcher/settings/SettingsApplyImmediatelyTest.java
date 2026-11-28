package org.chimeramc.client.launcher.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.ui.dialogs.LogcatOverlayVisibility;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A switch takes effect when it is flipped, not at the next launch.
 *
 * <p>The logcat overlay was reported as "needs a restart to register". The cause was a listener
 * registered inline: {@code registerOnSharedPreferenceChangeListener} holds its listener weakly, so
 * the lambda was collected at the next GC and the overlay stopped answering the switch — it only
 * came back when an activity resume re-read the value, which a user reads as "a restart fixes it".
 *
 * <p>Two rules come out of that. The show/hide decision is a pure function so it can be tested
 * here, and no shipped source may pass a lambda or a method reference straight to
 * {@code registerOnSharedPreferenceChangeListener} — it has to be a field, which is a strong
 * reference that lives as long as the object that registered it.
 */
public class SettingsApplyImmediatelyTest {

    @Test
    public void theOverlayFollowsTheSwitch() {
        assertFalse(LogcatOverlayVisibility.shouldShow(false, false, false));
        assertTrue(LogcatOverlayVisibility.shouldShow(true, false, false));
        assertFalse("the splash screen owns the window",
                LogcatOverlayVisibility.shouldShow(true, true, false));
        assertFalse("the loading screen owns the window",
                LogcatOverlayVisibility.shouldShow(true, false, true));
    }

    @Test
    public void noPreferenceListenerIsRegisteredInline() throws IOException {
        Pattern call = Pattern.compile(
                "registerOnSharedPreferenceChangeListener\\s*\\(([^;]*)\\)\\s*;", Pattern.DOTALL);
        List<String> offenders = new ArrayList<>();
        Path root = projectRoot().resolve("app/src/main/java");
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String name = p.getFileName().toString();
                if (!(name.endsWith(".java") || name.endsWith(".kt"))) continue;
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                Matcher m = call.matcher(text);
                while (m.find()) {
                    String argument = m.group(1).trim();
                    if (argument.contains("->") || argument.startsWith("(") || argument.contains("::")) {
                        int line = text.substring(0, m.start()).split("\n", -1).length;
                        offenders.add(projectRoot().relativize(p) + ":" + line);
                    }
                }
            }
        }
        assertTrue("a preference listener passed inline is only weakly held and is collected at the "
                + "next GC, after which the setting silently stops applying until a restart. Keep it "
                + "in a field. Offenders: " + offenders, offenders.isEmpty());
    }

    private static Path projectRoot() throws IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.isDirectory(dir.resolve("app/src/main/java"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
