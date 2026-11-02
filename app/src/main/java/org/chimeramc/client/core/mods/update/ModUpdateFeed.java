package org.chimeramc.client.core.mods.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses the mod-update feed into an id to version map (item 273).
 *
 * <p>The feed is a tiny document the project hosts, so a new mod release needs no app update:
 * <pre>
 * { "mods": [ { "id": "…", "version": "1.2.0" }, … ] }
 * </pre>
 *
 * <p>It is external data, so a malformed document parses to an empty map rather than throwing, and
 * an entry without an id or version is skipped. A partial parse is better than none: one bad entry
 * must not hide the updates the rest of the feed describes.
 */
public final class ModUpdateFeed {

    private ModUpdateFeed() {
    }

    /** Parses the feed. Never throws; bad input yields an empty map. */
    public static Map<String, String> parse(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return result;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root == null || !root.isJsonObject()) return result;
            JsonElement mods = root.getAsJsonObject().get("mods");
            if (mods == null || !mods.isJsonArray()) return result;
            for (JsonElement element : mods.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String id = stringOf(object, "id");
                String version = stringOf(object, "version");
                if (id.isEmpty() || version.isEmpty()) continue;
                result.put(id, version);
            }
        } catch (RuntimeException e) {
            return new LinkedHashMap<>();
        }
        return result;
    }

    private static String stringOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return "";
        try {
            return element.getAsString().trim();
        } catch (RuntimeException e) {
            return "";
        }
    }
}
