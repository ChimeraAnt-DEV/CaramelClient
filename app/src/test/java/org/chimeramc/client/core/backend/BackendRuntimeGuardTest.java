package org.chimeramc.client.core.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the two script-runtime safety counters: the per-feature call rate and the
 * dead-man switch that releases stuck input.
 */
public class BackendRuntimeGuardTest {

    // --- BackendCallMeter --------------------------------------------------------

    @Test
    public void callsAreCountedWithinTheirSecond() {
        BackendCallMeter meter = new BackendCallMeter();
        meter.record(GameBackend.Feature.KEY, 0L);
        meter.record(GameBackend.Feature.KEY, 10L);
        meter.record(GameBackend.Feature.KEY, 999L);
        // Nothing has rotated yet, so the last completed second is still empty.
        assertEquals(0, meter.lastSecondCount(GameBackend.Feature.KEY));

        meter.record(GameBackend.Feature.KEY, 1_000L);
        assertEquals(3, meter.lastSecondCount(GameBackend.Feature.KEY));
    }

    @Test
    public void aLongPauseRotatesOnceAndReportsThePriorSecond() {
        BackendCallMeter meter = new BackendCallMeter();
        meter.record(GameBackend.Feature.LOOK_DELTA, 0L);
        meter.record(GameBackend.Feature.LOOK_DELTA, 100L);
        // A ten-second gap must not replay ten empty buckets, and must not lose the last real one.
        meter.record(GameBackend.Feature.LOOK_DELTA, 10_000L);
        assertEquals(2, meter.lastSecondCount(GameBackend.Feature.LOOK_DELTA));
    }

    @Test
    public void readingDoesNotResetTheBucket() {
        BackendCallMeter meter = new BackendCallMeter();
        meter.record(GameBackend.Feature.KEY, 0L);
        meter.record(GameBackend.Feature.KEY, 1_000L);
        assertEquals(1, meter.lastSecondCount(GameBackend.Feature.KEY));
        // A second read in the same instant must see the same figure, not zero.
        assertEquals(1, meter.lastSecondCount(GameBackend.Feature.KEY));
    }

    @Test
    public void theTotalSumsEveryFeatureInTheLastSecond() {
        BackendCallMeter meter = new BackendCallMeter();
        meter.record(GameBackend.Feature.KEY, 0L);
        meter.record(GameBackend.Feature.SCROLL, 0L);
        meter.record(GameBackend.Feature.SCROLL, 0L);
        meter.record(GameBackend.Feature.KEY, 1_000L);
        assertEquals(3, meter.lastSecondTotal());
    }

    @Test
    public void aNullFeatureIsIgnored() {
        BackendCallMeter meter = new BackendCallMeter();
        meter.record(null, 0L);
        assertEquals(0, meter.lastSecondTotal());
        assertEquals(0, meter.lastSecondCount(null));
    }

    @Test
    public void tickAdvancesTheFigureOnASecondWithNoCalls() {
        BackendCallMeter meter = new BackendCallMeter();
        meter.record(GameBackend.Feature.KEY, 0L);
        assertTrue(meter.tick(1_000L));
        assertEquals(1, meter.lastSecondCount(GameBackend.Feature.KEY));
        assertTrue(meter.tick(2_000L));
        // A second in which nothing was called reports zero, which is the useful signal.
        assertEquals(0, meter.lastSecondCount(GameBackend.Feature.KEY));
    }

    @Test
    public void resetClearsEverything() {
        BackendCallMeter meter = new BackendCallMeter();
        meter.record(GameBackend.Feature.KEY, 0L);
        meter.tick(1_000L);
        meter.reset();
        assertEquals(0, meter.lastSecondTotal());
    }

    // --- BackendDeadManSwitch ----------------------------------------------------

    @Test
    public void aFewFailuresDoNotTripTheSwitch() {
        BackendDeadManSwitch sw = new BackendDeadManSwitch();
        assertFalse(sw.record(false, 0L));
        assertFalse(sw.record(false, 10L));
        assertFalse("two failures is not a dead backend", sw.isTripped());
    }

    @Test
    public void sustainedFailureTripsOnceAndReleasesInput() {
        BackendDeadManSwitch sw = new BackendDeadManSwitch();
        sw.record(false, 0L);
        sw.record(false, 100L);
        assertFalse(sw.record(false, 1_000L));
        // Past the silence window with the streak intact: this is the trip.
        assertTrue(sw.record(false, 6_000L));
        assertTrue(sw.isTripped());
        // It must trip at most once, so the caller cannot be spammed with release warnings.
        assertFalse(sw.record(false, 6_100L));
    }

    @Test
    public void oneSuccessfulCallProvesLivenessAndResetsTheSwitch() {
        BackendDeadManSwitch sw = new BackendDeadManSwitch();
        sw.record(false, 0L);
        sw.record(false, 100L);
        sw.record(false, 6_000L);
        assertTrue(sw.isTripped());
        assertFalse(sw.record(true, 6_100L));
        assertFalse(sw.isTripped());
        assertEquals(0, sw.consecutiveFailures());
        // After recovery, the streak must start from scratch rather than re-trip on one failure.
        assertFalse(sw.record(false, 6_200L));
        assertFalse(sw.isTripped());
    }

    @Test
    public void theWindowIsMeasuredFromTheFirstFailureNotTheLast() {
        BackendDeadManSwitch sw = new BackendDeadManSwitch();
        // Failures spread across more than the window but each close together: the first failure is
        // the anchor, so this must trip once the elapsed time passes the window.
        sw.record(false, 0L);
        sw.record(false, 3_000L);
        assertTrue(sw.record(false, 5_100L));
    }

    @Test
    public void resetClearsATrippedSwitch() {
        BackendDeadManSwitch sw = new BackendDeadManSwitch();
        sw.record(false, 0L);
        sw.record(false, 100L);
        sw.record(false, 6_000L);
        assertTrue(sw.isTripped());
        sw.reset();
        assertFalse(sw.isTripped());
        assertEquals(0, sw.consecutiveFailures());
    }
}
