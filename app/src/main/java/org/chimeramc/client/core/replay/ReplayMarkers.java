package org.chimeramc.client.core.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The timestamped markers on a recording (item 241).
 *
 * <p>A replay is more useful when the moments that matter are labelled: a death, a kill streak, a
 * combo, a join. This class is the pure store for those labels so the timeline can draw a tick at
 * each one and the player can jump to it, and so the rules are unit-tested without a device.
 *
 * <p>Markers are kept in ascending time order and de-duplicated within {@link #MERGE_WINDOW_MS}, so
 * a burst of triggers in one second (a combo firing on every landed hit) becomes one tick rather
 * than a stack of overlapping ones. Each marker carries a {@link Kind} and an optional detail
 * string (the streak length, the combo count), so the label can say "Kill streak 5" rather than a
 * bare dot.
 *
 * <p>Honest scope, matching the rest of the replay system: markers come from the client's own
 * signals (the local-player feed's health for death, the hit-timing counter for combos, the
 * consecutive-opponent-death counter for streaks, the session lifecycle for a join). The build has
 * no server-side event feed, so a marker never claims an event the client cannot see.
 *
 * <p>Pure and Android-free: the caller supplies the elapsed time, so serialisation, merging and the
 * jump-to-marker rule are all testable without a clock.
 */
public final class ReplayMarkers {

    /**
     * Two markers of the same kind inside this window are merged into one.
     *
     * <p>One second is the granularity the timeline is drawn at, so two ticks closer than that
     * would overlap anyway; merging keeps a rapid combo from filling the bar with a single event.
     */
    public static final long MERGE_WINDOW_MS = 1000L;

    /** The kind of moment a marker records. */
    public enum Kind {
        /** The local player died. */
        DEATH,
        /** A run of credited kills reached the given length. */
        KILL_STREAK,
        /** A run of landed-in-window attacks reached the given length. */
        COMBO,
        /** The clip's world was joined (the start of the recording). */
        JOIN
    }

    /** One marker: when it happened on the clip's own timeline, what it was, and a detail. */
    public static final class Marker {
        public final long tMs;
        public final Kind kind;
        /** A detail string (the streak/combo length), or empty when the kind carries none. */
        public final String detail;

        Marker(long tMs, Kind kind, String detail) {
            this.tMs = Math.max(0L, tMs);
            this.kind = kind == null ? Kind.DEATH : kind;
            this.detail = detail == null ? "" : detail;
        }

        /** A short human label, e.g. {@code Death}, {@code Kill streak 5}, {@code Combo 3}. */
        public String label() {
            switch (kind) {
                case DEATH:
                    return "Death";
                case KILL_STREAK:
                    return detail.isEmpty() ? "Kill streak" : "Kill streak " + detail;
                case COMBO:
                    return detail.isEmpty() ? "Combo" : "Combo " + detail;
                case JOIN:
                    return "Joined world";
                default:
                    return "Marker";
            }
        }
    }

    private final List<Marker> markers = new ArrayList<>();

    public static ReplayMarkers empty() {
        return new ReplayMarkers();
    }

    /**
     * Adds a marker, merging into an existing one of the same kind inside the window.
     *
     * <p>A negative time is clamped to zero (the clip's start). The later marker of a merged pair
     * wins the detail, so a combo that grew from 2 to 3 shows 3 rather than the first reading.
     *
     * @return true when a new marker was added (false when it merged into an existing one)
     */
    public boolean add(long tMs, Kind kind, String detail) {
        if (kind == null) return false;
        long time = Math.max(0L, tMs);
        for (int i = 0; i < markers.size(); i++) {
            Marker existing = markers.get(i);
            if (existing.kind == kind && Math.abs(existing.tMs - time) <= MERGE_WINDOW_MS) {
                markers.set(i, new Marker(existing.tMs, kind, detail));
                return false;
            }
        }
        Marker marker = new Marker(time, kind, detail);
        int insertAt = markers.size();
        for (int i = 0; i < markers.size(); i++) {
            if (markers.get(i).tMs > time) {
                insertAt = i;
                break;
            }
        }
        markers.add(insertAt, marker);
        return true;
    }

    /** The number of markers. */
    public int size() {
        return markers.size();
    }

    public boolean isEmpty() {
        return markers.isEmpty();
    }

    /** The marker at an index, or null when out of range. */
    public Marker at(int index) {
        if (index < 0 || index >= markers.size()) return null;
        return markers.get(index);
    }

    /** An unmodifiable view of the markers, in ascending time order. */
    public List<Marker> all() {
        return Collections.unmodifiableList(markers);
    }

    /** The first marker at or after {@code tMs}, or null when none is later. */
    public Marker next(long tMs) {
        for (Marker marker : markers) {
            if (marker.tMs >= tMs) return marker;
        }
        return null;
    }

    /** The last marker at or before {@code tMs}, or null when none is earlier. */
    public Marker previous(long tMs) {
        Marker found = null;
        for (Marker marker : markers) {
            if (marker.tMs <= tMs) {
                found = marker;
            } else {
                break;
            }
        }
        return found;
    }

    /**
     * Serialises the markers to one line for the sidecar.
     *
     * <p>Each marker is {@code tMs:kind:detail}; markers are joined with {@code ;} and the detail's
     * own separators are stripped on the way in, so the line stays parseable. An empty set
     * serialises to the empty string, which the sidecar stores as "no markers".
     */
    public String serialize() {
        StringBuilder builder = new StringBuilder(markers.size() * 16);
        for (int i = 0; i < markers.size(); i++) {
            if (i > 0) builder.append(';');
            Marker marker = markers.get(i);
            builder.append(marker.tMs).append(':').append(marker.kind.name()).append(':')
                    .append(sanitize(marker.detail));
        }
        return builder.toString();
    }

    /**
     * Parses a serialised marker line.
     *
     * <p>A malformed entry is skipped rather than failing the whole set, so one bad write cannot
     * make a clip's markers unusable.
     */
    public static ReplayMarkers parse(String text) {
        ReplayMarkers out = new ReplayMarkers();
        if (text == null || text.isEmpty()) return out;
        for (String raw : text.split(";")) {
            String entry = raw.trim();
            if (entry.isEmpty()) continue;
            int first = entry.indexOf(':');
            int second = first < 0 ? -1 : entry.indexOf(':', first + 1);
            if (first <= 0 || second < 0) continue;
            try {
                long tMs = Long.parseLong(entry.substring(0, first).trim());
                Kind kind = Kind.valueOf(entry.substring(first + 1, second).trim());
                String detail = entry.substring(second + 1).trim();
                out.markers.add(new Marker(tMs, kind, detail));
            } catch (IllegalArgumentException ignored) {
                // A corrupt entry is dropped; the rest of the set is still valid.
            }
        }
        Collections.sort(out.markers, (a, b) -> Long.compare(a.tMs, b.tMs));
        return out;
    }

    private static String sanitize(String detail) {
        if (detail == null) return "";
        StringBuilder builder = new StringBuilder(detail.length());
        for (int i = 0; i < detail.length(); i++) {
            char c = detail.charAt(i);
            if (c != ';' && c != ':' && c != '\n' && c != '\r') builder.append(c);
        }
        return builder.toString();
    }
}
