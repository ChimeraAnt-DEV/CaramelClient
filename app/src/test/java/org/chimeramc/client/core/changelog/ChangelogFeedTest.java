package org.chimeramc.client.core.changelog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The changelog feed: a "newest" section, an "upcoming" section and sneak-peek entries.
 *
 * <p>The changelog was one flat list, so there was nowhere to say "this is what just shipped" apart
 * from "this is what is coming" — and no way to carry the Discord sneak peeks the team posts. The
 * feed is the two-section shape the spec asks for, with each entry able to carry a sneak-peek image
 * and caption, and it is pure so the parsing and the ordering are unit-tested without a device.
 */
public class ChangelogFeedTest {

    private static final String JSON = "{"
            + "\"schemaVersion\":1,"
            + "\"newest\":{"
            + "  \"version\":\"BETA3\","
            + "  \"headline\":\"Glass, capes and a replay engine\","
            + "  \"items\":[{\"text\":\"Buttons now press.\"},{\"text\":\"Capes sample authored art.\"}]"
            + "},"
            + "\"upcoming\":{"
            + "  \"headline\":\"What is next\","
            + "  \"items\":[{\"text\":\"Cinematic replay recorder.\"}]"
            + "},"
            + "\"sneakPeeks\":["
            + "  {\"caption\":\"New launch stage\",\"image\":\"sneak_launch.png\"},"
            + "  {\"caption\":\"Cape physics\",\"image\":\"sneak_cape.png\"}"
            + "]"
            + "}";

    @Test
    public void itParsesTheTwoSections() {
        ChangelogFeed feed = ChangelogFeed.parse(JSON);
        assertTrue(feed.hasNewest());
        assertTrue(feed.hasUpcoming());
        assertEquals("BETA3", feed.newest().version);
        assertEquals("Glass, capes and a replay engine", feed.newest().headline);
        assertEquals(2, feed.newest().items.size());
        assertEquals("What is next", feed.upcoming().headline);
        assertEquals(1, feed.upcoming().items.size());
    }

    @Test
    public void itParsesTheSneakPeeks() {
        ChangelogFeed feed = ChangelogFeed.parse(JSON);
        assertEquals(2, feed.sneakPeeks().size());
        assertEquals("New launch stage", feed.sneakPeeks().get(0).caption);
        assertEquals("sneak_launch.png", feed.sneakPeeks().get(0).image);
    }

    @Test
    public void aMissingSectionIsEmptyNotAnError() {
        ChangelogFeed feed = ChangelogFeed.parse("{\"schemaVersion\":1,\"newest\":{\"headline\":\"x\"}}");
        assertTrue(feed.hasNewest());
        assertFalse("a missing upcoming section is empty", feed.hasUpcoming());
        assertEquals(0, feed.upcoming().items.size());
        assertEquals(0, feed.sneakPeeks().size());
    }

    @Test
    public void aCorruptOrForeignBodyReadsAsEmpty() {
        assertFalse(ChangelogFeed.parse(null).hasNewest());
        assertFalse(ChangelogFeed.parse("").hasNewest());
        assertFalse(ChangelogFeed.parse("not json").hasNewest());
        // A wrong schema version is refused, matching the existing changelog loader.
        assertFalse(ChangelogFeed.parse("{\"schemaVersion\":99,\"newest\":{\"headline\":\"x\"}}")
                .hasNewest());
    }

    @Test
    public void aSneakPeekWithoutAnImageIsDropped() {
        // An entry with no image has nothing to show; keeping it would draw an empty tile.
        ChangelogFeed feed = ChangelogFeed.parse("{\"schemaVersion\":1,\"sneakPeeks\":["
                + "{\"caption\":\"no image\"},{\"caption\":\"ok\",\"image\":\"a.png\"}]}");
        assertEquals(1, feed.sneakPeeks().size());
        assertEquals("ok", feed.sneakPeeks().get(0).caption);
    }

    @Test
    public void theLineIsTheSectionsJoinedForALoadingScreen() {
        // The loading screen shows one line; it must read as both sections, not just the newest.
        ChangelogFeed feed = ChangelogFeed.parse(JSON);
        String line = feed.headlineLine();
        assertTrue(line.contains("Glass, capes and a replay engine"));
        assertTrue(line.contains("What is next"));
    }
}
