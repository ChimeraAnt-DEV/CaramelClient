package org.chimeramc.client.launcher.util;

import java.util.Locale;

/**
 * Builds the one-line build identity shown on the About screen and copied into a report.
 *
 * <p>Pure and Android-free, so the formatting — the part that silently drops the commit or prints
 * "null" — is unit-tested without a device. The About screen and the diagnostics report both read
 * it, so the two cannot describe the same build differently.
 */
public final class BuildSummary {

    private BuildSummary() {
    }

    /**
     * Formats the build identity.
     *
     * @param versionName the version name, e.g. {@code BETA3}
     * @param versionCode the monotonically increasing version code
     * @param commit      the short commit hash, or null/blank when unknown
     * @param buildType   the build type, e.g. {@code release}
     * @param beta        whether this is the beta channel
     */
    public static String describe(String versionName, long versionCode, String commit,
                                  String buildType, boolean beta) {
        String version = isBlank(versionName) ? "unknown" : versionName.trim();
        String hash = isBlank(commit) ? "unknown" : commit.trim();
        String type = isBlank(buildType) ? "unknown" : buildType.trim();

        StringBuilder sb = new StringBuilder();
        sb.append(version).append(" (").append(versionCode).append(')');
        sb.append(" · ").append(hash);
        sb.append(" · ").append(type);
        if (beta) {
            sb.append(" · beta");
        }
        return sb.toString();
    }

    /**
     * The native-library status line: the sweetloader ABI the client expects and the one it found.
     *
     * <p>Reports the actual numbers rather than only "ok"/"not ok", because "which ABI did it load"
     * is the question a report needs answered, and a bare "mismatch" cannot distinguish a missing
     * library (version 0) from a genuinely different revision.
     */
    public static String nativeStatus(int expectedAbi, int reportedAbi) {
        if (reportedAbi <= 0) {
            return String.format(Locale.US,
                    "native: unavailable (expected ABI %d)", expectedAbi);
        }
        if (expectedAbi == reportedAbi) {
            return String.format(Locale.US, "native: ABI %d (matches)", reportedAbi);
        }
        return String.format(Locale.US,
                "native: ABI %d (client expects %d)", reportedAbi, expectedAbi);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
