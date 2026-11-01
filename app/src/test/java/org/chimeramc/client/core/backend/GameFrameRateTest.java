package org.chimeramc.client.core.backend;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The frame-rate derivation rule, driven with a fake tick counter and a fake clock.
 *
 * <p>No Android types and no renderer, so the rate maths and every "unknown" branch is pinned here
 * rather than only being visible on a device.
 */
public class GameFrameRateTest {

    @Test
    public void reportsUnknownUntilAWindowElapses() {
        GameFrameRate rate = new GameFrameRate();
        assertEquals(GameFrameRate.UNKNOWN, rate.sample(100, 0));
        assertEquals(GameFrameRate.UNKNOWN, rate.sample(130, 500));
        assertEquals(GameFrameRate.UNKNOWN, rate.fps());
    }

    @Test
    public void derivesSixtyFpsFromSixtyTicksInOneSecond() {
        GameFrameRate rate = new GameFrameRate();
        rate.sample(0, 0);
        assertEquals(60, rate.sample(60, 1000));
    }

    @Test
    public void derivesThirtyFpsFromThirtyTicksInOneSecond() {
        GameFrameRate rate = new GameFrameRate();
        rate.sample(0, 0);
        assertEquals(30, rate.sample(30, 1000));
    }

    @Test
    public void aLongerWindowScalesTheRate() {
        GameFrameRate rate = new GameFrameRate();
        rate.sample(0, 0);
        // 120 ticks over 2s is 60 fps.
        assertEquals(60, rate.sample(120, 2000));
    }

    @Test
    public void aStalledRendererReadsUnknownNotZero() {
        GameFrameRate rate = new GameFrameRate();
        rate.sample(0, 0);
        assertEquals(60, rate.sample(60, 1000));
        // The hook is still installed but nothing is being drawn: one tick in the next second.
        assertEquals(GameFrameRate.UNKNOWN, rate.sample(61, 2000));
    }

    @Test
    public void aMissingHookReadsUnknown() {
        GameFrameRate rate = new GameFrameRate();
        assertEquals(GameFrameRate.UNKNOWN, rate.sample(-1, 0));
        assertEquals(GameFrameRate.UNKNOWN, rate.sample(-1, 5000));
    }

    @Test
    public void aCounterThatGoesBackwardsReanchors() {
        GameFrameRate rate = new GameFrameRate();
        rate.sample(1000, 0);
        // A new session: the tick restarted from a low value. Re-anchor rather than report a
        // negative elapsed count.
        assertEquals(GameFrameRate.UNKNOWN, rate.sample(2, 1000));
        // And the next window measures normally from the new anchor.
        assertEquals(60, rate.sample(62, 2000));
    }

    @Test
    public void resetClearsTheWindow() {
        GameFrameRate rate = new GameFrameRate();
        rate.sample(0, 0);
        rate.sample(60, 1000);
        assertEquals(60, rate.fps());
        rate.reset();
        assertEquals(GameFrameRate.UNKNOWN, rate.fps());
        assertEquals(GameFrameRate.UNKNOWN, rate.sample(9999, 2000));
    }
}
