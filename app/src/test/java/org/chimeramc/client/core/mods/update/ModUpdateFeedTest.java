package org.chimeramc.client.core.mods.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

/** Pins the mod-update feed parser, including its tolerance of malformed input. */
public class ModUpdateFeedTest {

    @Test
    public void parsesIdVersionPairs() {
        Map<String, String> feed = ModUpdateFeed.parse(
                "{\"mods\":[{\"id\":\"a\",\"version\":\"1.2.0\"},{\"id\":\"b\",\"version\":\"2.0\"}]}");
        assertEquals(2, feed.size());
        assertEquals("1.2.0", feed.get("a"));
        assertEquals("2.0", feed.get("b"));
    }

    @Test
    public void aMalformedDocumentYieldsAnEmptyMap() {
        assertTrue(ModUpdateFeed.parse(null).isEmpty());
        assertTrue(ModUpdateFeed.parse("").isEmpty());
        assertTrue(ModUpdateFeed.parse("   ").isEmpty());
        assertTrue(ModUpdateFeed.parse("not json").isEmpty());
        assertTrue(ModUpdateFeed.parse("[]").isEmpty());
        assertTrue(ModUpdateFeed.parse("{\"mods\":\"nope\"}").isEmpty());
        assertTrue(ModUpdateFeed.parse("{\"other\":[]}").isEmpty());
    }

    @Test
    public void anEntryMissingIdOrVersionIsSkippedButTheRestSurvives() {
        Map<String, String> feed = ModUpdateFeed.parse(
                "{\"mods\":[{\"id\":\"a\"},{\"version\":\"1.0\"},{\"id\":\"c\",\"version\":\"3.0\"}]}");
        assertEquals(1, feed.size());
        assertEquals("3.0", feed.get("c"));
    }

    @Test
    public void aNonObjectEntryIsSkipped() {
        Map<String, String> feed = ModUpdateFeed.parse(
                "{\"mods\":[1,\"two\",{\"id\":\"d\",\"version\":\"4.0\"}]}");
        assertEquals(1, feed.size());
        assertEquals("4.0", feed.get("d"));
    }

    @Test
    public void aNullVersionIsSkipped() {
        assertTrue(ModUpdateFeed.parse(
                "{\"mods\":[{\"id\":\"a\",\"version\":null}]}").isEmpty());
    }

    @Test
    public void aNumericVersionIsReadAsItsText() {
        Map<String, String> feed = ModUpdateFeed.parse("{\"mods\":[{\"id\":\"a\",\"version\":2}]}");
        assertEquals("2", feed.get("a"));
    }
}
