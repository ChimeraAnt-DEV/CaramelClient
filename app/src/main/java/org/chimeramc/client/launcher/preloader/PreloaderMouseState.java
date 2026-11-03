package org.chimeramc.client.preloader;

import android.view.MotionEvent;

/**
 * The launcher-side mouse button state machine.
 *
 * <p>Split out of {@code PreloaderInput} so the transition rules are testable without a native
 * library and so the input facade is one concern lighter. The native declarations must stay on
 * {@code PreloaderInput} — a JNI symbol is resolved from the declaring class, so moving a
 * {@code native} method would break the binding.
 *
 * <p>Why the state machine exists at all: the preloader may claim a mouse button, and once it has,
 * the launcher must keep treating that button's release and the pointer stream that follows as
 * consumed, even though the native call no longer reports consumption. Otherwise a click the game
 * handled would also be delivered as a UI tap. The rules are:
 *
 * <ul>
 *   <li>a transition that matches the tracked state is a duplicate: it is consumed only if the last
 *       real transition for that button was,</li>
 *   <li>a press the native side consumed marks the button blocked, and a release stays consumed
 *       while the button was blocked,</li>
 *   <li>the pointer stream stays blocked from a consumed press until every button is up, so a drag
 *       that began as a claimed click is not delivered as a hover or move.</li>
 * </ul>
 *
 * <p>Every method takes the native consumption as a parameter, so the class is pure: no allocation,
 * no locking beyond the caller's, and no {@code MotionEvent} needed to exercise it.
 */
public final class PreloaderMouseState {

    private static final int[] MOUSE_BUTTONS = {
            MotionEvent.BUTTON_PRIMARY,
            MotionEvent.BUTTON_SECONDARY,
            MotionEvent.BUTTON_TERTIARY,
            MotionEvent.BUTTON_BACK,
            MotionEvent.BUTTON_FORWARD,
            MotionEvent.BUTTON_STYLUS_PRIMARY,
            MotionEvent.BUTTON_STYLUS_SECONDARY
    };

    private int buttonsDown;
    private int blockedButtons;
    private int lastConsumedTransitions;
    private boolean blockedPointerStream;

    /** Forgets every button, so a resumed activity cannot inherit a stale press. */
    public void reset() {
        buttonsDown = 0;
        blockedButtons = 0;
        lastConsumedTransitions = 0;
        blockedPointerStream = false;
    }

    /**
     * Applies one button transition.
     *
     * @param button  the button mask
     * @param isDown  true for a press, false for a release
     * @param consumed whether the native side claimed this transition
     * @return whether the transition should be treated as consumed by the caller
     */
    public boolean transition(int button, boolean isDown, boolean consumed) {
        boolean alreadyDown = (buttonsDown & button) != 0;
        if (alreadyDown == isDown) {
            return (lastConsumedTransitions & button) != 0;
        }

        if (isDown) {
            buttonsDown |= button;
            if (consumed) {
                blockedButtons |= button;
                lastConsumedTransitions |= button;
            } else {
                blockedButtons &= ~button;
                lastConsumedTransitions &= ~button;
            }
            return consumed;
        }

        boolean effectiveConsumed = consumed || (blockedButtons & button) != 0;
        buttonsDown &= ~button;
        blockedButtons &= ~button;
        if (effectiveConsumed) {
            lastConsumedTransitions |= button;
        } else {
            lastConsumedTransitions &= ~button;
        }
        return effectiveConsumed;
    }

    /**
     * The buttons that changed between the tracked state and {@code buttonState}, as two masks.
     *
     * <p>Returned through a caller-owned array so the hot path allocates nothing: index 0 is the
     * pressed mask, index 1 the released mask.
     */
    public void diff(int buttonState, int[] out) {
        out[0] = buttonState & ~buttonsDown;
        out[1] = buttonsDown & ~buttonState;
    }

    /** The mask of buttons currently held. */
    public int buttonsDown() {
        return buttonsDown;
    }

    /** The mask of buttons the native side claimed and whose release must stay consumed. */
    public int blockedButtons() {
        return blockedButtons;
    }

    /** True while the pointer stream is being swallowed after a claimed press. */
    public boolean blockedPointerStream() {
        return blockedPointerStream;
    }

    /** Whether the last real transition for {@code button} was consumed. */
    public boolean lastTransitionConsumed(int button) {
        return (lastConsumedTransitions & button) != 0;
    }

    public void setBlockedPointerStream(boolean blocked) {
        blockedPointerStream = blocked;
    }

    /** The buttons the dispatcher iterates; exposed so the caller does not keep a second copy. */
    public static int[] buttons() {
        return MOUSE_BUTTONS;
    }

    /** The press mask computed by the last {@link #diff} call, for a caller holding no array. */
    public int pressedFrom(int buttonState) {
        return buttonState & ~buttonsDown;
    }

    /** The release mask computed for {@code buttonState}. */
    public int releasedFrom(int buttonState) {
        return buttonsDown & ~buttonState;
    }
}
