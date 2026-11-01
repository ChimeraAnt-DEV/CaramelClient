package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the gameplay track: cadence, bounded growth, interpolation and the serialisation round-trip.
 *
 * <p>Pure, no mocks, no device. The track is the half of a replay the video cannot carry — where the
 * player was and which way they looked — so the rules that decide what gets stored and what a
 * missing reading reads as are exactly the ones that must not drift.
 */
public class ReplayTrackTest {

    private static final float EPS = 1e-3f;

    @Test
    public void aFrameInsideTheCadenceIsDroppedNotReplaced() {
        ReplayTrack track = ReplayTrack.empty();
        assertTrue(track.sample(0L, 1f, 2f, 3f, 0f, 0f, 20f));
        // 50ms later is inside the 100ms cadence: ignored, so the first frame stays at t=0.
        assertFalse(track.sample(50L, 9f, 9f, 9f, 0f, 0f, 20f));
        assertEquals(1, track.size());
        assertEquals(1f, track.frameAt(0).x, EPS);
        // Exactly the cadence later is stored.
        assertTrue(track.sample(100L, 9f, 9f, 9f, 0f, 0f, 20f));
        assertEquals(2, track.size());
    }

    @Test
    public void aFrameWithNoReadingAtAllIsNotStored() {
        ReplayTrack track = ReplayTrack.empty();
        assertFalse(track.sample(0L, ReplayTrack.UNKNOWN, ReplayTrack.UNKNOWN,
                ReplayTrack.UNKNOWN, ReplayTrack.UNKNOWN, ReplayTrack.UNKNOWN,
                ReplayTrack.UNKNOWN));
        assertTrue(track.isEmpty());
    }

    @Test
    public void aBackwardsClockStartsAFreshCadenceRatherThanRejectingEverything() {
        ReplayTrack track = ReplayTrack.empty();
        assertTrue(track.sample(10_000L, 0f, 0f, 0f, 0f, 0f, 20f));
        // An uptime wrap: the new time is below the last. It must still be stored.
        assertTrue(track.sample(5L, 1f, 1f, 1f, 0f, 0f, 20f));
        assertEquals(2, track.size());
    }

    @Test
    public void theTrackIsBoundedAndDropsTheOldestFrames() {
        ReplayTrack track = ReplayTrack.empty();
        for (int i = 0; i < ReplayTrack.MAX_FRAMES + 50; i++) {
            track.sample(i * ReplayTrack.MIN_SAMPLE_INTERVAL_MS, i, 0f, 0f, 0f, 0f, 20f);
        }
        assertEquals(ReplayTrack.MAX_FRAMES, track.size());
        // The oldest 50 were dropped, so the first frame is the 50th sample.
        assertEquals(50 * ReplayTrack.MIN_SAMPLE_INTERVAL_MS, track.first().tMs);
    }

    @Test
    public void interpolationIsLinearAndClampsAtBothEnds() {
        ReplayTrack track = ReplayTrack.empty();
        track.sample(0L, 0f, 0f, 0f, 0f, 0f, 20f);
        track.sample(1000L, 10f, 0f, 0f, 0f, 0f, 0f);
        assertEquals(5f, track.at(500L).x, EPS);
        // Before the start and after the end clamp to the nearest frame, never extrapolate.
        assertEquals(0f, track.at(-100L).x, EPS);
        assertEquals(10f, track.at(9999L).x, EPS);
        assertEquals(0f, track.at(1000L).health, EPS);
    }

    @Test
    public void yawInterpolatesAlongTheShorterArc() {
        ReplayTrack track = ReplayTrack.empty();
        track.sample(0L, 0f, 0f, 0f, 179f, 0f, 20f);
        track.sample(1000L, 0f, 0f, 0f, -179f, 0f, 20f);
        float mid = track.at(500L).yawDeg;
        // The short way from 179 to -179 passes through 180/-180, not through 0.
        assertTrue("mid yaw was " + mid, Math.abs(Math.abs(mid) - 180f) < 0.01f);
    }

    @Test
    public void aFieldEitherBracketingFrameLacksCarriesFromTheOneThatHasIt() {
        ReplayTrack track = ReplayTrack.empty();
        track.sample(0L, 1f, 2f, 3f, ReplayTrack.UNKNOWN, ReplayTrack.UNKNOWN, 20f);
        track.sample(1000L, 5f, 6f, 7f, 90f, 10f, 20f);
        ReplayTrack.Frame mid = track.at(500L);
        // Position interpolates; the rotation comes from the frame that has it.
        assertEquals(3f, mid.x, EPS);
        assertEquals(90f, mid.yawDeg, EPS);
    }

    @Test
    public void serialisationRoundTrips() {
        ReplayTrack track = ReplayTrack.empty();
        track.sample(0L, 1.5f, 64f, -3.25f, 90f, -12f, 18.5f);
        track.sample(500L, 2.5f, 65f, -4.25f, 95f, -13f, 17f);
        ReplayTrack parsed = ReplayTrack.parse(track.serializeLine());
        assertEquals(track.size(), parsed.size());
        assertEquals(1.5f, parsed.frameAt(0).x, EPS);
        assertEquals(64f, parsed.frameAt(0).y, EPS);
        assertEquals(-3.25f, parsed.frameAt(0).z, EPS);
        assertEquals(90f, parsed.frameAt(0).yawDeg, EPS);
        assertEquals(18.5f, parsed.frameAt(0).health, EPS);
        assertEquals(2.5f, parsed.frameAt(1).x, EPS);
    }

    @Test
    public void theSingleLineFormSurvivesTheLineOrientedSidecar() {
        // A frame per line would break every key after it in a key=value sidecar; the single-line
        // form is what the sidecar stores, so it must not contain a newline.
        ReplayTrack track = ReplayTrack.empty();
        track.sample(0L, 1f, 2f, 3f, 4f, 5f, 6f);
        track.sample(500L, 7f, 8f, 9f, 10f, 11f, 12f);
        String line = track.serializeLine();
        assertFalse(line.contains("\n"));
        assertEquals(2, ReplayTrack.parse(line).size());
        // The multi-line form is still parseable too.
        assertEquals(2, ReplayTrack.parse(track.serialize()).size());
    }

    @Test
    public void aCorruptFrameIsSkippedAndTheRestSurvives() {
        String text = "0,1,2,3,4,5,6;garbage;500,7,8,9,10,11,12";
        ReplayTrack parsed = ReplayTrack.parse(text);
        assertEquals(2, parsed.size());
        assertEquals(7f, parsed.frameAt(1).x, EPS);
    }

    @Test
    public void anEmptyTrackParsesToEmptyAndSerialisesToNothing() {
        assertTrue(ReplayTrack.parse("").isEmpty());
        assertTrue(ReplayTrack.parse(null).isEmpty());
        assertEquals("", ReplayTrack.empty().serializeLine());
    }

    @Test
    public void toCameraSeedsTheFirstUsablePoseOrNull() {
        assertNull(ReplayTrack.empty().toCamera());
        ReplayTrack track = ReplayTrack.empty();
        // A frame with only health is not a pose.
        track.sample(0L, ReplayTrack.UNKNOWN, ReplayTrack.UNKNOWN, ReplayTrack.UNKNOWN,
                ReplayTrack.UNKNOWN, ReplayTrack.UNKNOWN, 20f);
        assertNull(track.toCamera());
        track.sample(500L, 10f, 64f, -10f, 90f, -5f, 20f);
        ReplayCamera camera = track.toCamera();
        assertNotNull(camera);
        assertEquals(10f, camera.x(), EPS);
        assertEquals(64f, camera.y(), EPS);
        assertEquals(90f, camera.yawDeg(), EPS);
    }

    @Test
    public void durationSpansFirstToLastFrame() {
        ReplayTrack track = ReplayTrack.empty();
        assertEquals(0L, track.durationMs());
        track.sample(1000L, 0f, 0f, 0f, 0f, 0f, 20f);
        assertEquals(0L, track.durationMs());
        track.sample(4000L, 0f, 0f, 0f, 0f, 0f, 20f);
        assertEquals(3000L, track.durationMs());
    }
}
