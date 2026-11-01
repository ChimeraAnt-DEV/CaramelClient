package org.chimeramc.client.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class SettingsStorage {
    public static final String SP_NAME = "feature_settings";
    public static final String KEY_SETTINGS_JSON = "settings_json";
    private static final Gson gson = new Gson();

    /**
     * The stored JSON for [settings]. Pure, so the round trip is unit-tested without a device
     * (item 351).
     */
    public static String toJson(FeatureSettings settings) {
        return gson.toJson(settings);
    }

    /**
     * Parses stored JSON, or null when it is absent or malformed.
     *
     * <p>A field the JSON omits keeps the value its declaration initialises it to, because Gson
     * runs the no-argument constructor and only overwrites the keys that are present. That is what
     * makes an older settings file - written before a switch existed - load with the new switch's
     * documented default rather than a JVM zero. A field the JSON carries that this build does not
     * know is ignored, so a file written by a newer build still loads.
     */
    public static FeatureSettings fromJson(String json) {
        if (json == null) return null;
        try {
            return gson.fromJson(json, FeatureSettings.class);
        } catch (com.google.gson.JsonParseException e) {
            return null;
        }
    }

    public static void save(Context context, FeatureSettings settings) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_SETTINGS_JSON, toJson(settings)).apply();
    }

    public static FeatureSettings load(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_SETTINGS_JSON, null);
        FeatureSettings parsed = fromJson(json);
        return parsed == null ? new FeatureSettings() : parsed;
    }

    public static void clear(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_SETTINGS_JSON).apply();
    }
}