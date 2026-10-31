package org.chimeramc.client.core.mods.inbuilt.overlay;

import org.chimeramc.client.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The pause-menu button row's entries (item 224).
 *
 * <p>The row offers the four actions a player reaches for while the game is paused — take a
 * screenshot, hide the HUD for that screenshot, switch the overlay layout, and open the log viewer.
 * The set is modelled as a pure list so the row and its tests agree about what exists, and so a new
 * entry is one line rather than another hand-built button.
 */
public final class PauseMenuActions {

    /** Take a screenshot of the launcher's own window. */
    public static final String ACTION_SCREENSHOT = "screenshot";
    /** Hide or show every HUD overlay, the same state the three-finger tap toggles. */
    public static final String ACTION_HIDE_HUD = "hide_hud";
    /** Cycle to the next saved overlay layout. */
    public static final String ACTION_SWITCH_LAYOUT = "switch_layout";
    /** Show or hide the log viewer. */
    public static final String ACTION_OPEN_LOGS = "open_logs";

    /** One entry in the row. */
    public static final class Entry {
        private final String id;
        private final int labelRes;
        private final int iconRes;

        Entry(String id, int labelRes, int iconRes) {
            this.id = id;
            this.labelRes = labelRes;
            this.iconRes = iconRes;
        }

        public String getId() {
            return id;
        }

        public int getLabelRes() {
            return labelRes;
        }

        public int getIconRes() {
            return iconRes;
        }
    }

    private static final List<Entry> ENTRIES = Collections.unmodifiableList(build());

    private PauseMenuActions() {
    }

    private static List<Entry> build() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(ACTION_SCREENSHOT, R.string.pause_menu_screenshot,
                R.drawable.ic_screenshot));
        entries.add(new Entry(ACTION_HIDE_HUD, R.string.pause_menu_hide_hud,
                R.drawable.ic_hud));
        entries.add(new Entry(ACTION_SWITCH_LAYOUT, R.string.pause_menu_switch_layout,
                R.drawable.ic_layout));
        entries.add(new Entry(ACTION_OPEN_LOGS, R.string.pause_menu_open_logs,
                R.drawable.ic_logs));
        return entries;
    }

    /** Every entry, in display order. */
    public static List<Entry> all() {
        return ENTRIES;
    }

    /** The entry with this id, or null. */
    public static Entry byId(String id) {
        if (id == null) return null;
        for (Entry entry : ENTRIES) {
            if (id.equals(entry.id)) return entry;
        }
        return null;
    }
}
