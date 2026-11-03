package org.chimeramc.client.launcher.core.changelog;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The repo's {@code CHANGELOG.md} is the source the in-app screen renders, so it must
 * exist and parse into something a user can read. A file that only holds a title would open a blank
 * screen, which is the failure this pins.
 */
public class ChangelogAssetTest {

    /** Gradle runs unit tests from the module directory, so the repo root is one level up. */
    private static final Path CHANGELOG = Paths.get("..", "CHANGELOG.md");

    @Test
    public void theRepoChangelogExistsAndParsesIntoContent() throws Exception {
        assertTrue("CHANGELOG.md must exist at the repo root", Files.exists(CHANGELOG));
        String markdown = new String(Files.readAllBytes(CHANGELOG), StandardCharsets.UTF_8);
        List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse(markdown);
        assertTrue("the changelog must parse into renderable blocks",
                ChangelogMarkdown.hasContent(blocks));
        boolean hasVersion = blocks.stream().anyMatch(b -> b.kind == ChangelogMarkdown.Kind.VERSION);
        assertTrue("the changelog must name at least one release heading", hasVersion);
    }
}
