package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The {@code .glowreplay} container: one file that carries a whole recording.
 *
 * <p>The replay data was scattered across a sidecar's flat keys and the native capture file, so a
 * replay was not a thing you could move, share or version — it was an install's internal state. This
 * is the container the spec names: a self-describing JSON document holding the metadata (player
 * skin, world seed, time of day, server address), the gameplay track, the markers and the authored
 * camera path, with a format tag and a version so a reader can tell what it is holding.
 *
 * <p>Pure apart from the file helpers, so the round trip and the fail-closed rules are JVM tests.
 */
public class ReplayDocumentTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static ReplayTrack track() {
        ReplayTrack t = ReplayTrack.empty();
        t.sample(0L, 10f, 64f, -20f, 90f, 0f, 20f);
        t.sample(500L, 12f, 64f, -18f, 95f, -3f, 18f);
        t.sample(1000L, 15f, 64f, -15f, 100f, -5f, 16f);
        return t;
    }

    private static ReplayMarkers markers() {
        ReplayMarkers m = ReplayMarkers.empty();
        m.add(0L, ReplayMarkers.Kind.JOIN, "");
        m.add(1000L, ReplayMarkers.Kind.DEATH, "");
        return m;
    }

    private static ReplayCameraPath path() {
        ReplayCameraPath p = new ReplayCameraPath();
        p.addKeyframe(new ReplayCameraPath.Keyframe(0L, 10f, 70f, -20f, 90f, -10f));
        p.addKeyframe(new ReplayCameraPath.Keyframe(1000L, 20f, 75f, -10f, 180f, -20f));
        p.setPreset(ReplayCameraPath.Preset.ORBIT);
        return p;
    }

    private static ReplayDocument sample() {
        return ReplayDocument.of(1_700_000_000_000L, "My World", "1.26.60.28", "survival")
                .setPlayerSkin("c8f1e2a4b6d8f0a2c4e6b8d0f2a4c6e8")
                .setWorldSeed("8675309")
                .setTimeOfDay(6000L)
                .setServerAddress("play.example.com:19132")
                .setTrack(track())
                .setMarkers(markers())
                .setCameraPath(path());
    }

    @Test
    public void theDocumentCarriesTheSpecMetadata() {
        ReplayDocument doc = sample();
        assertEquals("glowreplay", doc.format());
        assertEquals(ReplayDocument.VERSION, doc.version());
        assertEquals(1_700_000_000_000L, doc.recordedAtMs());
        assertEquals("My World", doc.world());
        assertEquals("1.26.60.28", doc.gameVersion());
        assertEquals("survival", doc.gameMode());
        assertEquals("c8f1e2a4b6d8f0a2c4e6b8d0f2a4c6e8", doc.playerSkin());
        assertEquals("8675309", doc.worldSeed());
        assertEquals(6000L, doc.timeOfDay());
        assertEquals("play.example.com:19132", doc.serverAddress());
    }

    @Test
    public void theWholeRecordingSurvivesARoundTrip() {
        ReplayDocument doc = sample();
        ReplayDocument back = ReplayDocument.parse(doc.serialize());

        assertEquals(doc.recordedAtMs(), back.recordedAtMs());
        assertEquals(doc.world(), back.world());
        assertEquals(doc.gameVersion(), back.gameVersion());
        assertEquals(doc.gameMode(), back.gameMode());
        assertEquals(doc.playerSkin(), back.playerSkin());
        assertEquals(doc.worldSeed(), back.worldSeed());
        assertEquals(doc.timeOfDay(), back.timeOfDay());
        assertEquals(doc.serverAddress(), back.serverAddress());

        assertEquals("the gameplay track must round-trip", doc.track().size(), back.track().size());
        ReplayTrack.Frame f = back.track().at(500L);
        assertEquals(12f, f.x, 0.001f);
        assertEquals(95f, f.yawDeg, 0.001f);

        assertEquals("the markers must round-trip", 2, back.markers().size());
        assertEquals(ReplayMarkers.Kind.DEATH, back.markers().at(1).kind);

        assertEquals("the camera path must round-trip", 2, back.cameraPath().size());
        assertEquals(ReplayCameraPath.Preset.ORBIT, back.cameraPath().preset());
    }

    @Test
    public void theDocumentIsSelfDescribing() {
        // A reader that is handed an arbitrary file must be able to tell what it is holding, so the
        // format tag and version are on the wire, not inferred from the file name.
        String json = sample().serialize();
        assertTrue("the format tag must be written", json.contains("\"format\""));
        assertTrue(json.contains("glowreplay"));
        assertTrue("the version must be written", json.contains("\"version\""));
        assertTrue(ReplayDocument.looksLikeGlowReplay(json));
        assertFalse(ReplayDocument.looksLikeGlowReplay("{\"format\":\"something-else\"}"));
        assertFalse(ReplayDocument.looksLikeGlowReplay("not json at all"));
    }

    @Test
    public void itWritesAndReadsAGlowReplayFile() throws Exception {
        File dir = folder.newFolder("replays");
        ReplayDocument doc = sample();
        File file = ReplayDocument.write(dir, "clip-001", doc);

        assertEquals("the file must carry the .glowreplay extension", ".glowreplay",
                ReplayDocument.extension());
        assertTrue(file.getName().endsWith(".glowreplay"));
        assertTrue(file.isFile());

        ReplayDocument back = ReplayDocument.read(file);
        assertTrue(ReplayDocument.looksLikeGlowReplay(
                new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)));
        assertEquals(doc.world(), back.world());
        assertEquals(doc.track().size(), back.track().size());
        assertEquals(doc.markers().size(), back.markers().size());
    }

    @Test
    public void aCorruptOrForeignFileFailsClosed() throws Exception {
        // A reader must never throw on a bad file: an unreadable replay is "no replay", not a crash.
        File dir = folder.newFolder("bad");
        File notJson = new File(dir, "junk.glowreplay");
        Files.write(notJson.toPath(), "this is not json".getBytes(StandardCharsets.UTF_8));
        assertEquals(0, ReplayDocument.read(notJson).track().size());
        assertFalse(ReplayDocument.looksLikeGlowReplay("this is not json"));

        // A foreign but valid JSON document is parsed as an empty recording rather than throwing.
        ReplayDocument foreign = ReplayDocument.parse("{\"format\":\"other\",\"version\":9}");
        assertEquals(0, foreign.track().size());
        assertEquals(0, foreign.markers().size());

        // A null or empty file reads as an empty recording.
        assertEquals(0, ReplayDocument.read(null).track().size());
    }

    @Test
    public void anEmptyDocumentStillCarriesItsIdentity() {
        // A recording that captured no track still has to say what it is and when it happened.
        ReplayDocument doc = ReplayDocument.of(123L, "w", "1.0", "creative");
        String json = doc.serialize();
        assertTrue(ReplayDocument.looksLikeGlowReplay(json));
        ReplayDocument back = ReplayDocument.parse(json);
        assertEquals(123L, back.recordedAtMs());
        assertEquals(0, back.track().size());
        assertEquals("", back.playerSkin());
    }

    @Test
    public void aMissingFieldReadsAsEmptyNotAsZero() {
        // The rule from the rest of the repo: a value with no source is absent, never a plausible 0.
        ReplayDocument doc = ReplayDocument.parse("{\"format\":\"glowreplay\",\"version\":1}");
        assertEquals("", doc.world());
        assertEquals("", doc.playerSkin());
        assertEquals("", doc.serverAddress());
        assertEquals("", doc.worldSeed());
        assertEquals(-1L, doc.timeOfDay());
    }

    @Test
    public void aDocumentBridgesToAndFromAClip() {
        // A portable replay must list in the library exactly like a recorded clip, and a recorded
        // clip must export to a container carrying its metadata and track.
        ReplayDocument doc = sample();
        ReplayClip clip = doc.toClip(new File("/replays/shared.glowreplay"), doc.track().durationMs());
        assertEquals("My World", clip.world());
        assertEquals("1.26.60.28", clip.gameVersion());
        assertEquals("survival", clip.gameMode());
        assertEquals(doc.track().size(), clip.track().size());
        assertEquals(2, clip.markers().size());
        assertEquals(ReplayCameraPath.Preset.ORBIT, clip.cameraPath().preset());

        ReplayDocument exported = ReplayDocument.fromClip(clip);
        assertEquals(clip.world(), exported.world());
        assertEquals(clip.track().size(), exported.track().size());
        assertEquals(clip.markers().size(), exported.markers().size());
    }

    @Test
    public void aGlowReplayFileIsSharedAsJsonNotVideo() {
        // Declaring a JSON document as video/mp4 makes a receiver try to play it and fail.
        assertEquals(ReplaySharing.REPLAY_MIME_TYPE,
                ReplaySharing.mimeType(new File("/replays/clip.glowreplay")));
        assertEquals(ReplaySharing.MIME_TYPE,
                ReplaySharing.mimeType(new File("/replays/clip.mp4")));
    }

    @Test
    public void theLibraryListsBothClipKinds() throws Exception {
        File dir = folder.newFolder("lib");
        File mp4 = new File(dir, "replay_2026.mp4");
        File glow = ReplayDocument.write(dir, "portable", sample());
        Files.write(mp4.toPath(), new byte[] {0, 1, 2});
        assertTrue("a recorded mp4 is a clip", ReplayStorage.isClip(mp4));
        assertTrue("a portable .glowreplay is a clip", ReplayStorage.isClip(glow));
        assertFalse(ReplayStorage.isClip(new File(dir, "notes.txt")));
    }
}
