package org.chimeramc.client.core.xbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The Xbox Live PeopleHub parser, pinned without a network or a device.
 *
 * <p>The parse is what breaks silently when Xbox reshapes a field, so it is exercised against real
 * response shapes: a populated list, an empty list, a malformed body, and an error body.
 */
public class XboxPeopleHubTest {

    private static final String TWO_FRIENDS = "{"
            + "\"people\":["
            + "{\"xuid\":\"2533274812345678\",\"displayName\":\"Steve\","
            + "\"displayPicRaw\":\"https://img/steve.png\",\"realName\":\"Alex Smith\","
            + "\"presenceState\":\"Online\",\"presenceText\":\"Playing Minecraft\"},"
            + "{\"xuid\":\"2533274887654321\",\"displayName\":\"Creeper\","
            + "\"presenceState\":\"Offline\",\"presenceText\":\"Last seen 2h ago\"}"
            + "]}";

    @Test
    public void aPopulatedListParsesEveryField() {
        List<XboxPeopleHub.Friend> friends = XboxPeopleHub.parsePeople(TWO_FRIENDS);
        assertEquals(2, friends.size());
        XboxPeopleHub.Friend steve = friends.get(0);
        assertEquals("2533274812345678", steve.xuid);
        assertEquals("Steve", steve.gamertag);
        assertEquals("https://img/steve.png", steve.displayPicRaw);
        assertEquals("Alex Smith", steve.realName);
        assertTrue(steve.isOnline());
        assertEquals("Playing Minecraft", steve.presenceText);

        XboxPeopleHub.Friend creeper = friends.get(1);
        assertFalse(creeper.isOnline());
        assertEquals("", creeper.displayPicRaw);
    }

    @Test
    public void anEmptyOrMalformedBodyReadsAsNoFriends() {
        assertTrue(XboxPeopleHub.parsePeople(null).isEmpty());
        assertTrue(XboxPeopleHub.parsePeople("").isEmpty());
        assertTrue(XboxPeopleHub.parsePeople("{}").isEmpty());
        assertTrue(XboxPeopleHub.parsePeople("{\"people\":[]}").isEmpty());
        assertTrue("a malformed body must not throw", XboxPeopleHub.parsePeople("not json").isEmpty());
        assertTrue(XboxPeopleHub.parsePeople("{\"people\":[null]}").isEmpty());
    }

    @Test
    public void anEntryWithoutAXuidIsSkipped() {
        List<XboxPeopleHub.Friend> friends = XboxPeopleHub.parsePeople(
                "{\"people\":[{\"displayName\":\"No Xuid\"},{\"xuid\":\"1\",\"displayName\":\"Ok\"}]}");
        assertEquals(1, friends.size());
        assertEquals("Ok", friends.get(0).gamertag);
    }

    @Test
    public void anErrorBodyParsesToItsMessage() {
        assertEquals("Unauthorized", XboxPeopleHub.parseError("{\"code\":403,\"message\":\"Unauthorized\"}"));
        assertEquals("Xbox Live error 404", XboxPeopleHub.parseError("{\"code\":404}"));
        assertNull("a non-error body reads as no error", XboxPeopleHub.parseError("{\"people\":[]}"));
        assertNull(XboxPeopleHub.parseError("not json"));
    }

    @Test
    public void theUrlAndHeaderAreBuiltCorrectly() {
        assertEquals("https://peoplehub.xboxlive.com/users/xuid(123)/people"
                        + "?decoration=preferredColor,presenceDetail&maxItems=100",
                XboxPeopleHub.friendsUrl("123", 100));
        // The cap is clamped to a sane range.
        assertTrue(XboxPeopleHub.friendsUrl("1", 0).endsWith("maxItems=1"));
        assertTrue(XboxPeopleHub.friendsUrl("1", 999).endsWith("maxItems=100"));
        assertEquals("XBL3.0 x=hash;token",
                XboxPeopleHub.authorizationHeader("hash", "token"));
        assertEquals("5", XboxPeopleHub.contractVersion());
    }
}
