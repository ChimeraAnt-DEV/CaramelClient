package org.chimeramc.client.docs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The agent docs stay an index that points at things that exist (item 320).
 *
 * <p>{@code AGENTS.md} grew past 2,500 lines and 97 sections, so the file a contributor is told to
 * read first was the last thing anyone would finish. It is now a one-page index over
 * {@code docs/agents/<slug>.md}, one file per area, kept in step by
 * {@code scripts/split_agents_md.py}.
 *
 * <p>A link that points at a deleted file, a section file nothing links to, and a section that was
 * dropped in a re-run are all silent failures - a reader follows the link, lands on a 404, and
 * concludes the docs are stale. This asserts the three properties that make the split real: the
 * index is short, every entry resolves, and every section file is reachable from the index.
 *
 * <p>Reads the real tree, so it runs on the JVM with no device.
 */
public class AgentsDocTest {

    /**
     * The repository root.
     *
     * <p>Gradle runs the unit tests from the module directory, so {@code AGENTS.md} is one level up
     * on the command line and in the module directory when an IDE runs a single test. Probing both
     * keeps the test runnable either way instead of passing in CI and failing in the editor.
     */
    private static final Path ROOT = findRoot();
    private static final Path AGENTS = ROOT.resolve("AGENTS.md");
    private static final Path SECTIONS = ROOT.resolve("docs").resolve("agents");

    private static Path findRoot() {
        Path cwd = Paths.get("").toAbsolutePath();
        for (Path candidate : new Path[]{cwd, cwd.resolve("..")}) {
            if (Files.isRegularFile(candidate.resolve("AGENTS.md"))
                    && Files.isDirectory(candidate.resolve("docs").resolve("agents"))) {
                return candidate.normalize();
            }
        }
        throw new IllegalStateException("cannot locate the repository root (AGENTS.md + docs/agents) "
                + "from " + cwd);
    }

    /** The cap that makes "one page" a fact rather than a hope. */
    private static final int MAX_INDEX_LINES = 160;

    @Test
    public void theIndexIsOnePage() throws IOException {
        assertTrue("AGENTS.md must exist", Files.isRegularFile(AGENTS));
        long lines = Files.readAllLines(AGENTS, StandardCharsets.UTF_8).size();
        assertTrue("AGENTS.md is the index; it grew back to " + lines + " lines, so a section was "
                        + "inlined instead of moved into docs/agents/. Run "
                        + "scripts/split_agents_md.py.",
                lines <= MAX_INDEX_LINES);
    }

    @Test
    public void everyIndexEntryPointsAtAFileThatExists() throws IOException {
        List<String> broken = new ArrayList<>();
        for (String link : indexLinks()) {
            if (!Files.isRegularFile(ROOT.resolve(link))) broken.add(link);
        }
        if (!broken.isEmpty()) {
            fail("AGENTS.md links to files that do not exist:\n  " + String.join("\n  ", broken));
        }
    }

    @Test
    public void everySectionFileIsReachableFromTheIndex() throws IOException {
        List<String> linked = indexLinks().stream()
                .map(l -> Paths.get(l).getFileName().toString())
                .collect(Collectors.toList());
        List<String> orphans = new ArrayList<>();
        try (Stream<Path> files = Files.list(SECTIONS)) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".md"))::iterator) {
                if (!linked.contains(p.getFileName().toString())) orphans.add(p.getFileName().toString());
            }
        }
        if (!orphans.isEmpty()) {
            fail("these docs/agents files are not linked from AGENTS.md, so nothing points a reader "
                    + "at them (a re-run of scripts/split_agents_md.py regenerates the index):\n  "
                    + String.join("\n  ", orphans));
        }
    }

    /**
     * The section files carry the detail, so the index must not be the only copy.
     *
     * <p>An empty or stub section file reads as "documented" while saying nothing, which is worse
     * than an obvious gap: the reader trusts it.
     */
    @Test
    public void noSectionFileIsAnEmptyStub() throws IOException {
        List<String> stubs = new ArrayList<>();
        try (Stream<Path> files = Files.list(SECTIONS)) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".md"))::iterator) {
                long meaningful = Files.readAllLines(p, StandardCharsets.UTF_8).stream()
                        .filter(l -> !l.trim().isEmpty())
                        .filter(l -> !l.trim().startsWith("#"))
                        .count();
                if (meaningful == 0) stubs.add(p.getFileName().toString());
            }
        }
        assertTrue("a section file with no body is a stub: " + stubs, stubs.isEmpty());
    }

    /** The relative links in the index, in the order they appear. */
    private static List<String> indexLinks() throws IOException {
        List<String> links = new ArrayList<>();
        Matcher m = Pattern.compile("\\]\\(([^)]+)\\)").matcher(
                new String(Files.readAllBytes(AGENTS), StandardCharsets.UTF_8));
        while (m.find()) {
            String target = m.group(1).trim();
            if (target.startsWith("http") || target.startsWith("#")) continue;
            links.add(target);
        }
        return links;
    }
}
