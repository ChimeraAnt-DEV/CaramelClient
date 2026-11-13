package org.chimeramc.client;

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
 * No source file cites a numbered item from an external planning list.
 *
 * <p>The tree used to carry hundreds of comments of the form of a comment that cites a list number, or a trailing
 * {@code}. Those numbers point at a list that lives outside the repository, so they are
 * Meaningless to anyone reading the code: the number names neither the behaviour nor the reason, and
 * A reader who wants the explanation has nowhere to look. Each one has been rewritten as the plain
 * Statement of what the code does and why.
 *
 * <p>This is the gate that keeps them from creeping back. A comment must explain itself in words; a
 * Bare item number is not an explanation. Third-party sources under the {@code libHttpClient}
 * Submodule are out of scope — the pattern there is coincidental English ("255 bytes long"), not a
 * Citation.
 */
public class NoItemNumberCommentsTest {

    private static final Pattern ITEM_REFERENCE = Pattern.compile("\\bItems?\\s+\\d+");

    @Test
    public void noSourceFileCitesAnExternalItemNumber() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String root : new String[]{
                "app/src/main", "app/src/test", "docs", "scripts", ".github",
                "app/src/main/cpp/sweetloader/src", "app/src/main/cpp/sweetloader/include",
                "app/src/main/cpp/sweetloader/tests", "app/src/main/cpp/sweetloader/docs"}) {
            Path dir = projectRoot().resolve(root);
            if (!Files.exists(dir)) continue;
            try (Stream<Path> s = Files.walk(dir)) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    if (!Files.isRegularFile(p) || !isScanned(p)) continue;
                    if (isThirdParty(p)) continue;
                    String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                    Matcher m = ITEM_REFERENCE.matcher(text);
                    while (m.find()) {
                        int line = text.substring(0, m.start()).split("\n", -1).length;
                        offenders.add(projectRoot().relativize(p) + ":" + line + " -> " + m.group());
                    }
                }
            }
        }
        assertTrue("a comment must explain what the code does and why, in words; a numbered item "
                        + "from an external list is not an explanation. Offenders: " + offenders,
                offenders.isEmpty());
    }

    /**
     * Whether a file is third-party source rather than ours.
     *
     * <p>The pattern is coincidental English in code we do not own, so a hit is not a citation. The
     * vendored submodules are excluded, and so is the fetched host-test header cache
     * ({@code app/src/main/cpp/.host-tools}, populated by {@code tests/cosmetics/run_tests.sh}),
     * where fmt's own sources contain the phrase.
     */
    private static boolean isThirdParty(Path p) {
        String path = p.toString().replace('\\', '/');
        return path.contains("libHttpClient") || path.contains(".host-tools");
    }

    private static boolean isScanned(Path p) {
        String file = p.getFileName().toString();
        return file.endsWith(".java") || file.endsWith(".kt") || file.endsWith(".md")
                || file.endsWith(".py") || file.endsWith(".cpp") || file.endsWith(".hpp")
                || file.endsWith(".h") || file.endsWith(".txt") || file.endsWith(".xml")
                || file.endsWith(".sh") || file.endsWith(".yml") || file.endsWith(".yaml");
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
