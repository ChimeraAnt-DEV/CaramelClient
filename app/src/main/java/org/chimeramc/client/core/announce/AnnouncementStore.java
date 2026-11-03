package org.chimeramc.client.core.announce;

import org.chimeramc.client.util.AppLog;

import android.content.Context;
import android.content.SharedPreferences;

import org.chimeramc.client.BuildConfig;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persists the announcements feed and which cards the user has dismissed.
 *
 * <p>Two things are stored: the last feed the app fetched (so a card is still shown offline) and the
 * set of dismissed ids (so a card the user closed stays closed). The dismissed set is bounded so it
 * cannot grow without limit across many releases; the oldest ids fall off, which is safe because a
 * feed only ever re-offers an id it is still publishing.
 */
public final class AnnouncementStore {

    private static final String PREFS = "launcher_announcements";
    private static final String KEY_FEED = "cached_feed";
    private static final String KEY_DISMISSED = "dismissed_ids";
    private static final int MAX_DISMISSED = 100;

    private AnnouncementStore() {
    }

    /** The cached feed document, or an empty string when none has been fetched. */
    public static String cachedFeed(Context context) {
        try {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_FEED, "");
        } catch (Throwable t) {
            return "";
        }
    }

    /** Stores the feed document, replacing the previous one. */
    public static void cacheFeed(Context context, String json) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_FEED, json).apply();
        } catch (Throwable ignored) {
            AppLog.warnOnce("GlowberryClient", "AnnouncementStore:48", "Ignored a failure at AnnouncementStore:48", ignored);
        }
    }

    /** The ids the user has dismissed. Never null. */
    public static Set<String> dismissedIds(Context context) {
        try {
            Set<String> stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getStringSet(KEY_DISMISSED, null);
            return stored != null ? new LinkedHashSet<>(stored) : new LinkedHashSet<>();
        } catch (Throwable t) {
            return new LinkedHashSet<>();
        }
    }

    /**
     * Marks a card dismissed, keeping the set bounded.
     *
     * <p>A {@code LinkedHashSet} preserves insertion order, so trimming the head drops the oldest
     * dismissals first — the ones least likely to be re-offered.
     */
    public static void dismiss(Context context, String id) {
        if (id == null || id.trim().isEmpty()) return;
        try {
            Set<String> current = dismissedIds(context);
            current.add(id.trim());
            while (current.size() > MAX_DISMISSED) {
                java.util.Iterator<String> it = current.iterator();
                it.next();
                it.remove();
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putStringSet(KEY_DISMISSED, current).apply();
        } catch (Throwable ignored) {
            AppLog.warnOnce("GlowberryClient", "AnnouncementStore:82", "Ignored a failure at AnnouncementStore:82", ignored);
        }
    }

    /** The feed URL the build was configured with, or an empty string when unset. */
    public static String configuredUrl() {
        try {
            String value = BuildConfig.ANNOUNCEMENTS_URL;
            return value != null ? value.trim() : "";
        } catch (Throwable t) {
            return "";
        }
    }
}
