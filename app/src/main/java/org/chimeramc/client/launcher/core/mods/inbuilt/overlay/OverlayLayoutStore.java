package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists the overlay layouts and the active one (item 212).
 *
 * <p>Stored in SharedPreferences as JSON, the same shape {@link WheelProfileStore} uses, so there is
 * one persistence idiom for a user-authored list. The store never throws: a corrupt blob reads as
 * "no layouts", because a wheel with an unreadable layout list must still be usable.
 */
public final class OverlayLayoutStore {

    private static final String PREFS_NAME = "overlay_layouts";
    private static final String KEY_LAYOUTS = "layouts";
    private static final String KEY_ACTIVE = "active_layout";
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<OverlayLayout>>() { }.getType();

    private final SharedPreferences prefs;

    public OverlayLayoutStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The stored layouts, sanitised; never null. */
    public List<OverlayLayout> load() {
        String json = prefs.getString(KEY_LAYOUTS, null);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            List<OverlayLayout> parsed = GSON.fromJson(json, LIST_TYPE);
            return sanitize(parsed);
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    /** Persists the layouts, dropping anything that cannot be restored. */
    public void save(List<OverlayLayout> layouts) {
        prefs.edit().putString(KEY_LAYOUTS, GSON.toJson(sanitize(layouts))).apply();
    }

    /** The active layout id, or "" when none is active. */
    public String getActiveId() {
        String stored = prefs.getString(KEY_ACTIVE, null);
        if (stored == null || stored.isEmpty()) return "";
        return findById(load(), stored) == null ? "" : stored;
    }

    /** Marks a layout active; a blank or unknown id clears the active layout. */
    public void setActiveId(String id) {
        if (id == null || id.isEmpty() || findById(load(), id) == null) {
            prefs.edit().remove(KEY_ACTIVE).apply();
            return;
        }
        prefs.edit().putString(KEY_ACTIVE, id).apply();
    }

    public static OverlayLayout findById(List<OverlayLayout> layouts, String id) {
        if (layouts == null || id == null) return null;
        for (OverlayLayout layout : layouts) {
            if (layout != null && id.equals(layout.id)) return layout;
        }
        return null;
    }

    /** A new id that no layout uses, so a created layout is never ambiguous. */
    public static String uniqueId(List<OverlayLayout> layouts, String base) {
        String stem = slug(base);
        if (findById(layouts, stem) == null) return stem;
        for (int i = 2; i < 1000; i++) {
            String candidate = stem + i;
            if (findById(layouts, candidate) == null) return candidate;
        }
        return stem + System.nanoTime();
    }

    /** A layout id derived from a display name: lower-case, non-alphanumerics dropped. */
    public static String slug(String name) {
        if (name == null) return "layout";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.length() == 0 ? "layout" : sb.toString();
    }

    /** Replaces a layout by id, or appends it. Returns the resulting list. */
    public static List<OverlayLayout> upsert(List<OverlayLayout> layouts, OverlayLayout layout) {
        List<OverlayLayout> result = layouts == null ? new ArrayList<>() : new ArrayList<>(layouts);
        if (layout == null || layout.id.isEmpty()) return result;
        for (int i = 0; i < result.size(); i++) {
            if (layout.id.equals(result.get(i).id)) {
                result.set(i, layout);
                return result;
            }
        }
        if (result.size() < OverlayLayout.MAX_LAYOUTS) result.add(layout);
        return result;
    }

    /**
     * Drops layouts that cannot be restored and repairs the ones that can.
     *
     * <p>Pure and static so the rule is unit-testable without a Context: a layout with no id cannot
     * be referenced by a wheel entry, and a duplicate id makes the reference ambiguous.
     */
    public static List<OverlayLayout> sanitize(List<OverlayLayout> layouts) {
        List<OverlayLayout> out = new ArrayList<>();
        if (layouts == null) return out;
        List<String> seen = new ArrayList<>();
        for (OverlayLayout layout : layouts) {
            if (layout == null) continue;
            if (layout.id == null || layout.id.trim().isEmpty()) continue;
            if (seen.contains(layout.id)) continue;
            seen.add(layout.id);
            if (layout.name == null || layout.name.trim().isEmpty()) layout.name = layout.id;
            if (layout.name.length() > OverlayLayout.MAX_NAME_LENGTH) {
                layout.name = layout.name.substring(0, OverlayLayout.MAX_NAME_LENGTH);
            }
            if (layout.positions == null) layout.positions = new java.util.LinkedHashMap<>();
            if (layout.positions.size() > OverlayLayout.MAX_ENTRIES) {
                java.util.Map<String, float[]> trimmed = new java.util.LinkedHashMap<>();
                for (java.util.Map.Entry<String, float[]> e : layout.positions.entrySet()) {
                    if (trimmed.size() >= OverlayLayout.MAX_ENTRIES) break;
                    trimmed.put(e.getKey(), e.getValue());
                }
                layout.positions = trimmed;
            }
            out.add(layout);
            if (out.size() >= OverlayLayout.MAX_LAYOUTS) break;
        }
        return out;
    }
}
