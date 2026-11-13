package org.chimeramc.client.core.xbox;

import org.chimeramc.client.util.AppLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.json.JSONObject;

/**
 * Reads the signed-in account's Xbox Live friends from PeopleHub.
 *
 * <p>PeopleHub is reached with the {@code http://xboxlive.com} XSTS token the Bedrock auth chain
 * already mints, so no extra consent is needed: the token is the account's own, obtained at sign-in.
 * The call is the standard {@code /users/xuid({xuid})/people} read.
 *
 * <p>The fetch runs on its own single-thread executor and reports back on a caller-supplied callback,
 * so the account screen never blocks on the network. Failures are reported as a reason string rather
 * than thrown, because a friends list that cannot load must degrade to an explanation, not a crash.
 */
public final class XboxFriendsClient {

    /** Where a fetch's result goes. {@code friends} is empty when {@code error} is non-null. */
    public interface Callback {
        void onResult(List<XboxPeopleHub.Friend> friends, String error);
    }

    private static final int MAX_ITEMS = 100;
    private static final int TIMEOUT_MS = 12_000;

    // The shared executor owner, so no raw thread is constructed here.
    private final ExecutorService executor = org.chimeramc.client.util.AppExecutors.single("xbox-friends");

    /**
     * Fetches the friends of the account identified by {@code xuid}, authenticating with the given
     * XSTS {@code userHash} and {@code token}. The callback is always invoked exactly once.
     */
    public void fetch(String xuid, String userHash, String token, Callback callback) {
        if (xuid == null || xuid.isEmpty() || token == null || token.isEmpty()) {
            callback.onResult(java.util.Collections.emptyList(),
                    "This account is not signed in, so its friends cannot be read.");
            return;
        }
        executor.execute(() -> {
            List<XboxPeopleHub.Friend> friends = java.util.Collections.emptyList();
            String error = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(XboxPeopleHub.friendsUrl(xuid, MAX_ITEMS));
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization",
                        XboxPeopleHub.authorizationHeader(userHash, token));
                connection.setRequestProperty("x-xbl-contract-version",
                        XboxPeopleHub.contractVersion());
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);

                int code = connection.getResponseCode();
                String body = read(code >= 400 ? connection.getErrorStream()
                        : connection.getInputStream());
                if (code >= 200 && code < 300) {
                    friends = XboxPeopleHub.parsePeople(body);
                } else {
                    String parsed = XboxPeopleHub.parseError(body);
                    error = parsed != null ? parsed
                            : ("Xbox Live returned " + code + ".");
                }
            } catch (Throwable t) {
                error = "Could not reach Xbox Live.";
                AppLog.warnOnce("CaramelClient", "XboxFriendsClient:fetch",
                        "Reading the Xbox Live friends list failed", t);
            } finally {
                if (connection != null) connection.disconnect();
            }
            callback.onResult(friends, error);
        });
    }

    /** Releases the fetch thread. */
    public void close() {
        executor.shutdownNow();
    }

    private static String read(java.io.InputStream stream) {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        } catch (Throwable t) {
            return sb.toString();
        }
        return sb.toString();
    }

    /**
     * Reads the XSTS {@code userHash} and {@code token} from a serialized {@code BedrockAuthManager}.
     *
     * <p>The account store keeps the whole auth manager as JSON; the Xbox Live XSTS token is one of
     * its holders. This reads it out without re-minting, so the friends fetch reuses the sign-in the
     * account already has. Returns a two-element array {@code {userHash, token}}, or null when the
     * token is absent or expired.
     */
    public static String[] xboxLiveXsts(String serializedAuthManagerJson) {
        if (serializedAuthManagerJson == null || serializedAuthManagerJson.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(serializedAuthManagerJson);
            JSONObject xsts = root.optJSONObject("xboxLiveXstsToken");
            if (xsts == null) return null;
            String token = xsts.optString("token", "");
            String userHash = xsts.optString("userHash", "");
            long expireTimeMs = xsts.optLong("expireTimeMs", 0L);
            if (token.isEmpty() || userHash.isEmpty()) return null;
            if (expireTimeMs > 0L && expireTimeMs <= System.currentTimeMillis()) return null;
            return new String[] { userHash, token };
        } catch (Throwable t) {
            return null;
        }
    }
}
