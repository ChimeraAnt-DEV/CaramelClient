package org.chimeramc.pojavcontrols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The left-handed mirror and the tablet scale (item 222).
 *
 * <p>These run without an Android runtime, which is the reason the arithmetic lives in
 * {@link TouchLayout} and not in the overlay layout pass.
 */
public class TouchLayoutTest {

    @Test
    public void phoneWidthsAreNotTablets() {
        assertFalse(TouchLayout.isTablet(320));
        assertFalse(TouchLayout.isTablet(411));
        assertFalse(TouchLayout.isTablet(TouchLayout.TABLET_MIN_WIDTH_DP - 1));
    }

    @Test
    public void theBreakpointItselfIsATablet() {
        assertTrue(TouchLayout.isTablet(TouchLayout.TABLET_MIN_WIDTH_DP));
        assertTrue(TouchLayout.isTablet(1280));
    }

    @Test
    public void scaleIsOneBelowTheBreakpoint() {
        assertEquals(1f, TouchLayout.scaleFor(320), 0.0001f);
        assertEquals(1f, TouchLayout.scaleFor(TouchLayout.TABLET_MIN_WIDTH_DP), 0.0001f);
    }

    @Test
    public void scaleRampsToTheMaximumAndStops() {
        assertEquals(TouchLayout.MAX_TABLET_SCALE, TouchLayout.scaleFor(1200), 0.0001f);
        assertEquals(TouchLayout.MAX_TABLET_SCALE, TouchLayout.scaleFor(2000), 0.0001f);
        float mid = TouchLayout.scaleFor(TouchLayout.TABLET_MIN_WIDTH_DP + 300);
        assertTrue("mid ramp must be between 1 and the max",
                mid > 1f && mid < TouchLayout.MAX_TABLET_SCALE);
    }

    @Test
    public void theMirrorReflectsAboutTheCentreAndKeepsTheWidth() {
        // A 50px control at x=10 on a 1000px screen ends at 940, the same 10px gap on the right.
        assertEquals(940, TouchLayout.mirrorX(10, 1000, 50));
    }

    @Test
    public void mirroringTwiceIsIdentity() {
        int once = TouchLayout.mirrorX(37, 1920, 120);
        assertEquals(37, TouchLayout.mirrorX(once, 1920, 120));
    }

    @Test
    public void aCentredControlDoesNotMove() {
        assertEquals(475, TouchLayout.mirrorX(475, 1000, 50));
    }

    @Test
    public void mirrorNeverPushesAControlOffScreen() {
        int x = TouchLayout.mirrorX(0, 1000, 200);
        assertEquals(800, x);
        assertTrue(x >= 0 && x + 200 <= 1000);
    }

    @Test
    public void tabletScaleGrowsEveryControlIncludingJoysticksAndDrawers() {
        CustomControls controls = CustomControls.createDefault();
        ControlData button = controls.mControlDataList.get(0);
        float beforeWidth = button.width;
        ControlJoystickData joystick = new ControlJoystickData();
        float joystickBefore = joystick.width;
        controls.mJoystickDataList.add(joystick);
        ControlDrawerData drawer = new ControlDrawerData();
        drawer.properties.width = 50;
        drawer.buttonProperties.add(new ControlData("Button", new int[]{KeyMapper.GLFW_KEY_SPACE},
                "0.5 * ${screen_width}", "0.5 * ${screen_height}", 50, 50));
        controls.mDrawerDataList.add(drawer);

        TouchLayout.apply(controls, 1200);

        assertTrue("a phone-sized control must grow on a tablet", button.width > beforeWidth);
        assertTrue(joystick.width > joystickBefore);
        assertTrue(drawer.properties.width > 50);
        assertTrue(drawer.buttonProperties.get(0).width > 50);
    }

    @Test
    public void aPhoneWidthLeavesTheProfileUntouched() {
        CustomControls controls = CustomControls.createDefault();
        ControlData button = controls.mControlDataList.get(0);
        float before = button.width;
        TouchLayout.apply(controls, 411);
        assertEquals(before, button.width, 0.0001f);
    }

    @Test
    public void aNullProfileIsRefusedRatherThanCrashing() {
        TouchLayout.apply(null, 1200);
    }

    @Test
    public void scalingIsBoundedSoAVeryWideWindowCannotProduceAbsurdButtons() {
        CustomControls controls = CustomControls.createDefault();
        ControlData button = controls.mControlDataList.get(0);
        TouchLayout.apply(controls, 4000);
        assertTrue(button.width <= 400f);
    }

    @Test
    public void mirroringAndScalingAreIndependent() {
        // The mirror must not change a control size, and a wider control mirrors differently.
        assertEquals(940, TouchLayout.mirrorX(10, 1000, 50));
        assertNotEquals(TouchLayout.mirrorX(10, 1000, 50), TouchLayout.mirrorX(10, 1000, 100));
    }
}
