package org.chimeramc.client.core.auth.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * The complete file set one account's XAL export consists of.
 *
 * <p>Pure: no {@code Context}, no disk, no clock beyond the token expiries the snapshot carries.
 * That is what lets a JVM test assert the exported files, the relying parties and the token
 * envelopes without a device, which is the only evidence available for this path.
 */
public final class XalExportBundle {

    /** The Bedrock Android title id that appears in every XAL file name. */
    public static final String TITLE_ID = "1739947436";

    public static final String RP_AUTH = "http://auth.xboxlive.com";
    public static final String RP_XBOX_LIVE = "http://xboxlive.com";
    public static final String RP_BEDROCK = "https://multiplayer.minecraft.net/";
    public static final String RP_PLAYFAB = "https://b980a380.minecraft.playfabapi.com/";
    public static final String RP_REALMS = "https://pocket.realms.minecraft.net/";
    public static final String RP_MINECRAFT_SERVICES = "rp://api.minecraftservices.com/";

    public static final String DEVICE_IDENTITY_FILE = "Xal.Production.RETAIL.DeviceIdentity.json";

    /** file name (relative to the account directory) to JSON content. */
    public final Map<String, String> files;
    /** crypto preference name to value: the device key pair and its id. */
    public final Map<String, String> cryptoPrefs;
    /** the relying parties whose token envelopes were written, in write order. */
    public final List<String> relyingParties;
    /** the soonest token expiry in the bundle, or 0 when no envelope carries one. */
    public final long soonestExpiryMs;

    private XalExportBundle(Map<String, String> files, Map<String, String> cryptoPrefs,
                            List<String> relyingParties, long soonestExpiryMs) {
        this.files = files;
        this.cryptoPrefs = cryptoPrefs;
        this.relyingParties = relyingParties;
        this.soonestExpiryMs = soonestExpiryMs;
    }

    public static String defaultFile() {
        return "Xal." + TITLE_ID + ".Production.Default.json";
    }

    public static String msaFile(String userIdBase64) {
        return "Xal." + TITLE_ID + ".Production.Msa." + userIdBase64 + ".json";
    }

    public static String userFile(String userIdBase64) {
        return "Xal." + TITLE_ID + ".Production.RETAIL.User." + userIdBase64 + ".json";
    }

    /**
     * Builds the export for one account.
     *
     * <p>Every token the snapshot carries is written as its own envelope, keyed by the relying
     * party XAL will look it up by. A token the snapshot does not carry is left out rather than
     * faked: XAL can mint a missing relying party itself, but only when it has the device token,
     * which is why the Dtoken below is not optional in practice.
     */
    public static XalExportBundle build(JsonObject authJson, String msaUserId,
                                        String gamertag, String xuid) {
        Map<String, String> files = new LinkedHashMap<>();
        Map<String, String> prefs = new LinkedHashMap<>();
        List<String> relyingParties = new ArrayList<>();
        long soonestExpiry = 0L;

        String userBase64 = com.microsoft.xal.androidjava.XalStorageContract.encodeUserId(msaUserId);

        JsonObject deviceToken = object(authJson, "xblDeviceToken");
        JsonObject userToken = object(authJson, "xblUserToken");
        JsonObject xboxLive = object(authJson, "xboxLiveXstsToken");
        JsonObject bedrock = object(authJson, "bedrockXstsToken");
        JsonObject playFab = object(authJson, "playFabXstsToken");
        JsonObject realms = object(authJson, "realmsXstsToken");
        JsonObject minecraftServices = object(authJson, "javaXstsToken");

        String deviceId = string(authJson, "deviceId");

        if (deviceId != null) {
            JsonObject identity = new JsonObject();
            identity.addProperty("Id", "{" + deviceId + "}");
            identity.addProperty("Key", "Serialized to SharedPreferences");
            files.put(DEVICE_IDENTITY_FILE, identity.toString());
        }

        JsonObject def = new JsonObject();
        def.addProperty("default", msaUserId);
        files.put(defaultFile(), def.toString());

        JsonObject msa = object(authJson, "msaToken");
        if (msa != null) {
            JsonObject root = new JsonObject();
            root.addProperty("user_id", msaUserId);
            root.addProperty("refresh_token", orEmpty(string(msa, "refreshToken")));
            root.addProperty("foci", "");

            JsonObject accessToken = new JsonObject();
            accessToken.addProperty("access_token", "t=" + orEmpty(string(msa, "accessToken")));
            accessToken.addProperty("xal_expires",
                    formatDate(longValue(msa, "expireTimeMs", System.currentTimeMillis())));
            accessToken.addProperty("scopes", "service::user.auth.xboxlive.com::mbi_ssl");
            JsonArray accessTokens = new JsonArray();
            accessTokens.add(accessToken);
            root.add("access_tokens", accessTokens);

            files.put(msaFile(userBase64), root.toString());
        }

        JsonArray tokens = new JsonArray();
        // The Dtoken is the device token. XAL needs it to mint an XSTS token for a relying party
        // that is not cached yet; without it the game can only use what is written here.
        String userHash = userToken != null ? orEmpty(string(userToken, "userHash")) : "";
        soonestExpiry = addEnvelope(tokens, relyingParties, soonestExpiry,
                "Dtoken", RP_AUTH, msaUserId, gamertag, xuid, deviceToken, userHash);
        soonestExpiry = addEnvelope(tokens, relyingParties, soonestExpiry,
                "Utoken", RP_AUTH, msaUserId, gamertag, xuid, userToken, userHash);
        soonestExpiry = addEnvelope(tokens, relyingParties, soonestExpiry,
                "Xtoken", RP_XBOX_LIVE, msaUserId, gamertag, xuid, xboxLive, userHash);
        soonestExpiry = addEnvelope(tokens, relyingParties, soonestExpiry,
                "Xtoken", RP_BEDROCK, msaUserId, gamertag, xuid, bedrock, userHash);
        soonestExpiry = addEnvelope(tokens, relyingParties, soonestExpiry,
                "Xtoken", RP_PLAYFAB, msaUserId, gamertag, xuid, playFab, userHash);
        soonestExpiry = addEnvelope(tokens, relyingParties, soonestExpiry,
                "Xtoken", RP_REALMS, msaUserId, gamertag, xuid, realms, userHash);
        soonestExpiry = addEnvelope(tokens, relyingParties, soonestExpiry,
                "Xtoken", RP_MINECRAFT_SERVICES, msaUserId, gamertag, xuid, minecraftServices, userHash);

        JsonObject userRoot = new JsonObject();
        userRoot.addProperty("deviceId", "{" + orEmpty(deviceId) + "}");
        userRoot.add("tokens", tokens);
        files.put(userFile(userBase64), userRoot.toString());

        JsonObject keyPair = object(authJson, "deviceKeyPair");
        if (keyPair != null) {
            String publicKey = urlSafeBase64(string(keyPair, "publicKey"));
            String privateKey = urlSafeBase64(string(keyPair, "privateKey"));
            if (publicKey != null && privateKey != null) {
                prefs.put("id", "{" + orEmpty(deviceId) + "}");
                prefs.put("public", publicKey);
                prefs.put("private", privateKey);
            }
        }

        return new XalExportBundle(files, prefs, relyingParties, soonestExpiry);
    }

    /**
     * The key material re-encoded the way the game's `Ecdsa` reads it.
     *
     * <p>`MinecraftAuth` writes the DER bytes in standard Base64 (`+`, `/`, `=`), while
     * `Ecdsa.restoreKeyAndId` decodes with `URL_SAFE | NO_PADDING | NO_WRAP` and throws
     * `IllegalArgumentException: bad base-64` on the first of those characters. A throw there leaves
     * XAL with no device key, so every proof-of-possession signing step fails. Both alphabets are
     * accepted on input, so a snapshot that already carries URL-safe text round-trips unchanged.
     *
     * @return URL-safe unpadded Base64, or null when the value is not Base64 at all — the crypto
     *         preferences are then left untouched and the game generates its own key instead of
     *         crashing on a restore.
     */
    static String urlSafeBase64(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String compact = value.replace("\n", "").replace("\r", "").trim();
        try {
            byte[] raw = compact.indexOf('-') >= 0 || compact.indexOf('_') >= 0
                    ? Base64.getUrlDecoder().decode(compact)
                    : Base64.getDecoder().decode(compact);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (IllegalArgumentException notBase64) {
            return null;
        }
    }

    /** The distinct relying parties, for a log line. */
    public List<String> distinctRelyingParties() {
        return new ArrayList<>(new LinkedHashSet<>(relyingParties));
    }

    private static long addEnvelope(JsonArray tokens, List<String> relyingParties, long soonestExpiry,
                                    String identityType, String relyingParty, String msaUserId,
                                    String gamertag, String xuid, JsonObject token, String userHash) {
        if (token == null) return soonestExpiry;
        long expiry = longValue(token, "expireTimeMs", 0L);
        tokens.add(envelope(identityType, relyingParty, msaUserId, gamertag, xuid, token, userHash, expiry));
        relyingParties.add(relyingParty);
        if (expiry > 0 && (soonestExpiry == 0 || expiry < soonestExpiry)) return expiry;
        return soonestExpiry;
    }

    private static JsonObject envelope(String identityType, String relyingParty, String msaUserId,
                                       String gamertag, String xuid, JsonObject token,
                                       String userHash, long expireTimeMs) {
        JsonObject xui = new JsonObject();
        xui.addProperty("uhs", userHash);
        xui.addProperty("gtg", orEmpty(gamertag));
        xui.addProperty("mgt", "");
        xui.addProperty("mgs", "");
        xui.addProperty("umg", "");
        xui.addProperty("xid", orEmpty(xuid));
        xui.addProperty("agg", "");
        xui.addProperty("prv", "");
        xui.addProperty("usr", "");
        xui.addProperty("uer", "");
        xui.addProperty("utr", "");

        JsonArray xuiArray = new JsonArray();
        xuiArray.add(xui);
        JsonObject displayClaims = new JsonObject();
        displayClaims.add("xui", xuiArray);

        JsonObject data = new JsonObject();
        data.addProperty("Token", orEmpty(string(token, "token")));
        data.addProperty("NotAfter", formatDate(expireTimeMs));
        data.addProperty("IssueInstant", formatDate(expireTimeMs - 8 * 60 * 60 * 1000L));
        data.addProperty("ClientAttested", false);
        data.add("DisplayClaims", displayClaims);

        JsonObject envelope = new JsonObject();
        envelope.addProperty("MsaUserId", msaUserId);
        envelope.addProperty("HasSignInDisplayClaims", true);
        envelope.addProperty("IdentityType", identityType);
        envelope.addProperty("Environment", "Production");
        envelope.addProperty("Sandbox", "RETAIL");
        envelope.addProperty("TokenType", "JWT");
        envelope.addProperty("RelyingParty", relyingParty);
        envelope.addProperty("SubRelyingParty", "");
        envelope.add("TokenData", data);
        return envelope;
    }

    static String formatDate(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }

    private static JsonObject object(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonObject()) return null;
        return root.getAsJsonObject(key);
    }

    private static String string(JsonObject root, String key) {
        if (root == null || !root.has(key) || root.get(key).isJsonNull()) return null;
        try {
            return root.get(key).getAsString();
        } catch (RuntimeException notAString) {
            return null;
        }
    }

    private static long longValue(JsonObject root, String key, long fallback) {
        if (root == null || !root.has(key) || root.get(key).isJsonNull()) return fallback;
        try {
            return root.get(key).getAsLong();
        } catch (RuntimeException notANumber) {
            return fallback;
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
