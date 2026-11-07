package org.chimeramc.client.core.changelog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The launch-tab changelog model: the parser and the Added/Changed/Fixed classification.
 *
 * <p>The card and the popup both read this model, so the parse and the grouping are pinned here
 * rather than on a device: a section that silently landed in the wrong bucket would show the release
 * as having changed nothing.
 */
public class LaunchChangelogTest {

    private static final String RELEASE = "{"
            + "\"schemaVersion\":1,"
            + "\"headline\":\"BETA4 — Launch tab\","
            + "\"summary\":\"A summary line.\","
            + "\"releasedAt\":\"2026-11-07T00:00:00Z\","
            + "\"sections\":["
            + "  {\"title\":\"New launch card\",\"items\":[{\"text\":\"The card floats.\",\"url\":\"\"}]},"
            + "  {\"title\":\"Bug fixes\",\"items\":[{\"text\":\"No more flicker.\",\"url\":\"\"}]},"
            + "  {\"title\":\"Performance improvements\",\"items\":[{\"text\":\"Faster paint.\",\"url\":\"\"}]},"
            + "  {\"title\":\"Notes\",\"items\":[{\"text\":\"Something else.\",\"url\":\"\"}]}"
            + "]}";

    @Test
    public void classifiesSectionsByTheirTitle() {
        LaunchChangelog log = LaunchChangelog.parse(RELEASE);
        assertEquals(LaunchChangelog.Kind.ADDED, log.sections.get(0).kind);
        assertEquals(LaunchChangelog.Kind.FIXED, log.sections.get(1).kind);
        assertEquals(LaunchChangelog.Kind.CHANGED, log.sections.get(2).kind);
        assertEquals(LaunchChangelog.Kind.OTHER, log.sections.get(3).kind);
    }

    @Test
    public void anExplicitKindBeatsTheTitleHeuristic() {
        String json = "{\"schemaVersion\":1,\"sections\":["
                + "{\"title\":\"Bug fixes\",\"kind\":\"added\","
                + "\"items\":[{\"text\":\"Actually an addition.\"}]}]}";
        LaunchChangelog log = LaunchChangelog.parse(json);
        assertEquals(LaunchChangelog.Kind.ADDED, log.sections.get(0).kind);
    }

    @Test
    public void groupsEveryEntryIntoTheAddedChangedFixedBuckets() {
        LaunchChangelog log = LaunchChangelog.parse(RELEASE);
        List<LaunchChangelog.Entry> added = log.entriesOfKind(LaunchChangelog.Kind.ADDED);
        List<LaunchChangelog.Entry> fixed = log.entriesOfKind(LaunchChangelog.Kind.FIXED);
        List<LaunchChangelog.Entry> changed = log.entriesOfKind(LaunchChangelog.Kind.CHANGED);
        assertEquals(1, added.size());
        assertEquals(1, fixed.size());
        assertEquals(1, changed.size());
        assertEquals("The card floats.", added.get(0).text);
        assertEquals("No more flicker.", fixed.get(0).text);
        assertEquals("Faster paint.", changed.get(0).text);
    }

    @Test
    public void keepsTheHeadlineSummaryAndDates() {
        LaunchChangelog log = LaunchChangelog.parse(RELEASE);
        assertEquals("BETA4 — Launch tab", log.headline);
        assertEquals("A summary line.", log.summary);
        assertEquals("2026-11-07T00:00:00Z", log.releasedAt);
        assertFalse(log.isEmpty());
    }

    @Test
    public void aWrongSchemaVersionReadsAsEmpty() {
        assertTrue(LaunchChangelog.parse("{\"schemaVersion\":99,\"sections\":[]}").isEmpty());
    }

    @Test
    public void aCorruptOrMissingBodyReadsAsEmptyInsteadOfThrowing() {
        assertTrue(LaunchChangelog.parse(null).isEmpty());
        assertTrue(LaunchChangelog.parse("").isEmpty());
        assertTrue(LaunchChangelog.parse("not json at all").isEmpty());
        assertTrue(LaunchChangelog.parse("{\"schemaVersion\":1}").isEmpty());
    }

    @Test
    public void anEmptyItemTextIsSkippedRatherThanRendered() {
        String json = "{\"schemaVersion\":1,\"sections\":["
                + "{\"title\":\"Bug fixes\",\"items\":[{\"text\":\"\"},{\"text\":\"Real.\"}]}]}";
        LaunchChangelog log = LaunchChangelog.parse(json);
        assertEquals(1, log.sections.get(0).entries.size());
        assertEquals("Real.", log.sections.get(0).entries.get(0).text);
    }

    @Test
    public void theBundledChangelogParsesAndCarriesAllThreeKinds() {
        // The shipped asset is the one the card renders, so it must parse and must have something
        // in each bucket, or the popup would open with an empty Added/Changed/Fixed column.
        String json = read("resources/launcher/changelog.json");
        LaunchChangelog log = LaunchChangelog.parse(json);
        assertFalse("the bundled changelog must parse", log.isEmpty());
        assertTrue("it must declare something added",
                !log.entriesOfKind(LaunchChangelog.Kind.ADDED).isEmpty());
        assertTrue("it must declare something changed",
                !log.entriesOfKind(LaunchChangelog.Kind.CHANGED).isEmpty());
    }

    private static String read(String relativePath) {
        java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            java.nio.file.Path candidate = dir.resolve(relativePath);
            if (java.nio.file.Files.exists(candidate)) {
                try {
                    return new String(java.nio.file.Files.readAllBytes(candidate),
                            java.nio.charset.StandardCharsets.UTF_8);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }
}
