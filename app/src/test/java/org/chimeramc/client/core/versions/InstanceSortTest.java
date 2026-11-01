package org.chimeramc.client.core.versions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pins {@link InstanceSort}: most recently played first, version code as the tiebreak, and the two
 * edge cases the list has to survive (never played, and a null entry in the list).
 */
public class InstanceSortTest {

    private static final class Row implements InstanceSort.Entry {
        final long played;
        final String code;

        Row(long played, String code) {
            this.played = played;
            this.code = code;
        }

        @Override
        public long lastPlayedMs() {
            return played;
        }

        @Override
        public String versionCode() {
            return code;
        }
    }

    @Test
    public void mostRecentlyPlayedSortsFirst() {
        Row older = new Row(1_000L, "1.20.0");
        Row newer = new Row(2_000L, "1.19.0");
        List<Row> rows = new ArrayList<>(Arrays.asList(older, newer));
        InstanceSort.sort(rows);
        assertEquals(newer, rows.get(0));
        assertEquals(older, rows.get(1));
    }

    @Test
    public void aNeverPlayedInstanceSortsAfterEveryPlayedOne() {
        Row played = new Row(1L, "1.20.0");
        Row never = new Row(0L, "1.26.60.28");
        List<Row> rows = new ArrayList<>(Arrays.asList(never, played));
        InstanceSort.sort(rows);
        assertEquals(played, rows.get(0));
        assertEquals(never, rows.get(1));
    }

    @Test
    public void theVersionCodeBreaksATieByDescendingText() {
        Row low = new Row(5_000L, "1.20.0");
        Row high = new Row(5_000L, "1.26.60.28");
        List<Row> rows = new ArrayList<>(Arrays.asList(low, high));
        InstanceSort.sort(rows);
        assertEquals(high, rows.get(0));
        assertEquals(low, rows.get(1));
    }

    @Test
    public void aNullVersionCodeSortsLastAmongTiesAndDoesNotThrow() {
        Row withCode = new Row(7L, "1.21.0");
        Row noCode = new Row(7L, null);
        List<Row> rows = new ArrayList<>(Arrays.asList(noCode, withCode));
        InstanceSort.sort(rows);
        assertEquals(withCode, rows.get(0));
        assertEquals(noCode, rows.get(1));
    }

    @Test
    public void aNullEntryIsDroppedRatherThanCrashingTheList() {
        Row a = new Row(1L, "1.20.0");
        List<Row> rows = new ArrayList<>(Arrays.asList(null, a, null));
        InstanceSort.sort(rows);
        assertEquals(1, rows.size());
        assertEquals(a, rows.get(0));
    }

    @Test
    public void sortingAnEmptyOrNullListIsANoOp() {
        List<Row> empty = new ArrayList<>();
        InstanceSort.sort(empty);
        assertEquals(0, empty.size());
        InstanceSort.sort(null);
    }
}
