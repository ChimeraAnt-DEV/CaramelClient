package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The entity track: the other players' positions over a recording.
 *
 * <p>The recorder captured only the local player, so a replay had nothing to show beside the camera.
 * The shipped build cannot read other <em>game</em> entities (the doc records why), but a proximity
 * voice peer advertises its position and facing, so those are recorded — the one honest entity feed
 * the client has. This class is the pure store, keyed by entity id, so the multi-entity rules are
 * unit-tested without a device.
 */
public class ReplayEntityTrackTest {

    @Test
    public void itStoresAFramePerEntityAndInterpolatesEach() {
        ReplayEntityTrack track = new ReplayEntityTrack();
        assertTrue(track.sample(0L, "alice", 0f, 64f, 0f, 0f, 0f));
        assertTrue(track.sample(0L, "bob", 100f, 64f, 100f, 0f, 0f));
        assertTrue(track.sample(1000L, "alice", 10f, 64f, 0f, 90f, 0f));

        assertEquals(2, track.entityCount());
        ReplayEntityTrack.Frame f = track.at(500L, "alice");
        assertEquals(5f, f.x, 0.001f);
        assertEquals(45f, f.yawDeg, 0.001f);
        // bob has only one frame, so he holds it.
        assertEquals(100f, track.at(500L, "bob").x, 0.001f);
        // An unknown entity has no frame.
        assertEquals(null, track.at(500L, "carol"));
    }

    @Test
    public void itHonoursTheCadencePerEntity() {
        ReplayEntityTrack track = new ReplayEntityTrack();
        assertTrue(track.sample(0L, "alice", 0f, 0f, 0f, 0f, 0f));
        // Inside the cadence for the same entity: dropped.
        assertFalse(track.sample(50L, "alice", 1f, 0f, 0f, 0f, 0f));
        // A different entity has its own cadence, so its first frame lands immediately.
        assertTrue(track.sample(50L, "bob", 1f, 0f, 0f, 0f, 0f));
    }

    @Test
    public void itRefusesAnEntityWithNoPosition() {
        ReplayEntityTrack track = new ReplayEntityTrack();
        assertFalse(track.sample(0L, "alice", Float.NaN, 0f, 0f, 0f, 0f));
        assertFalse(track.sample(0L, "", 1f, 2f, 3f, 0f, 0f));
        assertFalse(track.sample(0L, null, 1f, 2f, 3f, 0f, 0f));
        assertEquals(0, track.entityCount());
    }

    @Test
    public void itSerialisesAndParsesARoundTrip() {
        ReplayEntityTrack track = new ReplayEntityTrack();
        track.sample(0L, "alice", 1f, 64f, 2f, 90f, -5f);
        track.sample(1000L, "alice", 11f, 64f, 2f, 95f, -5f);
        track.sample(0L, "bob", 5f, 70f, 6f, 0f, 0f);

        String encoded = track.serialize();
        assertFalse(encoded.isEmpty());
        ReplayEntityTrack back = ReplayEntityTrack.parse(encoded);
        assertEquals(2, back.entityCount());
        assertEquals(11f, back.at(1000L, "alice").x, 0.001f);
        assertEquals(5f, back.at(0L, "bob").x, 0.001f);
    }

    @Test
    public void aCorruptEntryIsSkippedNotThrown() {
        ReplayEntityTrack track = ReplayEntityTrack.parse("alice|nonsense&bob|0,1,2,3,4,5");
        // The good entity survives; the bad frame is dropped and never registers an entity.
        assertEquals(1, track.entityCount());
        assertEquals(1f, track.at(0L, "bob").x, 0.001f);
        assertTrue(ReplayEntityTrack.parse(null).isEmpty());
        assertTrue(ReplayEntityTrack.parse("").isEmpty());
    }

    @Test
    public void itCapsTheEntitiesAndFramesPerEntity() {
        ReplayEntityTrack track = new ReplayEntityTrack();
        for (int i = 0; i < ReplayEntityTrack.MAX_ENTITIES + 20; i++) {
            track.sample(0L, "e" + i, i, 0f, 0f, 0f, 0f);
        }
        assertTrue("the entity count must be bounded",
                track.entityCount() <= ReplayEntityTrack.MAX_ENTITIES);
    }
}
