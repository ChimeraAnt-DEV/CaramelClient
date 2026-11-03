package org.chimeramc.client.core.privacy;

import android.content.Context;

import org.chimeramc.client.settings.FeatureSettings;

/**
 * Reads the two privacy switches into a {@link PrivacyMode}.
 *
 * <p>The only Android-aware piece of the pair. A caller asks this for the current mode and then asks
 * the pure {@link PrivacyMode} what to hide, so the rule is tested without a Context and the
 * preferences are read in one place. An unreadable preference reads as "off", which is the safe
 * default for a mode whose whole purpose is to be opted into.
 */
public final class PrivacyModes {

    private PrivacyModes() {
    }

    /** The mode in force right now. Never null. */
    public static PrivacyMode current() {
        try {
            FeatureSettings fs = FeatureSettings.getInstance();
            return new PrivacyMode(fs.isGuestModeEnabled(), fs.isStreamingSafeModeEnabled());
        } catch (Throwable ignored) {
            return PrivacyMode.open();
        }
    }

    /** The mode in force right now, initialising the settings store if it has not been yet. */
    public static PrivacyMode current(Context context) {
        if (context != null) {
            FeatureSettings.init(context.getApplicationContext());
        }
        return current();
    }
}
