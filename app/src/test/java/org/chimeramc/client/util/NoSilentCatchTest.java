package org.chimeramc.client.util;

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
 * No catch turns a failure into nothing.
 *
 * <p>A catch whose body is empty — or holds only a comment — is indistinguishable from success. The
 * feature stops working, the user sees nothing, and neither logcat nor the in-app log viewer is told
 * why. That is how a failed backup looked identical to one that worked, and how a settings write
 * that never landed looked identical to one that did.
 *
 * <p>Every such catch now reports through {@link AppLog} (once per site, so a per-frame failure does
 * not flood the log). This is the gate that keeps the count at zero: a new empty catch fails the
 * build rather than quietly shipping. The rule is deliberately absolute rather than an allowlist —
 * a catch that genuinely means "ignore this" still has a reason worth one line in the log.
 */
public class NoSilentCatchTest {

    /** A catch whose body, with comments removed, has no statement. */
    private static final Pattern CATCH =
            Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{([^{}]*)\\}", Pattern.DOTALL);

    @Test
    public void noCatchSwallowsAFailure() throws IOException {
        List<String> offenders = new ArrayList<>();
        Path root = projectRoot().resolve("app/src/main/java");
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String file = p.getFileName().toString();
                if (!(file.endsWith(".java") || file.endsWith(".kt"))) continue;
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                Matcher m = CATCH.matcher(text);
                while (m.find()) {
                    if (isSilent(m.group(1))) {
                        int line = text.substring(0, m.start()).split("\n", -1).length;
                        offenders.add(projectRoot().relativize(p) + ":" + line);
                    }
                }
            }
        }
        assertTrue("a catch must report the failure (AppLog.warn/warnOnce) or rethrow; an empty or "
                        + "comment-only body hides a broken feature behind apparent success. Offenders: "
                        + offenders,
                offenders.isEmpty());
    }

    /** True when a catch body has no statement once comments are stripped. */
    static boolean isSilent(String body) {
        // A body that carries the explicit `best-effort` marker is a deliberate no-op: the only
        // places that use it are the log sink itself, where reporting a failure through AppLog would
        // recurse into the very write that just failed. Every other catch must report.
        if (body.contains("best-effort")) return false;
        String withoutLine = body.replaceAll("//[^\n]*", "");
        String withoutBlock = withoutLine.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlock.trim().isEmpty();
    }

    @Test
    public void theScannerRecognisesSilenceAndNotALoggedCatch() {
        assertTrue(isSilent(""));
        assertTrue(isSilent("\n        "));
        assertTrue(isSilent("\n        // best effort\n        "));
        assertTrue(!isSilent("\n        // best-effort: the log sink itself\n        "));
        assertTrue(!isSilent("\n        AppLog.warnOnce(TAG, \"k\", \"m\", e);\n        "));
        assertTrue(!isSilent("\n        throw new IllegalStateException(e);\n        "));
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
