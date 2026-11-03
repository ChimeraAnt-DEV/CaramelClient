package org.chimeramc.client;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A large source file may only get smaller.
 *
 * <p>The client's biggest classes and layouts had been growing for years — ModMenuOverlay past 3,300
 * lines, MainActivity past 3,000, activity_settings.xml past 2,400 — because nothing stopped them.
 * Every new feature was cheaper to add to an existing file than to open a new one, so the files that
 * are hardest to reason about were the ones that kept absorbing work.
 *
 * <p>This is the ratchet. The current size of every file at or above the threshold is recorded in
 * {@code scripts/hygiene/file-sizes-baseline.txt}; a file may shrink (the baseline is then lowered)
 * but never grow, and a new file may not cross the threshold at all. Splitting a file is always
 * allowed and is what lowering an entry looks like. Third-party sources under the {@code
 * libHttpClient} submodule are out of scope.
 */
public class FileSizeBudgetTest {

    /** A file at or above this many lines is "large" and must not grow. */
    private static final int THRESHOLD = 1500;

    @Test
    public void noLargeFileGrowsAndNoNewOneCrossesTheThreshold() throws IOException {
        Map<String, Integer> baseline = readBaseline();
        List<String> offenders = new ArrayList<>();
        Path root = projectRoot().resolve("app/src/main/java");
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (p.toString().contains("libHttpClient")) continue;
                String file = p.getFileName().toString();
                if (!(file.endsWith(".java") || file.endsWith(".kt"))) continue;
                int lines = (int) Files.readAllLines(p, StandardCharsets.UTF_8).size();
                if (lines < THRESHOLD) continue;
                String rel = projectRoot().relativize(p).toString();
                Integer recorded = baseline.get(rel);
                if (recorded == null) {
                    offenders.add(rel + " is a new file at " + lines + " lines (threshold "
                            + THRESHOLD + "); split it or lower it under the threshold");
                } else if (lines > recorded) {
                    offenders.add(rel + " grew from " + recorded + " to " + lines
                            + " lines; split it instead of growing it");
                }
            }
        }
        assertTrue("a large file may only shrink. " + offenders, offenders.isEmpty());
    }

    private static Map<String, Integer> readBaseline() throws IOException {
        Path path = projectRoot().resolve("scripts/hygiene/file-sizes-baseline.txt");
        Map<String, Integer> out = new HashMap<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int space = trimmed.lastIndexOf(' ');
            out.put(trimmed.substring(0, space), Integer.parseInt(trimmed.substring(space + 1)));
        }
        return out;
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
