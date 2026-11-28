package org.chimeramc.client.core.auth.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.xal.androidjava.XalStorageContract;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

/**
 * The XAL export's file set and token envelopes.
 *
 * <p>This is the evidence for the "the launcher is signed in but the game asks to sign in" report:
 * the game reads a fixed set of files, keyed by relying party, so the export is either complete or
 * the game starts signed out. There is no device here, so these assertions are the only proof that
 * the writer produces what the reader looks for.
 */
public class XalExportBundleTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long MINUTE = 60_000L;
    private static final String USER_ID = "msa-user-id";
    private static final String GAMERTAG = "CaramelTester";
    private static final String XUID = "2535400000000001";

    @Test
    public void theExportWritesTheFourFilesTheGameReads() {
        XalExportBundle bundle = XalExportBundle.build(fullSnapshot(), USER_ID, GAMERTAG, XUID);
        String b64 = XalStorageContract.encodeUserId(USER_ID);

        assertEquals(new HashSet<>(Arrays.asList(
                        XalExportBundle.DEVICE_IDENTITY_FILE,
                        XalExportBundle.defaultFile(),
                        XalExportBundle.msaFile(b64),
                        XalExportBundle.userFile(b64))),
                bundle.files.keySet());

        // The device identity carries the id the crypto key pair belongs to.
        JsonObject identity = JsonParser.parseString(
                bundle.files.get(XalExportBundle.DEVICE_IDENTITY_FILE)).getAsJsonObject();
        assertEquals("{device-uuid}", identity.get("Id").getAsString());

        // The MSA file carries the refresh token and the "t=" prefixed access token XAL expects.
        JsonObject msa = JsonParser.parseString(
                bundle.files.get(XalExportBundle.msaFile(b64))).getAsJsonObject();
        assertEquals(USER_ID, msa.get("user_id").getAsString());
        assertEquals("msa-refresh", msa.get("refresh_token").getAsString());
        String accessToken = msa.getAsJsonArray("access_tokens").get(0).getAsJsonObject()
                .get("access_token").getAsString();
        assertEquals("t=msa-access", accessToken);
    }

    @Test
    public void everyRelyingPartyTheGameAsksForIsWritten() {
        XalExportBundle bundle = XalExportBundle.build(fullSnapshot(), USER_ID, GAMERTAG, XUID);

        assertEquals(Arrays.asList(
                        XalExportBundle.RP_AUTH,        // Dtoken
                        XalExportBundle.RP_AUTH,        // Utoken
                        XalExportBundle.RP_XBOX_LIVE,
                        XalExportBundle.RP_BEDROCK,
                        XalExportBundle.RP_PLAYFAB,
                        XalExportBundle.RP_REALMS,
                        XalExportBundle.RP_MINECRAFT_SERVICES),
                bundle.relyingParties);
    }

    @Test
    public void theDeviceTokenIsWrittenSoXalCanMintAMissingRelyingParty() {
        XalExportBundle bundle = XalExportBundle.build(fullSnapshot(), USER_ID, GAMERTAG, XUID);
        JsonObject user = userJson(bundle);

        JsonObject dtoken = envelopeFor(user, "Dtoken", XalExportBundle.RP_AUTH);
        assertNotNull("XAL needs a Dtoken to mint an uncached relying party", dtoken);
        assertEquals("device-token",
                dtoken.getAsJsonObject("TokenData").get("Token").getAsString());
        // The library's device token has no user hash of its own; the user token's is reused.
        assertEquals("uhs-1", dtoken.getAsJsonObject("TokenData")
                .getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                .get(0).getAsJsonObject().get("uhs").getAsString());
    }

    @Test
    public void aSnapshotWithoutAMinecraftServicesTokenOmitsThatRelyingParty() {
        JsonObject snapshot = fullSnapshot();
        snapshot.remove("javaXstsToken");

        XalExportBundle bundle = XalExportBundle.build(snapshot, USER_ID, GAMERTAG, XUID);

        assertFalse("a token that does not exist must not be faked",
                bundle.relyingParties.contains(XalExportBundle.RP_MINECRAFT_SERVICES));
        assertEquals(6, bundle.relyingParties.size());
        assertNull(envelopeFor(userJson(bundle), "Xtoken", XalExportBundle.RP_MINECRAFT_SERVICES));
    }

    @Test
    public void theSoonestExpiryIsReportedForTheLogLine() {
        XalExportBundle bundle = XalExportBundle.build(fullSnapshot(), USER_ID, GAMERTAG, XUID);
        assertEquals(NOW + MINUTE, bundle.soonestExpiryMs);
    }

    @Test
    public void theDeviceKeyPairGoesToTheCryptoPreferences() {
        XalExportBundle bundle = XalExportBundle.build(fullSnapshot(), USER_ID, GAMERTAG, XUID);

        assertEquals("{device-uuid}", bundle.cryptoPrefs.get("id"));
        assertEquals("PUBLIC-KEY", bundle.cryptoPrefs.get("public"));
        assertEquals("PRIVATE-KEY", bundle.cryptoPrefs.get("private"));
    }

    private static JsonObject userJson(XalExportBundle bundle) {
        String userFile = XalExportBundle.userFile(XalStorageContract.encodeUserId(USER_ID));
        return JsonParser.parseString(bundle.files.get(userFile)).getAsJsonObject();
    }

    private static JsonObject envelopeFor(JsonObject user, String identityType, String relyingParty) {
        JsonArray tokens = user.getAsJsonArray("tokens");
        for (int i = 0; i < tokens.size(); i++) {
            JsonObject envelope = tokens.get(i).getAsJsonObject();
            if (identityType.equals(envelope.get("IdentityType").getAsString())
                    && relyingParty.equals(envelope.get("RelyingParty").getAsString())) {
                return envelope;
            }
        }
        return null;
    }

    private static JsonObject fullSnapshot() {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("deviceId", "device-uuid");

        JsonObject keyPair = new JsonObject();
        keyPair.addProperty("publicKey", "PUBLIC-KEY");
        keyPair.addProperty("privateKey", "PRIVATE-KEY");
        snapshot.add("deviceKeyPair", keyPair);

        JsonObject msa = new JsonObject();
        msa.addProperty("accessToken", "msa-access");
        msa.addProperty("refreshToken", "msa-refresh");
        msa.addProperty("expireTimeMs", NOW + 60 * MINUTE);
        snapshot.add("msaToken", msa);
        snapshot.add("xblDeviceToken", token("device-token", NOW + MINUTE, null));
        snapshot.add("xblUserToken", token("user-token", NOW + 2 * MINUTE, "uhs-1"));
        snapshot.add("xboxLiveXstsToken", token("xbox-live", NOW + 3 * MINUTE, "uhs-1"));
        snapshot.add("bedrockXstsToken", token("bedrock", NOW + 4 * MINUTE, "uhs-1"));
        snapshot.add("playFabXstsToken", token("playfab", NOW + 5 * MINUTE, "uhs-1"));
        snapshot.add("realmsXstsToken", token("realms", NOW + 6 * MINUTE, "uhs-1"));
        snapshot.add("javaXstsToken", token("minecraft-services", NOW + 7 * MINUTE, "uhs-1"));
        return snapshot;
    }

    /** A minecraftauth snapshot token: {@code token}, {@code expireTimeMs} and maybe a user hash. */
    private static JsonObject token(String token, long expireTimeMs, String userHash) {
        JsonObject json = new JsonObject();
        json.addProperty("token", token);
        json.addProperty("expireTimeMs", expireTimeMs);
        if (userHash != null) json.addProperty("userHash", userHash);
        return json;
    }
}
