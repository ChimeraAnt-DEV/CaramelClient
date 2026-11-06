package org.chimeramc.client.launcher.ui.splash;

import android.content.Context;
import android.content.SharedPreferences;

import org.chimeramc.client.R;
import org.chimeramc.client.util.AppLog;

import java.util.Random;

/**
 * The splash screen's background list and the remembered choice.
 *
 * <p>The images are Minecraft key art and wallpapers published by Mojang on minecraft.net. The
 * Minecraft Usage Guidelines cover "assets ... including videos or screenshots taken from our games"
 * for community creations; the attribution the guidelines require is rendered on the splash itself
 * (see {@link #DISCLAIMER}).
 *
 * <p>The selection rule itself is pure and lives in {@link SplashBackgrounds}; this class only
 * supplies the candidate list and remembers the last choice.
 */
public final class SplashBackgroundRegistry {

    /**
     * The attribution the Minecraft Usage Guidelines require on related materials.
     *
     * <p>Verbatim from the guidelines' "Essential guidelines" section, which asks for a prominent
     * disclaimer in this form on the product and its related materials.
     */
    public static final String DISCLAIMER =
            "NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT";

    /** Every shipped background, in a stable order. */
    private static final int[] BACKGROUNDS = {
            R.drawable.splash_bg_1,
            R.drawable.splash_bg_2,
            R.drawable.splash_bg_3,
            R.drawable.splash_bg_4,
            R.drawable.splash_bg_5,
            R.drawable.splash_bg_6,
            R.drawable.splash_bg_7,
            R.drawable.splash_bg_8,
    };

    private static final String TAG = "SplashBackgroundRegistry";

    private static final String PREFS = "splash_prefs";
    private static final String KEY_LAST_INDEX = "splash_last_bg_index";

    private SplashBackgroundRegistry() {
    }

    /** The number of shipped backgrounds. */
    public static int count() {
        return BACKGROUNDS.length;
    }

    /**
     * Picks this launch's background, remembering it so the next launch can avoid it.
     *
     * <p>A failure to read or write the preference is not fatal: the worst case is that the same
     * background shows twice, which is cosmetic, not a crash.
     */
    public static int pickForThisLaunch(Context context) {
        int previous = -1;
        SharedPreferences prefs = null;
        try {
            prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            previous = prefs.getInt(KEY_LAST_INDEX, -1);
        } catch (Throwable t) {
            // Fall through with previous = -1: the worst case is that the same background shows
            // twice, which is cosmetic. Logged so a persistent prefs failure is visible.
            AppLog.warnOnce(TAG, "splash-bg-read", "Could not read the last splash background", t);
        }

        final int index = SplashBackgrounds.pick(BACKGROUNDS.length, previous, new Random());

        if (prefs != null && index >= 0) {
            try {
                prefs.edit().putInt(KEY_LAST_INDEX, index).apply();
            } catch (Throwable t) {
                AppLog.warnOnce(TAG, "splash-bg-write",
                        "Could not remember the splash background", t);
            }
        }
        return index;
    }

    /** The drawable resource for an index, or the first background when the index is unknown. */
    public static int drawableFor(int index) {
        if (index < 0 || index >= BACKGROUNDS.length) return BACKGROUNDS[0];
        return BACKGROUNDS[index];
    }
}
