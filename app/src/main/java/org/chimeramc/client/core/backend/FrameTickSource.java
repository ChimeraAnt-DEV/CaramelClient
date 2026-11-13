package org.chimeramc.client.core.backend;

/**
 * Supplies the game's own monotonic per-frame tick, so the backend can derive a real frame rate.
 *
 * <p>The sweetloader's player-render hook increments one tick per frame it draws; the launcher reads
 * that counter and hands it here. Kept as its own interface — rather than a method on
 * {@link GameInputHost} — because the two capabilities come from different places (input is the
 * activity's dispatcher, the tick is the native render hook) and a build can have one without the
 * other.
 *
 * <p>Implemented on the device by reading {@code PreloaderInput.nativeReadPlayerRenderStats()} and
 * returning its first element (the render tick), or a negative value when the hook has not run. A
 * test supplies a counter it controls.
 */
public interface FrameTickSource {

    /**
     * The game's monotonic per-frame tick, or a negative value when the render hook has not run
     * this session (so the caller reports "unknown" rather than a fabricated rate).
     */
    long renderTick();
}
