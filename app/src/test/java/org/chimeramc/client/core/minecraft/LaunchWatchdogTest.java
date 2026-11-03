package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the launch-watchdog rule.
 *
 * <p>The boundaries matter: a launch that is still progressing must never be interrupted, and the
 * two escalation states must not fire a moment early or a moment late.
 */
public class LaunchWatchdogTest {

    private static final long T0 = 1_000_000L;

    @Test
    public void aFreshLaunchIsOk() {
        assertEquals(LaunchWatchdog.State.OK,
                LaunchWatchdog.evaluate(T0, T0, false, T0));
    }

    @Test
    public void aFirstFrameAlwaysMeansOk() {
        assertEquals(LaunchWatchdog.State.OK,
                LaunchWatchdog.evaluate(T0, T0, true, T0 + LaunchWatchdog.DEAD_MS * 10));
    }

    @Test
    public void progressResetsTheStallClock() {
        // No progress since T0, but a progress event at T0 + 80s means only 20s of silence at T0+100s.
        long now = T0 + 100_000L;
        assertEquals(LaunchWatchdog.State.OK,
                LaunchWatchdog.evaluate(T0, T0 + 80_000L, false, now));
    }

    @Test
    public void aLongSilenceIsStalled() {
        assertEquals(LaunchWatchdog.State.STALLED,
                LaunchWatchdog.evaluate(T0, T0, false, T0 + LaunchWatchdog.STALLED_MS));
    }

    @Test
    public void aVeryLongSilenceIsDead() {
        assertEquals(LaunchWatchdog.State.DEAD,
                LaunchWatchdog.evaluate(T0, T0, false, T0 + LaunchWatchdog.DEAD_MS));
    }

    @Test
    public void justBeforeTheStallThresholdIsOk() {
        assertEquals(LaunchWatchdog.State.OK,
                LaunchWatchdog.evaluate(T0, T0, false, T0 + LaunchWatchdog.STALLED_MS - 1));
    }

    @Test
    public void justBeforeTheDeadThresholdIsStillStalled() {
        assertEquals(LaunchWatchdog.State.STALLED,
                LaunchWatchdog.evaluate(T0, T0, false, T0 + LaunchWatchdog.DEAD_MS - 1));
    }

    @Test
    public void aProgressEventNewerThanTheStartAnchorsTheClock() {
        long last = T0 + 50_000L;
        assertEquals(LaunchWatchdog.State.OK,
                LaunchWatchdog.evaluate(T0, last, false, last + LaunchWatchdog.STALLED_MS - 1));
        assertEquals(LaunchWatchdog.State.STALLED,
                LaunchWatchdog.evaluate(T0, last, false, last + LaunchWatchdog.STALLED_MS));
    }

    @Test
    public void onlyTheEscalatedStatesAreActionable() {
        assertFalse(LaunchWatchdog.isActionable(LaunchWatchdog.State.OK));
        assertTrue(LaunchWatchdog.isActionable(LaunchWatchdog.State.STALLED));
        assertTrue(LaunchWatchdog.isActionable(LaunchWatchdog.State.DEAD));
    }

    @Test
    public void theNextCheckIsTheStallThresholdFirst() {
        long at = LaunchWatchdog.nextCheckAt(T0, T0, false, T0);
        assertEquals(T0 + LaunchWatchdog.STALLED_MS, at);
    }

    @Test
    public void theNextCheckIsTheDeadThresholdAfterTheStall() {
        long now = T0 + LaunchWatchdog.STALLED_MS;
        long at = LaunchWatchdog.nextCheckAt(T0, T0, false, now);
        assertEquals(T0 + LaunchWatchdog.DEAD_MS, at);
    }

    @Test
    public void theNextCheckFiresImmediatelyWhenAlreadyPastBoth() {
        long now = T0 + LaunchWatchdog.DEAD_MS + 5_000L;
        assertEquals(now, LaunchWatchdog.nextCheckAt(T0, T0, false, now));
    }

    @Test
    public void aFirstFrameDisablesScheduling() {
        assertEquals(Long.MAX_VALUE, LaunchWatchdog.nextCheckAt(T0, T0, true, T0));
    }
}
