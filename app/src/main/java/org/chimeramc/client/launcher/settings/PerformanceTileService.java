package org.chimeramc.client.launcher.settings;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.chimeramc.client.settings.PerformancePresetManager;
import org.chimeramc.client.util.ApiLevels;

/**
 * Quick Settings tile for the performance preset.
 *
 * <p>{@code TileService} exists from API 24, so at {@code minSdk 28} it needs no guard and no
 * fallback — the tile is available on every device the launcher runs on. It reuses
 * {@link PerformancePresetManager}, so the tile and the Basic settings screen cannot disagree
 * about what the preset is: the tile writes through the same coordinator that writes the
 * individual toggles.
 *
 * <p>Tap cycles Battery → Balanced → Performance → Battery. A long press opens the launcher so the
 * user can reach the full setting. The tile label is the current preset's own name, so it always
 * says which one is active rather than a generic "Performance".
 */
public class PerformanceTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    @Override
    public void onClick() {
        super.onClick();
        PerformancePresetManager.Preset next = next(PerformancePresetManager.current(this));
        PerformancePresetManager.apply(this, next);
        refresh();
    }

    private void refresh() {
        Tile tile = getQsTile();
        if (tile == null) return;
        PerformancePresetManager.Preset preset = PerformancePresetManager.current(this);
        if (ApiLevels.atLeastQ()) {
            tile.setSubtitle(PerformancePresetManager.describe(preset));
        }
        tile.setLabel(getString(labelFor(preset)));
        tile.setState(preset == PerformancePresetManager.Preset.BATTERY
                ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.setIcon(Icon.createWithResource(this, iconFor(preset)));
        tile.updateTile();
    }

    /** The next preset in the tap cycle; pure and pinned by the unit test. */
    static PerformancePresetManager.Preset next(PerformancePresetManager.Preset current) {
        switch (current == null ? PerformancePresetManager.Preset.BALANCED : current) {
            case BATTERY:
                return PerformancePresetManager.Preset.BALANCED;
            case PERFORMANCE:
                return PerformancePresetManager.Preset.BATTERY;
            case BALANCED:
            default:
                return PerformancePresetManager.Preset.PERFORMANCE;
        }
    }

    static int labelFor(PerformancePresetManager.Preset preset) {
        switch (preset == null ? PerformancePresetManager.Preset.BALANCED : preset) {
            case BATTERY:
                return org.chimeramc.client.R.string.performance_preset_battery;
            case PERFORMANCE:
                return org.chimeramc.client.R.string.performance_preset_performance;
            case BALANCED:
            default:
                return org.chimeramc.client.R.string.performance_preset_balanced;
        }
    }

    static int iconFor(PerformancePresetManager.Preset preset) {
        // The monochrome mark, because a Quick Settings tile is tinted by the system and a
        // full-colour launcher icon reads as a smudge. The label carries which preset is active.
        return org.chimeramc.client.R.drawable.ic_caramel_mono;
    }
}
