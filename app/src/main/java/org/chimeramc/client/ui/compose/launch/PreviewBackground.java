package org.chimeramc.client.ui.compose.launch;

import java.util.ArrayList;
import java.util.List;

/**
 * The backgrounds a player can put behind the launch tab's character preview.
 *
 * <p>The set is a small, ordered catalogue so the picker can show a preview of each and the chosen
 * one can be stored by a stable id (a stored index would point at the wrong image if the list is
 * ever reordered). The drawable resource id is carried here rather than resolved by the caller, so
 * adding a background is one entry.
 *
 * <p>Pure: it holds ids and resource numbers, so the ordering, the id lookup and the fallback are
 * unit-tested without a device.
 */
public final class PreviewBackground {

    /** One background. */
    public static final class Entry {
        public final String id;
        public final int drawableRes;
        public final String label;

        public Entry(String id, int drawableRes, String label) {
            this.id = id;
            this.drawableRes = drawableRes;
            this.label = label;
        }
    }

    /** The default background's id, used when nothing has been chosen or a stored id is unknown. */
    public static final String DEFAULT_ID = "paint_1";

    private static final List<Entry> ENTRIES = build();

    private PreviewBackground() {
    }

    /** Every background, in display order. */
    public static List<Entry> all() {
        return ENTRIES;
    }

    /** The background with this id, or the default when the id is unknown. */
    public static Entry resolve(String id) {
        if (id != null) {
            for (Entry e : ENTRIES) {
                if (e.id.equals(id)) return e;
            }
        }
        return ENTRIES.get(0);
    }

    /** The index of an id in {@link #all()}, or 0 when unknown. */
    public static int indexOf(String id) {
        if (id != null) {
            for (int i = 0; i < ENTRIES.size(); i++) {
                if (ENTRIES.get(i).id.equals(id)) return i;
            }
        }
        return 0;
    }

    /** The next id in the list, wrapping, so a tap can cycle without the picker. */
    public static String next(String id) {
        int i = indexOf(id);
        return ENTRIES.get((i + 1) % ENTRIES.size()).id;
    }

    private static List<Entry> build() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(DEFAULT_ID,
                org.chimeramc.client.R.drawable.launch_bg_1, "Paint splash"));
        out.add(new Entry("paint_2",
                org.chimeramc.client.R.drawable.launch_bg_2, "Ripped paper"));
        out.add(new Entry("paint_3",
                org.chimeramc.client.R.drawable.launch_bg_3, "Brush stroke"));
        return out;
    }
}
