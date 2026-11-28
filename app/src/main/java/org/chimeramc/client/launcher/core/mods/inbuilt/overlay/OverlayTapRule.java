package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * When a touch on a floating overlay counts as a tap that runs its action.
 *
 * <p>The rule used to be "finger up within 200 ms", which silently swallowed the most common way a
 * person presses a small floating button: a deliberate, slightly slow tap. Nothing happened — the
 * gesture was neither a tap nor a drag — so the in-game menu button looked dead. A tap is now
 * decided by travel, not by speed: if the finger did not move, the press is a tap however long it
 * took, up to the point where it is clearly a hold.
 */
public final class OverlayTapRule {

    /** Finger travel, in raw pixels (the same units the drag code compares), above which it is a drag. */
    public static final float MOVE_SLOP_PX = 10f;

    /** Beyond this a stationary press is a hold, not a tap. */
    public static final long MAX_TAP_MS = 1200;

    private OverlayTapRule() {
    }

    /**
     * @param dx            total horizontal travel since the press, in pixels
     * @param dy            total vertical travel since the press, in pixels
     * @param elapsedMs     how long the finger was down
     * @param hudEditorMode true while the HUD editor owns the overlay, where a press selects and
     *                      resizes instead of acting
     */
    public static boolean isTap(float dx, float dy, long elapsedMs, boolean hudEditorMode) {
        if (hudEditorMode) return false;
        if (elapsedMs < 0 || elapsedMs > MAX_TAP_MS) return false;
        return Math.abs(dx) <= MOVE_SLOP_PX && Math.abs(dy) <= MOVE_SLOP_PX;
    }
}
