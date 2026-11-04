package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The cinematic editor's motion-control arithmetic: the step size, the direction and the clamp.
 *
 * <p>Pinned so the three buttons cannot drift apart, and so a press at a bound is a no-op rather
 * than wrapping to the far end.
 */
public class ReplayCinematicControlsTest {

    @Test
    public void smoothingStepsByAFixedAmountAndClamps() {
        assertEquals(0.1f, ReplayCinematicControls.stepSmoothing(0f, 1), 0.001f);
        assertEquals(0.5f, ReplayCinematicControls.stepSmoothing(0.4f, 1), 0.001f);
        assertEquals(0.3f, ReplayCinematicControls.stepSmoothing(0.4f, -1), 0.001f);
        // A press at a bound is a no-op, not a wrap.
        assertEquals(1f, ReplayCinematicControls.stepSmoothing(1f, 1), 0.001f);
        assertEquals(0f, ReplayCinematicControls.stepSmoothing(0f, -1), 0.001f);
        // A non-finite current is treated as the floor.
        assertEquals(0.1f, ReplayCinematicControls.stepSmoothing(Float.NaN, 1), 0.001f);
    }

    @Test
    public void theFieldOfViewStepsAndClampsToTheCameraRange() {
        assertEquals(75f, ReplayCinematicControls.stepFov(70f, 1), 0.001f);
        assertEquals(65f, ReplayCinematicControls.stepFov(70f, -1), 0.001f);
        assertEquals(ReplayCamera.MAX_FOV, ReplayCinematicControls.stepFov(ReplayCamera.MAX_FOV, 1),
                0.001f);
        assertEquals(ReplayCamera.MIN_FOV, ReplayCinematicControls.stepFov(ReplayCamera.MIN_FOV, -1),
                0.001f);
        // A non-finite current starts from the default rather than a bound.
        assertEquals(ReplayCamera.DEFAULT_FOV + ReplayCinematicControls.FOV_STEP,
                ReplayCinematicControls.stepFov(Float.NaN, 1), 0.001f);
    }

    @Test
    public void theEasingCyclesAndWraps() {
        assertEquals(ReplayCameraPath.Easing.EASE_IN,
                ReplayCinematicControls.nextEasing(ReplayCameraPath.Easing.LINEAR));
        assertEquals(ReplayCameraPath.Easing.EASE_IN_OUT,
                ReplayCinematicControls.nextEasing(ReplayCameraPath.Easing.EASE_OUT));
        assertEquals("the cycle must wrap back to the first easing",
                ReplayCameraPath.Easing.LINEAR,
                ReplayCinematicControls.nextEasing(ReplayCameraPath.Easing.EASE_IN_OUT));
        assertEquals("a null current must start at the first easing",
                ReplayCameraPath.Easing.LINEAR, ReplayCinematicControls.nextEasing(null));
    }
}
