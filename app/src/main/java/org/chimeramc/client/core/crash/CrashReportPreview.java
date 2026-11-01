package org.chimeramc.client.core.crash;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes exactly what a crash report upload contains, before it is sent (item 518).
 *
 * <p>The Settings switch is a consent, and consent is only meaningful if the user can see what they
 * are consenting to. This class builds that description from the same values the uploader sends —
 * the headers and the report body — so the preview cannot drift from the request. It is pure and
 * Android-free, so the rules are unit-tested without a device.
 *
 * <p>Two rules matter and are pinned by {@code CrashReportPreviewTest}:
 * <ul>
 *   <li>the body shown is truncated to the same cap the store reads, so the preview never claims to
 *       be smaller than what is sent;</li>
 *   <li>a missing report still produces a preview that says so, rather than an empty dialog that
 *       looks like nothing is being sent.</li>
 * </ul>
 */
public final class CrashReportPreview {

    /** How much of the report body the preview shows. Short enough to read in a dialog. */
    public static final int PREVIEW_CHARS = 4000;

    private CrashReportPreview() {
    }

    /** One header the upload carries, name and value, for display. */
    public static final class Header {
        public final String name;
        public final String value;

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * The headers the uploader attaches.
     *
     * <p>These are the only identifying values sent: the app version and the report's own file name
     * and truncation flag. No account id, gamertag or device id is included, and that is a property
     * of the uploader this preview reflects rather than a promise made separately.
     */
    public static List<Header> headers(String appVersion, String reportName, boolean truncated) {
        List<Header> headers = new ArrayList<>();
        headers.add(new Header("X-Crash-App-Version", appVersion == null ? "" : appVersion));
        headers.add(new Header("X-Crash-Report-Name", reportName == null ? "" : reportName));
        headers.add(new Header("X-Crash-Report-Truncated", String.valueOf(truncated)));
        return headers;
    }

    /** The body shown for a report, truncated to {@link #PREVIEW_CHARS}. */
    public static String body(String reportText) {
        if (reportText == null || reportText.isEmpty()) {
            return "(no crash report is saved on this device right now)";
        }
        if (reportText.length() <= PREVIEW_CHARS) return reportText;
        return reportText.substring(0, PREVIEW_CHARS) + "\n… (truncated for this preview)";
    }

    /**
     * The full preview text: a sentence, the headers, then the body.
     *
     * @param endpoint the collector address the report would go to, or empty when none is configured
     */
    public static String describe(String endpoint, String appVersion, String reportName,
                                  boolean truncated, String reportText) {
        StringBuilder sb = new StringBuilder();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            sb.append("No collector is configured in this build, so nothing can be sent.\n\n");
        } else {
            sb.append("A crash report would be sent to:\n").append(endpoint.trim()).append("\n\n");
        }
        sb.append("Headers:\n");
        for (Header header : headers(appVersion, reportName, truncated)) {
            sb.append("  ").append(header.name).append(": ").append(header.value).append('\n');
        }
        sb.append("\nBody:\n").append(body(reportText));
        return sb.toString();
    }
}
