package org.chimeramc.client.core.news;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * What a remotely published announcement is allowed to become.
 *
 * <p>The document is the one input in this class that arrives from outside the build, so its rules are
 * pinned: a post needs a title and a body, an expired post is dropped, a repeated id is shown once, and
 * a document that is not JSON yields no posts rather than an exception in the News tab.
 */
public class EmergencyNewsTest {

    private static final String NOW = "2026-11-28T00:00:00Z";

    private static long now() {
        return EmergencyNews.parseInstant(NOW);
    }

    @Test
    public void aWellFormedPostIsRead() {
        String json = "{\"schemaVersion\":1,\"posts\":[{"
                + "\"id\":\"outage-1\",\"title\":\"Sign-in is down\","
                + "\"body\":\"Xbox Live is rejecting tokens. Nothing to reinstall.\","
                + "\"meta\":\"Status\",\"url\":\"https://status.example\"}]}";
        List<EmergencyNews.Post> posts = EmergencyNews.parse(json, now());
        assertEquals(1, posts.size());
        assertEquals("outage-1", posts.get(0).id);
        assertEquals("Sign-in is down", posts.get(0).title);
        assertEquals("Status", posts.get(0).meta);
        assertEquals("https://status.example", posts.get(0).url);
    }

    @Test
    public void aPostWithoutATitleOrBodyIsNotShown() {
        String json = "{\"posts\":["
                + "{\"id\":\"a\",\"title\":\"\",\"body\":\"body\"},"
                + "{\"id\":\"b\",\"title\":\"title\",\"body\":\"   \"},"
                + "{\"id\":\"c\",\"title\":\"Kept\",\"body\":\"Kept body\"}]}";
        List<EmergencyNews.Post> posts = EmergencyNews.parse(json, now());
        assertEquals(1, posts.size());
        assertEquals("Kept", posts.get(0).title);
    }

    @Test
    public void anExpiredPostIsDropped() {
        String json = "{\"posts\":["
                + "{\"id\":\"old\",\"title\":\"Old\",\"body\":\"Old body\","
                + "\"expiresAt\":\"2026-01-01T00:00:00Z\"},"
                + "{\"id\":\"new\",\"title\":\"New\",\"body\":\"New body\","
                + "\"expiresAt\":\"2027-01-01T00:00:00Z\"}]}";
        List<EmergencyNews.Post> posts = EmergencyNews.parse(json, now());
        assertEquals(1, posts.size());
        assertEquals("New", posts.get(0).title);
    }

    @Test
    public void aMissingExpiryNeverExpiresAndABadOneIsIgnored() {
        String json = "{\"posts\":["
                + "{\"id\":\"a\",\"title\":\"A\",\"body\":\"A body\"},"
                + "{\"id\":\"b\",\"title\":\"B\",\"body\":\"B body\",\"expiresAt\":\"not a date\"}]}";
        assertEquals(2, EmergencyNews.parse(json, now()).size());
    }

    @Test
    public void theSameIdIsShownOnceAndTheListIsCapped() {
        String json = "{\"posts\":["
                + "{\"id\":\"same\",\"title\":\"One\",\"body\":\"b\"},"
                + "{\"id\":\"same\",\"title\":\"Duplicate\",\"body\":\"b\"},"
                + "{\"id\":\"t\",\"title\":\"Two\",\"body\":\"b\"},"
                + "{\"id\":\"th\",\"title\":\"Three\",\"body\":\"b\"},"
                + "{\"id\":\"f\",\"title\":\"Four\",\"body\":\"b\"}]}";
        List<EmergencyNews.Post> posts = EmergencyNews.parse(json, now());
        assertEquals(3, posts.size());
        assertEquals("One", posts.get(0).title);
        assertEquals("Two", posts.get(1).title);
    }

    @Test
    public void aDocumentThatIsNotJsonYieldsNothing() {
        assertTrue(EmergencyNews.parse("", now()).isEmpty());
        assertTrue(EmergencyNews.parse(null, now()).isEmpty());
        assertTrue(EmergencyNews.parse("<html>404</html>", now()).isEmpty());
        assertTrue(EmergencyNews.parse("{\"schemaVersion\":1}", now()).isEmpty());
    }

    @Test
    public void theSourceIsTheRepositorysOwnDocument() {
        assertTrue(EmergencyNews.SOURCE_URL,
                EmergencyNews.SOURCE_URL.startsWith("https://raw.githubusercontent.com/"));
        assertTrue(EmergencyNews.SOURCE_URL, EmergencyNews.SOURCE_URL.endsWith("/resources/launcher/emergency_news.json"));
    }
}
