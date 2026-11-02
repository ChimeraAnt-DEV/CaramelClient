package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the cloud-relay poll backoff (item 244).
 *
 * <p>The property that matters is the shape: a healthy relay keeps the one-second freshness, a
 * single blip does not change anything, and a sustained outage settles at the cap rather than
 * growing without bound.
 */
public class CloudPollBackoffTest {

    @Test
    public void aFreshBackoffUsesTheBaseDelay() {
        assertEquals(CloudPollBackoff.BASE_DELAY_MS, new CloudPollBackoff().nextDelayMs());
    }

    @Test
    public void oneFailureDoesNotChangeTheDelay() {
        CloudPollBackoff b = new CloudPollBackoff();
        b.onFailure();
        assertEquals(CloudPollBackoff.BASE_DELAY_MS, b.nextDelayMs());
    }

    @Test
    public void twoFailuresDoubleTheDelay() {
        CloudPollBackoff b = new CloudPollBackoff();
        b.onFailure();
        b.onFailure();
        assertEquals(2_000L, b.nextDelayMs());
    }

    @Test
    public void threeFailuresDoubleAgain() {
        CloudPollBackoff b = new CloudPollBackoff();
        b.onFailure();
        b.onFailure();
        b.onFailure();
        assertEquals(4_000L, b.nextDelayMs());
    }

    @Test
    public void theDelayIsCapped() {
        CloudPollBackoff b = new CloudPollBackoff();
        for (int i = 0; i < 50; i++) b.onFailure();
        assertEquals(CloudPollBackoff.MAX_DELAY_MS, b.nextDelayMs());
    }

    @Test
    public void aSuccessResetsToTheBaseDelay() {
        CloudPollBackoff b = new CloudPollBackoff();
        for (int i = 0; i < 10; i++) b.onFailure();
        b.onSuccess();
        assertEquals(CloudPollBackoff.BASE_DELAY_MS, b.nextDelayMs());
        assertEquals(0, b.consecutiveFailures());
    }

    @Test
    public void resetClearsAStaleFailureCount() {
        CloudPollBackoff b = new CloudPollBackoff();
        b.onFailure();
        b.onFailure();
        b.onFailure();
        b.reset();
        assertEquals(CloudPollBackoff.BASE_DELAY_MS, b.nextDelayMs());
    }

    @Test
    public void aHugeFailureCountDoesNotOverflowTheShift() {
        CloudPollBackoff b = new CloudPollBackoff();
        for (int i = 0; i < 5_000; i++) b.onFailure();
        long delay = b.nextDelayMs();
        assertTrue("delay must stay positive", delay > 0);
        assertEquals(CloudPollBackoff.MAX_DELAY_MS, delay);
    }

    @Test
    public void theCapIsLongerThanTheBaseAndFinite() {
        assertTrue(CloudPollBackoff.MAX_DELAY_MS > CloudPollBackoff.BASE_DELAY_MS);
        assertEquals(60_000L, CloudPollBackoff.MAX_DELAY_MS);
    }
}
