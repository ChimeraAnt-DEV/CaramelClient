package org.chimeramc.client.launcher.core.content;

import java.io.File;
import java.util.Locale;

/**
 * The pure rules behind sharing a screenshot: what to call the shared item and which MIME type to
 * declare.
 *
 * <p>Android-free on purpose, so the naming and typing are unit-testable on the JVM; the
 * {@code ACTION_SEND} intent itself is assembled by {@link ScreenshotShareSheet}, which needs a
 * Context. This is the same split {@code ReplaySharing}/{@code ReplayShareSheet} uses, so the two
 * share paths cannot disagree about how a file is described.
 *
 * <p>The title is the file's name without its extension: a share sheet that shows
 * {@code Screenshot_2026-10-30-14-03-11.png} as the subject reads like a file dump.
 */
public final class ScreenshotSharing {

    /** The MIME type a launcher screenshot is shared as. */
    public static final String MIME_TYPE = "image/png";

    /** Shown when the screenshot has no usable name, so the subject is never empty. */
    public static final String FALLBACK_TITLE = "CaramelClient screenshot";

    private ScreenshotSharing() {
    }

    /**
     * The MIME type to declare for a screenshot file.
     *
     * <p>Bedrock writes {@code .png}, so that is the default. A {@code .jpg}/{@code .jpeg} is
     * declared as {@code image/jpeg} rather than always {@code image/png}, because a receiver that
     * trusts the declared type renders a JPEG as garbage; anything else falls back to the PNG type
     * rather than {@code *&#47;*}, which would let a renamed file be declared as something no
     * receiver can open.
     */
    public static String mimeType(File file) {
        String extension = extension(file == null ? null : file.getName());
        if ("jpg".equals(extension) || "jpeg".equals(extension)) return "image/jpeg";
        return MIME_TYPE;
    }

    /** A human-readable share subject: the file name without its extension. */
    public static String shareTitle(File file) {
        if (file == null) return FALLBACK_TITLE;
        String name = file.getName();
        if (name == null || name.trim().isEmpty()) return FALLBACK_TITLE;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return base.trim().isEmpty() ? FALLBACK_TITLE : base.trim();
    }

    /** True when a file looks like a screenshot worth sharing (exists and is a regular file). */
    public static boolean isShareable(File file) {
        return file != null && file.isFile();
    }

    /** The extension of a file name, lower-cased and without the dot, or "" when there is none. */
    public static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
