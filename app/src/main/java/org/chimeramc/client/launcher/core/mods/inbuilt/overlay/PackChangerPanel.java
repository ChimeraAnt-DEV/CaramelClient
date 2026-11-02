package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.core.content.InGamePackChanger;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.util.LauncherStorage;

import java.io.File;
import java.util.List;

/**
 * The Packs section of the in-game Mod Menu: the instance's resource packs with an on/off switch
 * each, so the player can change what is applied without leaving the world.
 *
 * <p>It is only offered when the instance has the in-game pack changer enabled in Instance
 * Settings. The section is built in code because the pack list is data-driven and must be
 * re-read after every toggle. It writes the same {@code global_resource_packs.json} the game
 * reads, through {@link InGamePackChanger}.
 */
public final class PackChangerPanel {

    private final Activity activity;
    private final boolean compact;
    private final LinearLayout root;
    private final LinearLayout listColumn;
    private TextView statusLine;
    /** Sizes and texture resolutions are scanned off the main thread and cached (item 253). */
    private final org.chimeramc.client.core.content.PackAssetSummaryCache assetCache =
            new org.chimeramc.client.core.content.PackAssetSummaryCache();

    public PackChangerPanel(Activity activity, boolean compact) {
        this.activity = activity;
        this.compact = compact;

        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(compact ? 12 : 18), dp(compact ? 8 : 14),
                dp(compact ? 12 : 18), dp(compact ? 8 : 14));

        TextView title = new TextView(activity);
        title.setText(R.string.pack_changer_title);
        title.setTextSize(compact ? 13f : 15f);
        title.setTextColor(0xFFE6E9EC);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        statusLine = new TextView(activity);
        statusLine.setTextSize(compact ? 10f : 11f);
        statusLine.setTextColor(0xFF8F979F);
        statusLine.setPadding(0, dp(2), 0, dp(8));
        root.addView(statusLine);

        ScrollView scroller = new ScrollView(activity);
        scroller.setFillViewport(true);
        listColumn = new LinearLayout(activity);
        listColumn.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(listColumn, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        rebuild();
    }

    public View getView() {
        return root;
    }

    private void rebuild() {
        listColumn.removeAllViews();

        if (!isEnabledForInstance()) {
            statusLine.setText(R.string.pack_changer_disabled);
            return;
        }
        File gameDataDir = resolveListingDir();
        if (gameDataDir == null) {
            statusLine.setText(R.string.pack_changer_no_instance);
            return;
        }

        List<File> dirs = resolveGameDataDirs();
        List<InGamePackChanger.PackEntry> packs = InGamePackChanger.listPacks(dirs);
        int activeCount = 0;
        for (InGamePackChanger.PackEntry pack : packs) {
            if (pack.active) activeCount++;
        }
        statusLine.setText(activity.getString(R.string.pack_changer_status,
                activeCount, packs.size()));

        if (packs.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText(R.string.pack_changer_empty);
            empty.setTextSize(compact ? 11f : 12f);
            empty.setTextColor(0xFF8F979F);
            empty.setPadding(0, dp(12), 0, 0);
            listColumn.addView(empty);
            return;
        }

        // The applied order (last entry wins a conflict) is what the move buttons act on. Rows
        // are listed in that order so "move up" always means "higher priority", never "up the
        // alphabetical list".
        List<String> order = InGamePackChanger.activeOrder(resolveListingDir());
        final List<String> displayed = order.isEmpty() ? activeUuidsInListOrder(packs) : order;
        java.util.Map<String, InGamePackChanger.PackEntry> byUuid = new java.util.HashMap<>();
        for (InGamePackChanger.PackEntry pack : packs) {
            byUuid.put(org.chimeramc.client.core.content.ResourcePackOrder.normalise(pack.uuid), pack);
        }
        java.util.List<InGamePackChanger.PackEntry> ordered = new java.util.ArrayList<>();
        for (String uuid : displayed) {
            InGamePackChanger.PackEntry entry = byUuid.get(uuid);
            if (entry != null) ordered.add(entry);
        }
        for (InGamePackChanger.PackEntry pack : packs) {
            if (!displayed.contains(org.chimeramc.client.core.content.ResourcePackOrder
                    .normalise(pack.uuid))) {
                ordered.add(pack);
            }
        }
        for (int i = 0; i < ordered.size(); i++) {
            InGamePackChanger.PackEntry pack = ordered.get(i);
            listColumn.addView(buildRow(pack, i, ordered));
        }
    }

    /** The active packs' uuids in the list's own order, used when there is no global file. */
    private List<String> activeUuidsInListOrder(List<InGamePackChanger.PackEntry> packs) {
        List<String> result = new java.util.ArrayList<>();
        for (InGamePackChanger.PackEntry pack : packs) {
            if (pack.active) {
                result.add(org.chimeramc.client.core.content.ResourcePackOrder.normalise(pack.uuid));
            }
        }
        return result;
    }

    private View buildRow(InGamePackChanger.PackEntry pack, int index,
                          List<InGamePackChanger.PackEntry> ordered) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(activity);
        name.setText(pack.name);
        name.setTextSize(compact ? 12f : 13f);
        name.setTextColor(0xFFE6E9EC);
        labels.addView(name);

        TextView meta = new TextView(activity);
        meta.setTextSize(compact ? 10f : 11f);
        meta.setTextColor(pack.active ? 0xFF71ACEF : 0xFF8F979F);
        labels.addView(meta);
        bindPackMeta(meta, pack);

        // Priority controls. Only meaningful for an active pack: a pack the game has not applied
        // has no position to change.
        if (pack.active && ordered.size() > 1) {
            LinearLayout priority = new LinearLayout(activity);
            priority.setOrientation(LinearLayout.HORIZONTAL);
            TextView up = buildPriorityButton(R.string.pack_changer_move_up, index > 0);
            up.setOnClickListener(v -> movePriority(ordered, index, index - 1));
            TextView down = buildPriorityButton(R.string.pack_changer_move_down,
                    index < ordered.size() - 1);
            down.setOnClickListener(v -> movePriority(ordered, index, index + 1));
            priority.addView(up);
            priority.addView(down);
            labels.addView(priority);
        }

        // A pill toggle rather than a SwitchMaterial: the overlay is inflated from a plain
        // FrameLayout and has no Material theme context here.
        TextView toggle = new TextView(activity);
        toggle.setPadding(dp(14), dp(6), dp(14), dp(6));
        toggle.setTextSize(compact ? 11f : 12f);
        toggle.setText(pack.active ? R.string.pack_changer_disable
                : R.string.pack_changer_enable);
        toggle.setTextColor(pack.active ? 0xFFE6E9EC : 0xFFA8B0B8);
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(16));
        background.setColor(pack.active ? 0x556236E8 : 0x3324282C);
        background.setStroke(dp(1), pack.active ? 0xFF0F5BE4 : 0x44FFFFFF);
        toggle.setBackground(background);
        toggle.setOnClickListener(v -> {
            // Apply to every candidate root the way the cape/installer does: the game picks its
            // storage from isolation + internal/external, and a write to only the wrong guess is
            // silently ignored. InGamePackChanger.apply then applies it live (in-place reload, or
            // a relaunch) and tells us which happened.
            InGamePackChanger.ApplyOutcome outcome = InGamePackChanger.apply(
                    resolveGameDataDirs(), pack.uuid, pack.version, !pack.active);
            if (outcome == InGamePackChanger.ApplyOutcome.FAILED) {
                statusLine.setText(R.string.pack_changer_write_failed);
                return;
            }
            // A relaunch tears this overlay and the game down; there is nothing left to rebuild,
            // and rebuilding now would race the teardown.
            if (outcome == InGamePackChanger.ApplyOutcome.RESTARTING) {
                statusLine.setText(R.string.pack_changer_restarting);
                return;
            }
            rebuild();
            statusLine.setText(outcome == InGamePackChanger.ApplyOutcome.RELOADED
                    ? R.string.pack_changer_reloaded
                    : R.string.pack_changer_reload_on_next_load);
        });
        row.addView(toggle);

        return row;
    }

    /**
     * Fills a row's meta line with the pack's version/state plus its size and texture resolution.
     *
     * <p>The size and resolution come from a background scan, so the line first shows what is known
     * synchronously and is repainted when the scan lands (item 253).
     */
    private void bindPackMeta(TextView meta, InGamePackChanger.PackEntry pack) {
        String state = pack.active ? activity.getString(R.string.pack_changer_on)
                : activity.getString(R.string.pack_changer_off);
        org.chimeramc.client.core.content.PackAssetSummary.Result summary =
                assetCache.peek(pack.directory);
        if (summary != null) {
            meta.setText(activity.getString(R.string.pack_changer_pack_size,
                    formatBytes(summary.totalBytes), state));
            return;
        }
        meta.setText(activity.getString(R.string.pack_changer_pack_meta, pack.version, state));
        assetCache.get(pack.directory, (dir, result) -> {
            // The row may have been recycled to another pack by the time the scan lands; re-find
            // the view for this pack rather than trusting the captured one.
            if (result != null) rebuild();
        });
    }

    /** A small pill button for the priority controls, greyed when the move is not possible. */
    private TextView buildPriorityButton(int labelRes, boolean enabled) {
        TextView button = new TextView(activity);
        button.setText(labelRes);
        button.setTextSize(compact ? 10f : 11f);
        button.setPadding(dp(10), dp(4), dp(10), dp(4));
        button.setTextColor(enabled ? 0xFFE6E9EC : 0xFF5A6068);
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(12));
        background.setColor(0x3324282C);
        background.setStroke(dp(1), enabled ? 0x66FFFFFF : 0x22FFFFFF);
        button.setBackground(background);
        button.setEnabled(enabled);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), dp(8), 0);
        button.setLayoutParams(params);
        return button;
    }

    /** Moves one pack's priority and rewrites the global list to match. */
    private void movePriority(List<InGamePackChanger.PackEntry> ordered, int from, int to) {
        List<String> uuids = new java.util.ArrayList<>();
        for (InGamePackChanger.PackEntry entry : ordered) {
            uuids.add(org.chimeramc.client.core.content.ResourcePackOrder.normalise(entry.uuid));
        }
        List<String> moved = org.chimeramc.client.core.content.ResourcePackOrder.move(uuids, from, to);
        File dir = resolveListingDir();
        if (dir == null || !InGamePackChanger.reorderActive(dir, moved)) {
            statusLine.setText(R.string.pack_changer_reorder_failed);
            return;
        }
        rebuild();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) {
            return String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format(java.util.Locale.US, "%.1f GB",
                bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /** True when the selected instance opted into the in-game pack changer. */
    private boolean isEnabledForInstance() {
        try {
            GameVersion version = VersionManager.get(activity).getSelectedVersion();
            return version != null && version.inGamePackChangerEnabled;
        } catch (Throwable t) {
            return false;
        }
    }

    /** The first candidate root, used only for listing (the game's packs live in one root). */
    private File resolveListingDir() {
        List<File> dirs = resolveGameDataDirs();
        return dirs.isEmpty() ? null : dirs.get(0);
    }

    private List<File> resolveGameDataDirs() {
        try {
            GameVersion version = VersionManager.get(activity).getSelectedVersion();
            if (version == null) return java.util.Collections.emptyList();
            return LauncherStorage.getCandidateGameDataDirs(
                    activity, version.getStorageProfileId(), version.versionIsolation);
        } catch (Throwable t) {
            return java.util.Collections.emptyList();
        }
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    /** Stops the background scan worker; call when the panel is removed. */
    public void dispose() {
        assetCache.shutdown();
    }
}
