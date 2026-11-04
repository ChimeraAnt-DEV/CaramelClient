package org.chimeramc.client.core.replay;

/**
 * The immutable data the timeline strip draws from.
 *
 * <p>Compose only recomposes a composable when a state it reads changes. The camera path is a mutable
 * Java object, so mutating it (adding a keyframe) would not redraw the strip. This value object is
 * rebuilt from the path and markers whenever either changes and handed to the strip as a state, so the
 * strip redraws on a keyframe add without the Compose layer holding a reference to the mutable path.
 *
 * <p>Pure, so the projection is unit-tested without a device.
 */
public final class ReplayTimelineData {

    public final long durationMs;
    public final long positionMs;
    /** The fraction across the bar for each keyframe, in ascending order. */
    public final float[] keyframeFractions;
    /** The time of each marker, for snapping. */
    public final long[] markerTimes;
    /** The fraction across the bar for each marker, in the same order as {@link #markerTimes}. */
    public final float[] markerFractions;

    private ReplayTimelineData(long durationMs, long positionMs, float[] keyframeFractions,
                               long[] markerTimes, float[] markerFractions) {
        this.durationMs = Math.max(0L, durationMs);
        this.positionMs = Math.max(0L, positionMs);
        this.keyframeFractions = keyframeFractions;
        this.markerTimes = markerTimes;
        this.markerFractions = markerFractions;
    }

    /** An empty strip: no duration, no keyframes, no markers. */
    public static ReplayTimelineData empty() {
        return new ReplayTimelineData(0L, 0L, new float[0], new long[0], new float[0]);
    }

    /** Projects a path and markers onto the bar for a clip of {@code durationMs}. */
    public static ReplayTimelineData of(long durationMs, long positionMs, ReplayCameraPath path,
                                        ReplayMarkers markers) {
        float[] keyframes = ReplayTimelineLayout.keyframeFractions(path, durationMs);
        int markerCount = markers == null ? 0 : markers.size();
        long[] times = new long[markerCount];
        float[] fractions = new float[markerCount];
        for (int i = 0; i < markerCount; i++) {
            ReplayMarkers.Marker m = markers.at(i);
            times[i] = m.tMs;
            fractions[i] = ReplayTimelineLayout.fraction(m.tMs, durationMs);
        }
        return new ReplayTimelineData(durationMs, positionMs, keyframes, times, fractions);
    }

    /** A copy with a new scrub position, keeping the keyframes and markers. */
    public ReplayTimelineData withPosition(long positionMs) {
        return new ReplayTimelineData(durationMs, positionMs, keyframeFractions, markerTimes,
                markerFractions);
    }
}
