package org.chimeramc.client.core.changelog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The bundled release notes, shaped for the Launch tab's changelog card and popup.
 *
 * <p>The card shows a one-line summary and the popup shows the whole release grouped as
 * <b>Added</b>, <b>Changed</b> and <b>Fixed</b>, which is the shape a player reads a release in. The
 * bundled JSON groups its notes by theme ("Module Catalogue — Phase A", "Native route cleanup"),
 * which is useful to a contributor and opaque to a player, so each section is classified into one of
 * the three buckets.
 *
 * <p>A section may carry an explicit {@code "kind"} of {@code added} / {@code changed} / {@code fixed}
 * to override the guess; otherwise the title decides, and a title that matches nothing is
 * {@link Kind#OTHER} and is shown after the three buckets rather than being dropped.
 *
 * <p>Pure and Android-free, so the parse and the classification are unit-tested without a device. A
 * body that is missing, corrupt or the wrong schema reads as {@link #isEmpty() empty} rather than
 * throwing.
 */
public final class LaunchChangelog {

    /** The schema this reader understands; a different version is refused, matching the loader. */
    public static final int SCHEMA_VERSION = 1;

    /** The three buckets a player reads, plus the catch-all for a section that names neither. */
    public enum Kind { ADDED, CHANGED, FIXED, OTHER }

    /** One release note. {@code url} is empty when the note has no link. */
    public static final class Entry {
        public final String text;
        public final String url;

        Entry(String text, String url) {
            this.text = text == null ? "" : text;
            this.url = url == null ? "" : url;
        }

        public boolean hasUrl() {
            return !url.isEmpty();
        }
    }

    /** A titled group of notes, classified into one {@link Kind}. */
    public static final class Section {
        public final String title;
        public final Kind kind;
        public final List<Entry> entries;

        Section(String title, Kind kind, List<Entry> entries) {
            this.title = title == null ? "" : title;
            this.kind = kind;
            this.entries = Collections.unmodifiableList(entries);
        }
    }

    public final String headline;
    public final String summary;
    public final String releasedAt;
    public final String supportUntil;
    public final List<Section> sections;

    private LaunchChangelog(String headline, String summary, String releasedAt,
                            String supportUntil, List<Section> sections) {
        this.headline = headline;
        this.summary = summary;
        this.releasedAt = releasedAt;
        this.supportUntil = supportUntil;
        this.sections = Collections.unmodifiableList(sections);
    }

    /** True when the body carried nothing renderable. */
    public boolean isEmpty() {
        if (sections.isEmpty()) return true;
        for (Section section : sections) {
            if (!section.entries.isEmpty()) return false;
        }
        return true;
    }

    /** Every entry in the sections of one {@link Kind}, in document order. */
    public List<Entry> entriesOfKind(Kind kind) {
        List<Entry> out = new ArrayList<>();
        for (Section section : sections) {
            if (section.kind == kind) out.addAll(section.entries);
        }
        return out;
    }

    /** True when at least one entry falls in {@code kind}, so a column can be hidden when empty. */
    public boolean hasKind(Kind kind) {
        return !entriesOfKind(kind).isEmpty();
    }

    /** An empty changelog, so a caller never has to handle null. */
    public static LaunchChangelog empty() {
        return new LaunchChangelog("", "", "", "", new ArrayList<>());
    }

    /**
     * Parses the bundled release notes.
     *
     * <p>Never throws: a missing, blank, non-JSON or wrong-schema body yields {@link #empty()}.
     */
    public static LaunchChangelog parse(String json) {
        if (json == null || json.trim().isEmpty()) return empty();
        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(json);
            if (element == null || !element.isJsonObject()) return empty();
            root = element.getAsJsonObject();
        } catch (RuntimeException e) {
            return empty();
        }

        if (intOr(root, "schemaVersion", -1) != SCHEMA_VERSION) return empty();

        List<Section> sections = new ArrayList<>();
        JsonArray rawSections = arrayOr(root, "sections");
        for (JsonElement raw : rawSections) {
            if (raw == null || !raw.isJsonObject()) continue;
            JsonObject section = raw.getAsJsonObject();
            String title = stringOr(section, "title", "");
            Kind kind = classify(title, stringOr(section, "kind", ""));
            List<Entry> entries = new ArrayList<>();
            for (JsonElement rawItem : arrayOr(section, "items")) {
                if (rawItem == null || !rawItem.isJsonObject()) continue;
                JsonObject item = rawItem.getAsJsonObject();
                String text = stringOr(item, "text", "").trim();
                if (text.isEmpty()) continue;
                entries.add(new Entry(text, stringOr(item, "url", "")));
            }
            sections.add(new Section(title, kind, entries));
        }

        return new LaunchChangelog(
                stringOr(root, "headline", ""),
                stringOr(root, "summary", ""),
                stringOr(root, "releasedAt", ""),
                stringOr(root, "supportUntil", ""),
                sections);
    }

    /**
     * Classifies a section, preferring an explicit {@code kind} over the title.
     *
     * <p>The title match is deliberately ordered: "performance improvements" is a change, not an
     * addition, and "bug fixes" is a fix even though "fix" also reads as a change. Matching the fix
     * words first keeps "fixed" out of the changed bucket.
     */
    static Kind classify(String title, String explicit) {
        Kind declared = fromWord(explicit);
        if (declared != null) return declared;
        Kind fromTitle = fromWord(title);
        return fromTitle != null ? fromTitle : Kind.OTHER;
    }

    /** The kind a word names, or null when it names none. */
    private static Kind fromWord(String value) {
        if (value == null || value.isEmpty()) return null;
        String text = value.toLowerCase(Locale.US);
        // Fix words first: "bug fixes" must not read as a change.
        if (containsAny(text, "fix", "bug", "patch", "repair", "correct")) return Kind.FIXED;
        if (containsAny(text, "add", "new", "introduc", "ship", "support for")) return Kind.ADDED;
        if (containsAny(text, "chang", "improv", "updat", "redo", "redesign", "revamp", "refactor",
                "tweak", "adjust", "rework", "polish", "performance", "optimi")) {
            return Kind.CHANGED;
        }
        return null;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    private static String stringOr(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        JsonElement value = object.get(key);
        if (!value.isJsonPrimitive()) return fallback;
        return value.getAsString();
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static JsonArray arrayOr(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) return new JsonArray();
        return object.getAsJsonArray(key);
    }
}
