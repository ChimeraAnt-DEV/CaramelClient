package org.chimeramc.client.ui.navigation;

/**
 * Whether a navigation destination carries an attention dot (item 83).
 *
 * <p>A tab that is a list of things the user has to act on - mods that failed to load, mods the
 * crash guard disabled, an update waiting - is worth a dot, because the failure is otherwise only
 * visible after opening the screen and reading a banner. A dot that is always on is noise, so the
 * rule is deliberately narrow: only a *broken* or *pending* state earns one, and a normal, healthy
 * list does not.
 *
 * <p>Pure and Android-free: it takes the signals the callers already compute (they are separate
 * subsystems, so a Context would pull half the app in) and returns one answer, so the rule is
 * unit-tested without a device and cannot drift between the bar and the rail.
 */
public final class NavTabBadge {

    private NavTabBadge() {
    }

    /** The signals that can put a dot on a destination. Each one is a fact a caller can read. */
    public static final class Signals {
        /** A mod failed to load and the failure is recorded for the Mods tab to show. */
        public final boolean modsFailed;
        /** The crash guard disabled one or more mods after a crash loop. */
        public final boolean modsAutoDisabled;
        /** The previous session never completed, so the Mods tab offers a safe-mode escape. */
        public final boolean crashLoopPending;
        /** A launcher update is available. */
        public final boolean updateAvailable;

        public Signals(boolean modsFailed, boolean modsAutoDisabled, boolean crashLoopPending,
                       boolean updateAvailable) {
            this.modsFailed = modsFailed;
            this.modsAutoDisabled = modsAutoDisabled;
            this.crashLoopPending = crashLoopPending;
            this.updateAvailable = updateAvailable;
        }

        /** No attention needed anywhere. */
        public static Signals none() {
            return new Signals(false, false, false, false);
        }
    }

    /**
     * Whether {@code tab} should show an attention dot for {@code signals}.
     *
     * <p>The Mods tab is the only one a mod failure can attach to, so it is the only destination
     * that reads the mod signals. An update is launcher-wide, so it is reported on Settings, where
     * the update check lives - not on every tab, which would make the dot meaningless.
     */
    public static boolean showsBadge(LauncherTab tab, Signals signals) {
        if (tab == null || signals == null) return false;
        switch (tab) {
            case MODS:
                return signals.modsFailed || signals.modsAutoDisabled || signals.crashLoopPending;
            case SETTINGS:
                return signals.updateAvailable;
            default:
                return false;
        }
    }

    /** True when any destination carries a dot, so a caller can skip the whole pass cheaply. */
    public static boolean anyBadge(Signals signals) {
        if (signals == null) return false;
        for (LauncherTab tab : LauncherTab.values()) {
            if (showsBadge(tab, signals)) return true;
        }
        return false;
    }
}
