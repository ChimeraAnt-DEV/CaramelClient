package org.chimeramc.client.util;

import android.app.Activity;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

/**
 * Inflates a heavy layout off the main thread (item 130).
 *
 * <p>The Settings screen is over two thousand lines of XML; inflating it on the main thread blocks
 * the first frame for the whole parse and view-construction pass. {@link AsyncLayoutInflater} moves
 * that to a background thread and calls back on the main thread with the built hierarchy, so the
 * window can draw its background first.
 *
 * <p>Two rules keep it from becoming a source of crashes:
 * <ul>
 *   <li>The callback is always invoked on the main thread, exactly once, and a failure falls back
 *       to a synchronous inflate rather than leaving the screen empty. A layout that
 *       {@code AsyncLayoutInflater} cannot build (a {@code <fragment>}, or a view without a
 *       two-argument constructor) therefore still works — just without the head start.</li>
 *   <li>The host is checked for destruction before the result is applied. The inflate can outlive a
 *       screen the user already backed out of, and attaching a view to a finished Activity is the
 *       leak this would otherwise introduce.</li>
 * </ul>
 */
public final class AsyncInflate {

    /** What the caller does with the built view. Runs on the main thread. */
    public interface Callback {
        void onInflated(View view);
    }

    private AsyncInflate() {
    }

    /**
     * Builds {@code layoutRes} on a background thread and delivers it to {@code callback} on the
     * main thread.
     *
     * <p>{@code activity} is used to create the inflater; if it is finishing or destroyed when the
     * inflate completes, the callback is not invoked at all, so the caller never attaches to a dead
     * window.
     */
    public static void inflate(@NonNull Activity activity, @LayoutRes int layoutRes,
                               @Nullable Callback callback) {
        if (activity == null || callback == null) return;
        final AsyncLayoutInflater inflater = new AsyncLayoutInflater(activity);
        inflater.inflate(layoutRes, null, (view, resId, parent) -> {
            if (isGone(activity)) return;
            if (view == null) {
                // AsyncLayoutInflater hands back null when it cannot build the tree; fall back so
                // the screen still appears.
                callback.onInflated(activity.getLayoutInflater().inflate(resId, parent));
            } else {
                callback.onInflated(view);
            }
        });
    }

    /** Attaches the inflated view as the activity content, falling back if it cannot be attached. */
    public static void setContentView(@NonNull Activity activity, @LayoutRes int layoutRes,
                                      @Nullable Runnable afterAttach) {
        inflate(activity, layoutRes, view -> {
            try {
                activity.setContentView(view);
            } catch (RuntimeException e) {
                // A root that needs layout params this path did not supply: take the plain route.
                activity.setContentView(layoutRes);
            }
            if (afterAttach != null) afterAttach.run();
        });
    }

    private static boolean isGone(Activity activity) {
        return activity.isFinishing() || activity.isDestroyed();
    }
}
