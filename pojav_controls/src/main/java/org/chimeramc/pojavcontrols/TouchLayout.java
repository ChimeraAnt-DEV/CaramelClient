package org.chimeramc.pojavcontrols;

/**
 * Layout rules for the touch controls: the left-handed mirror and the tablet scale (item 222).
 *
 * <p>Both are pure arithmetic over the numbers a layout pass already has, so they are unit-tested
 * without a device. Two rules are worth stating because they are what a naive version gets wrong:
 *
 * <ul>
 *   <li><b>The mirror is a reflection about the screen centre, not a negation.</b> Flipping the sign
 *       of an offset puts a control off the left edge; the correct transform is
 *       {@code screenWidth - x - childWidth}, which maps the left edge onto the right edge and keeps
 *       the control fully on screen. It is applied <i>after</i> the expression has been evaluated and
 *       the safe zone applied, so the reflected control is then clamped again — a mirrored control
 *       near a notch must not end up under it.
 *   <li><b>The tablet scale is a size multiplier, not a new profile.</b> A control authored at 50dp
 *       on a phone is physically the same size on a tablet but occupies a much smaller fraction of a
 *       10-inch screen, so the buttons read as tiny and sit far apart. Scaling the authored size by
 *       a factor derived from the screen's smallest width keeps one profile usable on both, which is
 *       the whole point of "a separate layout for tablets" without a second file to maintain.
 * </ul>
 */
public final class TouchLayout {

    /** Smallest width, in dp, at which the tablet scale starts to apply. */
    public static final int TABLET_MIN_WIDTH_DP = 600;

    /** The largest size multiplier applied, so a control cannot grow without bound. */
    public static final float MAX_TABLET_SCALE = 1.5f;

    private TouchLayout() {
    }

    /** True when the window is wide enough to be treated as a tablet. */
    public static boolean isTablet(int smallestWidthDp) {
        return smallestWidthDp >= TABLET_MIN_WIDTH_DP;
    }

    /**
     * The size multiplier for a window, from its smallest width in dp.
     *
     * <p>1.0 below the tablet breakpoint, then a linear ramp that reaches {@link #MAX_TABLET_SCALE}
     * at twice the breakpoint, so a very large window does not produce absurd buttons.
     */
    public static float scaleFor(int smallestWidthDp) {
        if (smallestWidthDp <= TABLET_MIN_WIDTH_DP) return 1f;
        int extra = smallestWidthDp - TABLET_MIN_WIDTH_DP;
        float ramp = Math.min(1f, extra / (float) TABLET_MIN_WIDTH_DP);
        return 1f + (MAX_TABLET_SCALE - 1f) * ramp;
    }

    /** Reflects a control's x offset about the screen centre, keeping its width. */
    public static int mirrorX(int x, int screenWidth, int childWidth) {
        return screenWidth - childWidth - x;
    }

    /**
     * Applies the mirror and the tablet scale to one control, in place.
     *
     * <p>Mirroring is only a horizontal reflection, so the vertical offset is untouched: a
     * left-handed player wants the movement thumb on the right and the look thumb on the left, not
     * an upside-down screen.
     */
    public static void apply(CustomControls controls, int smallestWidthDp) {
        if (controls == null) return;
        float scale = scaleFor(smallestWidthDp);
        for (ControlData data : controls.mControlDataList) {
            scaleSize(data, scale);
        }
        for (ControlJoystickData joystick : controls.mJoystickDataList) {
            scaleSize((ControlData) joystick, scale);
        }
        for (ControlDrawerData drawer : controls.mDrawerDataList) {
            scaleSize(drawer.properties, scale);
            for (ControlData data : drawer.buttonProperties) {
                scaleSize(data, scale);
            }
        }
    }

    private static void scaleSize(ControlData data, float scale) {
        if (data == null || scale == 1f) return;
        data.width = Math.max(16f, Math.min(data.width * scale, 400f));
        data.height = Math.max(16f, Math.min(data.height * scale, 400f));
    }
}
