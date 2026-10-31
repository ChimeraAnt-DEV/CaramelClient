package org.chimeramc.client.core.privacy;

/**
 * Masks the three things streaming-safe mode must not show: a world position, a gamertag and a
 * server address (item 277).
 *
 * <p>Pure and Android-free, so every rule is unit-tested. The shapes it masks are deliberately
 * conservative: a coordinate is only masked when it looks like three numbers, so an unrelated line
 * is not mangled, and an address is only masked when it has a host and a port, so a plain word is
 * left alone. A mask that hides the wrong thing is its own bug in a support log.
 */
public final class PrivacyRedactor {

    /** What a masked value reads as. Deliberately not a plausible value, so it is obviously a mask. */
    public static final String MASK = "[hidden]";
    /** A masked server address reads as the same marker; kept named for callers that read by intent. */
    public static final String ADDRESS_MASK = MASK;

    private PrivacyRedactor() {
    }

    /**
     * Masks a gamertag wherever it appears in {@code text}.
     *
     * <p>Reuses {@link org.chimeramc.client.launcher.util.DebugBundle#redactIdentity} rather than a
     * second implementation, so a name is masked the same way in the debug bundle and here.
     */
    public static String identity(String text, String... names) {
        return org.chimeramc.client.launcher.util.DebugBundle.redactIdentity(text, names);
    }

    /**
     * Masks a "X: 12.5 Y: 64.0 Z: -30.2" style reading, and a bare "12.5, 64.0, -30.2" triple.
     *
     * @return the masked text, or the input unchanged when no coordinate reading is present.
     */
    public static String coordinates(String text) {
        if (text == null || text.isEmpty()) return text;
        // A labelled single axis ("X: 12.5"). The word boundary stops "Memory: 512" matching the
        // "y: 512" inside it, which is the failure mode that made this mask eat unrelated numbers.
        String out = text.replaceAll("(?i)\\b([XYZ])\\s*:\\s*-?\\d+(?:\\.\\d+)?", "$1: " + MASK);
        // A bare triple separated by commas or slashes, which is how a coordinate reaches a log.
        out = out.replaceAll(
                "-?\\d+(?:\\.\\d+)?(?:\\s*[,/]\\s*-?\\d+(?:\\.\\d+)?){2}", MASK);
        return out;
    }

    /**
     * Masks a {@code host:port} address, keeping nothing but a mask.
     *
     * <p>A hostname or an IPv4 address followed by a port is what a server list shows; the port
     * alone is not identifying, so the mask is a whole replacement rather than a partial one. An
     * address with no port is left alone, because that is more likely to be prose than a server.
     */
    public static String serverAddress(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replaceAll(
                "(?<!\\d)(?:\\d{1,3}(?:\\.\\d{1,3}){3}|[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?):\\d{1,5}",
                ADDRESS_MASK);
    }

    /**
     * Applies the identity, coordinate and address masks in that order.
     *
     * <p>The single entry point a caller that exports text should use, so a new export path cannot
     * apply only one of the three.
     */
    public static String everything(String text, String... names) {
        if (text == null || text.isEmpty()) return text;
        return serverAddress(coordinates(identity(text, names)));
    }
}
