package org.chimeramc.client.core.minecraft;

/**
 * Which multiplayer transport a Bedrock build speaks, decided by its version code.
 *
 * <p>Bedrock 26.60 removed RakNet from the client: joining is NetherNet only (WebRTC, with a
 * signaling step first). Everything in the launcher that assumed a RakNet/UDP peer has to know
 * which world it is in, so the rule lives here once — pure, Android-free, unit-testable — instead
 * of as scattered version comparisons. Callers branch on {@link #forVersion(String)}.
 *
 * <p>Two version-code spellings are in use in this repo: the four-part form the extracted build
 * reports ({@code 1.26.60.28}) and the shorter form the download sites print ({@code 26.60.28}).
 * {@link #canonicalVersion(String)} folds the second into the first before comparing, the same way
 * {@code PreloaderRules.normalizeVersionParts} treats a leading {@code 1.} as optional.
 *
 * <p>The boundary is the release itself, so there is no separate beta target: a pre-release of
 * 26.60 ({@code 1.26.60.20-beta}) already ships NetherNet, and comparing it against
 * {@code 1.26.60} puts it on the new path, while a pre-release of an older line stays on RakNet.
 * Anything unparseable or null answers {@link Transport#RAKNET}: the old path is the one that has
 * shipped for years, so a version string this class cannot read must not be able to move a user
 * onto the new one.
 */
public final class BedrockTransport {

    /** Which transport carries a session on a given build. */
    public enum Transport {
        /** RakNet/UDP, every build before 26.60. */
        RAKNET,
        /** NetherNet (WebRTC over a signaling step), 26.60 and later. */
        NETHERNET,
    }

    /** The first build that ships NetherNet only, in the four-part form. */
    public static final String NETHERNET_RELEASE = "1.26.60";

    private BedrockTransport() {
    }

    /**
     * The transport for {@code versionCode}, or {@link Transport#RAKNET} when the code cannot be
     * read.
     *
     * @param versionCode an instance's version code in either spelling, with or without a channel
     *     suffix ({@code -beta}, {@code _RC3}); may be null
     * @return the transport to use for that build
     */
    public static Transport forVersion(String versionCode) {
        String canonical = canonicalVersion(versionCode);
        if (canonical.isEmpty()) return Transport.RAKNET;
        return NativeLibraryVersionGate.isVersionAtLeast(canonical, NETHERNET_RELEASE)
                ? Transport.NETHERNET
                : Transport.RAKNET;
    }

    /** Whether {@code versionCode} is a 26.60-or-later build. */
    public static boolean isNetherNet(String versionCode) {
        return forVersion(versionCode) == Transport.NETHERNET;
    }

    /**
     * The four-part form of a version code, or {@code ""} when the input is not a version.
     *
     * <p>{@code "26.60.28"} becomes {@code "1.26.60.28"}; {@code "1.26.60.28"} is unchanged; a
     * channel suffix is dropped; a single-part or non-numeric value yields {@code ""}.
     */
    static String canonicalVersion(String versionCode) {
        String digits = VersionCodeNormalizer.normalize(versionCode);
        if (digits.isEmpty()) return "";
        String[] parts = digits.split("\\.");
        if (parts.length < 2) return "";
        // A two-digit major ("26.60") is the same build as "1.26.60"; a real leading 1 is one digit.
        if (parts[0].length() >= 2 && parsePart(parts[0]) >= 20) {
            return "1." + digits;
        }
        return digits;
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** True when {@code versionCode} names a beta channel build, as the library gate reads it. */
    public static boolean isBeta(String versionCode) {
        return NativeLibraryVersionGate.isBeta(versionCode);
    }
}
