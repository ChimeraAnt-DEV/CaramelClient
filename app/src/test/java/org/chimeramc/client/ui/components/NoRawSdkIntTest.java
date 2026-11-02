package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertTrue;

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
 * API-level gates go through {@code ApiLevels}, never a raw comparison (items 26, 310).
 *
 * <p>{@code ApiLevels} is the one owner of every version gate, with a named predicate per boundary
 * so a JVM test can assert both sides without a device. That only holds while nothing writes
 * {@code Build.VERSION.SDK_INT >= N} inline again: an inline gate cannot be tested on both sides and
 * has no single place to update when a level moves.
 *
 * <p>{@code ApiLevels} itself is the one file allowed to read {@link android.os.Build} — it is the
 * reader. Everything else must ask a predicate.
 */
public class NoRawSdkIntTest {

    private static final Path SOURCE_ROOT = Paths.get("src", "main", "java");

    /** A raw comparison against the SDK level, in either direction. */
    private static final Pattern RAW_GATE = Pattern.compile(
            "SDK_INT\\s*(?:>=|<=|>|<|==|!=)"
                    + "|(?:>=|<=|>|<|==|!=)\\s*Build\\.VERSION\\.SDK_INT"
                    + "|Build\\.VERSION_CODES\\.[A-Z_]+\\s*(?:>=|<=|>|<)");

    /** The single reader of the platform level. */
    private static final String ALLOWED = "ApiLevels.java";

    @Test
    public void noFileComparesTheSdkLevelInline() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaFiles()) {
            if (file.getFileName().toString().equals(ALLOWED)) continue;
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher m = RAW_GATE.matcher(text);
            while (m.find()) {
                offenders.add(file.getFileName() + " -> " + m.group());
            }
        }
        assertTrue("every API gate must go through ApiLevels (a named predicate, testable on both "
                + "sides); a raw SDK_INT comparison is a gate with no single owner. Offenders: "
                + offenders, offenders.isEmpty());
    }

    private static List<Path> javaFiles() throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".kt"))
                    .forEach(out::add);
        }
        return out;
    }
}
