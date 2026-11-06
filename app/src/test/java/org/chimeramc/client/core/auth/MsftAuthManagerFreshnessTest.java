package org.chimeramc.client.core.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The sign-in freshness rule in {@link MsftAuthManager}.
 *
 * <p>This is the rule whose absence caused "signed in, but the game starts signed out, so sign in
 * again": a serialised chain is a snapshot with its own expiries, and reading it back renews
 * nothing, so an old snapshot exported at launch is an expired sign-in.
 */
public class MsftAuthManagerFreshnessTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long HOUR = 60L * 60L * 1000L;

    @Test
    public void materialWrittenJustNowIsNotRefreshed() {
        assertFalse(MsftAuthManager.shouldRefreshTokens(NOW, NOW));
        assertFalse(MsftAuthManager.shouldRefreshTokens(NOW - HOUR, NOW));
    }

    @Test
    public void materialOlderThanTheTrustWindowIsRefreshed() {
        assertTrue(MsftAuthManager.shouldRefreshTokens(
                NOW - MsftAuthManager.TOKEN_MAX_TRUST_MS, NOW));
        assertTrue(MsftAuthManager.shouldRefreshTokens(
                NOW - 2 * MsftAuthManager.TOKEN_MAX_TRUST_MS, NOW));
    }

    @Test
    public void theBoundaryIsExactlyTheTrustWindow() {
        assertFalse(MsftAuthManager.shouldRefreshTokens(
                NOW - MsftAuthManager.TOKEN_MAX_TRUST_MS + 1, NOW));
        assertTrue(MsftAuthManager.shouldRefreshTokens(
                NOW - MsftAuthManager.TOKEN_MAX_TRUST_MS, NOW));
    }

    @Test
    public void anUnstampedEntryIsAlwaysRefreshed() {
        assertTrue(MsftAuthManager.shouldRefreshTokens(0, NOW));
        assertTrue(MsftAuthManager.shouldRefreshTokens(-5, NOW));
    }

    @Test
    public void aBackwardsClockDoesNotForceARefreshLoop() {
        assertFalse(MsftAuthManager.shouldRefreshTokens(NOW, NOW - 10 * HOUR));
    }

    @Test
    public void aNonsenseNowDoesNotForceARefresh() {
        assertFalse(MsftAuthManager.shouldRefreshTokens(NOW, 0));
    }

    @Test
    public void usableMaterialMustBeNonEmptyJson() {
        assertTrue(MsftAuthManager.hasUsableAuthMaterial("{\"msaToken\":{}}"));
        assertTrue(MsftAuthManager.hasUsableAuthMaterial("  {\"a\":1}  "));
        assertFalse(MsftAuthManager.hasUsableAuthMaterial(null));
        assertFalse(MsftAuthManager.hasUsableAuthMaterial(""));
        assertFalse(MsftAuthManager.hasUsableAuthMaterial("   "));
        assertFalse(MsftAuthManager.hasUsableAuthMaterial("not-json"));
    }
}
