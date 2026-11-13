package org.chimeramc.client.core.mods.inbuilt.overlay;

import org.chimeramc.client.util.AppLog;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Display;
import android.view.WindowManager;

import org.chimeramc.client.core.content.OptionsEditor;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.DisplayModePreference;
import org.chimeramc.client.util.SurfaceFrameRate;
import org.chimeramc.client.util.LauncherStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the in-game quick settings.
 *
 * <p>Two different mechanisms, deliberately kept apart:
 * <ul>
 *   <li>The <b>resolution scale</b> is a game option. A running world has already read
 *       {@code options.txt}, so the write is reported as applying on the next load — the same
 *       honest contract {@code CatalogOptionsWriter} uses.</li>
 *   <li>The <b>frame cap</b> is applied to the window at once through
 *       {@code preferredDisplayModeId}, reusing {@link DisplayModePreference} so a cap can never
 *       silently lower the resolution. A cap the panel cannot honour exactly is reported as not
 *       applied rather than pretended.</li>
 * </ul>
 *
 * <p>The options path is resolved exactly as {@code ContentManagementActivity} resolves it, so a
 * quick setting cannot write to a different root than the game loads.
 */
public final class QuickSettingsApplier {

    private QuickSettingsApplier() {
    }

    /** The outcome of an apply attempt, so the panel can say what actually happened. */
    public enum Result {
        /** Written and will take effect at the next world load. */
        NEXT_LOAD,
        /** Applied to the window now. */
        APPLIED,
        /** No instance/options file was reachable. */
        NO_INSTANCE,
        /** The write or the window change failed. */
        FAILED,
        /** The panel cannot honour this value exactly (no same-resolution mode at the cap). */
        UNSUPPORTED
    }

    /**
     * Writes the resolution scale into the selected instance's options file.
     *
     * @return {@link Result#NEXT_LOAD} when written, otherwise the reason it was not
     */
    public static Result applyResolutionScale(Activity activity, int scale) {
        File optionsFile = optionsFile(activity);
        if (optionsFile == null) return Result.NO_INSTANCE;
        try {
            OptionsEditor editor = new OptionsEditor(optionsFile);
            editor.load();
            editor.setValue(QuickSettings.OPTION_RESOLUTION_SCALE,
                    QuickSettings.resolutionOptionValue(scale));
            editor.save();
            return Result.NEXT_LOAD;
        } catch (Throwable t) {
            android.util.Log.w("QuickSettingsApplier", "Could not write resolution scale", t);
            return Result.FAILED;
        }
    }

    /**
     * Applies a frame cap to the window immediately.
     *
     * <p>{@link QuickSettings#CAP_UNLIMITED} leaves the window alone and reports
     * {@link Result#APPLIED}, because "no cap" is a valid state the panel should be able to show.
     */
    public static Result applyRefreshCap(Activity activity, int capHz) {
        if (activity == null) return Result.FAILED;
        if (capHz == QuickSettings.CAP_UNLIMITED) {
            // Clear any rate a previous cap requested, then leave the panel alone.
            SurfaceFrameRate.apply(activity, capHz);
            return Result.APPLIED;
        }
        // API 30+: ask the surface itself for the rate. This cannot change the resolution, which
        // is the whole point of preferring it, and it works on a panel that exposes no matching
        // display mode. Only when it is unavailable do we fall back to the display-mode path.
        if (SurfaceFrameRate.apply(activity, capHz)) return Result.APPLIED;
        try {
            WindowManager windowManager =
                    (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) return Result.FAILED;
            Display display = windowManager.getDefaultDisplay();
            if (display == null) return Result.FAILED;
            Display.Mode current = display.getMode();
            if (current == null) return Result.FAILED;

            List<DisplayModePreference.Mode> modes = new ArrayList<>();
            for (Display.Mode mode : display.getSupportedModes()) {
                modes.add(new DisplayModePreference.Mode(mode.getModeId(), mode.getRefreshRate(),
                        mode.getPhysicalWidth(), mode.getPhysicalHeight()));
            }
            int chosen = DisplayModePreference.selectModeAtOrBelow(modes,
                    current.getPhysicalWidth(), current.getPhysicalHeight(), capHz);
            if (chosen == DisplayModePreference.NO_MODE) {
                // No same-resolution mode at or below the cap: the panel cannot honour it exactly,
                // and switching resolution to honour a frame cap would trade sharpness for frames.
                return Result.UNSUPPORTED;
            }
            if (chosen == current.getModeId()) return Result.APPLIED;
            WindowManager.LayoutParams params = activity.getWindow().getAttributes();
            params.preferredDisplayModeId = chosen;
            activity.getWindow().setAttributes(params);
            return Result.APPLIED;
        } catch (Throwable t) {
            android.util.Log.w("QuickSettingsApplier", "Could not apply frame cap", t);
            return Result.FAILED;
        }
    }

    private static File optionsFile(Context context) {
        VersionManager manager = VersionManager.getIfInitialized();
        if (manager == null) return null;
        GameVersion version = manager.getSelectedVersion();
        if (version == null) return null;

        SharedPreferences prefs = context.getSharedPreferences(
                LauncherStorage.CONTENT_MANAGEMENT_PREFS_NAME, Context.MODE_PRIVATE);
        FeatureSettings.StorageType stored = FeatureSettings.StorageType.INTERNAL;
        try {
            stored = FeatureSettings.StorageType.valueOf(prefs.getString(
                    LauncherStorage.CONTENT_STORAGE_TYPE_KEY,
                    FeatureSettings.StorageType.INTERNAL.name()));
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "QuickSettingsApplier:143", "Ignored a failure at QuickSettingsApplier:143", ignored);
        }
        FeatureSettings.StorageType resolved = LauncherStorage.normalizeContentStorageType(
                stored, version.versionIsolation);
        File gameDataDir = LauncherStorage.getContentGameDataDir(
                context, version.getStorageProfileId(), resolved);
        if (gameDataDir == null) return null;
        return new File(gameDataDir, "minecraftpe/options.txt");
    }
}
