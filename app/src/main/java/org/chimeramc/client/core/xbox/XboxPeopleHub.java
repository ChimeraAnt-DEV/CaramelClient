package org.chimeramc.client.core.xbox;

import java.util.ArrayList;
import java.util.List;

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

        public Friend(String xuid, String gamertag, String displayPicRaw, String realName,
                      String presenceState, String presenceText) {
            this.xuid = nz(xuid);
            this.gamertag = nz(gamertag);
            this.displayPicRaw = nz(displayPicRaw);
            this.realName = nz(realName);
            this.presenceState = nz(presenceState);
            this.presenceText = nz(presenceText);
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
                        person.optString("presenceText", "")));
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
