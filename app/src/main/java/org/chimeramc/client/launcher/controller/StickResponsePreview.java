package org.chimeramc.client.launcher.controller;

/**
 * The live raw-vs-filtered stick reading shown while a dead zone is edited (item 220).
 *
 * <p>A dead-zone slider on its own tells the player a number, not what it does. This holds both
 * points — where the stick physically is and what the profile makes of it — so a view can draw them
 * together and the effect of a drag is visible immediately.
 *
 * <p>Pure and Android-free: the maths is the same {@link ControllerResponse#adjustStickPair} the
 * gameplay path runs, so the picture cannot disagree with the game. It holds no reference to the
 * response, so swapping the profile mid-drag is a single assignment.
 */
public final class StickResponsePreview {

    /** The stick being previewed. Left and right can carry different dead zones. */
    public enum Side {
        LEFT,
        RIGHT
    }

    private final float[] scratch = new float[2];

    private ControllerResponse response;
    private Side side = Side.LEFT;
    private float rawX;
    private float rawY;
    private float filteredX;
    private float filteredY;
    private boolean outsideDeadZone;

    /** Installs the response to preview; null clears the reading to the centre. */
    public void setResponse(ControllerResponse response) {
        this.response = response;
        if (response == null) {
            clear();
        } else {
            recompute();
        }
    }

    /** The response currently previewed, or null. */
    public ControllerResponse getResponse() {
        return response;
    }

    /** Selects which stick's dead zone and curve the reading uses. */
    public void setSide(Side side) {
        this.side = side == null ? Side.LEFT : side;
        recompute();
    }

    public Side getSide() {
        return side;
    }

    /**
     * Feeds a new raw stick position.
     *
     * <p>The pair is shaped together, not per axis, because the anti-drift filter is radial: a
     * per-axis reading would show a resting stick on one axis as centred while the game still sees a
     * constant diagonal push.
     */
    public void setRaw(float x, float y) {
        if (Float.isNaN(x) || Float.isInfinite(x)) x = 0f;
        if (Float.isNaN(y) || Float.isInfinite(y)) y = 0f;
        rawX = clampUnit(x);
        rawY = clampUnit(y);
        recompute();
    }

    /** Clears the reading back to the centre, for when the stick is released or the view hides. */
    public void clear() {
        rawX = 0f;
        rawY = 0f;
        filteredX = 0f;
        filteredY = 0f;
        outsideDeadZone = false;
    }

    public float rawX() {
        return rawX;
    }

    public float rawY() {
        return rawY;
    }

    public float filteredX() {
        return filteredX;
    }

    public float filteredY() {
        return filteredY;
    }

    /** True when the raw reading is far enough from centre to produce output. */
    public boolean isOutsideDeadZone() {
        return outsideDeadZone;
    }

    /** The dead-zone radius the current side uses, as a fraction of full deflection. */
    public float deadZoneRadius() {
        return response == null ? 0f : response.driftThreshold(side == Side.LEFT);
    }

    /** The raw deflection as a fraction of full travel. */
    public float rawMagnitude() {
        return (float) Math.sqrt(rawX * rawX + rawY * rawY);
    }

    /** The filtered deflection as a fraction of full output. */
    public float filteredMagnitude() {
        return (float) Math.sqrt(filteredX * filteredX + filteredY * filteredY);
    }

    private void recompute() {
        if (response == null) {
            filteredX = 0f;
            filteredY = 0f;
            outsideDeadZone = false;
            return;
        }
        boolean left = side == Side.LEFT;
        // adjustStickPair's return value reports "either axis changed", which is true for a
        // reading the dead zone flattens. The state the view needs is the radial threshold test.
        outsideDeadZone = response.isPairOutsideThreshold(left, rawX, rawY);
        response.adjustStickPair(left, rawX, rawY, scratch);
        filteredX = scratch[0];
        filteredY = scratch[1];
    }

    private static float clampUnit(float value) {
        if (value > 1f) return 1f;
        if (value < -1f) return -1f;
        return value;
    }
}
