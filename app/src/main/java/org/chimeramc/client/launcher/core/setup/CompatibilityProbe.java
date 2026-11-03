package org.chimeramc.client.launcher.core.setup;

import org.chimeramc.client.util.ApiLevels;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import org.chimeramc.client.core.setup.CompatibilityCheck;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.util.PermissionsHandler;
import org.chimeramc.client.util.LauncherStorage;

import java.io.File;

/**
 * Gathers the device facts the compatibility checks read.
 *
 * <p>The decision itself lives in {@link CompatibilityCheck}, which is pure; this is the small
 * amount of Android code that reads the device. Every read is contained here and every read can
 * fail for a reason that has nothing to do with the user, so an unreadable fact degrades to the
 * benign default (or to the "unknown" sentinel for free space) rather than throwing.
 */
public final class CompatibilityProbe {

    private CompatibilityProbe() {
    }

    /** Reads every fact for the given selected instance (which may be null). */
    public static CompatibilityCheck.Facts gather(Context context, GameVersion version) {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts();
        facts.versionSelected(version != null);
        facts.freeBytes(freeBytes(context));
        facts.playStoreInstall(version != null && version.isInstalled);
        facts.abiCompatible(abiCompatible(context, version));
        facts.storagePermissionGranted(storageGranted(context));
        return facts;
    }

    /**
     * Free bytes on the volume the game data lives on, or the unknown sentinel.
     *
     * <p>Uses the app's own data directory when it can, because that is the volume an extraction
     * actually writes to; a failure to read it is reported as unknown rather than as zero, so the
     * user is not told to free space that is already free.
     */
    static long freeBytes(Context context) {
        try {
            File target = null;
            try {
                target = LauncherStorage.getStorageFilesRoot(context, null, false, false);
            } catch (Throwable ignored) {
            }
            if (target == null) {
                target = context != null ? context.getFilesDir() : null;
            }
            if (target == null) return CompatibilityCheck.UNKNOWN_BYTES;
            StatFs stat = new StatFs(target.getAbsolutePath());
            return stat.getAvailableBytes();
        } catch (Throwable t) {
            return CompatibilityCheck.UNKNOWN_BYTES;
        }
    }

    /**
     * True when the selected APK ships an ABI this process can load.
     *
     * <p>Delegates to the same probe the launch preflight uses, so the setup screen and the launch
     * cannot disagree about whether a version is loadable. An unreadable APK is not proven
     * incompatible, so a failed read reports compatible and lets the launch preflight name the real
     * reason.
     */
    static boolean abiCompatible(Context context, GameVersion version) {
        if (version == null) return true;
        try {
            return !new org.chimeramc.client.launcher.core.minecraft.AndroidLaunchProbe(
                    context, version, null).abiIncompatible();
        } catch (Throwable t) {
            return true;
        }
    }

    /** True when the launcher holds the storage access it needs for its own directories. */
    static boolean storageGranted(Context context) {
        // The launcher writes its game data under its own private/external-files directories, which
        // need no runtime permission. On API 30+ all-files access is only needed for the migration
        // path; its absence is a warning the checks express, not a hard requirement. The check
        // therefore reports the device's real state (all-files access, or the legacy permission on
        // older releases) and lets CompatibilityCheck decide how much it matters.
        try {
            if (ApiLevels.atLeastR()) {
                return Environment.isExternalStorageManager();
            }
            PermissionsHandler handler = PermissionsHandler.getInstance();
            return handler.hasPermission(PermissionsHandler.PermissionType.STORAGE);
        } catch (Throwable t) {
            return true;
        }
    }
}
