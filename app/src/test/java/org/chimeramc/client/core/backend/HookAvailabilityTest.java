package org.chimeramc.client.core.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * The preloader-hook availability table.
 *
 * <p>A hook that did not resolve must read as not live, never as "unknown-but-try-it": a feature
 * that calls into a missing hook draws nothing, which is the exact ambiguity this table removes. The
 * tests pin the fail-closed default, the per-hook mapping, and that a throwing probe reads as not
 * live rather than taking a draw call down.
 */
public class HookAvailabilityTest {

    @After
    public void tearDown() {
        HookAvailability.resetForTests();
    }

    @Test
    public void withNoSourceEveryHookReadsAsNotLive() {
        // The fail-closed default. A caller that consults this before drawing does nothing.
        for (HookAvailability.Hook hook : HookAvailability.Hook.values()) {
            assertFalse(hook.id(), HookAvailability.isLive(hook));
        }
        assertTrue(HookAvailability.summary(), HookAvailability.summary().contains("no preloader hook"));
    }

    @Test
    public void aNullHookIsNeverLive() {
        HookAvailability.install(h -> true);
        assertFalse(HookAvailability.isLive(null));
    }

    @Test
    public void anInstalledSourceIsConsultedPerHook() {
        HookAvailability.install(hook -> hook == HookAvailability.Hook.PLAYER_RENDER);
        assertTrue(HookAvailability.isLive(HookAvailability.Hook.PLAYER_RENDER));
        assertFalse(HookAvailability.isLive(HookAvailability.Hook.MCE_IMAGE));
        String summary = HookAvailability.summary();
        assertTrue(summary, summary.contains("player_render=on"));
        assertTrue(summary, summary.contains("mce_image=off"));
    }

    @Test
    public void aThrowingProbeReadsAsNotLive() {
        HookAvailability.install(hook -> {
            throw new IllegalStateException("native read failed");
        });
        assertFalse(HookAvailability.isLive(HookAvailability.Hook.PLAYER_RENDER));
        assertNotNull(HookAvailability.summary());
    }

    @Test
    public void aNullSourceIsTreatedAsNoHooks() {
        HookAvailability.install(null);
        assertFalse(HookAvailability.isLive(HookAvailability.Hook.PLAYER_RENDER));
    }

    @Test
    public void theFpsFeatureDependsOnTheRenderHook() {
        assertEquals(HookAvailability.Hook.PLAYER_RENDER,
                HookAvailability.hookFor(GameBackend.Feature.FPS));
        // A host-side feature has no hook of its own and must not be gated by this table.
        assertEquals(null, HookAvailability.hookFor(GameBackend.Feature.KEY));
        assertEquals(null, HookAvailability.hookFor(null));
        assertTrue(HookAvailability.featureHookIsLive(GameBackend.Feature.KEY));
        assertFalse(HookAvailability.featureHookIsLive(GameBackend.Feature.FPS));
    }

    @Test
    public void everyHookHasADistinctStableId() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (HookAvailability.Hook hook : HookAvailability.Hook.values()) {
            assertTrue("duplicate hook id " + hook.id(), ids.add(hook.id()));
            assertFalse(hook.id().isEmpty());
        }
    }
}
