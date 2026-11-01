package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * The marker store behind item 241.
 *
 * <p>No mocks: the class is pure, so the rules (ordering, merging, serialisation, parse tolerance)
 * are exercised directly. The serialisation round-trip is the one that matters most — a marker set
 * that survives a write and a read is what makes a timeline tick reappear after the app restarts.
 */
public class ReplayMarkersTest {

    @Test
    public void markersAreKeptInTimeOrder() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(5_000L, ReplayMarkers.Kind.DEATH, "");
        markers.add(1_000L, ReplayMarkers.Kind.JOIN, "");
        markers.add(3_000L, ReplayMarkers.Kind.COMBO, "3");
        List<ReplayMarkers.Marker> all = markers.all();
        assertEquals(3, all.size());
        assertEquals(1_000L, all.get(0).tMs);
        assertEquals(3_000L, all.get(1).tMs);
        assertEquals(5_000L, all.get(2).tMs);
    }

    @Test
    public void sameKindWithinTheWindowMerges() {
        ReplayMarkers markers = ReplayMarkers.empty();
        assertTrue(markers.add(1_000L, ReplayMarkers.Kind.COMBO, "2"));
        // 500ms later: inside the merge window, so it updates the existing marker.
        assertFalse(markers.add(1_500L, ReplayMarkers.Kind.COMBO, "3"));
        assertEquals(1, markers.size());
        assertEquals("3", markers.at(0).detail);
    }

    @Test
    public void differentKindsInTheSameWindowDoNotMerge() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(1_000L, ReplayMarkers.Kind.DEATH, "");
        markers.add(1_200L, ReplayMarkers.Kind.KILL_STREAK, "5");
        assertEquals(2, markers.size());
    }

    @Test
    public void sameKindOutsideTheWindowStaysSeparate() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(1_000L, ReplayMarkers.Kind.DEATH, "");
        markers.add(1_000L + ReplayMarkers.MERGE_WINDOW_MS + 1L, ReplayMarkers.Kind.DEATH, "");
        assertEquals(2, markers.size());
    }

    @Test
    public void aNegativeTimeClampsToZero() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(-500L, ReplayMarkers.Kind.JOIN, "");
        assertEquals(0L, markers.at(0).tMs);
    }

    @Test
    public void serialisationRoundTrips() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(0L, ReplayMarkers.Kind.JOIN, "");
        markers.add(2_500L, ReplayMarkers.Kind.KILL_STREAK, "5");
        markers.add(9_000L, ReplayMarkers.Kind.DEATH, "");
        ReplayMarkers parsed = ReplayMarkers.parse(markers.serialize());
        assertEquals(markers.size(), parsed.size());
        for (int i = 0; i < markers.size(); i++) {
            assertEquals(markers.at(i).tMs, parsed.at(i).tMs);
            assertEquals(markers.at(i).kind, parsed.at(i).kind);
            assertEquals(markers.at(i).detail, parsed.at(i).detail);
        }
    }

    @Test
    public void aCorruptEntryIsSkippedNotFatal() {
        ReplayMarkers parsed = ReplayMarkers.parse("1000:DEATH:;garbage;2000:COMBO:4");
        assertEquals(2, parsed.size());
        assertEquals(ReplayMarkers.Kind.DEATH, parsed.at(0).kind);
        assertEquals(ReplayMarkers.Kind.COMBO, parsed.at(1).kind);
    }

    @Test
    public void anEmptyStringParsesToNoMarkers() {
        assertTrue(ReplayMarkers.parse("").isEmpty());
        assertTrue(ReplayMarkers.parse(null).isEmpty());
    }

    @Test
    public void aDetailWithSeparatorsIsSanitised() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(1_000L, ReplayMarkers.Kind.COMBO, "a;b:c");
        ReplayMarkers parsed = ReplayMarkers.parse(markers.serialize());
        assertEquals(1, parsed.size());
        assertEquals("abc", parsed.at(0).detail);
    }

    @Test
    public void navigationHelpersFindTheNeighbours() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(1_000L, ReplayMarkers.Kind.JOIN, "");
        markers.add(3_000L, ReplayMarkers.Kind.DEATH, "");
        assertEquals(3_000L, markers.next(2_000L).tMs);
        assertEquals(1_000L, markers.previous(2_000L).tMs);
        assertNull(markers.next(4_000L));
        assertNull(markers.previous(0L));
    }

    @Test
    public void labelsReadNaturally() {
        ReplayMarkers markers = ReplayMarkers.empty();
        markers.add(1_000L, ReplayMarkers.Kind.KILL_STREAK, "5");
        assertEquals("Kill streak 5", markers.at(0).label());
        markers.add(5_000L, ReplayMarkers.Kind.DEATH, "");
        assertEquals("Death", markers.at(1).label());
        markers.add(9_000L, ReplayMarkers.Kind.COMBO, "");
        assertEquals("Combo", markers.at(2).label());
        markers.add(12_000L, ReplayMarkers.Kind.JOIN, "");
        assertEquals("Joined world", markers.at(3).label());
    }

    @Test
    public void anOutOfRangeIndexReturnsNull() {
        ReplayMarkers markers = ReplayMarkers.empty();
        assertNull(markers.at(0));
        assertNull(markers.at(-1));
        markers.add(1_000L, ReplayMarkers.Kind.JOIN, "");
        assertNotNull(markers.at(0));
        assertNull(markers.at(1));
    }
}
