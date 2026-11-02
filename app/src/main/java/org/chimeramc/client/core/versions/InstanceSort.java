package org.chimeramc.client.core.versions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The order the Instances list renders in (item 143).
 *
 * <p>The list used to sort on the version code alone, so an instance played this morning sat
 * wherever its version string fell. The default is now most-recently-played first, because the
 * instance a user wants is almost always the one they played last; a never-played instance sorts
 * after every played one, ordered among themselves by version code so the order is stable rather
 * than arbitrary.
 *
 * <p>Pure and Android-free, so the ordering rule is unit-tested without a device or a
 * {@code VersionManager}.
 */
public final class InstanceSort {

    /** One sortable instance: what it was last played at, and its version code as the tiebreak. */
    public interface Entry {
        /** The wall-clock time of the last session start, or 0 when never played. */
        long lastPlayedMs();

        /** The version code, used to order two instances that have the same last-played time. */
        String versionCode();
    }

    private InstanceSort() {
    }

    /**
     * A comparator implementing the default order: last played descending, then version code
     * descending.
     *
     * <p>Generic over the element type so a caller can sort its own list without wrapping each
     * element in an {@link Entry}; the two accessors are supplied as functions. A null element is
     * ordered last rather than throwing, because a list with a null in it must still render.
     *
     * <p>A version code is compared as text, which is the convention the list already used and is
     * correct for the launcher's own strings ("1.26.60.28"); it is not a numeric compare, and it is
     * deliberately not one because the codes are dotted and a numeric compare would misread them.
     */
    public static <T> Comparator<T> comparator(java.util.function.ToLongFunction<T> lastPlayedMs,
                                               java.util.function.Function<T, String> versionCode) {
        return (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            int byPlayed = Long.compare(lastPlayedMs.applyAsLong(b), lastPlayedMs.applyAsLong(a));
            if (byPlayed != 0) return byPlayed;
            String ca = versionCode.apply(a);
            String cb = versionCode.apply(b);
            return (cb == null ? "" : cb).compareTo(ca == null ? "" : ca);
        };
    }

    /** The default order for an {@link Entry}. */
    public static <T extends Entry> Comparator<T> defaultOrder() {
        return comparator(Entry::lastPlayedMs, Entry::versionCode);
    }

    /** Sorts a list in place with the default order. Null entries sort last and never throw. */
    public static <T extends Entry> void sort(List<T> list) {
        if (list == null) return;
        List<T> copy = new ArrayList<>(list);
        copy.removeAll(Collections.singleton(null));
        Collections.sort(copy, defaultOrder());
        list.clear();
        list.addAll(copy);
    }
}
