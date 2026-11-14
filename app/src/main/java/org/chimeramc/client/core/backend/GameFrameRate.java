package org.chimeramc.client.core.backend;

/**
 * Derives the game's frame rate from the sweetloader's render-hook frame counter.
 *
 * <p>The game strips its symbols, so there is no frame-rate API to call. The sweetloader does,
 * however, own a hook on the game's own player renderer ({@code GamePlayerRender}), and that hook
 * advances a monotonic tick once per frame. Counting how many of those ticks elapse in a wall-clock
 * window is a reading of the game's own render path — not a guess and not the host's vsync callback.
 *
 * <p><b>How exact the reading is.</b> The tick is the true frame boundary only while the engine's
 * present hook (the {@code eglSwapBuffers} detour) is live and counting; then it is exact. Without
 * that hook the tick is inferred from render-entry timestamps (a few-millisecond floor), which can
 * collapse or split frames when the visible-model count changes, so the derived rate is close but not
 * exact. A caller must read {@code PreloaderInput.isRenderTickExact()} and label the rate
 * accordingly rather than presenting an inferred rate as the game's exact frame rate.
 *
 * <p>The counter is read on demand (the FPS overlay polls it), so this class is fed a
 * {@code (renderTick, nowMs)} sample on each call and keeps only the first sample of the current
 * window. When the tick has advanced far enough from that anchor and enough wall time has passed,
 * the rate is {@code ticks / seconds}; a tick that has not advanced (the hook is not running, or
 * the renderer is idle on a loading screen) reads as {@code UNKNOWN} rather than {@code 0}, so a
 * caller can fall back to the host-side meter instead of rendering a frozen "0 FPS".
 *
 * <p>Pure and allocation-free: one anchor pair and one cached value, no collections, no Android
 * types, so the rate rule is unit-tested with a fake clock and a fake tick counter.
 */
public final class GameFrameRate {

    /** Returned when no rate can be derived yet; a caller must treat it as "unknown", not zero. */
    public static final int UNKNOWN = -1;

    /**
     * The window the rate is measured over, in milliseconds. One second matches the FPS overlay's
     * own cadence and is long enough that a single dropped frame does not swing the number, while
     * short enough to react to a real stall.
     */
    public static final long WINDOW_MS = 1000L;

    /** The minimum ticks in a window before a rate is reported, so a 1-2 frame blip cannot read. */
    public static final long MIN_TICKS = 3L;

    private long anchorTick = Long.MIN_VALUE;
    private long anchorMs;
    private int fps = UNKNOWN;

    /**
     * Feeds one sample and returns the current rate.
     *
     * @param renderTick the sweetloader's monotonic per-frame tick, or a negative value when the
     *     render hook has not run this session
     * @param nowMs a monotonic wall clock, in milliseconds
     * @return the frame rate, or {@link #UNKNOWN} when no rate can be derived yet
     */
    public int sample(long renderTick, long nowMs) {
        if (renderTick < 0) {
            reset();
            return UNKNOWN;
        }
        if (anchorTick == Long.MIN_VALUE) {
            anchorTick = renderTick;
            anchorMs = nowMs;
            return fps;
        }
        // A counter that went backwards is a new session (or a wrap); re-anchor rather than
        // computing a negative elapsed tick count.
        if (renderTick < anchorTick) {
            anchorTick = renderTick;
            anchorMs = nowMs;
            fps = UNKNOWN;
            return UNKNOWN;
        }
        long elapsedMs = nowMs - anchorMs;
        if (elapsedMs < WINDOW_MS) {
            return fps;
        }
        long ticks = renderTick - anchorTick;
        if (ticks < MIN_TICKS) {
            // The hook is installed but not drawing (a loading screen, a paused session). Report
            // unknown so the caller falls back instead of freezing on the last real reading.
            fps = UNKNOWN;
        } else {
            long rate = Math.round(ticks * 1000.0 / elapsedMs);
            fps = (int) Math.max(0L, Math.min(rate, Integer.MAX_VALUE));
        }
        anchorTick = renderTick;
        anchorMs = nowMs;
        return fps;
    }

    /** The last derived rate, or {@link #UNKNOWN}. */
    public int fps() {
        return fps;
    }

    /** Clears the window, so a new session starts from a fresh anchor. */
    public void reset() {
        anchorTick = Long.MIN_VALUE;
        anchorMs = 0L;
        fps = UNKNOWN;
    }
}
