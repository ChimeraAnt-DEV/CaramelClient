package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The account-skin fetch guard.
 *
 * <p>These pin the defect that made a fresh sign-in show a default skin: a single boolean guard
 * dropped the fetch for the account that had just signed in whenever a fetch for the previous
 * account was still running.
 */
public class SkinFetchGuardTest {

    @Test
    public void theFirstRequestForAnAccountIsAllowed() {
        SkinFetchGuard guard = new SkinFetchGuard();
        assertTrue(guard.tryStart("account:1"));
    }

    @Test
    public void aSecondRequestForTheSameAccountIsDropped() {
        SkinFetchGuard guard = new SkinFetchGuard();
        assertTrue(guard.tryStart("account:1"));
        // Duplicate work for a result that is already coming.
        assertFalse(guard.tryStart("account:1"));
    }

    @Test
    public void aDifferentAccountSupersedesTheOneInFlight() {
        // This is the regression. With a single boolean the second call returned false and the
        // newly signed-in account's skin was never fetched, so the preview resolved to Steve.
        SkinFetchGuard guard = new SkinFetchGuard();
        assertTrue("the first account starts", guard.tryStart("account:1"));
        assertTrue("a different account must not be dropped", guard.tryStart("account:2"));
        assertTrue("the newest account owns the slot", "account:2".equals(guard.currentKey()));
    }

    @Test
    public void theSameAccountIsAllowedAgainAfterItFinishes() {
        SkinFetchGuard guard = new SkinFetchGuard();
        assertTrue(guard.tryStart("account:1"));
        guard.finish("account:1");
        assertNull(guard.currentKey());
        assertFalse(guard.isRunning());
        assertTrue("a later refresh for the same account is allowed", guard.tryStart("account:1"));
    }

    @Test
    public void aSupersededFetchFinishingLateDoesNotClearTheNewClaim() {
        // The account-1 fetch completes after account-2 superseded it. Clearing the slot here would
        // let a second account-2 fetch start while the first is still running.
        SkinFetchGuard guard = new SkinFetchGuard();
        guard.tryStart("account:1");
        guard.tryStart("account:2");
        guard.finish("account:1");
        assertTrue("account 2 must still own the slot", "account:2".equals(guard.currentKey()));
        assertFalse("and a duplicate account-2 fetch is still refused",
                guard.tryStart("account:2"));
    }

    @Test
    public void anEmptyOrNullKeyIsNeverFetched() {
        SkinFetchGuard guard = new SkinFetchGuard();
        assertFalse(guard.tryStart(null));
        assertFalse(guard.tryStart(""));
        assertFalse(guard.isRunning());
    }

    @Test
    public void finishingAnUnknownKeyIsHarmless() {
        SkinFetchGuard guard = new SkinFetchGuard();
        guard.tryStart("account:1");
        guard.finish("account:9");
        assertTrue("an unrelated finish must not clear the slot",
                "account:1".equals(guard.currentKey()));
        guard.finish(null);
        assertTrue("a null finish must not clear the slot",
                "account:1".equals(guard.currentKey()));
    }
}
