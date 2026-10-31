package org.chimeramc.client.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * LeakCanary is a debug-only dependency and must stay out of release (item 297).
 *
 * <p>Its whole purpose is to watch for activity-context leaks while a developer runs the app, and it
 * carries a heap-analysis engine that has no place in a shipped APK: a few hundred kilobytes of
 * classes, an always-on object watcher, and a notification. Declaring it with {@code implementation}
 * instead of {@code debugImplementation} would ship all of that and never tell anyone, because the
 * build succeeds either way.
 *
 * <p>This reads the real module build script and asserts the declaration is debug-scoped.
 */
public class LeakCanaryScopeTest {

    private static final Pattern LEAKCANARY = Pattern.compile(
            "^\\s*(\\w+)\\s+libs\\.leakcanary\\.android\\b", Pattern.MULTILINE);

    @Test
    public void leakCanaryIsDeclaredAsDebugOnly() throws IOException {
        String build = read(findBuildFile());
        Matcher m = LEAKCANARY.matcher(build);
        assertTrue("leakcanary-android is no longer declared in app/build.gradle", m.find());
        String scope = m.group(1);
        if (!"debugImplementation".equals(scope)) {
            fail("leakcanary-android must be debugImplementation (found '" + scope
                    + "'), or the heap-analysis engine ships in release");
        }
    }

    private static Path findBuildFile() {
        Path candidate = Paths.get("build.gradle");
        if (Files.exists(candidate)) return candidate;
        candidate = Paths.get("app", "build.gradle");
        if (Files.exists(candidate)) return candidate;
        candidate = Paths.get("..", "build.gradle");
        if (Files.exists(candidate)) return candidate;
        throw new IllegalStateException("cannot locate build.gradle from "
                + Paths.get("").toAbsolutePath());
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
