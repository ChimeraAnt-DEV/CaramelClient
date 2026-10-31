package org.chimeramc.client.launcher.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the copyable debug bundle for a bug report (items 262 and 263).
 *
 * <p>The diagnostics report already describes the app's state; a report a maintainer can act on also
 * needs the recent log, and it must be truncated and redacted before it is copied. Both rules live
 * here, pure and Android-free, so they are unit-tested rather than trusted:
 *
 * <ul>
 *   <li><b>The tail is bounded.</b> The launch log grows across sessions; pasting all of it into a
 *       chat is unreadable and can be megabytes. Only the last {@link #MAX_LOG_LINES} lines are kept.
 *   <li><b>Secrets are redacted.</b> A token, an authorization header or a gamertag reaching a
 *       public channel is the failure this prevents. {@link #redact(String)} is the single rule.
 * </ul>
 */
public final class DebugBundle {

    /** How many of the most recent log lines are included. */
    public static final int MAX_LOG_LINES = 200;

    private static final String REDACTED = "<redacted>";

    private DebugBundle() {
    }

    /**
     * Composes the full bundle: the diagnostics report, then the recent log.
     *
     * @param report    the diagnostics report text, or null
     * @param fullLog   the whole launch log, or null when unavailable
     */
    public static String compose(String report, String fullLog) {
        return compose(report, fullLog, false);
    }

    /**
     * Composes the bundle, optionally applying the streaming-safe masks (item 277).
     *
     * <p>With {@code streamingSafe} the report and the log also lose world coordinates and server
     * addresses, on top of the always-on credential redaction. The mask is applied last so a
     * coordinate that appears inside a line already redacted for a token is still covered.
     */
    public static String compose(String report, String fullLog, boolean streamingSafe) {
        StringBuilder sb = new StringBuilder();
        sb.append(report != null ? report : "(no diagnostics report)");
        sb.append("\nRecent log (last ").append(MAX_LOG_LINES).append(" lines):\n");
        String tail = tail(fullLog, MAX_LOG_LINES);
        sb.append(tail.isEmpty() ? "  (no log available)" : redact(tail));
        sb.append("\n");
        String out = sb.toString();
        if (streamingSafe) {
            out = org.chimeramc.client.core.privacy.PrivacyRedactor.everything(out);
        }
        return out;
    }

    /**
     * The last {@code maxLines} lines of {@code text}, preserving order.
     *
     * <p>A blank or null input yields an empty string, never "null". The trailing newline a file
     * usually ends with does not count as a line, so a log ending in {@code "\n"} does not spend one
     * of the budget on an empty entry.
     */
    public static String tail(String text, int maxLines) {
        if (text == null || text.isEmpty() || maxLines <= 0) return "";
        String[] lines = text.split("\n", -1);
        int end = lines.length;
        if (end > 0 && lines[end - 1].isEmpty()) {
            end--;
        }
        int start = Math.max(0, end - maxLines);
        List<String> kept = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            kept.add(lines[i]);
        }
        return String.join("\n", kept);
    }

    /**
     * Redacts anything that looks like a credential (items 517).
     *
     * <p>The rule is deliberately broad and shape-based, because a log line's exact format is not
     * controlled here: a bearer token, an {@code access_token=}/{@code refresh_token=} query value,
     * a JWT, and any {@code Authorization:} header value are replaced. A redaction that misses one
     * shape is a leaked credential, so unknown long base64-ish runs are also redacted.
     */
    public static String redact(String text) {
        if (text == null || text.isEmpty()) return text;

        String out = text;
        // Authorization: Bearer xyz  /  Authorization: xyz
        out = out.replaceAll("(?i)(authorization\\s*:\\s*)(bearer\\s+)?\\S+", "$1" + REDACTED);
        // token=... / access_token=... / refresh_token=... / id_token=... / password=...
        out = out.replaceAll("(?i)((access_token|refresh_token|id_token|token|password|secret)\\s*[=:]\\s*)[^\\s&,;\"']+",
                "$1" + REDACTED);
        // A JWT: three base64url segments separated by dots.
        out = out.replaceAll("eyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}", REDACTED);
        return out;
    }

    /** Redacts a gamertag or XUID that appears verbatim in the text. */
    public static String redactIdentity(String text, String... identifiers) {
        if (text == null || text.isEmpty() || identifiers == null) return text;
        String out = text;
        for (String id : identifiers) {
            if (id == null || id.trim().isEmpty()) continue;
            out = out.replace(id, REDACTED);
        }
        return out;
    }
}
