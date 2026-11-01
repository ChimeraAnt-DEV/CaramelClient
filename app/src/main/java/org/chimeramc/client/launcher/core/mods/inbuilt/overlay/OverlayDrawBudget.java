package org.chimeramc.client.core.mods.inbuilt.overlay;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks how long each overlay's {@code onDraw} takes and reports the ones that overrun (item 214).
 *
 * <p>An overlay shares the game's frame budget, so a draw that quietly costs several milliseconds is
 * a dropped frame the player feels but never sees attributed. The budget is only enforced in debug
 * builds; in release the class is still consulted but {@link #record} short-circuits, so nothing is
 * logged and no state is kept.
 *
 * <p>Pure apart from the caller's clock, so the throttle and the threshold are unit-testable: the
 * budget is passed in, and every call supplies {@code nowMs}.
 */
public final class OverlayDrawBudget {

    /** Any draw at or above this many milliseconds is reported. */
    public static final float BUDGET_MS = 2f;
    /** The shortest gap between two reports for the same overlay, in milliseconds. */
    public static final long REPORT_COOLDOWN_MS = 5000L;

    private final Map<String, Long> lastReportMs = new HashMap<>();
    private final float budgetMs;

    public OverlayDrawBudget() {
        this(BUDGET_MS);
    }

    public OverlayDrawBudget(float budgetMs) {
        this.budgetMs = budgetMs;
    }

    /**
     * Records one draw.
     *
     * @param overlayId  the overlay's name, used as the report key
     * @param drawMs     how long the draw took
     * @param nowMs      the current time in milliseconds
     * @return true when the draw overran and has not been reported within the cooldown
     */
    public boolean record(String overlayId, float drawMs, long nowMs) {
        if (overlayId == null || drawMs < budgetMs) return false;
        Long last = lastReportMs.get(overlayId);
        if (last != null && nowMs - last < REPORT_COOLDOWN_MS) return false;
        lastReportMs.put(overlayId, nowMs);
        return true;
    }

    /** The configured budget in milliseconds. */
    public float budgetMs() {
        return budgetMs;
    }

    /** Forgets every cooldown, so the next overrun for an overlay reports immediately. */
    public void reset() {
        lastReportMs.clear();
    }
}
