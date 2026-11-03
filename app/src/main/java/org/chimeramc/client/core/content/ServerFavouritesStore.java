package org.chimeramc.client.core.content;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists the starred servers.
 *
 * <p>A small {@link SharedPreferences} set of {@code ip:port} keys, kept apart from the game's own
 * server file so a star never changes what the game reads. Every write is {@code apply()}: the star
 * is presentation state, and blocking the caller to guarantee it is the wrong trade.
 *
 * <p>A malformed entry is dropped rather than crashing the server screen; an empty or unreadable
 * store reads as "no favourites".
 */
public final class ServerFavouritesStore {

    private static final String PREFS = "server_favourites";
    private static final String KEY = "favourites";

    private ServerFavouritesStore() {
    }

    /** The starred keys, never null. */
    public static Set<String> load(Context context) {
        if (context == null) return Collections.emptySet();
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Set<String> stored = prefs.getStringSet(KEY, null);
            if (stored == null) return Collections.emptySet();
            Set<String> clean = new HashSet<>();
            for (String key : stored) {
                if (key != null && !key.isEmpty()) clean.add(key);
            }
            return clean;
        } catch (Throwable t) {
            return Collections.emptySet();
        }
    }

    /** Writes the starred keys back. Never throws. */
    public static void save(Context context, Set<String> keys) {
        if (context == null) return;
        try {
            Set<String> copy = keys != null ? new HashSet<>(keys) : new HashSet<>();
            context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(KEY, copy)
                    .apply();
        } catch (Throwable ignored) {
            // A favourite that cannot be written costs one star, not the screen.
        }
    }

    /** Toggles one key, persists, and returns the new set. */
    public static Set<String> toggle(Context context, String key) {
        Set<String> next = ServerFavourites.toggle(load(context), key);
        save(context, next);
        return next;
    }
}
