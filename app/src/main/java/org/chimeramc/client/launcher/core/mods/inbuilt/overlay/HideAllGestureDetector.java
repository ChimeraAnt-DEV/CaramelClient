package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Detects a three-finger tap, the gesture that hides every HUD overlay for a screenshot.
 *
 * <p>Pure and Android-free: it is fed the raw facts of each event (the action, the pointer count,
 * the pointer that changed and its position), so the recognition rule is unit-testable without a
 * device. The activity owns the {@code MotionEvent} plumbing.
 *
 * <p>The rule is deliberately narrow. A gesture counts only when:
 *
 * <ul>
 *   <li>exactly three fingers are down at some point during the gesture,</li>
 *   <li>after all three fingers are down, no finger moves more than {@link #SLOP_PX} from the
 *       anchor, so a three-finger swipe (a game action) is not mistaken for a tap, and</li>
 *   <li>the whole gesture lasts no longer than {@link #MAX_DURATION_MS}, so a three-finger hold is
 *       not a tap.</li>
 * </ul>
 *
 * <p>A fourth finger arriving at any point cancels the gesture rather than being ignored: otherwise
 * a palm resting on the screen alongside a deliberate three-finger tap would still fire.
 */
public final class HideAllGestureDetector {

    /** The number of fingers the gesture needs. */
    public static final int FINGER_COUNT = 3;
    /** How far a finger may travel, in pixels, and still count as a tap. */
    public static final float SLOP_PX = 40f;
    /** The longest a gesture may last and still count as a tap, in milliseconds. */
    public static final long MAX_DURATION_MS = 400L;

    private boolean active;
    private boolean cancelled;
    private boolean reachedThree;
    private long startMs;
    private float anchorX;
    private float anchorY;

    /**
     * Feeds one pointer event.
     *
     * @param actionMasked   the {@code MotionEvent} action with the pointer index masked off
     * @param pointerCount   the number of pointers down in this event
     * @param x              the x of the pointer the action refers to
     * @param y              the y of the pointer the action refers to
     * @param nowMs          the event time in milliseconds
     * @return true when this event completed a three-finger tap
     */
    public boolean onEvent(int actionMasked, int pointerCount, float x, float y, long nowMs) {
        switch (actionMasked) {
            case 0: // ACTION_DOWN
                reset();
                active = true;
                startMs = nowMs;
                anchorX = x;
                anchorY = y;
                return false;
            case 5: // ACTION_POINTER_DOWN
                if (!active) return false;
                if (pointerCount > FINGER_COUNT) {
                    cancelled = true;
                    return false;
                }
                if (pointerCount == FINGER_COUNT) {
                    reachedThree = true;
                    // Re-anchor on the finger that just landed, so the movement check measures the
                    // gesture from the moment all three are down rather than from the first finger.
                    anchorX = x;
                    anchorY = y;
                }
                return false;
            case 2: // ACTION_MOVE
                // Movement is only judged once all three fingers are down: the anchor is re-set
                // when the third finger lands, so a stray slide while placing the fingers cannot
                // cancel a deliberate tap. A real swipe keeps moving afterwards and is caught here.
                if (!active || cancelled || !reachedThree) return false;
                if (Math.abs(x - anchorX) > SLOP_PX || Math.abs(y - anchorY) > SLOP_PX) {
                    cancelled = true;
                }
                return false;
            case 6: // ACTION_POINTER_UP
                if (!active || cancelled) return false;
                if (pointerCount > FINGER_COUNT) {
                    cancelled = true;
                }
                return false;
            case 1: // ACTION_UP
                boolean fired = active && !cancelled && reachedThree
                        && nowMs - startMs <= MAX_DURATION_MS;
                reset();
                return fired;
            case 3: // ACTION_CANCEL
                reset();
                return false;
            default:
                return false;
        }
    }

    /** True while a gesture is being tracked, so a caller can decide whether to hold an event. */
    public boolean isTracking() {
        return active;
    }

    /** Forgets any in-flight gesture, so a resumed activity cannot complete a stale one. */
    public void reset() {
        active = false;
        cancelled = false;
        reachedThree = false;
        startMs = 0L;
        anchorX = 0f;
        anchorY = 0f;
    }
}
