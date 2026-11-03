package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.util.Log;

import org.chimeramc.client.BuildConfig;

/**
 * Times an overlay's {@code onDraw} and logs an overrun in debug builds.
 *
 * <p>The clock and the threshold live in {@link OverlayDrawBudget} so the rule is testable; this
 * class is only the Android-side plumbing that turns a returned {@code true} into a log line. The
 * whole body is skipped in a release build, so the timing calls cost nothing there.
 */
public final class OverlayDrawTimer {

    private static final String TAG = "OverlayDrawBudget";
    private static final OverlayDrawBudget BUDGET = new OverlayDrawBudget();

    private OverlayDrawTimer() {
    }

    /** Marks the start of a draw; pass the result to {@link #finish}. */
    public static long start() {
        return BuildConfig.DEBUG ? System.nanoTime() : 0L;
    }

    /**
     * Records the draw that started at {@code startNanos}.
     *
     * @param overlayId the overlay's name, used as the report key
     * @param startNanos the value {@link #start()} returned, or 0 when timing is off
     */
    public static void finish(String overlayId, long startNanos) {
        if (!BuildConfig.DEBUG || startNanos == 0L) return;
        float drawMs = (System.nanoTime() - startNanos) / 1_000_000f;
        if (BUDGET.record(overlayId, drawMs, System.currentTimeMillis())) {
            Log.w(TAG, overlayId + " onDraw took " + drawMs + "ms (budget "
                    + BUDGET.budgetMs() + "ms)");
        }
    }
}
