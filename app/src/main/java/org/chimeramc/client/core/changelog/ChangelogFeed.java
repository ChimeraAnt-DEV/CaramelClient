package org.chimeramc.client.core.changelog;

import org.chimeramc.client.util.AppLog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The two-section changelog feed: what just shipped, and what is coming.
 *
 * <p>The changelog used to be one flat list, so "this release" and "what is next" could not be told
 * apart, and the team's Discord sneak peeks had nowhere to live. This is the feed the spec asks for:
 * a {@code newest} section (the shipped release), an {@code upcoming} section (what is being worked
 * on), and a list of sneak peeks, each an image with a caption.
 *
 * <p>It is the same JSON shape the existing changelog loader reads (a {@code schemaVersion} gate and
 * a sectioned body), extended with the upcoming section and the sneak peeks, so a document that only
 * has the old {@code sections} still parses.
 *
 * <p>Pure: it takes the body text and returns the feed, so the parsing, the empty-section rules and
 * the sneak-peek filter are unit-tested without a device. A corrupt, foreign or wrong-version body
 * reads as an empty feed rather than throwing.
 */
public final class ChangelogFeed {

    /** The schema this reader understands. A different version is refused, matching the loader. */
    public static final int SCHEMA_VERSION = 1;

    /** One changelog section: a headline and its lines. */
    public static final class Section {
        public final String version;
        public final String headline;
        public final List<String> items;

        Section(String version, String headline, List<String> items) {
            this.version = version == null ? "" : version;
            this.headline = headline == null ? "" : headline;
            this.items = Collections.unmodifiableList(items);
        }
    }

    /** One sneak peek: an image asset name and its caption. */
    public static final class SneakPeek {
        public final String caption;
        public final String image;

        SneakPeek(String caption, String image) {
            this.caption = caption == null ? "" : caption;
            this.image = image == null ? "" : image;
        }
    }

    private static final Section EMPTY_SECTION = new Section("", "", new ArrayList<>());

    private final Section newest;
    private final Section upcoming;
    private final List<SneakPeek> sneakPeeks;

    private ChangelogFeed(Section newest, Section upcoming, List<SneakPeek> sneakPeeks) {
        this.newest = newest;
        this.upcoming = upcoming;
        this.sneakPeeks = Collections.unmodifiableList(sneakPeeks);
    }

    /** An empty feed. */
    public static ChangelogFeed empty() {
        return new ChangelogFeed(EMPTY_SECTION, EMPTY_SECTION, new ArrayList<>());
    }

    public Section newest() {
        return newest;
    }

    public Section upcoming() {
        return upcoming;
    }

    public List<SneakPeek> sneakPeeks() {
        return sneakPeeks;
    }

    /** True when the newest section carries anything to show. */
    public boolean hasNewest() {
        return !newest.headline.isEmpty() || !newest.items.isEmpty();
    }

    /** True when the upcoming section carries anything to show. */
    public boolean hasUpcoming() {
        return !upcoming.headline.isEmpty() || !upcoming.items.isEmpty();
    }

    /**
     * Both headlines on one line, for the loading screen.
     *
     * <p>The loading screen has one line, so it must not silently show only the newest section and
     * hide that there is an upcoming one. An empty section is omitted rather than left as a
     * dangling separator.
     */
    public String headlineLine() {
        List<String> parts = new ArrayList<>();
        if (hasNewest() && !newest.headline.isEmpty()) parts.add(newest.headline);
        if (hasUpcoming() && !upcoming.headline.isEmpty()) parts.add(upcoming.headline);
        return String.join(" \u00b7 ", parts);
    }

    /** Parses a feed body. A null, malformed, foreign or wrong-version body reads as empty. */
    public static ChangelogFeed parse(String json) {
        if (json == null || json.trim().isEmpty()) return empty();
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed == null || !parsed.isJsonObject()) return empty();
            JsonObject root = parsed.getAsJsonObject();
            int schema = optInt(root, "schemaVersion", -1);
            if (schema != SCHEMA_VERSION) return empty();
            return new ChangelogFeed(
                    parseSection(root.getAsJsonObject("newest")),
                    parseSection(root.getAsJsonObject("upcoming")),
                    parseSneakPeeks(root.getAsJsonArray("sneakPeeks")));
        } catch (Exception e) {
            AppLog.warnOnce("GlowberryClient", "ChangelogFeed:parse",
                    "Ignored a malformed changelog feed", e);
            return empty();
        }
    }

    private static Section parseSection(JsonObject object) {
        if (object == null) return EMPTY_SECTION;
        List<String> items = new ArrayList<>();
        JsonArray array = object.getAsJsonArray("items");
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) continue;
                String text = optString(element.getAsJsonObject(), "text", "");
                if (!text.isEmpty()) items.add(text);
            }
        }
        return new Section(optString(object, "version", ""),
                optString(object, "headline", ""), items);
    }

    private static List<SneakPeek> parseSneakPeeks(JsonArray array) {
        List<SneakPeek> out = new ArrayList<>();
        if (array == null) return out;
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            String image = optString(object, "image", "");
            // An entry with no image has nothing to show, so it is dropped rather than drawn empty.
            if (image.isEmpty()) continue;
            out.add(new SneakPeek(optString(object, "caption", ""), image));
        }
        return out;
    }

    private static String optString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) return fallback;
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) return fallback;
        try {
            return value.getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int optInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) return fallback;
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) return fallback;
        try {
            return value.getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }
}
