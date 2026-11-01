package org.chimeramc.client.ui.navigation;

import org.chimeramc.client.R;

/**
 * What a long press on a destination offers (item 92).
 *
 * <p>A tab is a navigation entry, so a long press must not navigate somewhere else by surprise; it
 * offers the destinations that are <em>sections</em> of that tab. Library holds Instances, Install
 * and Replay, so a long press is a direct route to one of them; Mods holds the Mod Hub; Settings
 * holds Diagnostics. Play is the launcher's home and has nothing under it, so it offers nothing
 * rather than a menu with a single filler entry.
 *
 * <p>The rule is a pure function of the tab, so it is unit-tested and the two nav layouts cannot
 * disagree about it. The action only names a label resource and an identity; {@code BaseActivity}
 * is what turns one into an intent, which keeps Android types out of this class.
 */
public final class NavTabLongPress {

    private NavTabLongPress() {
    }

    /** A shortcut a long press can offer. */
    public enum Action {
        LIBRARY_INSTANCES(R.string.nav_instances),
        LIBRARY_INSTALL(R.string.nav_installations),
        LIBRARY_REPLAY(R.string.launch_toggle_replay),
        MOD_HUB(R.string.mod_hub_title),
        DIAGNOSTICS(R.string.diagnostics_title);

        private final int labelRes;

        Action(int labelRes) {
            this.labelRes = labelRes;
        }

        /** The string resource to show for this shortcut. */
        public int labelRes() {
            return labelRes;
        }
    }

    /**
     * The shortcuts {@code tab} offers, in the order they are shown. Never null; an empty array
     * means the tab has no long-press menu at all.
     */
    public static Action[] actionsFor(LauncherTab tab) {
        if (tab == null) return new Action[0];
        switch (tab) {
            case LIBRARY:
                return new Action[]{Action.LIBRARY_INSTANCES, Action.LIBRARY_INSTALL,
                        Action.LIBRARY_REPLAY};
            case MODS:
                return new Action[]{Action.MOD_HUB};
            case SETTINGS:
                return new Action[]{Action.DIAGNOSTICS};
            default:
                return new Action[0];
        }
    }

    /** Whether {@code tab} has a long-press menu, so the listener is only attached where useful. */
    public static boolean hasActions(LauncherTab tab) {
        return actionsFor(tab).length > 0;
    }
}
