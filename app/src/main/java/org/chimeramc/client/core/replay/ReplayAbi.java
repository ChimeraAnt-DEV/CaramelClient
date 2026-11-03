package org.chimeramc.client.core.replay;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The client's mirror of BerryAPI's replay C ABI status values and its JSON status line.
 *
 * <p>BerryAPI's replay surface is reached through a C struct of function pointers
 * ({@code berry/replay_c.h}); the client cannot include that header, so it mirrors the status enum
 * and parses the {@code status()} string. Keeping the mirror pure means the mapping is unit-tested
 * without the native library, which is what makes an ABI drift visible in CI rather than on device.
 *
 * <p>The status enum values are copied from the header; {@link #abiVersion()} is the
 * {@code BERRY_REPLAY_ABI_VERSION} this mirror was written against. A library reporting a different
 * version is refused rather than driven, so a layout change cannot be read as a valid call.
 */
public final class ReplayAbi {

    private ReplayAbi() {
    }

    /** The {@code BERRY_REPLAY_ABI_VERSION} this mirror targets. */
    public static final int ABI_VERSION = 1;

    // Status codes, mirroring BerryReplayStatus.
    public static final int OK = 0;
    public static final int NOT_ATTACHED = 1;
    public static final int BAD_REPLAY = 2;
    public static final int BAD_ARGUMENT = 3;
    public static final int NOT_LOADED = 4;

    /** A human-readable name for a status code, mirroring {@code BERRY_ReplayStatusName}. */
    public static String statusName(int status) {
        switch (status) {
            case OK: return "ok";
            case NOT_ATTACHED: return "not attached";
            case BAD_REPLAY: return "bad replay";
            case BAD_ARGUMENT: return "bad argument";
            case NOT_LOADED: return "no replay loaded";
            default: return "unknown";
        }
    }

    /**
     * Whether a library's reported ABI version and struct size are usable by this mirror.
     *
     * <p>The size is checked against the value the library itself reports
     * ({@code BERRY_ReplayStructSize}) rather than a hard-coded number, because the client cannot know
     * the C++ layout; the version is what pins the call semantics. A mismatch is refused, so a drift
     * is a clean refusal instead of a misread function pointer.
     */
    public static boolean isCompatible(int reportedAbiVersion, int reportedStructSize) {
        return reportedAbiVersion == ABI_VERSION && reportedStructSize > 0;
    }

    /** A parsed {@code status()} line: the fields the panel shows. */
    public static final class Status {
        public final boolean loaded;
        public final long firstTick;
        public final long lastTick;
        public final int sections;
        public final int records;
        public final long payloadBytes;
        public final String raw;

        Status(boolean loaded, long firstTick, long lastTick, int sections, int records,
               long payloadBytes, String raw) {
            this.loaded = loaded;
            this.firstTick = firstTick;
            this.lastTick = lastTick;
            this.sections = sections;
            this.records = records;
            this.payloadBytes = payloadBytes;
            this.raw = raw;
        }

        /** The clip length in ticks, or 0 when empty. */
        public long tickLength() {
            return loaded && lastTick >= firstTick ? lastTick - firstTick : 0L;
        }
    }

    /**
     * Parses the {@code status()} line.
     *
     * <p>BerryAPI's line is human-readable rather than a strict format, so this reads what it can and
     * never throws: a line that does not parse yields a not-loaded status carrying the raw text, which
     * the panel shows verbatim. That is deliberate — a status line is a diagnostic, and dropping it
     * because one field moved would hide the very thing it exists to show.
     */
    public static Status parseStatus(String raw) {
        if (raw == null) return new Status(false, 0, 0, 0, 0, 0, "");
        String s = raw.trim();
        boolean loaded = s.contains("loaded") && !s.contains("not loaded");
        return new Status(loaded,
                numberAfter(s, "first="),
                numberAfter(s, "last="),
                (int) numberAfter(s, "sections="),
                (int) numberAfter(s, "records="),
                numberAfter(s, "bytes="),
                s);
    }

    /** The integer following {@code key} in {@code s}, or 0 when absent or unparseable. */
    private static long numberAfter(String s, String key) {
        int i = s.indexOf(key);
        if (i < 0) return 0L;
        int start = i + key.length();
        int end = start;
        while (end < s.length() && (Character.isDigit(s.charAt(end)) || s.charAt(end) == '-')) end++;
        if (end == start) return 0L;
        try {
            return Long.parseLong(s.substring(start, end));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Parses a replay summary the client reads from the library's {@code info} call, given as JSON.
     *
     * <p>Exposed for the Java bridge, which marshals the C struct to JSON before crossing into
     * managed code; keeping the parse here makes it testable without the library.
     */
    public static Status parseInfoJson(String json) {
        if (json == null || json.isEmpty()) return new Status(false, 0, 0, 0, 0, 0, "");
        try {
            JSONObject o = new JSONObject(json);
            boolean loaded = o.optInt("loaded", 0) == 1;
            return new Status(loaded,
                    o.optLong("first_tick", 0L),
                    o.optLong("last_tick", 0L),
                    o.optInt("section_count", 0),
                    o.optInt("record_count", 0),
                    o.optLong("payload_bytes", 0L),
                    json);
        } catch (JSONException e) {
            return new Status(false, 0, 0, 0, 0, 0, json);
        }
    }

    /** The marker the panel uses to say the library is absent or too old. */
    public static String unavailableReason(boolean libraryPresent, int reportedAbiVersion) {
        if (!libraryPresent) {
            return "BerryAPI is not present, so its replay system cannot be used.";
        }
        if (reportedAbiVersion != ABI_VERSION) {
            return "BerryAPI reports replay ABI v" + reportedAbiVersion
                    + "; this client expects v" + ABI_VERSION + ".";
        }
        return "";
    }

    /** A JSON array of the fields the panel needs, for a debug report. */
    public static String toJson(Status status) {
        try {
            JSONObject o = new JSONObject();
            o.put("loaded", status.loaded ? 1 : 0);
            o.put("first_tick", status.firstTick);
            o.put("last_tick", status.lastTick);
            o.put("section_count", status.sections);
            o.put("record_count", status.records);
            o.put("payload_bytes", status.payloadBytes);
            return o.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }
}
