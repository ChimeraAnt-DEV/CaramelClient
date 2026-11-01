package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the recorder's sampling decisions: what it reads, what a missing reading becomes, and that a
 * throwing feed degrades to "no data" rather than taking the recorder down.
 *
 * <p>Pure, no mocks: the {@link ReplayTrackRecorder.Source} is a small script rather than a mock
 * library, which is enough because the seam is three array-returning methods.
 */
public class ReplayTrackRecorderTest {

    private static final float EPS = 1e-3f;

    /** A scriptable feed: each method returns whatever the test set, or null. */
    private static final class FakeSource implements ReplayTrackRecorder.Source {
        float[] position;
        float[] rotation;
        float[] health;
        boolean throwOnPosition;

        @Override
        public float[] position() {
            if (throwOnPosition) throw new IllegalStateException("feed down");
            return position;
        }

        @Override
        public float[] rotation() {
            return rotation;
        }

        @Override
        public float[] health() {
            return health;
        }
    }

    @Test
    public void aFullReadingIsStored() {
        FakeSource source = new FakeSource();
        source.position = new float[]{1f, 64f, -2f};
        source.rotation = new float[]{90f, -10f};
        source.health = new float[]{18f};
        ReplayTrackRecorder recorder = new ReplayTrackRecorder(source);
        assertTrue(recorder.tick(0L));
        ReplayTrack.Frame frame = recorder.track().frameAt(0);
        assertEquals(1f, frame.x, EPS);
        assertEquals(64f, frame.y, EPS);
        assertEquals(-2f, frame.z, EPS);
        assertEquals(90f, frame.yawDeg, EPS);
        assertEquals(18f, frame.health, EPS);
    }

    @Test
    public void aMissingReadingBecomesUnknownNotZero() {
        FakeSource source = new FakeSource();
        source.position = new float[]{1f, 2f, 3f};
        // No rotation, no health.
        ReplayTrackRecorder recorder = new ReplayTrackRecorder(source);
        assertTrue(recorder.tick(0L));
        ReplayTrack.Frame frame = recorder.track().frameAt(0);
        assertTrue(Float.isNaN(frame.yawDeg));
        assertTrue(Float.isNaN(frame.health));
        assertFalse(frame.hasRotation());
        assertTrue(frame.hasPosition());
    }

    @Test
    public void aShortArrayIsTreatedAsNoReading() {
        FakeSource source = new FakeSource();
        source.position = new float[]{1f, 2f}; // needs three
        source.rotation = new float[]{90f};    // needs two
        source.health = new float[0];
        ReplayTrackRecorder recorder = new ReplayTrackRecorder(source);
        // Nothing usable and nothing to sample: the frame is not stored.
        assertFalse(recorder.tick(0L));
        assertTrue(recorder.track().isEmpty());
    }

    @Test
    public void aThrowingFeedDegradesToNoDataRatherThanCrashing() {
        FakeSource source = new FakeSource();
        source.throwOnPosition = true;
        source.rotation = new float[]{45f, 0f};
        ReplayTrackRecorder recorder = new ReplayTrackRecorder(source);
        assertTrue(recorder.tick(0L));
        ReplayTrack.Frame frame = recorder.track().frameAt(0);
        assertFalse(frame.hasPosition());
        assertTrue(frame.hasRotation());
    }

    @Test
    public void aNullSourceNeverSamples() {
        ReplayTrackRecorder recorder = new ReplayTrackRecorder(null);
        assertFalse(recorder.tick(0L));
        assertTrue(recorder.track().isEmpty());
    }

    @Test
    public void theCadenceIsEnforcedAcrossTicks() {
        FakeSource source = new FakeSource();
        source.position = new float[]{0f, 0f, 0f};
        ReplayTrackRecorder recorder = new ReplayTrackRecorder(source);
        assertTrue(recorder.tick(0L));
        assertFalse(recorder.tick(50L));
        assertTrue(recorder.tick(ReplayTrack.MIN_SAMPLE_INTERVAL_MS));
        assertEquals(2, recorder.track().size());
    }
}
