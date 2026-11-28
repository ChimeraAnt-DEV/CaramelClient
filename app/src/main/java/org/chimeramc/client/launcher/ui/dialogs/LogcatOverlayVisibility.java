package org.chimeramc.client.ui.dialogs;

/** When the logcat overlay may be on screen, as a rule with no Android in it. */
public final class LogcatOverlayVisibility {

    private LogcatOverlayVisibility() {
    }

    /**
     * The overlay is shown when it is switched on and the app is not on the splash or loading
     * screens. Those two cover the whole window with their own artwork, so a log strip there would
     * sit over the logo the user is looking at.
     */
    public static boolean shouldShow(boolean enabled, boolean onSplash, boolean onLoading) {
        return enabled && !onSplash && !onLoading;
    }
}
