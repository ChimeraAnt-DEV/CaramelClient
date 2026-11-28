package org.chimeramc.client.core.xbox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parses the Xbox Live PeopleHub responses into a plain friends list.
 *
 * <p>PeopleHub is the Xbox Live social graph behind {@code peoplehub.xboxlive.com}. The launcher
 * reads it with the account's {@code http://xboxlive.com} XSTS token, the same relying party the
 * Bedrock auth chain already mints for {@code getXboxUserProfile()}, so no extra consent is needed.
 *
 * <p>Pure and unit-tested: the parsing is the part that can silently break when the API reshapes a
 * field, so it is exercised against real response shapes without a network or a device.
 */
public final class XboxPeopleHub {

    private XboxPeopleHub() {
    }

    /** One friend as the UI needs it. */
    public static final class Friend {
        public final String xuid;
        public final String gamertag;
        public final String displayPicRaw;
        public final String realName;
        public final String presenceState;
        public final String presenceText;
        /** Whether this account follows the caller, when the response said so. */
        public final boolean followingCaller;
        /** Whether the caller follows this account, when the response said so. */
        public final boolean followedByCaller;

        public Friend(String xuid, String gamertag, String displayPicRaw, String realName,
                      String presenceState, String presenceText) {
            this(xuid, gamertag, displayPicRaw, realName, presenceState, presenceText, false, false);
        }

        public Friend(String xuid, String gamertag, String displayPicRaw, String realName,
                      String presenceState, String presenceText,
                      boolean followingCaller, boolean followedByCaller) {
            this.xuid = nz(xuid);
            this.gamertag = nz(gamertag);
            this.displayPicRaw = nz(displayPicRaw);
            this.realName = nz(realName);
            this.presenceState = nz(presenceState);
            this.presenceText = nz(presenceText);
            this.followingCaller = followingCaller;
            this.followedByCaller = followedByCaller;
        }

        /** Whether the friend is online right now. */
        public boolean isOnline() {
            return "Online".equalsIgnoreCase(presenceState);
        }

        private static String nz(String s) {
            return s == null ? "" : s;
        }
    }

    /**
     * Parses a {@code /users/xuid({xuid})/people} response.
     *
     * <p>The response is {@code {"people":[{"xuid":..., "displayName":..., ...}]}}. A missing or
     * malformed list reads as empty rather than throwing, because a friends list that cannot be
     * parsed must degrade to "no friends shown", never take the account screen down.
     */
    public static List<Friend> parsePeople(String json) {
        List<Friend> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray people = root.optJSONArray("people");
            if (people == null) return out;
            for (int i = 0; i < people.length(); i++) {
                JSONObject person = people.optJSONObject(i);
                if (person == null) continue;
                String xuid = person.optString("xuid", "");
                if (xuid.isEmpty()) continue;
                out.add(new Friend(
                        xuid,
                        person.optString("displayName", ""),
                        person.optString("displayPicRaw", ""),
                        person.optString("realName", ""),
                        person.optString("presenceState", ""),
                        person.optString("presenceText", ""),
                        person.optBoolean("isFollowingCaller", false),
                        person.optBoolean("isFollowedByCaller", false)));
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return out;
    }

    /**
     * Parses a PeopleHub error body into a human-readable reason, or null when it is not an error.
     *
     * <p>PeopleHub reports a failure as {@code {"code":N,"message":"..."}}, and the codes that matter
     * are "the caller is not allowed to see this" (403) and "no such user" (404). Returning the
     * message lets the account screen explain the failure rather than showing an empty list.
     */
    public static String parseError(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(json);
            if (!root.has("code")) return null;
            String message = root.optString("message", "");
            return message.isEmpty() ? ("Xbox Live error " + root.optInt("code", 0)) : message;
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * The three lists the UI needs, split by who follows whom.
     *
     * <p>Xbox has no separate "friend request" object: a follow from an account the caller has not
     * followed back is a request the caller received, and a follow by the caller that is not
     * returned is a request the caller sent. Reading only {@code /people} - the accounts the caller
     * follows - therefore made both directions invisible: every row looked like a friend.
     *
     * <p>{@code friends} and {@code outgoing} come from {@code /people}; {@code incoming} is the
     * difference between {@code /followers} and {@code /people}. When the follow read failed,
     * {@link #followersKnown} is false and nothing is claimed about requests: the rows are the plain
     * friend list, which is what this build showed before the split existed.
     */
    public static final class Graph {
        public final List<Friend> friends;
        public final List<Friend> incoming;
        public final List<Friend> outgoing;
        public final boolean followersKnown;

        Graph(List<Friend> friends, List<Friend> incoming, List<Friend> outgoing,
              boolean followersKnown) {
            this.friends = friends;
            this.incoming = incoming;
            this.outgoing = outgoing;
            this.followersKnown = followersKnown;
        }
    }

    /**
     * Splits the accounts the caller follows and the accounts that follow the caller into friends
     * and the two request directions.
     *
     * @param following the {@code /people} read: accounts the caller follows
     * @param followers the {@code /followers} read: accounts that follow the caller
     * @param followersKnown false when the follow read failed, in which case no request is reported
     */
    public static Graph classify(List<Friend> following, List<Friend> followers,
                                 boolean followersKnown) {
        List<Friend> followingList = following == null ? new ArrayList<Friend>() : following;
        Set<String> followedXuids = new HashSet<>();
        List<Friend> friends = new ArrayList<>();
        List<Friend> outgoing = new ArrayList<>();
        for (Friend f : followingList) {
            if (f == null || f.xuid.isEmpty()) continue;
            boolean mutual;
            if (followersKnown) {
                mutual = containsXuid(followers, f.xuid);
            } else if (f.followingCaller) {
                // Without the follow read the per-entry flag is the only evidence there is.
                mutual = true;
            } else if (f.followedByCaller) {
                mutual = false;
            } else {
                // No evidence either way: keep the row a friend rather than invent a request.
                mutual = true;
            }
            if (mutual) {
                friends.add(f);
            } else {
                outgoing.add(f);
            }
            followedXuids.add(f.xuid);
        }
        List<Friend> incoming = new ArrayList<>();
        if (followersKnown && followers != null) {
            for (Friend f : followers) {
                if (f == null || f.xuid.isEmpty()) continue;
                if (!followedXuids.contains(f.xuid)) incoming.add(f);
            }
        }
        return new Graph(friends, incoming, outgoing, followersKnown);
    }

    private static boolean containsXuid(List<Friend> people, String xuid) {
        if (people == null) return false;
        for (Friend f : people) {
            if (f != null && xuid.equals(f.xuid)) return true;
        }
        return false;
    }

    /** The PeopleHub URL for the accounts that follow {@code xuid}, capped. */
    public static String followersUrl(String xuid, int maxItems) {
        int cap = Math.max(1, Math.min(100, maxItems));
        return "https://peoplehub.xboxlive.com/users/xuid(" + xuid + ")/people/followers"
                + "?decoration=preferredColor,presenceDetail&maxItems=" + cap;
    }

    /** The PeopleHub URL that lists the friends of {@code xuid}, most recent first, capped. */
    public static String friendsUrl(String xuid, int maxItems) {
        int cap = Math.max(1, Math.min(100, maxItems));
        return "https://peoplehub.xboxlive.com/users/xuid(" + xuid + ")/people"
                + "?decoration=preferredColor,presenceDetail&maxItems=" + cap;
    }

    /** The {@code Authorization} header value PeopleHub expects from an XSTS token. */
    public static String authorizationHeader(String userHash, String token) {
        return "XBL3.0 x=" + userHash + ";" + token;
    }

    /**
     * The {@code x-xbl-contract-version} PeopleHub expects.
     *
     * <p>5 is the version that returns {@code displayName}, {@code displayPicRaw} and the presence
     * detail this screen shows.
     */
    public static String contractVersion() {
        return "5";
    }
}
