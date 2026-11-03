package org.chimeramc.client.core.backend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The preloader's hook self-test report, parsed for the Diagnostics screen.
 *
 * <p>The self-test installs and removes every hook site once, so its report answers "can these hooks
 * install on this build at all" — a different question from the per-feature availability report,
 * which describes the state the real install produced. The two are shown as separate rows because a
 * feature can be off either because its hook could not install or because the user turned it off.
 *
 * <p><b>Fail-closed.</b> A missing or malformed blob yields an empty report whose {@link #summary()}
 * says so, so the row always prints something a bug report can quote.
 *
 * <p>Android-free apart from {@code org.json}, so the parsing and the wording are pinned by
 * {@code HookSelfTestReportTest} with no device.
 */
public final class HookSelfTestReport {

    /** One hook site's round-trip outcome. */
    public static final class Site {
        public final String name;
        public final String state;
        public final String detail;

        Site(String name, String state, String detail) {
            this.name = name;
            this.state = state;
            this.detail = detail;
        }

        /** True when the site resolved and round-tripped. */
        public boolean isOk() {
            return "ok".equals(state);
        }

        /** True when the site's target did not resolve on this build. */
        public boolean isUnresolved() {
            return "unresolved".equals(state);
        }

        /** True when the site resolved but could not round-trip. */
        public boolean isFailed() {
            return !isOk() && !isUnresolved();
        }

        /** A "name=state" fragment, with the detail in parentheses when one was reported. */
        public String describe() {
            if (detail == null || detail.isEmpty()) {
                return name + "=" + state;
            }
            return name + "=" + state + " (" + detail + ")";
        }
    }

    /** True when the backend round-tripped its own control; false means no hook can install. */
    public final boolean backendOk;
    /** The sites, sorted by name. Never null. */
    public final List<Site> sites;
    /** The number of sites the preloader reported passing. */
    public final int reportedPassed;
    /** The number of sites the preloader reported in total. */
    public final int reportedTotal;
    /** The raw blob, for a report that wants to print exactly what the preloader said. */
    public final String raw;

    private HookSelfTestReport(boolean backendOk, List<Site> sites, int reportedPassed,
                               int reportedTotal, String raw) {
        this.backendOk = backendOk;
        this.sites = Collections.unmodifiableList(sites);
        this.reportedPassed = reportedPassed;
        this.reportedTotal = reportedTotal;
        this.raw = raw == null ? "" : raw;
    }

    /** An empty report: no preloader, or an unreadable blob. */
    public static HookSelfTestReport empty() {
        return new HookSelfTestReport(false, new ArrayList<>(), 0, 0, "");
    }

    /**
     * Parses the preloader's self-test blob.
     *
     * <p>Shape: {@code {"backend_ok":<bool>,"passed":N,"total":M,"sites":[{"name":"...",
     * "state":"...","detail":"..."}]}}. Anything else is treated as an empty report rather than an
     * error, because a diagnostic must not be the thing that breaks the screen.
     */
    public static HookSelfTestReport parse(String blob) {
        if (blob == null || blob.trim().isEmpty()) {
            return empty();
        }
        try {
            JSONObject root = new JSONObject(blob);
            boolean backendOk = root.optBoolean("backend_ok", false);
            int passed = root.optInt("passed", 0);
            int total = root.optInt("total", 0);
            List<Site> sites = new ArrayList<>();
            JSONArray array = root.optJSONArray("sites");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject entry = array.optJSONObject(i);
                    if (entry == null) continue;
                    String name = entry.optString("name", "");
                    String state = entry.optString("state", "unknown");
                    String detail = entry.has("detail") ? entry.optString("detail", "") : "";
                    sites.add(new Site(name, state, detail));
                }
            }
            // The preloader renders sites in resolution order; sort by name for a stable display,
            // matching the availability report's rule so the two rows do not shuffle between runs.
            Collections.sort(sites, (a, b) -> a.name.compareTo(b.name));
            return new HookSelfTestReport(backendOk, sites, passed, total, blob);
        } catch (Throwable t) {
            return empty();
        }
    }

    /** True when there is nothing to report. */
    public boolean isEmpty() {
        return sites.isEmpty() && !backendOk;
    }

    /** The sites that resolved but could not round-trip. */
    public List<Site> failed() {
        List<Site> out = new ArrayList<>();
        for (Site site : sites) {
            if (site.isFailed()) out.add(site);
        }
        return out;
    }

    /**
     * A one-line summary for the Diagnostics row and the copied report.
     *
     * <p>A broken backend dominates: if the control round-trip failed, the per-site results are
     * meaningless, so the summary says that rather than reporting a misleading count.
     */
    public String summary() {
        if (isEmpty()) {
            return "no self-test from the preloader";
        }
        if (!backendOk) {
            return "hook backend self-test FAILED; no hook can install on this build";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(reportedPassed).append('/').append(reportedTotal)
                .append(" hook sites round-tripped");
        List<Site> missing = failed();
        if (missing.isEmpty()) {
            return builder.toString();
        }
        builder.append("; failed: ");
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) builder.append("; ");
            builder.append(missing.get(i).describe());
        }
        return builder.toString();
    }

    /** Every site, one per line, for the copied report. */
    public String describeAll() {
        if (isEmpty()) {
            return "  (no self-test from the preloader)";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("  backend: ").append(backendOk ? "ok" : "FAILED").append('\n');
        for (Site site : sites) {
            builder.append("  - ").append(site.describe()).append('\n');
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }
}
