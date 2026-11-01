package org.chimeramc.client.core.backend;

import org.chimeramc.client.preloader.PreloaderInput;

/**
 * The device's {@link FrameTickSource}: the preloader's render-hook frame counter.
 *
 * <p>{@code PreloaderInput.nativeReadPlayerRenderStats()} returns
 * {@code {renderTick, callsThisFrame, totalCalls, msSinceLastRender}} once the game's own player
 * renderer has run, and null before that. The first element is the monotonic per-frame tick the
 * frame rate is derived from.
 *
 * <p>Fail-closed: a missing library, a null reading or a short array all read as {@code -1}
 * ("unknown"), so the backend reports no rate and the overlay falls back to its host-side meter
 * rather than a fabricated number.
 */
public final class PreloaderFrameTickSource implements FrameTickSource {

    @Override
    public long renderTick() {
        try {
            int[] stats = PreloaderInput.nativeReadPlayerRenderStats();
            if (stats == null || stats.length < 1) {
                return -1L;
            }
            // The native tick is an unsigned 32-bit counter; keep it non-negative so a value near
            // the wrap point does not read as "hook absent".
            return stats[0] & 0xFFFFFFFFL;
        } catch (Throwable t) {
            return -1L;
        }
    }
}
