package org.chimeramc.client.core.auth.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The pointer comparison that gates a re-export after a token refresh.
 *
 * <p>{@code Xal.Current.json} holds the account's {@code msUserId}; the launcher calls the refresh
 * with the account's random {@code id}. A comparison that only understood the id rejected every
 * refresh, so the store held fresh tokens while the game kept reading the old export.
 */
public class XalExporterRefreshTest {

    @Test
    public void thePointerMatchesTheMsUserIdTheExporterWrites() {
        assertTrue(XalExporter.pointerNamesAccount("ms-user-id", "random-uuid", "ms-user-id"));
    }

    @Test
    public void aPointerNamingAnotherAccountIsRefused() {
        assertFalse(XalExporter.pointerNamesAccount("someone-else", "random-uuid", "ms-user-id"));
    }

    @Test
    public void aMissingPointerIsRefused() {
        assertFalse(XalExporter.pointerNamesAccount(null, "random-uuid", "ms-user-id"));
    }

    @Test
    public void theRandomIdIsAlsoAccepted() {
        // An export pointer written by an older build may hold the store id; it still names the
        // same account, so the refresh must not be refused.
        assertTrue(XalExporter.pointerNamesAccount("random-uuid", "random-uuid", "ms-user-id"));
    }

    @Test
    public void surroundingWhitespaceIsIgnored() {
        assertTrue(XalExporter.pointerNamesAccount("  ms-user-id \n", "random-uuid", "ms-user-id"));
    }
}
