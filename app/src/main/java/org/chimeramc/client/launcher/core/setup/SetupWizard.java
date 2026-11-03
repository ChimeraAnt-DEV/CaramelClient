package org.chimeramc.client.launcher.core.setup;

import java.util.Locale;

/**
 * The first-run setup wizard's five steps, as a pure state machine.
 *
 * <p>The wizard walks a new user through the five things that make the client usable: accepting the
 * licence, installing an instance, signing in to Microsoft, customising the UI, and seeing what is
 * new / where the community is. Only the licence is mandatory — the other four can be skipped, so a
 * user who wants to play now is never trapped in a setup flow.
 *
 * <p>The machine is pure so the rules are unit-tested without a device: which step is first, whether
 * the current step may be left, whether it may be skipped, and what comes next. The activity renders
 * it and performs each step's action; it holds no ordering logic of its own.
 */
public final class SetupWizard {

    private SetupWizard() {
    }

    /** The wizard's steps, in order. {@link #READY} is the end state, not a shown step. */
    public enum Step {
        /** Read and accept the licence. The only step that cannot be skipped. */
        LICENSE,
        /** Install an instance (import or locate the game files). Skippable. */
        INSTALL_INSTANCE,
        /** Sign in to Microsoft. Skippable. */
        SIGN_IN,
        /** Customise the UI (theme, accent, animations). Skippable. */
        CUSTOMIZE,
        /** View the changelog and the Discord invite. Skippable. */
        CHANGELOG_DISCORD,
        /** Setup is finished; the launcher opens normally. */
        READY,
    }

    /** The five steps a user actually sees, in order. */
    public static final Step[] STEPS = {
            Step.LICENSE,
            Step.INSTALL_INSTANCE,
            Step.SIGN_IN,
            Step.CUSTOMIZE,
            Step.CHANGELOG_DISCORD,
    };

    /** The facts the wizard reads. */
    public static final class Facts {
        public boolean licenseAccepted;

        public Facts licenseAccepted(boolean v) { licenseAccepted = v; return this; }
    }

    /**
     * Whether the current step may be left with Next.
     *
     * <p>Only the licence gates Next, and only until it is accepted; that is what makes the checkbox
     * the thing that lights the button up. Every later step is always leavable.
     */
    public static boolean canProceed(Step step, Facts facts) {
        if (step == Step.LICENSE) {
            return facts != null && facts.licenseAccepted;
        }
        return true;
    }

    /** Whether a step may be skipped. The licence is the one step that may not. */
    public static boolean canSkip(Step step) {
        return step != null && step != Step.LICENSE && step != Step.READY;
    }

    /** The step after {@code step}: the next shown step, or {@link Step#READY} after the last. */
    public static Step next(Step step) {
        if (step == null) return Step.LICENSE;
        switch (step) {
            case LICENSE: return Step.INSTALL_INSTANCE;
            case INSTALL_INSTANCE: return Step.SIGN_IN;
            case SIGN_IN: return Step.CUSTOMIZE;
            case CUSTOMIZE: return Step.CHANGELOG_DISCORD;
            default: return Step.READY;
        }
    }

    /** The step before {@code step}, or {@link Step#LICENSE} at the start. */
    public static Step previous(Step step) {
        if (step == null) return Step.LICENSE;
        switch (step) {
            case INSTALL_INSTANCE: return Step.LICENSE;
            case SIGN_IN: return Step.INSTALL_INSTANCE;
            case CUSTOMIZE: return Step.SIGN_IN;
            case CHANGELOG_DISCORD: return Step.CUSTOMIZE;
            default: return Step.LICENSE;
        }
    }

    /** The 0-based index of a shown step within {@link #STEPS}, or -1 for {@link Step#READY}. */
    public static int index(Step step) {
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i] == step) return i;
        }
        return -1;
    }

    /** Whether the wizard is finished and the launcher should open normally. */
    public static boolean isComplete(Step step) {
        return step == Step.READY;
    }

    /** A stable key for the step, for persistence and the diagnostics report. */
    public static String key(Step step) {
        return step == null ? "unknown" : step.name().toLowerCase(Locale.ROOT);
    }
}
