package org.chimeramc.client.util;

import android.app.Activity;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Applies a frame-rate cap to the window's surface (item 261).
 *
 * <p>From API 30, {@code Surface.setFrameRate} is the mechanism the platform recommends: it asks
 * the compositor to present the surface at a rate directly, so a cap never touches the display
 * mode and therefore never changes the resolution — unlike {@code preferredDisplayModeId}, which
 * on many panels only offers a high-refresh mode at reduced resolution.
 *
 * <p>The surface is found by walking the view tree for the first {@link SurfaceView}: the launcher
 * does not own the game's surface directly, and a {@code SurfaceView}'s holder is the one surface a
 * plain Activity can reach. When there is none (or the API is below 30, or the call throws) this
 * returns false and the caller falls back to the display-mode path, which is the honest behaviour:
 * the cap is applied by whichever mechanism the device actually supports.
 *
 * <p>Every type that only exists above {@code minSdk} lives in the nested {@link Api30} holder and
 * is reached only behind the guard, so merely loading this class on API 28 cannot throw
 * {@code NoClassDefFoundError}.
 */
public final class SurfaceFrameRate {

    private SurfaceFrameRate() {
    }

    /**
     * Requests {@code capHz} on the window's surface.
     *
     * @return true when the request was handed to the platform, false to fall back
     */
    public static boolean apply(Activity activity, int capHz) {
        if (activity == null || !ApiLevels.atLeastR()) return false;
        try {
            View root = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
            Surface surface = findSurface(root);
            if (surface == null || !surface.isValid()) return false;
            return Api30.setFrameRate(surface, FrameRateTarget.rateForCap(capHz));
        } catch (Throwable t) {
            android.util.Log.w("SurfaceFrameRate", "Could not set the surface frame rate", t);
            return false;
        }
    }

    private static Surface findSurface(View view) {
        if (view instanceof SurfaceView) {
            Surface s = ((SurfaceView) view).getHolder().getSurface();
            if (s != null && s.isValid()) return s;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Surface found = findSurface(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Holds the API-30-only call so the outer class declares no type above {@code minSdk}. */
    private static final class Api30 {
        static boolean setFrameRate(Surface surface, float rate) {
            surface.setFrameRate(rate,
                    FrameRateTarget.COMPATIBILITY_FIXED_SOURCE,
                    FrameRateTarget.CHANGE_FRAME_RATE_ALWAYS);
            return true;
        }
    }
}
