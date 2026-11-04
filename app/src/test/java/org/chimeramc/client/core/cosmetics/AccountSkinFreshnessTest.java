package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The account skin is kept in sync, not fetched once and frozen.
 *
 * <p>The lookup fetched the skin only when the cache was empty, so a player who changed their
 * Bedrock skin kept seeing the old one until they cleared app data — the "skin sync is not constant"
 * report. The cache now carries the fetch time and the service re-syncs on the same rules a fresh
 * fetch uses: missing, for another account, or older than the trust window.
 */
public class AccountSkinFreshnessTest {

    private static byte[] png() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G'};
    }

    @Test
    public void aFreshEntryForTheAccountIsNotRefetched() {
        long now = 1_000_000_000L;
        AccountSkinCache.Entry entry =
                new AccountSkinCache.Entry(png(), "account:111", now - 1000L);
        assertFalse(AccountSkinCache.shouldRefresh(entry, "111", now, AccountSkinCache.MAX_AGE_MS));
    }

    @Test
    public void aStaleEntryIsRefetched() {
        long now = 1_000_000_000L;
        AccountSkinCache.Entry entry = new AccountSkinCache.Entry(png(), "account:111",
                now - AccountSkinCache.MAX_AGE_MS - 1L);
        assertTrue("an entry older than the trust window must be re-synced",
                AccountSkinCache.shouldRefresh(entry, "111", now, AccountSkinCache.MAX_AGE_MS));
    }

    @Test
    public void aMissingOrForeignEntryIsRefetched() {
        long now = 1_000_000_000L;
        assertTrue("no cache means fetch",
                AccountSkinCache.shouldRefresh(null, "111", now, AccountSkinCache.MAX_AGE_MS));
        AccountSkinCache.Entry other =
                new AccountSkinCache.Entry(png(), "account:222", now);
        assertTrue("another account's cache must be refreshed, not shown",
                AccountSkinCache.shouldRefresh(other, "111", now, AccountSkinCache.MAX_AGE_MS));
    }

    @Test
    public void anEntryWithNoStampIsStale() {
        long now = 1_000_000_000L;
        // A cache written before the stamp existed has no fetch time, so it is refreshed once.
        AccountSkinCache.Entry legacy = new AccountSkinCache.Entry(png(), "account:111");
        assertTrue(AccountSkinCache.shouldRefresh(legacy, "111", now, AccountSkinCache.MAX_AGE_MS));
    }

    @Test
    public void theTrustWindowCanBeDisabled() {
        long now = 1_000_000_000L;
        AccountSkinCache.Entry ancient =
                new AccountSkinCache.Entry(png(), "account:111", 1L);
        assertFalse("a non-positive window disables the age check",
                AccountSkinCache.shouldRefresh(ancient, "111", now, 0L));
        // But a wrong account is still refreshed even with the age check off.
        AccountSkinCache.Entry other =
                new AccountSkinCache.Entry(png(), "account:222", 1L);
        assertTrue(AccountSkinCache.shouldRefresh(other, "111", now, 0L));
    }

    @Test
    public void theStampSurvivesARoundTrip() throws Exception {
        java.io.File root = java.nio.file.Files.createTempDirectory("acct").toFile();
        long stamp = 1_700_000_000_000L;
        assertTrue(AccountSkinCache.write(root,
                new AccountSkinCache.Entry(png(), AccountSkinCache.accountKeyFor("111"), stamp)));
        AccountSkinCache.Entry read = AccountSkinCache.read(root);
        org.junit.Assert.assertEquals("the fetch time must round-trip", stamp, read.fetchedAtMs);
    }

    @Test
    public void theSyncRunsAtStartupOnTheDeferredPath() throws Exception {
        // The re-sync must happen without the player opening a preview, and must not block the first
        // frame, so it is a deferred startup step. This pins both halves of that wiring.
        String app = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "Application.kt");
        assertTrue("startup must call the account skin re-sync",
                app.contains("AccountSkinService") && app.contains("refreshIfStale"));
        assertTrue("the re-sync must be a deferred step, not on the critical path",
                app.contains("step(\"account_skin_sync\")"));

        String phases = read("app/src/main/java/org/chimeramc/client/core/startup/"
                + "StartupPhases.java");
        assertTrue("the re-sync step must be classified", phases.contains("ACCOUNT_SKIN_SYNC"));
    }

    private static String read(String relativePath) throws java.io.IOException {
        java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (java.nio.file.Files.exists(dir.resolve("app/src/main/res/layout"))) {
                return new String(java.nio.file.Files.readAllBytes(
                        dir.resolve(relativePath)), java.nio.charset.StandardCharsets.UTF_8);
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
