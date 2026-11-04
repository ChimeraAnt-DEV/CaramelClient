package org.chimeramc.client.core.replay;

import java.util.ArrayList;
import java.util.List;

/**
 * The pure layout of the cinematic timeline strip.
 *
 * <p>The strip is drawn in Compose, but the arithmetic that turns a clip time into a position across
 * the bar (and a tap back into a time, and the nearest marker to snap to) is pure, so it is unit-tested
 * without a device. The Compose layer only multiplies by its own width and draws, so a change to the
 * snapping rule shows up as a failing number rather than a shrug on a phone.
 */
public final class ReplayTimelineLayout {

    private ReplayTimelineLayout() {
    }

    /**
     * Where a time sits across the bar, 0..1, clamped.
     *
     * <p>A zero-length clip has no span, so every time reads as the start rather than dividing by
     * zero.
     */
    public static float fraction(long timeMs, long durationMs) {
        if (durationMs <= 0L) return 0f;
        float f = (float) timeMs / (float) durationMs;
        if (f < 0f) return 0f;
        return f > 1f ? 1f : f;
    }

    /** The x position of a time across a bar of {@code width} pixels, clamped to the bar. */
    public static float xForTime(long timeMs, long durationMs, float width) {
        if (width <= 0f) return 0f;
        return fraction(timeMs, durationMs) * width;
    }

    /**
     * The time at an x position on the bar, clamped to the clip's span.
     *
     * <p>The inverse of {@link #xForTime}, so a tap or a drag resolves to the time under it.
     */
    public static long timeForX(float x, long durationMs, float width) {
        if (width <= 0f || durationMs <= 0L) return 0L;
        float f = x / width;
        if (f < 0f) f = 0f;
        if (f > 1f) f = 1f;
        return Math.round(f * durationMs);
    }

    /**
     * The marker nearest a time, within {@code toleranceMs}, or null when none is close enough.
     *
     * <p>This is what makes a marker reachable by touch: a tap near a tick snaps to it, and a tap in
     * open space resolves to nothing rather than the far-away tick.
     */
    public static ReplayMarkers.Marker nearestMarker(ReplayMarkers markers, long timeMs,
                                                     long toleranceMs) {
        if (markers == null || markers.isEmpty() || toleranceMs < 0L) return null;
        ReplayMarkers.Marker best = null;
        long bestDelta = Long.MAX_VALUE;
        for (ReplayMarkers.Marker m : markers.all()) {
            long delta = Math.abs(m.tMs - timeMs);
            if (delta <= toleranceMs && delta < bestDelta) {
                bestDelta = delta;
                best = m;
            }
        }
        return best;
    }

    /**
     * The fraction across the bar for each keyframe of a path, in ascending order.
     *
     * <p>The path keeps its keyframes time-ordered, so the result is in bar order too. An empty or
     * null path has no keyframes.
     */
    public static float[] keyframeFractions(ReplayCameraPath path, long durationMs) {
        if (path == null || path.isEmpty()) return new float[0];
        List<ReplayCameraPath.Keyframe> keys = path.keyframes();
        List<Float> out = new ArrayList<>(keys.size());
        for (ReplayCameraPath.Keyframe k : keys) {
            out.add(fraction(k.timeMs, durationMs));
        }
        float[] result = new float[out.size()];
        for (int i = 0; i < result.length; i++) result[i] = out.get(i);
        return result;
    }
}
