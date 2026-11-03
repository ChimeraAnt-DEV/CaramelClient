package org.chimeramc.client.core.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the local launch statistics. No mocks: the class is pure arithmetic, and the
 * point of the tests is that the arithmetic cannot quietly report a flattering number.
 */
public class LaunchStatsTest {

    @Test
    public void aFreshStoreHasNoLaunchAndNoRate() {
        LaunchStats stats = new LaunchStats();
        assertEquals(0, stats.launches());
        assertEquals(LaunchStats.NO_FIRST_FRAME, stats.averageFirstFrameMs());
        assertTrue("an empty store must not report a rate", stats.crashRate() < 0f);
        assertTrue(stats.successRate() < 0f);
    }

    @Test
    public void theAverageFirstFrameIgnoresMissingSamples() {
        LaunchStats stats = new LaunchStats();
        stats.recordLaunch();
        stats.withFirstFrame(1000L);
        stats.recordLaunch();
        // A launch whose first frame was never observed must not count as a zero.
        stats.withFirstFrame(LaunchStats.NO_FIRST_FRAME);
        stats.recordLaunch();
        stats.withFirstFrame(3000L);

        assertEquals(2, stats.firstFrameSamples());
        assertEquals(2000L, stats.averageFirstFrameMs());
    }

    @Test
    public void theAverageIsARealMeanNotTheLastSample() {
        LaunchStats stats = new LaunchStats();
        stats.withFirstFrame(100L);
        stats.withFirstFrame(200L);
        stats.withFirstFrame(600L);
        assertEquals(300L, stats.averageFirstFrameMs());
    }

    @Test
    public void aCompletedLaunchCannotExceedTheLaunchCount() {
        LaunchStats stats = new LaunchStats();
        stats.recordLaunch();
        stats.recordCompletedLaunch();
        stats.recordCompletedLaunch();
        stats.recordCompletedLaunch();
        assertEquals(1, stats.completedLaunches());
        assertEquals(1.0f, stats.successRate(), 0.0001f);
    }

    @Test
    public void theCrashRateIsOverTheLaunchCount() {
        LaunchStats stats = new LaunchStats();
        stats.recordLaunch();
        stats.recordLaunch();
        stats.recordLaunch();
        stats.recordLaunch();
        stats.recordCrash();
        assertEquals(0.25f, stats.crashRate(), 0.0001f);
    }

    @Test
    public void negativeCountsFromStorageAreClampedToZero() {
        LaunchStats stats = new LaunchStats(-5, -5, -5, -5, -5L);
        assertEquals(0, stats.launches());
        assertEquals(0, stats.crashes());
        assertEquals(0L, stats.firstFrameTotalMs());
    }

    @Test
    public void theFirstFrameFormatsWithAnEmDashWhenUnknown() {
        assertEquals("\u2014", LaunchStats.formatFirstFrame(LaunchStats.NO_FIRST_FRAME));
        assertEquals("0 ms", LaunchStats.formatFirstFrame(0L));
        assertEquals("450 ms", LaunchStats.formatFirstFrame(450L));
        assertEquals("1.5 s", LaunchStats.formatFirstFrame(1500L));
    }

    @Test
    public void theRateFormatsWithAnEmDashWhenUnknown() {
        assertEquals("\u2014", LaunchStats.formatRate(-1f));
        assertEquals("0%", LaunchStats.formatRate(0f));
        assertEquals("25%", LaunchStats.formatRate(0.25f));
        assertEquals("100%", LaunchStats.formatRate(1f));
    }
}
