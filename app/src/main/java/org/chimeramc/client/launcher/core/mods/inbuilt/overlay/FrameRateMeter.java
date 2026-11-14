package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.view.Choreographer;

/**
 * A host-side frame-rate meter.
 *
 * <p>The inbuilt native {@code FpsMod} only reports a value when its hook resolves a game address,
 * which does not happen on every build — so the FPS overlay could sit on "FPS: --" forever. This
 * meter counts the display's own vsync callbacks through {@link Choreographer}, which needs no
 * native hook and works on any build the launcher runs on.
 *
 * <p><b>What the number means.</b> This is the launcher's own refresh cadence: the rate at which
 * the launcher's main thread receives {@code Choreographer} vsync ticks. It is
 * <em>not</em> the game's frame rate and not the game's present rate. The launcher does not own the
 * game's {@code eglSwapBuffers}, so the meter keeps counting at the display refresh cadence while
 * the game is paused, loading or stalled. It is a liveness reading of last resort, and the overlay
 * labels it as the launcher refresh so it is never passed off as the game's frame rate.
 *
 * <p>Allocation-free on the frame path: a fixed ring of frame timestamps and a rolling one-second
 * window. {@link #post()} must be called from the main thread; the callback re-arms itself while
 * the meter is running.
 */
public final class FrameRateMeter {

    private final FrameRateCounter counter = new FrameRateCounter();
    private final FrameTimeHistory history = new FrameTimeHistory();

    private boolean running;
    private Choreographer choreographer;
    private OnRateListener listener;

    /** Receives the rate, in whole frames per second. */
    public interface OnRateListener {
        void onRate(int fps);
    }

    private final Choreographer.FrameCallback callback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!running) return;
            counter.record(frameTimeNanos);
            history.record(frameTimeNanos);
            if (listener != null) {
                listener.onRate(counter.fpsAt(frameTimeNanos));
            }
            choreographer.postFrameCallback(this);
        }
    };

    public void start(OnRateListener listener) {
        this.listener = listener;
        if (running) return;
        running = true;
        counter.reset();
        history.reset();
        if (choreographer == null) choreographer = Choreographer.getInstance();
        choreographer.postFrameCallback(callback);
    }

    public void stop() {
        running = false;
        listener = null;
        if (choreographer != null) choreographer.removeFrameCallback(callback);
    }

    public boolean isRunning() {
        return running;
    }

    /** The current rate without waiting for a callback; 0 when fewer than two frames are known. */
    public int currentFps() {
        return counter.fps();
    }

    /** The 1% low rate (the slowest recent frame), or 0 when there is no reading yet. */
    public int currentOnePercentLowFps() {
        return counter.onePercentLowFps();
    }

    /** The rolling frame-interval history, for the overlay's graph mode. */
    public FrameTimeHistory history() {
        return history;
    }
}
