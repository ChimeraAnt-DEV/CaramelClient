package org.chimeramc.client.core.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Resolves a player's skin texture URL from the official Minecraft Services API.
 *
 * <p><b>Why this exists.</b> The GeyserMC lookup the client used before is a third-party service
 * keyed by XUID. It returns <em>nothing</em> for a XUID it has never seen, so a brand-new account
 * resolved to no skin at all and the preview fell back to Steve. This uses the player's own
 * Minecraft account instead, which every licensed account has, so there is no "unknown player" case.
 *
 * <p><b>The endpoint.</b> {@code GET https://api.minecraftservices.com/minecraft/profile} returns
 * the account's profile including a {@code skins} array, and each entry carries a {@code url}
 * pointing at the skin texture. The call is bearer-authenticated, which is why it lives behind the
 * sign-in rather than being a public lookup.
 *
 * <p><b>Parsing is separate from the request</b> so the response shape is unit-tested with literal
 * payloads, and a shape change is caught by a test rather than by a player seeing the wrong skin.
 */
public final class MinecraftSkinFetcher {

    /** The profile endpoint, and the bearer header it needs. */
    public static final String PROFILE_URL =
            "https://api.minecraftservices.com/minecraft/profile";

    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int READ_TIMEOUT_MS = 6000;
    private static final int MAX_BYTES = 512 * 1024;

    private MinecraftSkinFetcher() {
    }

    /**
     * Fetches the account's profile JSON from the official service.
     *
     * <p>Blocking, so the caller must run it off the UI thread. Returns null on any failure (no
     * token, a non-200, a network error), so the caller falls back to the legacy lookup rather than
     * losing the skin entirely.
     *
     * @param authorizationHeader a ready {@code Bearer …} header value, or null when no token could
     *                            be minted
     */
    public static String fetchProfileJson(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) return null;
        java.net.HttpURLConnection connection = null;
        try {
            connection = (java.net.HttpURLConnection)
                    new java.net.URL(PROFILE_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", authorizationHeader);
            if (connection.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) return null;
            try (java.io.InputStream in = connection.getInputStream()) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int total = 0;
                int read;
                while ((read = in.read(buffer)) > 0) {
                    total += read;
                    if (total > MAX_BYTES) return null;
                    out.write(buffer, 0, read);
                }
                return out.toString(java.nio.charset.StandardCharsets.UTF_8.name());
            }
        } catch (Throwable t) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * The active skin's texture URL from a profile response, or null.
     *
     * <p>The response looks like:
     * <pre>
     * {"id":"…","name":"…","skins":[
     *    {"id":"…","state":"ACTIVE","url":"http://textures.minecraft.net/texture/…"},
     *    {"id":"…","state":"INACTIVE","url":"…"}]}
     * </pre>
     *
     * <p>The {@code ACTIVE} entry is preferred, because an account can hold several skins and the
     * inactive ones are not what the player is wearing. When no entry is marked active, the first
     * entry with a URL is used rather than returning nothing: a profile with exactly one skin is
     * common, and refusing it would send the caller to the fallback for no reason.
     */
    public static String activeSkinUrl(String body) {
        if (body == null || body.trim().isEmpty()) return null;
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) return null;
            JsonObject profile = root.getAsJsonObject();
            JsonElement skinsElement = profile.get("skins");
            if (skinsElement == null || !skinsElement.isJsonArray()) return null;
            JsonArray skins = skinsElement.getAsJsonArray();
            if (skins.size() == 0) return null;

            String firstWithUrl = null;
            for (JsonElement entry : skins) {
                if (entry == null || !entry.isJsonObject()) continue;
                JsonObject skin = entry.getAsJsonObject();
                String url = stringOrNull(skin, "url");
                if (url == null || url.isEmpty()) continue;
                if (firstWithUrl == null) firstWithUrl = url;
                if ("ACTIVE".equalsIgnoreCase(stringOrNull(skin, "state"))) {
                    return url;
                }
            }
            return firstWithUrl;
        } catch (Throwable t) {
            // A malformed or non-JSON body (an error page, a truncated read) is "no skin", never a
            // crash: the caller falls back to the legacy lookup.
            return null;
        }
    }

    /**
     * The profile's Minecraft UUID from a profile response, or null.
     *
     * <p>Used to confirm the response belongs to the account that asked, so a stale or crossed
     * response cannot be cached under the wrong account.
     */
    public static String profileId(String body) {
        if (body == null || body.trim().isEmpty()) return null;
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) return null;
            return stringOrNull(root.getAsJsonObject(), "id");
        } catch (Throwable t) {
            return null;
        }
    }

    private static String stringOrNull(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) return null;
        return value.getAsString();
    }
}
