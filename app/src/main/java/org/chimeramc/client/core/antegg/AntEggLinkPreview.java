package org.chimeramc.client.core.antegg;

import java.util.Locale;

/**
 * Describes a linked {@code .AntEgg} before it is downloaded and installed (item 233).
 *
 * <p>Importing from a link is the one path where the package comes from a server the user did not
 * choose from a list, so the user is shown the source, the file name and the size before anything
 * is fetched, and the digest of what actually arrived after. Pure and Android-free so the wording
 * rules are unit-tested without a device.
 *
 * <p>A size the server did not declare is reported as unknown, never as {@code 0 B}: a zero would
 * read as an empty file and hide a response that simply omitted {@code Content-Length}.
 */
public final class AntEggLinkPreview {

    private AntEggLinkPreview() {
    }

    /**
     * A short human-readable size.
     *
     * @param bytes the size, or a negative value when it is not known
     */
    public static String formatSize(long bytes) {
        if (bytes < 0) return "unknown size";
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.ROOT, "%.1f MB", mb);
    }

    /** The file name a link implies, or the link itself when it names no file. */
    public static String displayName(String url) {
        String name = AntEggLink.fileNameOf(url);
        return name == null ? (url == null ? "" : url) : name;
    }

    /**
     * The confirmation text shown before the download starts.
     *
     * @param sizeBytes the declared size, or a negative value when the server did not say
     */
    public static String describe(String url, long sizeBytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Package: ").append(displayName(url)).append('\n');
        sb.append("Size: ").append(formatSize(sizeBytes)).append('\n');
        sb.append("Source: ").append(url == null ? "" : url);
        return sb.toString();
    }

    /** The line appended after the download so the user can compare the digest with the publisher's. */
    public static String describeDigest(String sha256) {
        if (sha256 == null || sha256.isEmpty()) return "";
        return "SHA-256: " + sha256;
    }
}
