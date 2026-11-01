package org.chimeramc.client.util;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the extras of an intent that came from outside the app (item 178).
 *
 * <p>{@code IntentHandler} is an exported activity with a {@code VIEW} filter, so any app on the
 * device — or a web page via {@code minecraft://} — can hand it an intent. Every extra it reads is
 * therefore attacker-controlled input: a multi-megabyte string would be copied into the process and
 * then into a log line, a {@code file://} uri would let another app name a path this app then opens,
 * and a nested intent or bundle would smuggle a component this app never agreed to start.
 *
 * <p>The rules are pure and Android-light, so they are unit-tested without a device:
 * <ul>
 *   <li>a string extra longer than {@link #MAX_EXTRA_CHARS} is refused rather than truncated — a
 *       silently shortened deep link would resolve to the wrong thing;</li>
 *   <li>only {@code http}, {@code https}, {@code content} and {@code minecraft} uris are accepted;
 *       a {@code file://} or {@code javascript:} uri is not something an external app should be able
 *       to point this launcher at;</li>
 *   <li>a value that is not one of the expected types reads as absent, so a caller that sent an
 *       {@code int} where a string was expected gets {@code null} instead of a {@code ClassCastException}
 *       taking down the activity.</li>
 * </ul>
 */
public final class IntentSanitizer {

    /** The longest string extra this app will accept. A deep link or a share body is far shorter. */
    public static final int MAX_EXTRA_CHARS = 8 * 1024;

    /** The schemes an externally-supplied uri may use. */
    private static final String[] ALLOWED_SCHEMES = {"http", "https", "content", "minecraft"};

    private IntentSanitizer() {
    }

    /**
     * Reads a string extra, refusing an over-long value.
     *
     * @return the value, or {@code null} when absent or too long.
     */
    @Nullable
    public static String stringExtra(Intent intent, String name) {
        if (intent == null || name == null) return null;
        String value;
        try {
            value = intent.getStringExtra(name);
        } catch (Throwable t) {
            // An extra stored with the wrong type throws; treat it as absent rather than crashing.
            AppLog.warn("IntentSanitizer", "extra " + name + " had an unexpected type", t);
            return null;
        }
        if (value == null) return null;
        if (value.length() > MAX_EXTRA_CHARS) {
            AppLog.warn("IntentSanitizer",
                    "refused an over-long " + name + " extra (" + value.length() + " chars)");
            return null;
        }
        return value;
    }

    /** Reads a boolean extra, returning {@code fallback} when it is absent or the wrong type. */
    public static boolean booleanExtra(Intent intent, String name, boolean fallback) {
        if (intent == null || name == null) return fallback;
        try {
            return intent.getBooleanExtra(name, fallback);
        } catch (Throwable t) {
            return fallback;
        }
    }

    /**
     * Reads a uri extra, accepting only a scheme this app is willing to be pointed at.
     *
     * <p>A {@code file://} uri is deliberately refused: an external app that can name a path makes
     * the launcher read or open a file it never chose, and the launcher's own import path already
     * takes {@code content://} uris from the document picker.
     */
    @Nullable
    public static Uri safeUri(Intent intent, String name) {
        if (intent == null || name == null) return null;
        Uri uri;
        try {
            uri = intent.getParcelableExtra(name);
        } catch (Throwable t) {
            return null;
        }
        return sanitizeUri(uri);
    }

    /** The uri check, split out so it is testable without an Intent. */
    @Nullable
    public static Uri sanitizeUri(@Nullable Uri uri) {
        if (uri == null) return null;
        String scheme = uri.getScheme();
        if (scheme == null) return null;
        for (String allowed : ALLOWED_SCHEMES) {
            if (allowed.equalsIgnoreCase(scheme)) return uri;
        }
        AppLog.warn("IntentSanitizer", "refused a uri with scheme " + scheme);
        return null;
    }

    /** True when the scheme is one this app accepts from an external caller. */
    public static boolean isAllowedScheme(@Nullable String scheme) {
        if (scheme == null) return false;
        for (String allowed : ALLOWED_SCHEMES) {
            if (allowed.equalsIgnoreCase(scheme)) return true;
        }
        return false;
    }

    /**
     * Keeps only the uris whose scheme this app accepts.
     *
     * <p>Used on the {@code EXTRA_STREAM} list of a share, where one bad entry must not stop the
     * good ones from importing.
     */
    public static List<Uri> filterAllowed(List<Uri> uris) {
        List<Uri> out = new ArrayList<>();
        if (uris == null) return out;
        for (Uri uri : uris) {
            if (sanitizeUri(uri) != null) out.add(uri);
        }
        return out;
    }
}
