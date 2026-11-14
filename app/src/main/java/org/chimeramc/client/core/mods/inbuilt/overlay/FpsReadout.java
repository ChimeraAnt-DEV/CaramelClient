package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * Picks which frame-rate reading to show, and names where it came from.
 *
 * <p>Three readings can back the FPS readout, and they are not the same number:
 *
 * <ul>
 *   <li>The <b>exact game rate</b>, derived from the sweetloader's render-hook frame counter while
 *       the engine's present hook is live — the tick is the true frame boundary, so this is the
 *       game's real frame rate.
 *   <li>The <b>approximate game rate</b>, derived from the same counter while the present hook is
 *       absent. The tick is then inferred from render-entry timestamps, which can collapse or split
 *       frames when the visible-model count changes, so the rate is close but not exact.
 *   <li>The <b>launcher refresh cadence</b>, counted from the launcher process's own
 *       {@code Choreographer} callbacks. This is <em>not</em> the game's frame rate and not the
 *       game's present rate: the launcher does not own the game's {@code eglSwapBuffers}, so this
 *       counts the launcher main thread's vsync ticks (the display refresh cadence) and keeps
 *       counting while the game is paused or loading. It is a liveness reading of last resort.
 * </ul>
 *
 * <p>The exact game rate wins whenever it is available; the approximate game rate is preferred over
 * the host cadence (it is at least the game's own render path) and both are labelled. Keeping the
 * choice here — rather than inline in the overlay — means the rule is a pure function with a unit
 * test, so a surface can never quietly pass one measurement off as another.
 */
public final class FpsReadout {

    /** Where a reading came from, so a surface can name it instead of showing a bare number. */
    public enum Source {
        /** No reading from any source. */
        NONE,
        /** The game's own rate, from the exact present boundary. */
        GAME,
        /** The game's own rate, from an inferred frame boundary — close, but not exact. */
        GAME_APPROXIMATE,
        /**
         * The launcher's own refresh cadence, from this process's {@code Choreographer} callbacks.
         * Not the game's rate at all, and not the game's present rate either: it is the launcher
         * main thread's vsync tick.
         */
        HOST_REFRESH
    }

    /** The value to display, or 0 when there is no reading from any source. */
    public final int value;
    /** Where {@link #value} came from. */
    public final Source source;

    private FpsReadout(int value, Source source) {
        this.value = value;
        this.source = source;
    }

    /** True when there is a number to show. */
    public boolean hasReading() {
        return value > 0 && source != Source.NONE;
    }

    /** True when the value is not the game's exact frame rate. */
    public boolean approximate() {
        return source == Source.GAME_APPROXIMATE || source == Source.HOST_REFRESH;
    }

    /** True when the value is the launcher's refresh cadence rather than the game's own rate. */
    public boolean isHostRefresh() {
        return source == Source.HOST_REFRESH;
    }

    /**
     * The value to show next to an "FPS" unit, or 0 when there is none.
     *
     * <p>Only the game's own render rate may sit beside the unit: a headline of "60 FPS" that is
     * actually the launcher's vsync cadence is the fabricated-frame bug, and the "~" marker does not
     * repair it. The launcher's cadence is exposed separately by {@link #launcherRefreshHz()} so a
     * surface can name it as what it is.
     */
    public int gameValue() {
        return (source == Source.GAME || source == Source.GAME_APPROXIMATE) ? value : 0;
    }

    /**
     * The launcher's own refresh cadence, in Hz, or 0 when the headline is already the game's rate.
     *
     * <p>This is a liveness reading of the launcher main thread's vsync tick — it keeps counting
     * while the game is paused or loading — so it is a different measurement from the game's frame
     * rate and must never be rendered under an "FPS" unit.
     */
    public int launcherRefreshHz() {
        return source == Source.HOST_REFRESH ? value : 0;
    }

    /**
     * Chooses the reading, treating a positive game rate as exact.
     *
     * <p>Kept for callers that already know the tick is the exact present boundary (and for tests).
     */
    public static FpsReadout of(int gameFps, int hostFps) {
        return of(gameFps, true, hostFps);
    }

    /**
     * Chooses the reading.
     *
     * @param gameFps the game's own frame rate, or {@code <= 0} when the render hook has not produced
     *     one
     * @param gameExact whether that game rate came from the exact present boundary; when false it is
     *     shown as approximate
     * @param hostFps the launcher refresh cadence, or {@code <= 0} when no callback has fired yet
     */
    public static FpsReadout of(int gameFps, boolean gameExact, int hostFps) {
        if (gameFps > 0) {
            return new FpsReadout(gameFps, gameExact ? Source.GAME : Source.GAME_APPROXIMATE);
        }
        if (hostFps > 0) {
            return new FpsReadout(hostFps, Source.HOST_REFRESH);
        }
        return new FpsReadout(0, Source.NONE);
    }

    /**
     * The value as the readout text, marked with {@code ~} when it is not the game's exact rate.
     *
     * <p>The launcher's own refresh cadence has no game rate behind it, so it reads as the
     * no-reading dash here rather than as a number under "FPS". The cadence itself is available from
     * {@link #launcherRefreshHz()} for a surface that wants to name it.
     */
    public String text() {
        int game = gameValue();
        if (game <= 0) return ModStatsFormatter.NO_READING;
        return ModStatsFormatter.fps(game, approximate());
    }
}
