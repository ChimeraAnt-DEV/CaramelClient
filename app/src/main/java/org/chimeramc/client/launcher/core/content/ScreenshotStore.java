package org.chimeramc.client.launcher.core.content;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.LauncherStorage;

import java.io.File;

/**
 * Resolves the selected instance's {@code Screenshots} folder (item 224).
 *
 * <p>Two callers now need it — the catalogue's screenshot action and the pause-menu button row —
 * and a second copy of the storage-type resolution is exactly how the two would drift onto
 * different roots. The rule lives here once and both callers use it.
 */
public final class ScreenshotStore {

    private ScreenshotStore() {
    }

    /** The folder to save into, or null when no instance is selected or the root is unknown. */
    public static File screenshotsDir(Activity activity) {
        if (activity == null) return null;
        VersionManager manager = VersionManager.getIfInitialized();
        if (manager == null) return null;
        GameVersion version = manager.getSelectedVersion();
        if (version == null) return null;
        SharedPreferences prefs = activity.getSharedPreferences(
                LauncherStorage.CONTENT_MANAGEMENT_PREFS_NAME, Context.MODE_PRIVATE);
        FeatureSettings.StorageType stored = FeatureSettings.StorageType.INTERNAL;
        try {
            stored = FeatureSettings.StorageType.valueOf(prefs.getString(
                    LauncherStorage.CONTENT_STORAGE_TYPE_KEY,
                    FeatureSettings.StorageType.INTERNAL.name()));
        } catch (Throwable ignored) {
            // A malformed preference reads as the default rather than aborting the capture.
        }
        FeatureSettings.StorageType resolved = LauncherStorage.normalizeContentStorageType(
                stored, version.versionIsolation);
        File gameDataDir = LauncherStorage.getContentGameDataDir(
                activity, version.getStorageProfileId(), resolved);
        if (gameDataDir == null) return null;
        return new File(gameDataDir, "Screenshots");
    }
}
