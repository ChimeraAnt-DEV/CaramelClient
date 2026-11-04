package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The recorder accumulates a session into a {@code .glowreplay} document.
 *
 * <p>The playback half of the replay system existed but nothing wrote a portable recording: the track
 * and markers only ever reached a clip's sidecar, so a replay could not be moved or shared as one
 * file. The recorder is the other half — it collects the local-player track, the markers and the
 * metadata the client genuinely knows, and produces a {@link ReplayDocument}.
 *
 * <p>Honest scope: the shipped build exposes the local player's position, facing and health and
 * nothing else (no blocks, no other entities, no level time), so the recorder captures exactly that
 * and leaves the fields it cannot fill empty rather than inventing them.
 */
public class ReplayRecorderTest {

    @Test
    public void itRecordsSamplesIntoATrack() {
        ReplayRecorder recorder = new ReplayRecorder();
        recorder.start(1_700_000_000_000L, "1.26.60.28", "survival", "c8f1e2a4");
        assertTrue(recorder.isRecording());

        assertTrue(recorder.sample(0L, 10f, 64f, -20f, 90f, 0f, 20f));
        assertTrue(recorder.sample(500L, 12f, 64f, -18f, 95f, -3f, 18f));
        // Inside the cadence: dropped by the track, so the recorder reports it was not stored.
        assertFalse(recorder.sample(550L, 13f, 64f, -17f, 96f, -3f, 18f));
        assertTrue(recorder.sample(1000L, 15f, 64f, -15f, 100f, -5f, 16f));

        ReplayDocument doc = recorder.finish();
        assertEquals(3, doc.track().size());
        assertEquals(12f, doc.track().at(500L).x, 0.001f);
    }

    @Test
    public void itRecordsMarkers() {
        ReplayRecorder recorder = new ReplayRecorder();
        recorder.start(1L, "1.0", "creative", "");
        // The opening marker is the start of the recording, added by start().
        recorder.addMarker(2000L, ReplayMarkers.Kind.DEATH, "");
        recorder.addMarker(4000L, ReplayMarkers.Kind.KILL_STREAK, "5");

        ReplayDocument doc = recorder.finish();
        assertEquals(3, doc.markers().size());
        assertEquals(ReplayMarkers.Kind.JOIN, doc.markers().at(0).kind);
        assertEquals(ReplayMarkers.Kind.KILL_STREAK, doc.markers().at(2).kind);
        assertEquals("5", doc.markers().at(2).detail);
    }

    @Test
    public void itCarriesTheMetadataTheClientKnows() {
        ReplayRecorder recorder = new ReplayRecorder();
        recorder.start(1_700_000_000_000L, "1.26.60.28", "survival", "c8f1e2a4");
        ReplayDocument doc = recorder.finish();
        assertEquals(1_700_000_000_000L, doc.recordedAtMs());
        assertEquals("1.26.60.28", doc.gameVersion());
        assertEquals("survival", doc.gameMode());
        assertEquals("c8f1e2a4", doc.playerSkin());
        // The fields the build cannot see are empty, never invented.
        assertEquals("", doc.world());
        assertEquals("", doc.serverAddress());
        assertEquals("", doc.worldSeed());
        assertEquals(ReplayDocument.UNKNOWN_TIME, doc.timeOfDay());
        assertTrue("the document must be a real .glowreplay",
                ReplayDocument.looksLikeGlowReplay(doc.serialize()));
    }

    @Test
    public void itRecordsACameraPathWhenOneIsAuthored() {
        ReplayRecorder recorder = new ReplayRecorder();
        recorder.start(1L, "1.0", "survival", "");
        ReplayCameraPath path = new ReplayCameraPath();
        path.addKeyframe(new ReplayCameraPath.Keyframe(0L, 0f, 70f, 0f, 0f, 0f));
        path.addKeyframe(new ReplayCameraPath.Keyframe(1000L, 10f, 70f, 0f, 0f, 0f));
        path.setEasing(ReplayCameraPath.Easing.EASE_IN_OUT);
        recorder.setCameraPath(path);

        ReplayDocument doc = recorder.finish();
        assertEquals(2, doc.cameraPath().size());
        assertEquals(ReplayCameraPath.Easing.EASE_IN_OUT, doc.cameraPath().easing());
    }

    @Test
    public void finishingStopsRecordingAndIsIdempotent() {
        ReplayRecorder recorder = new ReplayRecorder();
        recorder.start(1L, "1.0", "survival", "");
        recorder.sample(0L, 0f, 0f, 0f, 0f, 0f, 20f);
        ReplayDocument first = recorder.finish();
        assertFalse(recorder.isRecording());
        // A sample after finish is refused, so a late tick cannot corrupt a saved recording.
        assertFalse(recorder.sample(1000L, 9f, 9f, 9f, 9f, 9f, 9f));
        ReplayDocument second = recorder.finish();
        assertEquals(first.track().size(), second.track().size());
    }

    @Test
    public void aRecorderThatNeverStartedFinishesEmpty() {
        ReplayRecorder recorder = new ReplayRecorder();
        assertFalse(recorder.isRecording());
        assertFalse(recorder.sample(0L, 1f, 2f, 3f, 4f, 5f, 6f));
        ReplayDocument doc = recorder.finish();
        assertEquals(0, doc.track().size());
        assertEquals(0, doc.markers().size());
    }

    @Test
    public void itWritesTheDocumentBesideTheClip() throws Exception {
        java.io.File dir = java.nio.file.Files.createTempDirectory("rec").toFile();
        ReplayRecorder recorder = new ReplayRecorder();
        recorder.start(1_700_000_000_000L, "1.26.60.28", "survival", "");
        recorder.sample(0L, 10f, 64f, -20f, 90f, 0f, 20f);
        recorder.sample(1000L, 15f, 64f, -15f, 100f, -5f, 16f);

        java.io.File written = recorder.writeTo(dir, "clip-001");
        assertTrue(written != null && written.isFile());
        assertTrue(written.getName().endsWith(ReplayDocument.EXTENSION));

        ReplayDocument back = ReplayDocument.read(written);
        assertEquals(2, back.track().size());
        assertEquals("1.26.60.28", back.gameVersion());
    }
}
