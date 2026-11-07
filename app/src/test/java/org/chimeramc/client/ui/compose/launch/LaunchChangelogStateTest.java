package org.chimeramc.client.ui.compose.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.changelog.LaunchChangelog;
import org.junit.Test;

/**
 * The mapping from the parsed release notes to the Compose state.
 *
 * <p>The card counts these buckets and the popup lists them, so a mapping that put a note in the
 * wrong bucket would show the release as having fixed nothing while the raw file said otherwise.
 */
public class LaunchChangelogStateTest {

    private static final String RELEASE = "{"
            + "\"schemaVersion\":1,"
            + "\"headline\":\"BETA4\","
            + "\"summary\":\"Summary.\","
            + "\"sections\":["
            + "  {\"title\":\"New launch card\",\"items\":[{\"text\":\"A.\"}]},"
            + "  {\"title\":\"Bug fixes\",\"items\":[{\"text\":\"B.\"}]},"
            + "  {\"title\":\"Performance improvements\",\"items\":[{\"text\":\"C.\"}]},"
            + "  {\"title\":\"Notes\",\"items\":[{\"text\":\"D.\"}]}"
            + "]}";

    @Test
    public void mapsEachSectionIntoItsBucketAndKeepsTheVersion() {
        LaunchChangelogState state = LaunchChangelogStateBuilder.build(
                LaunchChangelog.parse(RELEASE), "BETA4");
        assertEquals("BETA4", state.getVersion());
        assertEquals("BETA4", state.getHeadline());
        assertEquals("Summary.", state.getSummary());
        assertEquals(1, state.getAdded().size());
        assertEquals(1, state.getFixed().size());
        assertEquals(1, state.getChanged().size());
        assertEquals(1, state.getOther().size());
        assertEquals(4, state.getTotalCount());
        assertFalse(state.isEmpty());
    }

    @Test
    public void anEmptyChangelogIsEmptyAndCountsZero() {
        LaunchChangelogState state = LaunchChangelogStateBuilder.build(
                LaunchChangelog.parse("{}"), "BETA4");
        assertTrue(state.isEmpty());
        assertEquals(0, state.getTotalCount());
        assertEquals("BETA4", state.getVersion());
    }

    @Test
    public void aNullChangelogIsTreatedAsEmptyRatherThanThrowing() {
        LaunchChangelogState state = LaunchChangelogStateBuilder.build(null, null);
        assertTrue(state.isEmpty());
        assertEquals("", state.getVersion());
    }

    @Test
    public void entriesCarryTheirLink() {
        String json = "{\"schemaVersion\":1,\"sections\":[{\"title\":\"Bug fixes\","
                + "\"items\":[{\"text\":\"Linked.\",\"url\":\"https://example.com/x\"}]}]}";
        LaunchChangelogState state = LaunchChangelogStateBuilder.build(
                LaunchChangelog.parse(json), "BETA4");
        assertEquals(1, state.getFixed().size());
        assertTrue(state.getFixed().get(0).getHasUrl());
        assertEquals("https://example.com/x", state.getFixed().get(0).getUrl());
    }
}
