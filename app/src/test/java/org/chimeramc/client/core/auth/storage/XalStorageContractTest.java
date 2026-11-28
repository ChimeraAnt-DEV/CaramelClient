package org.chimeramc.client.core.auth.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.microsoft.xal.androidjava.XalStorageContract;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The rule that decides where the game reads its sign-in from.
 *
 * <p>The defect this covers: the launcher exported every account into {@code files/xal/<user>/}
 * while the game read plain {@code files/} unless a settings toggle happened to be on, so on a
 * default install a signed-in launcher handed the game nothing. The reader and the writer now share
 * this contract, and these tests pin both halves of it.
 */
public class XalStorageContractTest {

    private static final String ACTIVE_ID = "active-msa-id";
    private static final String BOUND_ID = "bound-msa-id";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void theAccountDirectoryIsUrlSafeBase64WithoutPadding() {
        // android.util.Base64.URL_SAFE | NO_PADDING produces exactly this, and the exporter used it
        // before this class existed, so existing installs keep resolving to the same directory.
        assertEquals("YWJj", XalStorageContract.encodeUserId("abc"));
        assertEquals("Pz8-", XalStorageContract.encodeUserId("??>"));
    }

    @Test
    public void theExportPointerWinsOverTheActiveAccount() throws Exception {
        // A bound instance exports an account that is deliberately not the active one.
        assertEquals(BOUND_ID, XalStorageContract.resolveAccountId(
                pointer(BOUND_ID), accounts(ACTIVE_ID, BOUND_ID)));
    }

    @Test
    public void withoutAPointerTheActiveAccountIsRead() throws Exception {
        assertEquals(ACTIVE_ID, XalStorageContract.resolveAccountId(null, accounts(ACTIVE_ID, BOUND_ID)));
        assertEquals(ACTIVE_ID, XalStorageContract.resolveAccountId("{}", accounts(ACTIVE_ID, BOUND_ID)));
    }

    @Test
    public void withoutAnActiveFlagTheFirstAccountIsRead() throws Exception {
        String list = "[{\"msUserId\":\"first\"},{\"msUserId\":\"second\"}]";
        assertEquals("first", XalStorageContract.resolveAccountId(null, list));
    }

    @Test
    public void anEmptyStoreHasNoAccount() {
        assertNull(XalStorageContract.resolveAccountId(null, null));
        assertNull(XalStorageContract.resolveAccountId("{}", "[]"));
        assertNull(XalStorageContract.resolveAccountId("not json", "not json"));
        assertFalse(XalStorageContract.hasExportedAccount("{}", "[]"));
    }

    @Test
    public void theCryptoPreferencesFollowTheSameDecisionAsTheStoragePath() {
        assertEquals(XalStorageContract.CRYPTO_PREFS_LAUNCHER,
                XalStorageContract.cryptoPrefsName(false, true));
        assertEquals(XalStorageContract.CRYPTO_PREFS_LAUNCHER,
                XalStorageContract.cryptoPrefsName(true, false));
        assertEquals(XalStorageContract.CRYPTO_PREFS_GAME,
                XalStorageContract.cryptoPrefsName(false, false));
    }

    @Test
    public void theExportedFileLandsInTheDirectoryTheGameResolves() throws Exception {
        // This is the agreement test: the bundle's per-account file name carries the base64 of the
        // account id, and the game resolves the same id from the pointer file to pick the directory.
        File xalRoot = folder.newFolder("xal");
        write(new File(xalRoot, XalStorageContract.CURRENT_ACCOUNT_FILE), pointer(BOUND_ID));
        write(new File(xalRoot, XalStorageContract.ACCOUNTS_FILE), accounts(ACTIVE_ID, BOUND_ID));

        File directory = XalStorageContract.accountDirectory(xalRoot);
        assertNotNull(directory);
        assertEquals(XalStorageContract.encodeUserId(BOUND_ID), directory.getName());
        assertTrue(XalStorageContract.launcherDataPresent(xalRoot));
    }

    @Test
    public void aStoreWithoutAnAccountDoesNotClaimToHoldOne() throws Exception {
        File xalRoot = folder.newFolder("empty-xal");
        assertNull(XalStorageContract.accountDirectory(xalRoot));
        assertFalse(XalStorageContract.launcherDataPresent(xalRoot));
    }

    private static String pointer(String id) {
        return "{\"msUserId\":\"" + id + "\"}";
    }

    private static String accounts(String activeId, String otherId) {
        return "[{\"msUserId\":\"" + activeId + "\",\"active\":true},"
                + "{\"msUserId\":\"" + otherId + "\",\"active\":false}]";
    }

    private static void write(File file, String content) throws Exception {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
