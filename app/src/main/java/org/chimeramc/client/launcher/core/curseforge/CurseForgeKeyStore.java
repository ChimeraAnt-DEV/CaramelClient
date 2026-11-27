package org.chimeramc.client.core.curseforge;

import android.content.Context;
import android.content.SharedPreferences;

import org.chimeramc.client.BuildConfig;

/**
 * Resolves the CurseForge API key.
 *
 * <p>CurseForge does not allow unauthenticated calls. The client ships its own key
 * ({@code BuildConfig.CURSEFORGE_API_KEY}) so a user never has to obtain one — the CurseForge tab
 * works out of the box. A key stored on the device takes precedence: that is the Developer-mode
 * override, and it exists for rotating the shared key or pointing at a private one.
 */
public final class CurseForgeKeyStore {
    private static final String PREFS_NAME = "curseforge_prefs";
    private static final String KEY_API = "api_key";

    private CurseForgeKeyStore() {
    }

    /** The effective key, or an empty string when none is configured. */
    public static String getApiKey(Context context) {
        if (context != null) {
            String stored = prefs(context).getString(KEY_API, "");
            if (stored != null && !stored.trim().isEmpty()) return stored.trim();
        }
        String build = BuildConfig.CURSEFORGE_API_KEY;
        return build != null ? build.trim() : "";
    }

    public static boolean hasApiKey(Context context) {
        return !getApiKey(context).isEmpty();
    }

    public static void setApiKey(Context context, String key) {
        if (context == null) return;
        String value = key == null ? "" : key.trim();
        if (value.isEmpty()) {
            prefs(context).edit().remove(KEY_API).apply();
        } else {
            prefs(context).edit().putString(KEY_API, value).apply();
        }
    }

    public static void clearApiKey(Context context) {
        setApiKey(context, "");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}