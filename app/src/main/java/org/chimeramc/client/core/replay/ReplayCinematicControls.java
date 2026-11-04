package org.chimeramc.client.core.replay;

/**
 * The stepping rules behind the cinematic editor's motion controls.
 *
 * <p>The dialog's easing / smoothing / field-of-view buttons each move a value by a fixed step and
 * clamp it. Keeping that arithmetic here, pure, means the step size, the direction and the clamp are
 * unit-tested rather than living in three click handlers that can drift apart, and the dialog only
 * wires a button to a call.
 */
public final class ReplayCinematicControls {

    /** The smoothing step per press, as a fraction of the 0..1 range. */
    public static final float SMOOTHING_STEP = 0.1f;
    /** The field-of-view step per press, in degrees. */
    public static final float FOV_STEP = 5f;

    private ReplayCinematicControls() {
    }

    /**
     * Steps the path smoothing by one press.
     *
     * @param current   the current smoothing, 0..1
     * @param direction +1 for smoother (more curve), -1 for straighter
     * @return the new smoothing, clamped to 0..1
     */
    public static float stepSmoothing(float current, int direction) {
        float next = clamp01(current) + SMOOTHING_STEP * Integer.signum(direction);
        return clamp01(next);
    }

    /**
     * Steps the field of view by one press.
     *
     * @param current   the current field of view in degrees
     * @param direction +1 for a wider view, -1 for a narrower (longer-lens) view
     * @return the new field of view, clamped to the camera's visible range
     */
    public static float stepFov(float current, int direction) {
        float base = Float.isNaN(current) || Float.isInfinite(current)
                ? ReplayCamera.DEFAULT_FOV : current;
        float next = base + FOV_STEP * Integer.signum(direction);
        return Math.max(ReplayCamera.MIN_FOV, Math.min(ReplayCamera.MAX_FOV, next));
    }

    /** The next easing in the cycle, wrapping; a null current starts at the first. */
    public static ReplayCameraPath.Easing nextEasing(ReplayCameraPath.Easing current) {
        ReplayCameraPath.Easing[] all = ReplayCameraPath.Easing.values();
        if (current == null) return all[0];
        int index = current.ordinal();
        return all[(index + 1) % all.length];
    }

    private static float clamp01(float v) {
        if (Float.isNaN(v)) return 0f;
        if (v < 0f) return 0f;
        return v > 1f ? 1f : v;
    }
}
