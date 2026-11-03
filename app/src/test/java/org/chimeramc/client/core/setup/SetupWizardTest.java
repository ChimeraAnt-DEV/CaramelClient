package org.chimeramc.client.core.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.launcher.core.setup.SetupWizard;
import org.junit.Test;

/**
 * The five-step setup wizard's rules, pinned without a device.
 *
 * <p>The ordering, the checkbox-gated Next, and the skip rules are what the whole flow rests on, so
 * they are asserted here rather than left to the activity.
 */
public class SetupWizardTest {

    @Test
    public void theWizardHasExactlyFiveShownStepsInOrder() {
        assertEquals(5, SetupWizard.STEPS.length);
        assertEquals(SetupWizard.Step.LICENSE, SetupWizard.STEPS[0]);
        assertEquals(SetupWizard.Step.INSTALL_INSTANCE, SetupWizard.STEPS[1]);
        assertEquals(SetupWizard.Step.SIGN_IN, SetupWizard.STEPS[2]);
        assertEquals(SetupWizard.Step.CUSTOMIZE, SetupWizard.STEPS[3]);
        assertEquals(SetupWizard.Step.CHANGELOG_DISCORD, SetupWizard.STEPS[4]);
    }

    @Test
    public void nextWalksTheStepsAndEndsAtReady() {
        SetupWizard.Step s = SetupWizard.Step.LICENSE;
        for (int i = 0; i < 5; i++) {
            assertEquals(i, SetupWizard.index(s));
            s = SetupWizard.next(s);
        }
        assertEquals(SetupWizard.Step.READY, s);
        assertTrue(SetupWizard.isComplete(s));
        // Past the end it stays ready rather than looping.
        assertEquals(SetupWizard.Step.READY, SetupWizard.next(SetupWizard.Step.READY));
    }

    @Test
    public void previousWalksBackAndClampsAtTheStart() {
        assertEquals(SetupWizard.Step.LICENSE, SetupWizard.previous(SetupWizard.Step.LICENSE));
        assertEquals(SetupWizard.Step.LICENSE, SetupWizard.previous(SetupWizard.Step.INSTALL_INSTANCE));
        assertEquals(SetupWizard.Step.CUSTOMIZE, SetupWizard.previous(SetupWizard.Step.CHANGELOG_DISCORD));
    }

    @Test
    public void nextIsBlockedOnTheLicenceUntilItIsAccepted() {
        SetupWizard.Facts facts = new SetupWizard.Facts();
        assertFalse("Next must be off until the licence is accepted",
                SetupWizard.canProceed(SetupWizard.Step.LICENSE, facts));
        facts.licenseAccepted(true);
        assertTrue("accepting the licence lights Next up",
                SetupWizard.canProceed(SetupWizard.Step.LICENSE, facts));
        // Every later step is always leavable.
        assertTrue(SetupWizard.canProceed(SetupWizard.Step.INSTALL_INSTANCE, facts));
        assertTrue(SetupWizard.canProceed(SetupWizard.Step.SIGN_IN, facts));
    }

    @Test
    public void onlyTheLicenceCannotBeSkipped() {
        assertFalse(SetupWizard.canSkip(SetupWizard.Step.LICENSE));
        assertTrue(SetupWizard.canSkip(SetupWizard.Step.INSTALL_INSTANCE));
        assertTrue(SetupWizard.canSkip(SetupWizard.Step.SIGN_IN));
        assertTrue(SetupWizard.canSkip(SetupWizard.Step.CUSTOMIZE));
        assertTrue(SetupWizard.canSkip(SetupWizard.Step.CHANGELOG_DISCORD));
        assertFalse(SetupWizard.canSkip(SetupWizard.Step.READY));
    }

    @Test
    public void aFreshWizardStartsAtTheLicence() {
        assertEquals(0, SetupWizard.index(SetupWizard.Step.LICENSE));
        assertFalse(SetupWizard.isComplete(SetupWizard.Step.LICENSE));
        assertFalse(SetupWizard.canProceed(SetupWizard.Step.LICENSE, new SetupWizard.Facts()));
    }
}
