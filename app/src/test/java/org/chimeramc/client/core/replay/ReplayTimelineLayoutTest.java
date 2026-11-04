package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The timeline strip's pure layout: where a keyframe, a marker and the playhead sit across the bar.
 *
 * <p>The strip is drawn in Compose, but the arithmetic that turns a clip time into an x position (and
 * back) is pure, so it is unit-tested without a device: the playhead maps the clip's span onto the
 * bar, a keyframe sits at its own time, and a tap on the bar resolves to the nearest marker so a
 * marker is reachable by touch.
 */
public class ReplayTimelineLayoutTest {

    private static ReplayCameraPath pathWithKeyframes(long... times) {
        ReplayCameraPath path = new ReplayCameraPath();
        for (long t : times) {
            path.addKeyframe(new ReplayCameraPath.Keyframe(t, 0f, 0f, 0f, 0f, 0f));
        }
        return path;
    }

    @Test
    public void aTimeMapsToAFractionOfTheBar() {
        assertEquals(0f, ReplayTimelineLayout.fraction(0L, 10_000L), 0.0001f);
        assertEquals(0.5f, ReplayTimelineLayout.fraction(5_000L, 10_000L), 0.0001f);
        assertEquals(1f, ReplayTimelineLayout.fraction(10_000L, 10_000L), 0.0001f);
        // Out of range clamps rather than running off the bar.
        assertEquals(0f, ReplayTimelineLayout.fraction(-100L, 10_000L), 0.0001f);
        assertEquals(1f, ReplayTimelineLayout.fraction(99_999L, 10_000L), 0.0001f);
        // A zero-length clip has no span, so everything sits at the start.
        assertEquals(0f, ReplayTimelineLayout.fraction(0L, 0L), 0.0001f);
    }

    @Test
    public void theXPositionIsTheFractionAcrossTheWidth() {
        assertEquals(0f, ReplayTimelineLayout.xForTime(0L, 10_000L, 200f), 0.0001f);
        assertEquals(100f, ReplayTimelineLayout.xForTime(5_000L, 10_000L, 200f), 0.0001f);
        assertEquals(200f, ReplayTimelineLayout.xForTime(10_000L, 10_000L, 200f), 0.0001f);
        assertEquals("a zero width has no position", 0f,
                ReplayTimelineLayout.xForTime(5_000L, 10_000L, 0f), 0.0001f);
    }

    @Test
    public void theTimeForAnXIsTheInverseOfTheXForATime() {
        // A tap at 40% of the bar is the time at 40% of the clip; the round trip must hold.
        long duration = 12_000L;
        float width = 320f;
        float x = 0.4f * width;
        assertEquals(4_800L, ReplayTimelineLayout.timeForX(x, duration, width), 1L);
        // And the inverse: the time back to x.
        assertEquals(x, ReplayTimelineLayout.xForTime(4_800L, duration, width), 0.5f);
        assertEquals(0L, ReplayTimelineLayout.timeForX(-10f, duration, width), 1L);
        assertEquals(duration, ReplayTimelineLayout.timeForX(width + 50f, duration, width), 1L);
    }

    @Test
    public void theNearestMarkerIsFoundForATap() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(2_000L, ReplayMarkers.Kind.JOIN, "");
        markers.add(6_000L, ReplayMarkers.Kind.DEATH, "");
        markers.add(9_000L, ReplayMarkers.Kind.COMBO, "3");

        // A tap just past the death marker snaps to it.
        ReplayMarkers.Marker near = ReplayTimelineLayout.nearestMarker(markers, 6_400L, 1_000L);
        assertTrue(near != null && near.kind == ReplayMarkers.Kind.DEATH);

        // A tap far from any marker snaps to nothing.
        assertEquals(null, ReplayTimelineLayout.nearestMarker(markers, 4_000L, 500L));

        // No markers at all is never a match.
        assertEquals(null, ReplayTimelineLayout.nearestMarker(ReplayMarkers.empty(), 0L, 1_000L));
        assertEquals(null, ReplayTimelineLayout.nearestMarker(null, 0L, 1_000L));
    }

    @Test
    public void theKeyframeXsAreInBarOrder() {
        ReplayCameraPath path = pathWithKeyframes(0L, 3_000L, 7_000L, 10_000L);
        float[] xs = ReplayTimelineLayout.keyframeFractions(path, 10_000L);
        assertEquals(4, xs.length);
        assertEquals(0f, xs[0], 0.0001f);
        assertEquals(0.3f, xs[1], 0.0001f);
        assertEquals(0.7f, xs[2], 0.0001f);
        assertEquals(1f, xs[3], 0.0001f);
        for (int i = 1; i < xs.length; i++) {
            assertTrue("keyframes must be in ascending order along the bar", xs[i] >= xs[i - 1]);
        }
        // An empty or null path has no keyframes to draw.
        assertEquals(0, ReplayTimelineLayout.keyframeFractions(new ReplayCameraPath(), 10_000L).length);
        assertEquals(0, ReplayTimelineLayout.keyframeFractions(null, 10_000L).length);
    }

    @Test
    public void theSnapshotProjectsThePathAndMarkersForTheStrip() {
        // The strip draws from an immutable snapshot so adding a keyframe redraws it; the projection
        // must carry both the keyframes and the markers, in bar fractions.
        ReplayCameraPath path = pathWithKeyframes(0L, 5_000L, 10_000L);
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(2_500L, ReplayMarkers.Kind.JOIN, "");
        markers.add(7_500L, ReplayMarkers.Kind.DEATH, "");

        ReplayTimelineData data = ReplayTimelineData.of(10_000L, 4_000L, path, markers);
        assertEquals(10_000L, data.durationMs);
        assertEquals(4_000L, data.positionMs);
        assertEquals(3, data.keyframeFractions.length);
        assertEquals(2, data.markerTimes.length);
        assertEquals(2_500L, data.markerTimes[0]);
        assertEquals(0.25f, data.markerFractions[0], 0.0001f);
        assertEquals(0.75f, data.markerFractions[1], 0.0001f);

        // A position update keeps the keyframes and markers.
        ReplayTimelineData moved = data.withPosition(9_000L);
        assertEquals(9_000L, moved.positionMs);
        assertEquals(data.keyframeFractions.length, moved.keyframeFractions.length);
        assertEquals(data.markerTimes.length, moved.markerTimes.length);

        // An empty snapshot is valid and draws nothing.
        ReplayTimelineData empty = ReplayTimelineData.empty();
        assertEquals(0, empty.keyframeFractions.length);
        assertEquals(0, empty.markerTimes.length);
    }
}
