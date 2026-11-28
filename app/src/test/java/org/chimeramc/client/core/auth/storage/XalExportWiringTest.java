package org.chimeramc.client.core.auth.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The wiring that makes an export reach the game: refresh, re-export, and which account is pointed at.
 *
 * <p>The refresh path itself is a network call and cannot run here, so these are source-level
 * guards in the shape the rest of the repo uses for wiring that a JVM test cannot execute. They are
 * deliberately narrow: each assertion names the call whose absence reproduces the reported symptom.
 */
public class XalExportWiringTest {

    @Test
    public void aRefreshReExportsTheAccountTheGameIsReading() throws IOException {
        String store = read("app/src/main/java/org/chimeramc/client/launcher/core/auth/MsftAccountStore.java");
        assertTrue("updateAuthMaterial must rewrite the exported copy after a refresh",
                store.contains("XalExporter.refreshExportedAccount"));
    }

    @Test
    public void everyLaunchExportsTheAccountTheGameWillRead() throws IOException {
        String preparer = read(
                "app/src/main/java/org/chimeramc/client/launcher/core/minecraft/MinecraftRuntimePreparer.kt");
        assertTrue("a launch without a binding must still export the active account",
                preparer.contains("XalExporter.exportActiveAccount"));
        assertTrue("a bound instance must export its own account",
                preparer.contains("XalExporter.exportAccount"));
    }

    @Test
    public void theRefreshRenewsEveryRelyingPartyTheExportWrites() throws IOException {
        String auth = read("app/src/main/java/org/chimeramc/client/launcher/core/auth/MsftAuthManager.java");
        assertTrue("the Realms relying party must be renewed",
                auth.contains("getRealmsXstsToken().getUpToDate()")
                        || auth.contains("refreshBestEffort(\"Realms\""));
        assertTrue("the multiplayer.minecraft.net relying party must be renewed",
                auth.contains("getBedrockXstsToken().getUpToDate()"));
        assertTrue("the Minecraft Services relying party must be minted and kept",
                auth.contains("mergeMinecraftServicesXsts"));
        assertTrue("and stored under the key the exporter reads",
                auth.contains("\"javaXstsToken\""));
    }

    @Test
    public void theReaderUsesTheSharedContractRatherThanTheSettingsToggleAlone() throws IOException {
        String storage = read("minecraft/src/main/java/com/microsoft/xal/androidjava/Storage.java");
        assertTrue("the storage path must consult the exported store",
                storage.contains("launcherDataPresent"));
        assertTrue("and resolve the account through the shared contract",
                storage.contains("XalStorageContract.accountDirectory"));
        assertFalse("the toggle alone must not decide the path any more",
                storage.contains("if (takeover)"));
    }

    @Test
    public void theExportLogLineNamesTheFactsAndCarriesNoToken() {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("deviceId", "device-uuid");
        snapshot.add("msaToken", token("SECRET-MSA-ACCESS", "SECRET-MSA-REFRESH", 5_000L));
        snapshot.add("xblDeviceToken", token("SECRET-DEVICE-TOKEN", null, 1_000L));
        snapshot.add("xblUserToken", token("SECRET-USER-TOKEN", null, 2_000L));
        snapshot.add("bedrockXstsToken", token("SECRET-BEDROCK-TOKEN", null, 3_000L));

        XalExportBundle bundle = XalExportBundle.build(snapshot, "account-id", "Gamertag", "xuid");
        String line = XalExporter.exportLogLine(bundle, new File("/data/user/0/app/files/xal/abc"), 0L);

        assertTrue("the directory must be named: " + line,
                line.contains("/data/user/0/app/files/xal/abc"));
        assertTrue("the relying parties must be named: " + line,
                line.contains(XalExportBundle.RP_BEDROCK));
        assertTrue("the time to expiry must be reported: " + line, line.contains("1s"));
        assertTrue("the device-key status must be reported: " + line, line.contains("no device key"));
        for (String secret : new String[]{"SECRET-MSA-ACCESS", "SECRET-MSA-REFRESH", "SECRET-DEVICE-TOKEN",
                "SECRET-USER-TOKEN", "SECRET-BEDROCK-TOKEN", "account-id", "Gamertag", "xuid"}) {
            assertFalse("the log line must not carry " + secret + ": " + line, line.contains(secret));
        }
    }

    /**
     * The export line answers "was the device key written?" without a debugger.
     *
     * <p>A key written in the wrong Base64 alphabet is the defect that leaves XAL unable to sign a
     * proof of possession, so the presence of the key in the exported preferences is the one fact
     * worth reading off a phone's logcat.
     */
    @Test
    public void theLogLineSaysWhenTheDeviceKeyWasWritten() {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("deviceId", "device-uuid");
        JsonObject keyPair = new JsonObject();
        keyPair.addProperty("publicKey", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE+/+//n4=");
        keyPair.addProperty("privateKey", "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCD//g==");
        snapshot.add("deviceKeyPair", keyPair);

        XalExportBundle bundle = XalExportBundle.build(snapshot, "account-id", "Gamertag", "xuid");
        String line = XalExporter.exportLogLine(bundle, new File("/data/user/0/app/files/xal/abc"), 0L);

        assertTrue("the written key must be reported: " + line, line.contains("(device key written)"));
        assertFalse("the key material must not be logged: " + line, line.contains("MFkwEwYHKoZIzj0"));
    }

    private static JsonObject token(String token, String refreshToken, long expireTimeMs) {
        JsonObject json = new JsonObject();
        json.addProperty("token", token);
        json.addProperty("expireTimeMs", expireTimeMs);
        if (refreshToken != null) {
            json.addProperty("accessToken", token);
            json.addProperty("refreshToken", refreshToken);
        }
        return json;
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relative)), StandardCharsets.UTF_8);
    }

    /** Walks up from the test working directory to the repository root. */
    private static java.nio.file.Path projectRoot() {
        java.nio.file.Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
