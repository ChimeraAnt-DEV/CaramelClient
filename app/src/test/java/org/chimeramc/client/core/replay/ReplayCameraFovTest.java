package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The cinematic rig's missing controls: field of view, Bezier easing and path smoothing.
 *
 * <p>The spec asks the timeline to "set the FOV" and "add smooth interpolation (Bezier curves)" and
 * a "path smoothing" control. The path had presets and Catmull-Rom but no FOV and no easing, so a
 * shot could not zoom and every segment was timed the same. These pin the three rules.
 */
public class ReplayCameraFovTest {

    /** A straight three-keyframe path: x runs 0..20, so a sample's x is its progress. */
    private static ReplayCameraPath straightPath() {
        ReplayCameraPath path = new ReplayCameraPath();
        path.addKeyframe(new ReplayCameraPath.Keyframe(0L, 0f, 0f, 0f, 0f, 0f));
        path.addKeyframe(new ReplayCameraPath.Keyframe(1000L, 10f, 0f, 0f, 0f, 0f));
        path.addKeyframe(new ReplayCameraPath.Keyframe(2000L, 20f, 0f, 0f, 0f, 0f));
        return path;
    }

    @Test
    public void theCameraHasADefaultFieldOfView() {
        ReplayCamera camera = new ReplayCamera();
        assertEquals(ReplayCamera.DEFAULT_FOV, camera.fovDeg(), 0.001f);
    }

    @Test
    public void theFieldOfViewIsClampedToAVisibleRange() {
        ReplayCamera camera = new ReplayCamera();
        camera.setFov(5f);
        assertEquals("a field of view below the floor must clamp up",
                ReplayCamera.MIN_FOV, camera.fovDeg(), 0.001f);
        camera.setFov(500f);
        assertEquals("a field of view above the ceiling must clamp down",
                ReplayCamera.MAX_FOV, camera.fovDeg(), 0.001f);
        // A non-finite value is ignored, so a bad read keeps the last good field of view rather
        // than pinning the camera to a bound.
        camera.setFov(70f);
        camera.setFov(Float.NaN);
        assertEquals("a non-finite field of view must be ignored",
                70f, camera.fovDeg(), 0.001f);
        camera.setFov(Float.POSITIVE_INFINITY);
        assertEquals("an infinite field of view must be ignored",
                70f, camera.fovDeg(), 0.001f);
    }

    @Test
    public void aKeyframeCanCarryItsOwnFieldOfView() {
        ReplayCameraPath.Keyframe k = new ReplayCameraPath.Keyframe(0L, 1f, 2f, 3f, 0f, 0f, 95f);
        assertEquals(95f, k.fovDeg, 0.001f);
        // A keyframe built without an explicit FOV uses the camera default, so existing callers
        // keep working.
        ReplayCameraPath.Keyframe plain = new ReplayCameraPath.Keyframe(0L, 1f, 2f, 3f, 0f, 0f);
        assertEquals(ReplayCamera.DEFAULT_FOV, plain.fovDeg, 0.001f);
    }

    @Test
    public void samplingInterpolatesTheFieldOfView() {
        ReplayCameraPath path = new ReplayCameraPath();
        path.addKeyframe(new ReplayCameraPath.Keyframe(0L, 0f, 0f, 0f, 0f, 0f, 60f));
        path.addKeyframe(new ReplayCameraPath.Keyframe(1000L, 0f, 0f, 0f, 0f, 0f, 90f));
        ReplayCamera out = new ReplayCamera();
        path.sample(500L, out);
        assertEquals("the field of view must interpolate between the keyframes",
                75f, out.fovDeg(), 0.001f);
    }

    @Test
    public void theEasingReshapesTheTimingAlongASegment() {
        ReplayCameraPath path = straightPath();
        ReplayCamera out = new ReplayCamera();

        path.setEasing(ReplayCameraPath.Easing.LINEAR);
        path.sample(250L, out);
        float linear = out.x();

        path.setEasing(ReplayCameraPath.Easing.EASE_IN);
        path.sample(250L, out);
        float easeIn = out.x();
        assertTrue("an ease-in must lag the linear sample at a quarter of the segment: linear="
                + linear + " easeIn=" + easeIn, easeIn < linear);

        path.setEasing(ReplayCameraPath.Easing.EASE_OUT);
        path.sample(250L, out);
        float easeOut = out.x();
        assertTrue("an ease-out must lead the linear sample at a quarter of the segment: linear="
                + linear + " easeOut=" + easeOut, easeOut > linear);

        // The easing is symmetric, so the midpoint is unchanged.
        path.setEasing(ReplayCameraPath.Easing.EASE_IN_OUT);
        path.sample(500L, out);
        path.setEasing(ReplayCameraPath.Easing.LINEAR);
        ReplayCamera linearOut = new ReplayCamera();
        path.sample(500L, linearOut);
        assertEquals("a symmetric ease must not move the midpoint",
                linearOut.x(), out.x(), 0.5f);
    }

    @Test
    public void theEasingCurvesAreBezierShapedAndBounded() {
        for (ReplayCameraPath.Easing easing : ReplayCameraPath.Easing.values()) {
            assertEquals("every easing must start at 0", 0f,
                    ReplayCameraPath.ease(easing, 0f), 0.001f);
            assertEquals("every easing must end at 1", 1f,
                    ReplayCameraPath.ease(easing, 1f), 0.001f);
            // Monotonic in between: a non-monotonic ease would make the camera stutter backwards.
            float prev = -1f;
            for (int i = 0; i <= 20; i++) {
                float v = ReplayCameraPath.ease(easing, i / 20f);
                assertTrue(easing + " must not go backwards", v >= prev - 0.0001f);
                prev = v;
            }
        }
    }

    @Test
    public void pathSmoothingBlendsTheCurveWithTheStraightLine() {
        // A middle keyframe off the line, so a curved segment and a straight one differ.
        ReplayCameraPath path = new ReplayCameraPath();
        path.addKeyframe(new ReplayCameraPath.Keyframe(0L, 0f, 0f, 0f, 0f, 0f));
        path.addKeyframe(new ReplayCameraPath.Keyframe(1000L, 5f, 5f, 0f, 0f, 0f));
        path.addKeyframe(new ReplayCameraPath.Keyframe(2000L, 10f, 0f, 0f, 0f, 0f));
        ReplayCamera out = new ReplayCamera();

        path.setSmoothing(0f);
        path.sample(500L, out);
        float straight = out.y();
        assertEquals("smoothing 0 must follow the straight line between keyframes",
                2.5f, straight, 0.01f);

        path.setSmoothing(1f);
        path.sample(500L, out);
        float curved = out.y();
        assertNotEquals("smoothing 1 must curve away from the straight line", straight, curved, 0.05f);

        path.setSmoothing(0.5f);
        path.sample(500L, out);
        float half = out.y();
        boolean between = (half > Math.min(straight, curved) && half < Math.max(straight, curved));
        assertTrue("a half smoothing must sit between the straight and the curved sample: "
                + straight + " / " + half + " / " + curved, between);
    }

    @Test
    public void theEasingAndFovSurviveTheSidecarRoundTrip() {
        ReplayCameraPath path = straightPath();
        path.setEasing(ReplayCameraPath.Easing.EASE_IN_OUT);
        path.setSmoothing(0.4f);
        path.clear();
        path.addKeyframe(new ReplayCameraPath.Keyframe(0L, 0f, 0f, 0f, 0f, 0f, 55f));
        path.addKeyframe(new ReplayCameraPath.Keyframe(1000L, 0f, 0f, 0f, 0f, 0f, 100f));

        ReplayCameraPath back = ReplayCameraPathCodec.parse(ReplayCameraPathCodec.serialize(path));
        assertEquals(ReplayCameraPath.Easing.EASE_IN_OUT, back.easing());
        assertEquals(0.4f, back.smoothing(), 0.001f);
        assertEquals(2, back.size());
        assertEquals(55f, back.keyframes().get(0).fovDeg, 0.001f);
        assertEquals(100f, back.keyframes().get(1).fovDeg, 0.001f);
    }

    @Test
    public void anOldPathWithoutFovOrEasingStillParses() {
        // A path saved before this change has six fields per keyframe and no easing segment.
        ReplayCameraPath old = ReplayCameraPathCodec.parse(
                "ORBIT;0,1,2,3,4,5;1000,6,7,8,9,10");
        assertEquals(ReplayCameraPath.Preset.ORBIT, old.preset());
        assertEquals(2, old.size());
        assertEquals(ReplayCameraPath.Easing.LINEAR, old.easing());
        assertEquals(ReplayCamera.DEFAULT_FOV, old.keyframes().get(0).fovDeg, 0.001f);
    }
}
