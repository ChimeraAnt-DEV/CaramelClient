package org.chimeramc.client.core.xbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Friends and the two request directions are told apart.
 *
 * <p>Xbox Live keeps no friend-request object: a follow that is not returned is a request, in
 * whichever direction it was made. The launcher read only {@code /people} - the accounts the caller
 * follows - so a request from someone and a request the caller sent both drew as an ordinary friend
 * row, and a request never appeared at all. These pin the split.
 */
public class XboxPeopleHubRequestsTest {

    private static XboxPeopleHub.Friend person(String xuid) {
        return new XboxPeopleHub.Friend(xuid, "tag" + xuid, "", "", "Offline", "");
    }

    private static List<String> xuids(List<XboxPeopleHub.Friend> people) {
        List<String> out = new ArrayList<>();
        for (XboxPeopleHub.Friend f : people) out.add(f.xuid);
        return out;
    }

    @Test
    public void theFollowFlagsAreRead() {
        String json = "{\"people\":[{"
                + "\"xuid\":\"1\",\"displayName\":\"A\","
                + "\"isFollowingCaller\":true,\"isFollowedByCaller\":false}]}";
        List<XboxPeopleHub.Friend> people = XboxPeopleHub.parsePeople(json);
        assertEquals(1, people.size());
        assertTrue(people.get(0).followingCaller);
        assertFalse(people.get(0).followedByCaller);
    }

    @Test
    public void aReturnedFollowIsAFriendAndAnUnreturnedOneIsARequest() {
        XboxPeopleHub.Graph graph = XboxPeopleHub.classify(
                Arrays.asList(person("1"), person("2")),
                Arrays.asList(person("1"), person("3")),
                true);

        assertEquals(Arrays.asList("1"), xuids(graph.friends));
        assertEquals("I follow 2, they do not follow back", Arrays.asList("2"), xuids(graph.outgoing));
        assertEquals("3 follows me and I have not followed back",
                Arrays.asList("3"), xuids(graph.incoming));
        assertTrue(graph.followersKnown);
    }

    @Test
    public void aFailedFollowReadClaimsNothingAboutRequests() {
        // Offline, or the follow endpoint refused: the friend list must still be the friend list,
        // not a list of people who all "sent a request".
        List<XboxPeopleHub.Friend> following = Arrays.asList(person("1"), person("2"));
        XboxPeopleHub.Graph graph = XboxPeopleHub.classify(following, null, false);

        assertEquals(Arrays.asList("1", "2"), xuids(graph.friends));
        assertTrue(graph.outgoing.isEmpty());
        assertTrue(graph.incoming.isEmpty());
        assertFalse(graph.followersKnown);
    }

    @Test
    public void thePerEntryFlagDecidesWhenTheFollowReadIsMissing() {
        XboxPeopleHub.Friend followedMe = new XboxPeopleHub.Friend(
                "1", "A", "", "", "", "", true, true);
        XboxPeopleHub.Friend notFollowed = new XboxPeopleHub.Friend(
                "2", "B", "", "", "", "", false, true);
        XboxPeopleHub.Graph graph = XboxPeopleHub.classify(
                Arrays.asList(followedMe, notFollowed), null, false);

        assertEquals(Arrays.asList("1"), xuids(graph.friends));
    }

    @Test
    public void emptyAndMissingListsAreSafe() {
        XboxPeopleHub.Graph empty = XboxPeopleHub.classify(null, null, true);
        assertTrue(empty.friends.isEmpty());
        assertTrue(empty.incoming.isEmpty());
        assertTrue(empty.outgoing.isEmpty());

        // A blank xuid is not a person. The follow read said nobody follows "4", so "4" is a
        // request the caller sent, not a friend.
        XboxPeopleHub.Graph blank = XboxPeopleHub.classify(
                Arrays.asList(person(""), person("4")), Arrays.asList(person("")), true);
        assertTrue(blank.friends.isEmpty());
        assertEquals(Arrays.asList("4"), xuids(blank.outgoing));
        assertTrue(blank.incoming.isEmpty());
    }

    @Test
    public void theFollowListIsReadFromTheFollowersPath() {
        String url = XboxPeopleHub.followersUrl("253327479", 500);
        assertTrue(url, url.startsWith("https://peoplehub.xboxlive.com/users/xuid(253327479)/people/followers"));
        assertTrue(url, url.contains("maxItems=100"));
        assertTrue("the friends read keeps its own path",
                XboxPeopleHub.friendsUrl("253327479", 10).endsWith("/people?decoration=preferredColor,presenceDetail&maxItems=10"));
    }
}
