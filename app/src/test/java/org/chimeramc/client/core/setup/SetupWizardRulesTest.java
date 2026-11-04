package org.chimeramc.client.core.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.launcher.core.setup.SetupWizard;
import org.junit.Test;

/**
 * The setup wizard's step rules: the import limit, the sign-in gate and the visible-step list.
 *
 * <p>Pinned pure, because these are the rules a build machine cannot exercise by walking the flow.
 */
public class SetupWizardRulesTest {

    @Test
    public void theImportLimitIsFive() {
        assertEquals(5, SetupWizard.IMPORT_LIMIT);
        for (int i = 0; i < SetupWizard.IMPORT_LIMIT; i++) {
            assertTrue("import " + (i + 1) + " must be allowed", SetupWizard.canImportMore(i));
        }
        assertFalse("the sixth import must be refused",
                SetupWizard.canImportMore(SetupWizard.IMPORT_LIMIT));
    }

    @Test
    public void theSignInStepIsHiddenWhenTheFeatureIsOff() {
        assertTrue("sign-in must be shown when the feature is on",
                SetupWizard.signInAvailable(true));
        assertFalse("sign-in must be hidden when the feature is off",
                SetupWizard.signInAvailable(false));

        // With the feature off, the visible list drops the sign-in step and the count follows.
        assertEquals(SetupWizard.STEPS.length, SetupWizard.visibleSteps(true).length);
        assertEquals(SetupWizard.STEPS.length - 1, SetupWizard.visibleSteps(false).length);
        for (SetupWizard.Step s : SetupWizard.visibleSteps(false)) {
            assertFalse("the sign-in step must not appear", s == SetupWizard.Step.SIGN_IN);
        }
    }

    @Test
    public void theVisibleIndexSkipsTheHiddenStep() {
        // With sign-in on, CUSTOMIZE is index 3 of 5; with it off, index 2 of 4.
        assertEquals(3, SetupWizard.visibleIndex(SetupWizard.Step.CUSTOMIZE, true));
        assertEquals(2, SetupWizard.visibleIndex(SetupWizard.Step.CUSTOMIZE, false));
        assertEquals("a hidden step has no visible index",
                -1, SetupWizard.visibleIndex(SetupWizard.Step.SIGN_IN, false));
    }

    @Test
    public void nextVisibleSkipsTheSignInStepWhenItIsOff() {
        // With sign-in off, Next from INSTALL_INSTANCE lands on CUSTOMIZE, not the dead-end sign-in.
        assertEquals(SetupWizard.Step.CUSTOMIZE,
                SetupWizard.nextVisible(SetupWizard.Step.INSTALL_INSTANCE, false));
        // With it on, the ordinary order is kept.
        assertEquals(SetupWizard.Step.SIGN_IN,
                SetupWizard.nextVisible(SetupWizard.Step.INSTALL_INSTANCE, true));
    }

    @Test
    public void previousVisibleSkipsTheSignInStepWhenItIsOff() {
        assertEquals(SetupWizard.Step.INSTALL_INSTANCE,
                SetupWizard.previousVisible(SetupWizard.Step.CUSTOMIZE, false));
        assertEquals(SetupWizard.Step.SIGN_IN,
                SetupWizard.previousVisible(SetupWizard.Step.CUSTOMIZE, true));
    }
}
