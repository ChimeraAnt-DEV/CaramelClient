package org.chimeramc.client.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * The one place a URL is opened.
 *
 * <p>{@code ACTION_VIEW} with only a URI is an implicit intent, which lint flags
 * ({@code UnsafeImplicitIntentLaunch}) because another app on the device could claim the scheme. For
 * a user-facing link that is the intended behaviour — the user chooses their browser — but the
 * exception should be deliberate and in one place rather than scattered, and every call site should
 * report a failure rather than crash when no app can handle it.
 *
 * <p>Keeping it here also means the {@code https} scheme is enforced: a link from a feed or a
 * config file cannot open a {@code file:} or {@code javascript:} URI.
 */
public final class UrlOpener {

    private UrlOpener() {
    }

    /** True when the URL is an https link this app is willing to open. */
    public static boolean isAllowed(String url) {
        return url != null && url.startsWith("https://");
    }

    /**
     * Opens an https URL, or returns false when it is not an allowed link or no app can handle it.
     *
     * <p>Only {@code https} is accepted, so a link from an external source cannot ask the system to
     * open a local file or a script URI.
     *
     * <p>{@code UnsafeImplicitIntentLaunch} is suppressed here on purpose and only here: a link is
     * meant to be handled by whichever app the user has chosen for https, so making it explicit
     * would defeat the point. Lint's warning is about a URI whose target could be hijacked, and the
     * scheme check above plus the single call site are what make it safe. Every other URL open in
     * the app routes through this method, so the exception is auditable in one place.
     */
    @SuppressLint("UnsafeImplicitIntentLaunch")
    public static boolean open(Context context, String url) {
        if (context == null || !isAllowed(url)) return false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // No package is set: the user's chosen browser is the point of a link. The scheme is
            // restricted above, which is the part that matters for safety.
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            AppLog.warnOnce("UrlOpener", "open:" + url,
                    "Could not open the link", t);
            return false;
        }
    }
}
