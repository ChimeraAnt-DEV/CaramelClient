package org.chimeramc.client.preloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.MotionEvent;

import org.junit.Test;

/**
 * The launcher-side mouse button state machine.
 *
 * <p>Runs against the real class with real {@code MotionEvent} button masks; no mocks. The Android
 * constants used here are inlined ints, so they resolve on the unit-test classpath.
 */
public class PreloaderMouseStateTest {

    private static final int PRIMARY = MotionEvent.BUTTON_PRIMARY;
    private static final int SECONDARY = MotionEvent.BUTTON_SECONDARY;

    @Test
    public void anUnclaimedPressAndReleaseAreNotConsumed() {
        PreloaderMouseState state = new PreloaderMouseState();
        assertFalse(state.transition(PRIMARY, true, false));
        assertEquals(PRIMARY, state.buttonsDown());
        assertFalse(state.transition(PRIMARY, false, false));
        assertEquals(0, state.buttonsDown());
    }

    @Test
    public void aClaimedPressIsConsumed() {
        PreloaderMouseState state = new PreloaderMouseState();
        assertTrue(state.transition(PRIMARY, true, true));
        assertEquals(PRIMARY, state.blockedButtons());
    }

    @Test
    public void aReleaseOfAClaimedButtonStaysConsumedEvenWhenNativeReportsNoClaim() {
        PreloaderMouseState state = new PreloaderMouseState();
        state.transition(PRIMARY, true, true);
        assertTrue("the release must not leak through to the UI",
                state.transition(PRIMARY, false, false));
        assertEquals(0, state.blockedButtons());
        assertEquals(0, state.buttonsDown());
    }

    @Test
    public void aDuplicatePressRepeatsTheLastVerdict() {
        PreloaderMouseState state = new PreloaderMouseState();
        assertTrue(state.transition(PRIMARY, true, true));
        // A second press without a release in between is a duplicate, not a new press.
        assertTrue(state.transition(PRIMARY, true, false));
    }

    @Test
    public void aDuplicatePressAfterAnUnclaimedPressIsNotConsumed() {
        PreloaderMouseState state = new PreloaderMouseState();
        assertFalse(state.transition(PRIMARY, true, false));
        assertFalse(state.transition(PRIMARY, true, true));
    }

    @Test
    public void aDuplicateReleaseAfterAClaimedReleaseIsConsumed() {
        PreloaderMouseState state = new PreloaderMouseState();
        state.transition(PRIMARY, true, true);
        state.transition(PRIMARY, false, false);
        assertTrue(state.transition(PRIMARY, false, false));
    }

    @Test
    public void aSecondButtonIsTrackedIndependently() {
        PreloaderMouseState state = new PreloaderMouseState();
        state.transition(PRIMARY, true, true);
        assertFalse("a different button is its own press", state.transition(SECONDARY, true, false));
        assertEquals(PRIMARY | SECONDARY, state.buttonsDown());
        assertEquals(PRIMARY, state.blockedButtons());
    }

    @Test
    public void theDiffReportsPressedAndReleasedMasks() {
        PreloaderMouseState state = new PreloaderMouseState();
        state.transition(PRIMARY, true, false);
        int[] out = new int[2];
        state.diff(SECONDARY, out);
        assertEquals(SECONDARY, out[0]);
        assertEquals(PRIMARY, out[1]);
    }

    @Test
    public void theDiffHelpersMatchTheArrayForm() {
        PreloaderMouseState state = new PreloaderMouseState();
        state.transition(PRIMARY, true, false);
        assertEquals(state.pressedFrom(SECONDARY), SECONDARY);
        assertEquals(state.releasedFrom(SECONDARY), PRIMARY);
    }

    @Test
    public void thePointerStreamFlagIsSettableAndClearedByReset() {
        PreloaderMouseState state = new PreloaderMouseState();
        state.setBlockedPointerStream(true);
        assertTrue(state.blockedPointerStream());
        state.reset();
        assertFalse(state.blockedPointerStream());
        assertEquals(0, state.buttonsDown());
        assertEquals(0, state.blockedButtons());
    }

    @Test
    public void resetForgetsAClaimedButton() {
        PreloaderMouseState state = new PreloaderMouseState();
        state.transition(PRIMARY, true, true);
        state.reset();
        assertFalse("a claimed button must not survive a reset",
                state.transition(PRIMARY, false, false));
    }

    @Test
    public void theButtonListCoversTheStandardSevenButtons() {
        assertEquals(7, PreloaderMouseState.buttons().length);
    }

    @Test
    public void aClaimedButtonKeepsTheReleaseConsumedAcrossARePress() {
        PreloaderMouseState state = new PreloaderMouseState();
        state.transition(PRIMARY, true, true);
        state.transition(PRIMARY, false, false);
        state.transition(PRIMARY, true, false);
        assertFalse("the second press was not claimed", state.lastTransitionConsumed(PRIMARY));
    }
}
