package org.chimeramc.client.core.announce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pins the announcement version gate, parsing and selection (item 255). No mocks. */
public class AnnouncementFeedTest {

    private static final String FEED = "{"
            + "\"cards\":["
            + "{\"id\":\"old\",\"title\":\"Old build only\",\"body\":\"b\","
            + "\"maxVersionCode\":100},"
            + "{\"id\":\"new\",\"title\":\"New builds\",\"body\":\"b\","
            + "\"minVersionCode\":200},"
            + "{\"id\":\"all\",\"title\":\"Everyone\",\"body\":\"b\"}"
            + "]}";

    @Test
    public void parsesTheFeedInOrder() {
        List<AnnouncementCard> cards = AnnouncementFeed.parse(FEED);
        assertEquals(3, cards.size());
        assertEquals("old", cards.get(0).id);
        assertEquals("new", cards.get(1).id);
        assertEquals("all", cards.get(2).id);
    }

    @Test
    public void aCardBelowItsFloorDoesNotMatch() {
        List<AnnouncementCard> cards = AnnouncementFeed.parse(FEED);
        AnnouncementCard newer = cards.get(1);
        assertFalse("150 is below the 200 floor", newer.matches(150));
        assertTrue("200 is the floor itself", newer.matches(200));
        assertTrue("250 is above the floor", newer.matches(250));
    }

    @Test
    public void aCardAboveItsCeilingDoesNotMatch() {
        List<AnnouncementCard> cards = AnnouncementFeed.parse(FEED);
        AnnouncementCard old = cards.get(0);
        assertTrue(old.matches(100));
        assertFalse(old.matches(101));
    }

    @Test
    public void anUnboundedCardMatchesEveryBuild() {
        List<AnnouncementCard> cards = AnnouncementFeed.parse(FEED);
        AnnouncementCard all = cards.get(2);
        assertTrue(all.matches(1));
        assertTrue(all.matches(Integer.MAX_VALUE));
    }

    @Test
    public void anInvertedRangeMatchesNothing() {
        // A feed mistake (max below min) must not turn into "matches everything".
        AnnouncementCard inverted = new AnnouncementCard("x", "t", "b", "",
                500, 100);
        assertFalse(inverted.matches(300));
    }

    @Test
    public void aCardWithoutATitleOrBodyIsNotRenderable() {
        assertFalse(new AnnouncementCard("id", "", "body", "", 0, 0).isRenderable());
        assertFalse(new AnnouncementCard("id", "title", "", "", 0, 0).isRenderable());
        assertFalse(new AnnouncementCard("", "title", "body", "", 0, 0).isRenderable());
        assertTrue(new AnnouncementCard("id", "title", "body", "", 0, 0).isRenderable());
    }

    @Test
    public void aMalformedDocumentParsesToAnEmptyList() {
        assertTrue(AnnouncementFeed.parse(null).isEmpty());
        assertTrue(AnnouncementFeed.parse("").isEmpty());
        assertTrue(AnnouncementFeed.parse("not json").isEmpty());
        assertTrue(AnnouncementFeed.parse("[1,2,3]").isEmpty());
        assertTrue(AnnouncementFeed.parse("{\"cards\":\"nope\"}").isEmpty());
    }

    @Test
    public void aMalformedCardIsSkippedNotShownEmpty() {
        List<AnnouncementCard> cards = AnnouncementFeed.parse(
                "{\"cards\":[{\"id\":\"a\"},{\"id\":\"b\",\"title\":\"t\",\"body\":\"b\"}]}");
        assertEquals(1, cards.size());
        assertEquals("b", cards.get(0).id);
    }

    @Test
    public void firstMatchingSkipsDismissedCards() {
        List<AnnouncementCard> cards = AnnouncementFeed.parse(FEED);
        Set<String> dismissed = new HashSet<>(Collections.singletonList("old"));
        AnnouncementCard picked = AnnouncementFeed.firstMatching(cards, 50, dismissed);
        assertNotNull(picked);
        assertEquals("all", picked.id);
    }

    @Test
    public void firstMatchingReturnsNullWhenEverythingIsDismissed() {
        List<AnnouncementCard> cards = AnnouncementFeed.parse(FEED);
        Set<String> dismissed = new HashSet<>(Arrays.asList("old", "new", "all"));
        assertNull(AnnouncementFeed.firstMatching(cards, 250, dismissed));
    }

    @Test
    public void firstMatchingPicksTheHighestPriorityMatch() {
        List<AnnouncementCard> cards = AnnouncementFeed.parse(FEED);
        // At version 250 both "new" and "all" match; feed order is priority order.
        AnnouncementCard picked = AnnouncementFeed.firstMatching(cards, 250, Collections.emptySet());
        assertNotNull(picked);
        assertEquals("new", picked.id);
    }

    @Test
    public void onlyHttpLinksAreFollowable() {
        assertTrue(new AnnouncementCard("a", "t", "b", "https://example.com", 0, 0).hasLink());
        assertTrue(new AnnouncementCard("a", "t", "b", "http://example.com", 0, 0).hasLink());
        assertFalse(new AnnouncementCard("a", "t", "b", "javascript:alert(1)", 0, 0).hasLink());
        assertFalse(new AnnouncementCard("a", "t", "b", "file:///etc/passwd", 0, 0).hasLink());
        assertFalse(new AnnouncementCard("a", "t", "b", "", 0, 0).hasLink());
    }
}
