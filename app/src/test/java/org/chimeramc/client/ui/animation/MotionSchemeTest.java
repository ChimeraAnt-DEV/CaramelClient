package org.chimeramc.client.ui.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.ui.components.UiTokens;
import org.junit.Test;

/**
 * The motion scheme is actually wired (U6).
 *
 * <p>{@code UiTokens.Motion} and {@code UiTokens.motionScheme} were declared but had no consumer:
 * the tokens were correct and nothing read them, so "Material 3 Expressive" was a description
 * rather than behaviour. This test pins the wiring itself — that the animation entry points select
 * and then use the scheme — because a dead token cannot fail a test that only checks the token.
 *
 * <p>Pure: it never constructs an Android view, so it runs as a plain JVM test.
 */
public class MotionSchemeTest {

    @Test
    public void expressiveIsTheDefault() {
        DynamicAnim.enableAnimations();
        assertSame(UiTokens.Motion.EXPRESSIVE, DynamicAnim.motionScheme());
        assertTrue(DynamicAnim.areAnimationsEnabled());
    }

    @Test
    public void reduceMotionSelectsTheFlatSchemeAndDisablesMotion() {
        DynamicAnim.applyMotionScheme(true, false);
        assertSame(UiTokens.Motion.REDUCED, DynamicAnim.motionScheme());
        assertFalse("reduced motion must report animations as disabled",
                DynamicAnim.areAnimationsEnabled());
    }

    /**
     * The platform setting has to win. A user who told Android to remove animations must not get
     * springs back because the app's own switch happens to be on.
     */
    @Test
    public void thePlatformSettingWinsOverTheAppSwitch() {
        DynamicAnim.applyMotionScheme(false, true);
        assertSame(UiTokens.Motion.REDUCED, DynamicAnim.motionScheme());
        assertFalse(DynamicAnim.areAnimationsEnabled());

        DynamicAnim.applyMotionScheme(false, false);
        assertSame(UiTokens.Motion.EXPRESSIVE, DynamicAnim.motionScheme());
    }

    @Test
    public void theLegacyEntryPointsKeepTheSchemeInStep() {
        DynamicAnim.disableAnimations();
        assertSame(UiTokens.Motion.REDUCED, DynamicAnim.motionScheme());
        assertFalse(DynamicAnim.areAnimationsEnabled());

        DynamicAnim.enableAnimations();
        assertSame(UiTokens.Motion.EXPRESSIVE, DynamicAnim.motionScheme());
        assertTrue(DynamicAnim.areAnimationsEnabled());
    }

    @Test
    public void aTransitionDurationFollowsTheScheme() {
        DynamicAnim.setGlobalSpeedMultiplier(1f);
        DynamicAnim.applyMotionScheme(false, false);
        assertEquals("expressive keeps the surface's own duration", 260L,
                DynamicAnim.motionDurationMs(260L));

        DynamicAnim.applyMotionScheme(true, false);
        assertEquals("reduced motion collapses to the scheme's short duration",
                UiTokens.Motion.REDUCED.durationMs, DynamicAnim.motionDurationMs(260L));
    }

    @Test
    public void theSpeedMultiplierStillAppliesToExpressiveMotion() {
        DynamicAnim.applyMotionScheme(false, false);
        DynamicAnim.setGlobalSpeedMultiplier(2f);
        assertEquals(130L, DynamicAnim.motionDurationMs(260L));
        DynamicAnim.setGlobalSpeedMultiplier(1f);
    }

    /**
     * The three durations are a closed scale (item 121).
     *
     * <p>They exist so a decorative transition names its intent instead of a literal, and the
     * ordering is what makes them usable as a scale: a "fast" step that is not the shortest, or a
     * "slow" step that is not the longest, would silently invert the meaning of the name at the
     * call site.
     */
    @Test
    public void theThreeDurationsAreAnOrderedScale() {
        assertTrue("fast must be the shortest step",
                UiTokens.MOTION_FAST_MS < UiTokens.MOTION_STANDARD_MS);
        assertTrue("slow must be the longest step",
                UiTokens.MOTION_STANDARD_MS < UiTokens.MOTION_SLOW_MS);
        assertTrue("a step must never be zero, which would make a transition an instant pop",
                UiTokens.MOTION_FAST_MS > 0);
    }

    /**
     * The spring presets are the ratios the class already animated on (item 121).
     *
     * <p>They replaced raw {@code SpringForce.DAMPING_RATIO_*} picks, so the numbers have to be the
     * same ones or every press, move and arrival in the launcher changes feel in one commit. The
     * values are pinned against the platform constants they name.
     */
    @Test
    public void theSpringPresetsMatchThePlatformConstantsTheyName() {
        assertEquals("a press must not wobble",
                androidx.dynamicanimation.animation.SpringForce.DAMPING_RATIO_NO_BOUNCY,
                UiTokens.Springs.PRESS_DAMPING, 0.0001f);
        assertEquals("a press must be immediate",
                androidx.dynamicanimation.animation.SpringForce.STIFFNESS_HIGH,
                UiTokens.Springs.PRESS_STIFFNESS, 0.0001f);
        assertEquals("a move may bounce a little",
                androidx.dynamicanimation.animation.SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY,
                UiTokens.Springs.MOVE_DAMPING, 0.0001f);
        assertTrue("an arrival settles rather than snapping, but must still be a spring",
                UiTokens.Springs.ARRIVE_DAMPING > 0f && UiTokens.Springs.ARRIVE_DAMPING < 1f);
    }

    /** Reduced motion must never be longer than expressive motion, or the switch makes it slower. */
    @Test
    public void reducedMotionIsNeverTheLongerTransition() {
        assertTrue(UiTokens.Motion.REDUCED.durationMs <= UiTokens.Motion.EXPRESSIVE.durationMs);
        assertTrue("a reduced spring must not overshoot", UiTokens.Motion.REDUCED.damping >= 1f);
    }

    // ---------------------------------------------------------------- platform animation scale (75)

    /**
     * The platform setting is a multiplier, not a boolean. Reading only its zero case (the previous
     * behaviour) meant a user who asked for 0.5x or 2x system animations still got the app's own
     * timing, which is the bug item 75 names.
     */
    @Test
    public void aFasterPlatformScaleShortensEveryDuration() {
        DynamicAnim.setGlobalSpeedMultiplier(1f);
        DynamicAnim.applyMotionScheme(false, 0.5f);
        assertSame(UiTokens.Motion.EXPRESSIVE, DynamicAnim.motionScheme());
        assertEquals("0.5x system scale halves the duration", 130L,
                DynamicAnim.motionDurationMs(260L));
        assertEquals(0.5f, DynamicAnim.platformAnimationScale(), 0.0001f);
    }

    @Test
    public void aSlowerPlatformScaleLengthensEveryDuration() {
        DynamicAnim.setGlobalSpeedMultiplier(1f);
        DynamicAnim.applyMotionScheme(false, 2f);
        assertEquals("2x system scale doubles the duration", 520L,
                DynamicAnim.motionDurationMs(260L));
    }

    /** A scale of zero is the platform's "no animation", so motion must switch off entirely. */
    @Test
    public void aZeroPlatformScaleDisablesMotion() {
        DynamicAnim.applyMotionScheme(false, 0f);
        assertSame(UiTokens.Motion.REDUCED, DynamicAnim.motionScheme());
        assertFalse(DynamicAnim.areAnimationsEnabled());
    }

    /** The boolean entry point still works and maps "off" to a zero scale. */
    @Test
    public void theBooleanEntryPointMapsToTheEquivalentScale() {
        DynamicAnim.applyMotionScheme(false, false);
        assertEquals(1f, DynamicAnim.platformAnimationScale(), 0.0001f);
        assertTrue(DynamicAnim.areAnimationsEnabled());

        DynamicAnim.applyMotionScheme(false, true);
        assertEquals(0f, DynamicAnim.platformAnimationScale(), 0.0001f);
        assertFalse(DynamicAnim.areAnimationsEnabled());
    }

    /** A bad read must not freeze or infinitely stretch an animation. */
    @Test
    public void anUnreadableScaleFallsBackToTheDefault() {
        DynamicAnim.applyMotionScheme(false, Float.NaN);
        assertEquals(1f, DynamicAnim.platformAnimationScale(), 0.0001f);
        assertEquals(260L, DynamicAnim.motionDurationMs(260L));

        DynamicAnim.applyMotionScheme(false, -3f);
        assertEquals(1f, DynamicAnim.platformAnimationScale(), 0.0001f);
    }

    /**
     * The two multipliers compose rather than one overwriting the other. They have opposite senses -
     * the app's value is a speed (larger is shorter) and Android's is a duration factor (larger is
     * longer) - so a 2x app speed on a 0.5x system scale is 4x faster overall.
     */
    @Test
    public void theAppSpeedAndThePlatformScaleCompose() {
        DynamicAnim.setGlobalSpeedMultiplier(2f);
        DynamicAnim.applyMotionScheme(false, 0.5f);
        assertEquals("2x app speed on a 0.5x system scale is 4x", 65L,
                DynamicAnim.motionDurationMs(260L));
        DynamicAnim.setGlobalSpeedMultiplier(1f);
    }
}
