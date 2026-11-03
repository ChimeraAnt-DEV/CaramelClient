package org.chimeramc.client.core.mods.fps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The adaptive quality policy, pinned without a device.
 *
 * <p>Each rule is asserted on its own: the dead band absorbs jitter, hysteresis needs several
 * consecutive frames, thermal pressure clamps and blocks restoration for the dwell, and tail latency
 * can veto a reduce that the average would have allowed.
 */
public class AdaptiveFramePolicyTest {

    private static AdaptiveFramePolicy policy() {
        return new AdaptiveFramePolicy(AdaptiveFramePolicy.Config.defaults());
    }

    /** 16.7ms is 60fps, the target; a frame above the slow threshold is 20ms. */
    private static final float TARGET_MS = 1000f / 60f;

    @Test
    public void aFrameInsideTheDeadBandDoesNothing() {
        AdaptiveFramePolicy p = policy();
        // Just above target but inside the 10% slow margin: no action.
        assertEquals(AdaptiveFramePolicy.Action.NONE, p.onFrame(TARGET_MS * 1.05f, 0f));
        assertEquals(1f, p.quality(), 1e-4f);
    }

    @Test
    public void hysteresisNeedsSeveralConsecutiveSlowFrames() {
        AdaptiveFramePolicy p = policy();
        float slow = TARGET_MS * 1.3f;
        // Fewer than framesToReact (8) slow frames: nothing yet.
        for (int i = 0; i < 7; i++) {
            assertEquals(AdaptiveFramePolicy.Action.NONE, p.onFrame(slow, 0f));
        }
        assertEquals("quality must not move before the reaction window",
                1f, p.quality(), 1e-4f);
        // The 8th slow frame reduces once.
        assertEquals(AdaptiveFramePolicy.Action.REDUCED, p.onFrame(slow, 0f));
        assertEquals(1f - 0.08f, p.quality(), 1e-4f);
    }

    @Test
    public void aSingleFastFrameResetsTheSlowRun() {
        AdaptiveFramePolicy p = policy();
        float slow = TARGET_MS * 1.3f;
        for (int i = 0; i < 7; i++) p.onFrame(slow, 0f);
        p.onFrame(TARGET_MS, 0f); // one good frame
        for (int i = 0; i < 7; i++) {
            assertEquals(AdaptiveFramePolicy.Action.NONE, p.onFrame(slow, 0f));
        }
        assertEquals("the slow run must have restarted", 1f, p.quality(), 1e-4f);
    }

    @Test
    public void qualityNeverGoesBelowTheFloor() {
        AdaptiveFramePolicy p = policy();
        float slow = TARGET_MS * 2f;
        for (int i = 0; i < 500; i++) p.onFrame(slow, 0f);
        assertEquals("the floor keeps some quality", 0.25f, p.quality(), 1e-4f);
    }

    @Test
    public void fastFramesRestoreQualitySlowly() {
        AdaptiveFramePolicy p = policy();
        float slow = TARGET_MS * 1.3f;
        for (int i = 0; i < 8; i++) p.onFrame(slow, 0f); // one reduce -> 0.92
        assertEquals(0.92f, p.quality(), 1e-4f);
        // A fast run of framesToReact restores one step.
        AdaptiveFramePolicy.Action last = AdaptiveFramePolicy.Action.NONE;
        for (int i = 0; i < 8; i++) last = p.onFrame(TARGET_MS * 0.5f, 0f);
        assertEquals(AdaptiveFramePolicy.Action.RESTORED, last);
        assertEquals(1f, p.quality(), 1e-4f);
    }

    @Test
    public void thermalPressureClampsAndBlocksRestoration() {
        AdaptiveFramePolicy p = policy();
        // Warm the quality down first so there is room to restore.
        for (int i = 0; i < 16; i++) p.onFrame(TARGET_MS * 1.3f, 0f);
        assertTrue(p.quality() < 1f);

        assertEquals(AdaptiveFramePolicy.Action.THERMAL_CLAMP,
                p.onThermal(AdaptiveFramePolicy.Thermal.SERIOUS));
        assertTrue("a thermally limited device is clamped", p.quality() <= 0.4f);
        assertTrue(p.thermallyLimited());

        // Even with fast frames, restoration is blocked during the dwell.
        for (int i = 0; i < 60; i++) {
            assertEquals("no restoration while thermally limited",
                    AdaptiveFramePolicy.Action.NONE, p.onFrame(TARGET_MS * 0.5f, 0f));
        }
        assertEquals(0.4f, p.quality(), 1e-4f);
    }

    @Test
    public void fairHoldsAtTheFairCeiling() {
        AdaptiveFramePolicy p = policy();
        p.onThermal(AdaptiveFramePolicy.Thermal.FAIR);
        assertFalse("Fair is warm, not throttling", p.thermallyLimited());
        // Quality is allowed up to the fair ceiling only.
        for (int i = 0; i < 40; i++) p.onFrame(TARGET_MS * 0.5f, 0f);
        assertTrue("quality must not exceed the fair ceiling", p.quality() <= 0.7f + 1e-4f);
    }

    @Test
    public void tailLatencyCanVetoAReduceTheAverageWouldAllow() {
        AdaptiveFramePolicy p = policy();
        float slow = TARGET_MS * 1.3f;
        // The average is slow, but the p99 is inside the band: the reduce is vetoed.
        for (int i = 0; i < 12; i++) {
            assertEquals(AdaptiveFramePolicy.Action.NONE, p.onFrame(slow, TARGET_MS));
        }
        assertEquals(1f, p.quality(), 1e-4f);
        // Once the tail agrees, it reduces.
        AdaptiveFramePolicy.Action last = AdaptiveFramePolicy.Action.NONE;
        for (int i = 0; i < 8; i++) last = p.onFrame(slow, slow * 1.2f);
        assertEquals(AdaptiveFramePolicy.Action.REDUCED, last);
    }

    @Test
    public void theOverlayIntervalRisesAsQualityFalls() {
        AdaptiveFramePolicy p = policy();
        long atFull = p.overlayTickIntervalMs(0, 64);
        assertEquals("full quality uses the shortest interval", 0L, atFull);
        float slow = TARGET_MS * 2f;
        for (int i = 0; i < 500; i++) p.onFrame(slow, 0f);
        long atFloor = p.overlayTickIntervalMs(0, 64);
        assertTrue("a lower quality must use a longer interval", atFloor > atFull);
        assertEquals("the interval is linear in quality", 48L, atFloor);
    }

    @Test
    public void aNonPositiveFrameIsIgnored() {
        AdaptiveFramePolicy p = policy();
        assertEquals(AdaptiveFramePolicy.Action.NONE, p.onFrame(0f, 0f));
        assertEquals(AdaptiveFramePolicy.Action.NONE, p.onFrame(-5f, 0f));
        assertEquals(1f, p.quality(), 1e-4f);
    }
}
