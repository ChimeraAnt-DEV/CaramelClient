package org.chimeramc.client.launcher.core.content;

import org.chimeramc.client.util.AppLog;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.LauncherStorage;

import java.io.File;

/**
 * Resolves the selected instance's {@code Screenshots} folder.
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
            AppLog.warnOnce("GlowberryClient", "ScreenshotStore:43", "Ignored a failure at ScreenshotStore:43", ignored);
        }
        FeatureSettings.StorageType resolved = LauncherStorage.normalizeContentStorageType(
                stored, version.versionIsolation);
        File gameDataDir = LauncherStorage.getContentGameDataDir(
                activity, version.getStorageProfileId(), resolved);
        if (gameDataDir == null) return null;
        return new File(gameDataDir, "Screenshots");
    }

    /**
     * The newest screenshot in the selected instance's folder, or null when there is none.
     *
     * <p>The in-game pause-menu row can only share something that already exists: it runs over a
     * live session, where a file picker would cover the game. "Share the last screenshot" is the
     * action that is actually available there, and it needs this one lookup.
     */
    public static File latestScreenshot(Activity activity) {
        File dir = screenshotsDir(activity);
        if (dir == null || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return null;
        File newest = null;
        for (File file : files) {
            if (file == null || !file.isFile()) continue;
            if (!isImage(file.getName())) continue;
            if (newest == null || file.lastModified() > newest.lastModified()) newest = file;
        }
        return newest;
    }

    /** True for a file name with an image extension the game or the launcher writes. */
    public static boolean isImage(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }
}
