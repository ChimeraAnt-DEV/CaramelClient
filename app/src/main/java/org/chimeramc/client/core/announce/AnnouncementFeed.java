package org.chimeramc.client.core.announce;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Parses the announcements feed and picks the card to show (item 255).
 *
 * <p>Android-free and pure so the two rules that matter are unit-tested: a malformed feed parses to
 * an empty list rather than throwing (the feed is a document a human edits and a machine hosts), and
 * the selection skips dismissed cards and cards whose version gate does not cover this build.
 *
 * <p>The feed shape is intentionally tiny:
 * <pre>
 * { "cards": [
 *   { "id": "…", "title": "…", "body": "…", "url": "https://…",
 *     "minVersionCode": 0, "maxVersionCode": 0 } ] }
 * </pre>
 */
public final class AnnouncementFeed {

    private AnnouncementFeed() {
    }

    /** Parses a feed document into cards, in feed order. Never throws; bad input yields an empty list. */
    public static List<AnnouncementCard> parse(String json) {
        List<AnnouncementCard> cards = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return cards;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root == null || !root.isJsonObject()) return cards;
            JsonArray array = root.getAsJsonObject().getAsJsonArray("cards");
            if (array == null) return cards;
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                AnnouncementCard card = new AnnouncementCard(
                        stringOf(object, "id"),
                        stringOf(object, "title"),
                        stringOf(object, "body"),
                        stringOf(object, "url"),
                        longOf(object, "minVersionCode"),
                        longOf(object, "maxVersionCode"));
                if (card.isRenderable()) cards.add(card);
            }
        } catch (RuntimeException e) {
            // A feed is external data; a parse failure must never take the Launch tab down.
            return Collections.emptyList();
        }
        return cards;
    }

    /**
     * The first card that applies to {@code versionCode} and has not been dismissed.
     *
     * <p>Feed order is priority order: the first matching, undismissed card wins, so the project can
     * put the most important notice at the top without a separate priority field.
     */
    public static AnnouncementCard firstMatching(List<AnnouncementCard> cards, long versionCode,
                                                 Set<String> dismissedIds) {
        if (cards == null) return null;
        for (AnnouncementCard card : cards) {
            if (!card.matches(versionCode)) continue;
            if (dismissedIds != null && dismissedIds.contains(card.id)) continue;
            return card;
        }
        return null;
    }

    private static String stringOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return "";
        try {
            return element.getAsString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static long longOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return AnnouncementCard.NO_BOUND;
        try {
            return element.getAsLong();
        } catch (RuntimeException e) {
            return AnnouncementCard.NO_BOUND;
        }
    }
}
