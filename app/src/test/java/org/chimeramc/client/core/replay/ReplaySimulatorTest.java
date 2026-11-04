package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The replay simulator, pinned without a device or a server.
 *
 * <p>A Bedrock replay viewer re-simulates a recorded session locally. What matters: the clock plays,
 * pauses, seeks and scales by the speed multiplier; it clamps at both ends instead of wrapping; a
 * bad delta or speed is refused rather than jumping the replay; and the world read is honest about
 * what the recording does not contain.
 */
public class ReplaySimulatorTest {

    private static ReplayTrack track() {
        ReplayTrack t = new ReplayTrack();
        t.sample(0L, 0f, 64f, 0f, 0f, 0f, 20f);
        t.sample(1000L, 10f, 64f, 0f, 90f, 0f, 18f);
        t.sample(2000L, 20f, 64f, 0f, 180f, 0f, 16f);
        return t;
    }

    private static ReplayCameraPath path() {
        ReplayCameraPath p = new ReplayCameraPath();
        p.addKeyframe(new ReplayCameraPath.Keyframe(0L, 0f, 70f, 10f, 0f, 0f));
        p.addKeyframe(new ReplayCameraPath.Keyframe(2000L, 20f, 70f, 10f, 0f, 0f));
        return p;
    }

    @Test
    public void theDurationIsTheLaterOfTheTrackAndThePath() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        assertEquals(2000L, sim.durationMs());
        // An empty replay has no length and refuses to play.
        ReplaySimulator empty = new ReplaySimulator(null, null, null);
        assertEquals(0L, empty.durationMs());
        empty.play();
        assertFalse("an empty replay must not claim to be playing", empty.isPlaying());
    }

    @Test
    public void playPauseAndStopMoveTheClock() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        sim.play();
        assertTrue(sim.isPlaying());
        sim.advance(1f);
        assertEquals(1000L, sim.positionMs());
        sim.pause();
        assertFalse(sim.isPlaying());
        assertEquals("a paused replay must not advance", 1000L, sim.advance(1f));
        sim.stop();
        assertEquals(0L, sim.positionMs());
        assertFalse(sim.isPlaying());
    }

    @Test
    public void theSpeedMultiplierScalesTheClockAndEndsAtTheEnd() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        sim.play();
        assertTrue(sim.setSpeed(2f));
        sim.advance(0.5f);  // 2x for half a second = 1 second of replay
        assertEquals(1000L, sim.positionMs());
        // Reaching the end stops playback rather than looping.
        sim.advance(10f);
        assertEquals(2000L, sim.positionMs());
        assertFalse("the replay must stop at the end, not loop", sim.isPlaying());
    }

    @Test
    public void aBackwardSpeedRunsTheReplayBackwards() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        // A UI picks the speed and then plays, so the speed is set first. play() only rewinds from
        // the end for a forward run; a backward one starts where the playhead is.
        sim.seek(2000L);
        assertTrue(sim.setSpeed(-1f));
        sim.play();
        sim.advance(0.5f);
        assertEquals(1500L, sim.positionMs());
    }

    @Test
    public void aBadDeltaOrSpeedIsRefused() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        sim.play();
        assertFalse("zero speed would stop the clock", sim.setSpeed(0f));
        assertFalse("NaN speed is refused", sim.setSpeed(Float.NaN));
        assertFalse("an infinite speed is refused", sim.setSpeed(Float.POSITIVE_INFINITY));
        assertEquals("a negative delta must not move the clock", 0L, sim.advance(-1f));
        assertEquals("a NaN delta must not move the clock", 0L, sim.advance(Float.NaN));
    }

    @Test
    public void seekingClampsAtBothEnds() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        sim.seek(-500L);
        assertEquals(0L, sim.positionMs());
        sim.seek(99999L);
        assertEquals(2000L, sim.positionMs());
        sim.seekFraction(0.5f);
        assertEquals(1000L, sim.positionMs());
        sim.seekFraction(-1f);
        assertEquals(0L, sim.positionMs());
        sim.seekFraction(5f);
        assertEquals(2000L, sim.positionMs());
    }

    @Test
    public void theSnapshotReconstructsThePlayerAndTheCamera() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        sim.seek(1000L);
        ReplaySimulator.Snapshot s = sim.snapshot();
        assertEquals(1000L, s.timeMs);
        assertNotNull("the recorded player frame must be reconstructed", s.player);
        assertTrue(s.hasPlayer());
        assertEquals(10f, s.player.x, 1e-3f);
        assertEquals(18f, s.player.health, 1e-3f);
        assertNotNull("the cinematic camera must be sampled", s.camera);
    }

    @Test
    public void theFreeCameraIsUsedWhenTheCinematicPathIsOff() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        assertTrue("the cinematic path is the default", sim.usesCinematicPath());
        sim.setUseCinematicPath(false);
        assertFalse(sim.usesCinematicPath());
        sim.seek(1000L);
        assertEquals("with the path off, the free-fly camera is the pose",
                sim.freeCamera(), sim.snapshot().camera);
    }

    @Test
    public void aMarkerIsReportedOnlyWhileThePlayheadIsNearIt() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(1000L, ReplayMarkers.Kind.DEATH, "death");
        ReplaySimulator sim = new ReplaySimulator(track(), path(), markers);
        sim.seek(1000L);
        assertNotNull("a marker at the playhead must be reported", sim.snapshot().marker);
        sim.seek(1600L);
        assertNotNull("a marker within the merge window is still reported",
                sim.snapshot().marker);
        sim.seek(1900L);
        assertNull("a marker past the merge window must not be reported",
                sim.snapshot().marker);
    }

    @Test
    public void theViewerIsHonestAboutWorldGeometry() {
        ReplaySimulator sim = new ReplaySimulator(track(), path(), ReplayMarkers.empty());
        assertFalse("the recording carries no world geometry, so the viewer must say so",
                sim.hasWorldGeometry());
        // And the player read is null for a time with no recorded frame.
        ReplaySimulator noTrack = new ReplaySimulator(ReplayTrack.empty(), path(),
                ReplayMarkers.empty());
        noTrack.seek(1000L);
        assertFalse(noTrack.snapshot().hasPlayer());
    }
}
