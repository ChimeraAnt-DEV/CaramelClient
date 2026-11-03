package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The client's mirror of BerryAPI's replay C ABI, pinned without the native library.
 *
 * <p>The mirror is what makes an ABI drift visible in CI: the status enum, the version guard and the
 * status-line parse are all exercised here, so a change to BerryAPI's contract fails a test rather
 * than a device.
 */
public class ReplayAbiTest {

    @Test
    public void theStatusNamesMatchTheHeader() {
        assertEquals("ok", ReplayAbi.statusName(ReplayAbi.OK));
        assertEquals("not attached", ReplayAbi.statusName(ReplayAbi.NOT_ATTACHED));
        assertEquals("bad replay", ReplayAbi.statusName(ReplayAbi.BAD_REPLAY));
        assertEquals("bad argument", ReplayAbi.statusName(ReplayAbi.BAD_ARGUMENT));
        assertEquals("no replay loaded", ReplayAbi.statusName(ReplayAbi.NOT_LOADED));
        assertEquals("unknown", ReplayAbi.statusName(999));
    }

    @Test
    public void theVersionGuardRefusesADrift() {
        assertTrue(ReplayAbi.isCompatible(ReplayAbi.ABI_VERSION, 120));
        assertFalse("a different abi version must be refused",
                ReplayAbi.isCompatible(ReplayAbi.ABI_VERSION + 1, 120));
        assertFalse("a zero struct size must be refused", ReplayAbi.isCompatible(1, 0));
    }

    @Test
    public void theUnavailableReasonNamesTheCause() {
        assertTrue(ReplayAbi.unavailableReason(false, 0).contains("not present"));
        assertTrue(ReplayAbi.unavailableReason(true, 2).contains("expects v1"));
        assertEquals("", ReplayAbi.unavailableReason(true, ReplayAbi.ABI_VERSION));
    }

    @Test
    public void theInfoJsonParsesIntoAStatus() {
        ReplayAbi.Status s = ReplayAbi.parseInfoJson("{\"loaded\":1,\"first_tick\":100,"
                + "\"last_tick\":400,\"section_count\":3,\"record_count\":42,"
                + "\"payload_bytes\":2048}");
        assertTrue(s.loaded);
        assertEquals(100L, s.firstTick);
        assertEquals(400L, s.lastTick);
        assertEquals(3, s.sections);
        assertEquals(42, s.records);
        assertEquals(2048L, s.payloadBytes);
        assertEquals(300L, s.tickLength());
    }

    @Test
    public void aNotLoadedInfoReadsAsEmpty() {
        ReplayAbi.Status s = ReplayAbi.parseInfoJson("{\"loaded\":0}");
        assertFalse(s.loaded);
        assertEquals(0L, s.tickLength());
        assertFalse(ReplayAbi.parseInfoJson(null).loaded);
        assertFalse("a malformed body must not throw", ReplayAbi.parseInfoJson("nope").loaded);
    }

    @Test
    public void theHumanStatusLineIsReadLeniently() {
        ReplayAbi.Status s = ReplayAbi.parseStatus(
                "replay loaded: sections=3 records=42 bytes=2048 first=100 last=400");
        assertTrue(s.loaded);
        assertEquals(3, s.sections);
        assertEquals(42, s.records);
        assertEquals(2048L, s.payloadBytes);
        assertEquals(100L, s.firstTick);
        assertEquals(400L, s.lastTick);

        // A line that does not parse keeps the raw text rather than dropping it.
        ReplayAbi.Status odd = ReplayAbi.parseStatus("something unexpected");
        assertFalse(odd.loaded);
        assertEquals("something unexpected", odd.raw);
    }

    @Test
    public void aStatusRoundTripsToJson() {
        ReplayAbi.Status s = ReplayAbi.parseInfoJson("{\"loaded\":1,\"first_tick\":1,"
                + "\"last_tick\":2,\"section_count\":1,\"record_count\":5,\"payload_bytes\":64}");
        String json = ReplayAbi.toJson(s);
        ReplayAbi.Status back = ReplayAbi.parseInfoJson(json);
        assertTrue(back.loaded);
        assertEquals(1L, back.firstTick);
        assertEquals(2L, back.lastTick);
        assertEquals(5, back.records);
    }
}
