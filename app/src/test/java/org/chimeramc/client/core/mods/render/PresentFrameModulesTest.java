package org.chimeramc.client.core.mods.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pins the two present-frame effect modules (no mocks).
 *
 * <p>Two things matter. The catalogue must be exactly the two modules with unique option keys, so
 * the config schema cannot bind a key to the wrong option. And the policy must be honest: neither
 * module may promise a frame rate, both must be off when the present hook is absent regardless of
 * their switches, and the blur budget must be clamped so the effect cannot cost more than it
 * returns.
 */
public class PresentFrameModulesTest {

    @Test
    public void theCatalogueHasTheTwoModulesWithTheirOptions() {
        List<PresentFrameCatalog.Module> modules = PresentFrameCatalog.modules();
        assertEquals(2, modules.size());
        assertEquals(4, PresentFrameCatalog.find(PresentFrameCatalog.FRAME_GENERATION).options.size());
        assertEquals(4, PresentFrameCatalog.find(PresentFrameCatalog.MOTION_BLUR).options.size());
    }

    @Test
    public void everyOptionKeyIsUnique() {
        Set<String> seen = new HashSet<>();
        for (PresentFrameCatalog.Option option : PresentFrameCatalog.allOptions()) {
            assertTrue("duplicate option key: " + option.key, seen.add(option.key));
        }
        assertEquals(8, seen.size());
    }

    @Test
    public void everyOptionHasAValidRange() {
        for (PresentFrameCatalog.Option option : PresentFrameCatalog.allOptions()) {
            assertNotNull(option.key);
            assertNotNull(option.label);
            if (option.isToggle()) {
                assertEquals("false", option.defaultValue);
            } else {
                int def = Integer.parseInt(option.defaultValue);
                assertTrue(option.key + " default below min", def >= option.min);
                assertTrue(option.key + " default above max", def <= option.max);
            }
        }
    }

    @Test
    public void noDescriptionClaimsAFrameRateGain() {
        // The honesty contract: neither module measures or promises a frame rate. The words a lie
        // would use must not appear in any description, and the frame-gen description must say the
        // render rate is unchanged.
        for (PresentFrameCatalog.Module module : PresentFrameCatalog.modules()) {
            String all = (module.description + " " + module.title).toLowerCase();
            assertFalse(module.id + " must not claim an FPS boost", all.contains("boost"));
            assertFalse(module.id + " must not claim a higher fps", all.contains("higher fps"));
            assertFalse(module.id + " must not claim a frame-rate gain", all.contains("frame rate gain"));
            for (PresentFrameCatalog.Option option : module.options) {
                String text = option.description.toLowerCase();
                assertFalse(option.key + " must not claim a frame-rate gain",
                        text.contains("higher fps") || text.contains("frame rate gain"));
            }
        }
    }

    @Test
    public void frameGenerationSaysItDoesNotPresentAnExtraFrame() {
        // The native seam blends the previous frame into the current one and then lets the single
        // real present happen; it does not insert or present an extra frame. A description that
        // claimed otherwise would be a lie about what the module does, so the honest statement is
        // required to be present rather than merely not contradicted.
        PresentFrameCatalog.Module frameGen =
                PresentFrameCatalog.find(PresentFrameCatalog.FRAME_GENERATION);
        String all = (frameGen.description + " " + frameGen.title).toLowerCase();
        assertTrue("frame generation must say it does not present an extra frame",
                all.contains("does not present an extra frame"));
        assertTrue("frame generation must say the render rate is unchanged",
                all.contains("same render rate"));
    }

    @Test
    public void thePlanIsInactiveWhenThePresentHookIsNotLive() {
        // Even with both switches stored on, an absent present hook means no effect: the modules
        // must not drive a seam that is not there.
        PresentFramePolicy.Plan plan = PresentFramePolicy.plan(false, true, true, allOn());
        assertFalse(plan.isActive());
        assertFalse(plan.frameGenerationEnabled);
        assertFalse(plan.motionBlurEnabled);
    }

    @Test
    public void thePlanIsInactiveWhenBothModuleSwitchesAreOff() {
        PresentFramePolicy.Plan plan = PresentFramePolicy.plan(true, false, false, allOn());
        assertFalse(plan.isActive());
    }

    @Test
    public void frameGenerationClampsTheMultiplierToTheCeiling() {
        PresentFramePolicy.Plan plan = PresentFramePolicy.plan(true, true, false, new Values() {
            @Override
            public int integer(String key, int fallback) {
                if (key.equals("fg_max_multiplier")) return 99;
                if (key.equals("fg_strength")) return 50;
                return fallback;
            }

            @Override
            public boolean bool(String key, boolean fallback) {
                return key.equals("fg_enabled") || fallback;
            }
        });
        assertTrue(plan.frameGenerationEnabled);
        assertEquals(PresentFramePolicy.MAX_MULTIPLIER, plan.maxFramesPerRendered);
        assertEquals(0.5f, plan.interpolationStrength, 1e-6);
    }

    @Test
    public void theBlurBudgetIsClampedToItsFloor() {
        PresentFramePolicy.Plan plan = PresentFramePolicy.plan(true, false, true, new Values() {
            @Override
            public int integer(String key, int fallback) {
                if (key.equals("mb_budget")) return 0; // below the 10% floor
                if (key.equals("mb_strength")) return 35;
                return fallback;
            }

            @Override
            public boolean bool(String key, boolean fallback) {
                return key.equals("mb_enabled") || fallback;
            }
        });
        assertTrue(plan.motionBlurEnabled);
        assertEquals(0.10f, plan.blurBudget, 1e-6);
    }

    @Test
    public void theInterpolationStrengthIsClampedToOne() {
        PresentFramePolicy.Plan plan = PresentFramePolicy.plan(true, true, false, new Values() {
            @Override
            public int integer(String key, int fallback) {
                if (key.equals("fg_strength")) return 500; // out of the 0..100 range
                if (key.equals("fg_max_multiplier")) return 2;
                return fallback;
            }

            @Override
            public boolean bool(String key, boolean fallback) {
                return key.equals("fg_enabled") || fallback;
            }
        });
        assertEquals(1.0f, plan.interpolationStrength, 1e-6);
    }

    private static PresentFramePolicy.ValueSource allOn() {
        return new Values() {
            @Override
            public int integer(String key, int fallback) {
                return fallback;
            }

            @Override
            public boolean bool(String key, boolean fallback) {
                return true;
            }
        };
    }

    /** A value source that only overrides what each test names; everything else reads its default. */
    private abstract static class Values implements PresentFramePolicy.ValueSource {
    }
}
