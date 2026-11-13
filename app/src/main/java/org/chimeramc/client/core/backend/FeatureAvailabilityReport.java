package org.chimeramc.client.core.backend;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * The sweetloader's per-feature hook availability report, parsed for the Diagnostics screen.
 *
 * <p>The sweetloader answers "did this feature's hook install?" per feature and renders the whole set
 * as one JSON object, because "the sweetloader loaded" and "this feature works" are different
 * questions. This class turns that blob into rows a screen can show and a report can quote, so the
 * launcher never has to hard-code the sweetloader's feature list.
 *
 * <p><b>Fail-closed.</b> A missing blob, malformed JSON or an unexpected shape yields an empty
 * report whose {@link #summary()} says so, so a diagnostics row always prints something a bug report
 * can quote rather than throwing on the screen whose whole job is to explain a failure.
 *
 * <p>Android-free apart from {@code org.json} (which is the platform's own parser on device and the
 * same API in the unit-test runtime), so the parsing and the wording are pinned by
 * {@code FeatureAvailabilityReportTest} with no device.
 */
public final class FeatureAvailabilityReport {

    /** One feature's state, as reported by the sweetloader. */
    public static final class Entry {
        public final String feature;
        public final String state;
        public final String reason;

        Entry(String feature, String state, String reason) {
            this.feature = feature;
            this.state = state;
            this.reason = reason;
        }

        /** True when the feature's seam resolved and its hook installed. */
        public boolean isAvailable() {
            return "available".equals(state);
        }

        /** True when the feature is off because its seam did not resolve on this build. */
        public boolean isUnavailable() {
            return "unavailable".equals(state);
        }

        /** A "feature=state" fragment, with the reason in parentheses when one was reported. */
        public String describe() {
            if (reason == null || reason.isEmpty()) {
                return feature + "=" + state;
            }
            return feature + "=" + state + " (" + reason + ")";
        }
    }

    /**
     * The parsed entries, sorted by feature name. Never null.
     *
     * <p>Sorted rather than in the sweetloader's render order because {@code org.json}'s
     * {@code JSONObject} is a hash map: iteration order is not the insertion order, so relying on it
     * would make the diagnostics row and the copied report shuffle between runs. Alphabetical order
     * is stable and still groups the features readably.
     */
    public final List<Entry> entries;

    /** The raw blob, for a report that wants to print exactly what the sweetloader said. */
    public final String raw;

    private FeatureAvailabilityReport(List<Entry> entries, String raw) {
        this.entries = Collections.unmodifiableList(entries);
        this.raw = raw == null ? "" : raw;
    }

    /** An empty report: no sweetloader, or an unreadable blob. */
    public static FeatureAvailabilityReport empty() {
        return new FeatureAvailabilityReport(new ArrayList<>(), "");
    }

    /**
     * Parses the sweetloader's report blob.
     *
     * <p>The shape is {@code {"<feature>":{"state":"<state>","reason":"<text>"}},} with the reason
     * optional. Anything else (a null blob, a non-object, a value that is not an object) is treated
     * as an empty report rather than an error, because a diagnostic must not be the thing that
     * breaks the screen.
     */
    public static FeatureAvailabilityReport parse(String blob) {
        if (blob == null || blob.trim().isEmpty()) {
            return empty();
        }
        List<Entry> entries = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(blob);
            // JSONObject iterates in hash order, so collect the names and sort them for a stable
            // display order.
            TreeSet<String> names = new TreeSet<>();
            for (Iterator<String> it = root.keys(); it.hasNext(); ) {
                names.add(it.next());
            }
            for (String feature : names) {
                JSONObject value = root.optJSONObject(feature);
                if (value == null) continue;
                String state = value.optString("state", "unknown");
                String reason = value.has("reason") ? value.optString("reason", "") : "";
                entries.add(new Entry(feature, state, reason));
            }
        } catch (Throwable t) {
            // A malformed blob reads as "no report", not as a crash on the diagnostics screen.
            return empty();
        }
        return new FeatureAvailabilityReport(entries, blob);
    }

    /** True when there is nothing to report (no sweetloader, or an unreadable blob). */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** The number of features reported available. */
    public int availableCount() {
        int count = 0;
        for (Entry entry : entries) {
            if (entry.isAvailable()) count++;
        }
        return count;
    }

    /** The features reported unavailable, in report order. */
    public List<Entry> unavailable() {
        List<Entry> out = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.isUnavailable()) out.add(entry);
        }
        return out;
    }

    /**
     * A one-line summary for the Diagnostics row and the copied report.
     *
     * <p>When every reported feature is available it says so with the count. When some are not, it
     * names them and their reasons, because "3/7 features available" without the names is not
     * actionable. An empty report says the sweetloader did not answer, which is distinct from "every
     * feature is broken".
     */
    public String summary() {
        if (entries.isEmpty()) {
            return "no report from the sweetloader";
        }
        int available = availableCount();
        StringBuilder builder = new StringBuilder();
        builder.append(available).append('/').append(entries.size()).append(" features available");
        List<Entry> missing = unavailable();
        if (missing.isEmpty()) {
            return builder.toString();
        }
        builder.append("; unavailable: ");
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) builder.append("; ");
            builder.append(missing.get(i).describe());
        }
        return builder.toString();
    }

    /** Every entry, one per line, for the copied report. */
    public String describeAll() {
        if (entries.isEmpty()) {
            return "  (no report from the sweetloader)";
        }
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries) {
            builder.append("  - ").append(entry.describe()).append('\n');
        }
        // Drop the trailing newline so the caller controls the spacing.
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }
}
