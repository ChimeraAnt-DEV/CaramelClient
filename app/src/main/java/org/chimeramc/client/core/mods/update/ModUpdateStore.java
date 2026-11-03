package org.chimeramc.client.core.mods.update;

import org.chimeramc.client.util.AppLog;

import android.content.Context;
import android.content.SharedPreferences;

import org.chimeramc.client.BuildConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists the mod-update check state.
 *
 * <p>Three facts are needed for "one notification, not one per mod": when the check last ran (so it
 * is throttled rather than run on every launch), the feed URL (so it is a build value, not a
 * hard-coded constant), and which update summary was already announced (so the same set of updates
 * does not notify twice).
 *
 * <p>The announced fingerprint is the sorted list of {@code id@version} pairs. A new version of an
 * already-announced mod changes the fingerprint and notifies again; nothing changing leaves it
 * alone. That is deliberately stricter than remembering a single timestamp: a check that runs twice
 * in a day with the same feed must not produce a second notification.
 */
public final class ModUpdateStore {

    public static final String PREFS_NAME = "mod_update_check";
    private static final String KEY_LAST_CHECK_AT = "last_check_at";
    private static final String KEY_ANNOUNCED = "announced_fingerprint";
    /** How long a check is trusted before another is allowed. */
    public static final long CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L;

    private ModUpdateStore() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The feed URL for this build, or empty when none is configured. */
    public static String configuredUrl() {
        String url = BuildConfig.MOD_UPDATE_FEED_URL;
        return url == null ? "" : url.trim();
    }

    public static long lastCheckAt(Context context) {
        try {
            return prefs(context).getLong(KEY_LAST_CHECK_AT, 0L);
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    public static void recordCheck(Context context, long nowMs) {
        try {
            prefs(context).edit().putLong(KEY_LAST_CHECK_AT, nowMs).apply();
        } catch (RuntimeException ignored) {
            AppLog.warnOnce("GlowberryClient", "ModUpdateStore:61", "Ignored a failure at ModUpdateStore:61", ignored);
        }
    }

    /** True when the last check is older than {@link #CHECK_INTERVAL_MS}. */
    public static boolean isDue(Context context, long nowMs) {
        long last = lastCheckAt(context);
        return last <= 0L || nowMs - last >= CHECK_INTERVAL_MS || nowMs < last;
    }

    public static Set<String> announced(Context context) {
        try {
            Set<String> stored = prefs(context).getStringSet(KEY_ANNOUNCED, null);
            return stored == null ? Collections.emptySet() : new HashSet<>(stored);
        } catch (RuntimeException e) {
            return Collections.emptySet();
        }
    }

    public static void setAnnounced(Context context, Set<String> fingerprint) {
        try {
            prefs(context).edit()
                    .putStringSet(KEY_ANNOUNCED, fingerprint == null
                            ? Collections.emptySet() : new HashSet<>(fingerprint))
                    .apply();
        } catch (RuntimeException ignored) {
            AppLog.warnOnce("GlowberryClient", "ModUpdateStore:87", "Ignored a failure at ModUpdateStore:87", ignored);
        }
    }

    /** Clears the throttle so the next check runs immediately (the Settings "Check now" path). */
    public static void clearThrottle(Context context) {
        try {
            prefs(context).edit().remove(KEY_LAST_CHECK_AT).apply();
        } catch (RuntimeException ignored) {
            AppLog.warnOnce("GlowberryClient", "ModUpdateStore:96", "Ignored a failure at ModUpdateStore:96", ignored);
        }
    }
}
