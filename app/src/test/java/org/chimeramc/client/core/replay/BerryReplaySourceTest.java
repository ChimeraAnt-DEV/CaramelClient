package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * The BerryAPI replay playback source, pinned without the library.
 *
 * <p>The source is the one place the panel talks to the native surface, so its fail-closed rules are
 * what keep a build without BerryAPI behaving exactly as before: unavailable means every call
 * answers the "not available" default and nothing is loaded.
 */
public class BerryReplaySourceTest {

    /** A recording backend, so every call the source makes is observable. */
    private static final class Fake implements BerryReplaySource.Backend {
        boolean available = true;
        boolean loaded;
        long[] info;
        long[] playback;
        final List<String> calls = new ArrayList<>();
        int loadStatus = ReplayAbi.OK;
        boolean speedApplied;

        @Override public boolean available() { return available; }
        @Override public boolean loaded() { return loaded; }
        @Override public long[] info() { return info; }
        @Override public long[] playback() { return playback; }
        @Override public int load(byte[] b) { calls.add("load"); loaded = loadStatus == ReplayAbi.OK; return loadStatus; }
        @Override public int play() { calls.add("play"); return ReplayAbi.OK; }
        @Override public int seek(long t) { calls.add("seek:" + t); return ReplayAbi.OK; }
        @Override public void pause() { calls.add("pause"); }
        @Override public void stop() { calls.add("stop"); }
        @Override public void unload() { calls.add("unload"); loaded = false; }
        @Override public void setSpeed(float s) { calls.add("speed:" + s); speedApplied = true; }
        @Override public String status() { return "status"; }
        @Override public byte[] save() { calls.add("save"); return saved; }
        byte[] saved = new byte[] {1, 2, 3};
    }

    @Test
    public void whenUnavailableEveryCallFailsClosed() {
        Fake fake = new Fake();
        fake.available = false;
        BerryReplaySource src = new BerryReplaySource(fake);

        assertFalse(src.available());
        assertFalse(src.loaded());
        assertEquals(ReplayAbi.NOT_ATTACHED, src.load(new byte[] {1, 2, 3}));
        assertEquals(ReplayAbi.NOT_ATTACHED, src.play());
        assertEquals(ReplayAbi.NOT_ATTACHED, src.seek(5));
        assertFalse(src.setSpeed(2f));
        assertEquals("", src.status());
        assertFalse(src.snapshot().loaded);
        // Nothing reached the backend.
        assertTrue("no call must reach an unavailable backend: " + fake.calls, fake.calls.isEmpty());
        // The no-op calls must not throw either.
        src.pause();
        src.stop();
        src.unload();
        assertTrue(fake.calls.isEmpty());
    }

    @Test
    public void loadRejectsNullOrEmptyBytesWithoutCallingTheBackend() {
        Fake fake = new Fake();
        BerryReplaySource src = new BerryReplaySource(fake);
        assertEquals(ReplayAbi.BAD_ARGUMENT, src.load(null));
        assertEquals(ReplayAbi.BAD_ARGUMENT, src.load(new byte[0]));
        assertTrue(fake.calls.isEmpty());
    }

    @Test
    public void aSuccessfulLoadIsReportedAndASnapshotCarriesEveryField() {
        Fake fake = new Fake();
        fake.info = new long[] {1, 100, 400, 3, 42, 2048, 7, 924};
        // speed 2.0f as a float bit pattern in the last slot.
        fake.playback = new long[] {1, 1, 1, 250, Float.floatToIntBits(2.0f)};
        BerryReplaySource src = new BerryReplaySource(fake);

        assertEquals(ReplayAbi.OK, src.load(new byte[] {9}));
        assertTrue(src.loaded());
        BerryReplaySource.Snapshot s = src.snapshot();
        assertTrue(s.loaded);
        assertEquals(100L, s.firstTick);
        assertEquals(400L, s.lastTick);
        assertEquals(3, s.sections);
        assertEquals(42, s.records);
        assertEquals(2048L, s.payloadBytes);
        assertEquals(300L, s.tickLength());
        assertTrue(s.playing);
        assertEquals(250L, s.tick);
        assertEquals(2.0f, s.speed, 1e-4f);
    }

    @Test
    public void aRefusedLoadLeavesNothingLoaded() {
        Fake fake = new Fake();
        fake.loadStatus = ReplayAbi.BAD_REPLAY;
        BerryReplaySource src = new BerryReplaySource(fake);
        assertEquals(ReplayAbi.BAD_REPLAY, src.load(new byte[] {1}));
        assertFalse("a refused load must not report loaded", src.loaded());
        assertFalse(src.snapshot().loaded);
    }

    @Test
    public void anAbsentInfoOrPlaybackReadsAsAnEmptySnapshot() {
        Fake fake = new Fake();
        BerryReplaySource src = new BerryReplaySource(fake);
        assertFalse(src.snapshot().loaded);
        fake.info = new long[] {0}; // not loaded
        assertFalse(src.snapshot().loaded);
        fake.info = new long[] {1, 0, 0, 0, 0, 0, 0, 0};
        fake.playback = null;
        BerryReplaySource.Snapshot s = src.snapshot();
        assertTrue(s.loaded);
        assertFalse("no playback reading means not playing", s.playing);
        assertEquals(1f, s.speed, 1e-4f);
    }

    @Test
    public void theSpeedIsRangeChecked() {
        Fake fake = new Fake();
        BerryReplaySource src = new BerryReplaySource(fake);
        assertTrue(src.setSpeed(2f));
        assertTrue(fake.speedApplied);
        assertFalse("zero is out of range", src.setSpeed(0f));
        assertFalse("negative is out of range", src.setSpeed(-1f));
        assertFalse("above the library's cap is refused", src.setSpeed(33f));
        assertFalse("NaN is refused", src.setSpeed(Float.NaN));
    }

    @Test
    public void saveExportsTheLibraryReplayAndFailsClosedWhenUnavailable() {
        Fake fake = new Fake();
        BerryReplaySource src = new BerryReplaySource(fake);
        byte[] bytes = src.save();
        assertEquals(3, bytes.length);
        assertTrue("save must reach the backend", fake.calls.contains("save"));

        // Unavailable: no bytes, and the backend is never called.
        Fake off = new Fake();
        off.available = false;
        BerryReplaySource offSrc = new BerryReplaySource(off);
        assertEquals(0, offSrc.save().length);
        assertFalse("an unavailable surface must not call save", off.calls.contains("save"));

        // A null backend answer reads as "nothing to save", never a null return.
        Fake nuller = new Fake();
        nuller.saved = null;
        assertEquals(0, new BerryReplaySource(nuller).save().length);
    }
}
