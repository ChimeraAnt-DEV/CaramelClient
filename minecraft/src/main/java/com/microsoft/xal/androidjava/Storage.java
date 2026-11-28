package com.microsoft.xal.androidjava;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;

/**
 * Where this XAL copy keeps its state.
 *
 * <p>The game asks for one directory and writes every token file into it. When the launcher has
 * exported an account, that directory must be the exported account's own, or the game reads a
 * store the launcher never wrote and shows a Sign In button next to a signed-in launcher.
 *
 * <p>The layout and the account-resolution rule live in {@link XalStorageContract}, which the
 * launcher's exporter also uses, so the writer and the reader cannot drift apart.
 */
public class Storage {
    private static final String TAG = "XalStorage";
    private static final String FEATURE_PREFS = "feature_settings";
    private static final String FEATURE_JSON = "settings_json";
    private static final String LAUNCHER_MANAGED_LOGIN = "launcherManagedMcLoginEnabled";

    @NotNull
    public static String getStoragePath(@NotNull Context context) {
        File filesDir = context.getApplicationContext().getFilesDir();
        if (!launcherStoreInUse(context)) {
            return filesDir.getPath();
        }

        File xalRoot = xalRoot(context);
        if (!xalRoot.exists() && !xalRoot.mkdirs()) {
            return filesDir.getPath();
        }

        File userDir = XalStorageContract.accountDirectory(xalRoot);
        if (userDir == null) {
            // No account to point at: the game still gets the launcher's directory, so anything
            // it writes lands beside the export instead of in a store nobody reads.
            return xalRoot.getAbsolutePath();
        }
        if (!userDir.exists() && !userDir.mkdirs()) {
            return xalRoot.getAbsolutePath();
        }
        return userDir.getAbsolutePath();
    }

    /**
     * Whether the launcher's store is the one to use.
     *
     * <p>True when the user asked for launcher-managed login, and also when an account has already
     * been exported: exporting an account while this XAL copy reads somewhere else is the defect
     * this rule removes, and the export runs unconditionally on sign-in and on every launch.
     */
    public static boolean launcherStoreInUse(@NotNull Context context) {
        return launcherManagedLoginEnabled(context) || launcherDataPresent(context);
    }

    /** The crypto preferences name that matches the storage-path decision. */
    public static String cryptoPrefsName(@NotNull Context context) {
        return XalStorageContract.cryptoPrefsName(
                launcherManagedLoginEnabled(context), launcherDataPresent(context));
    }

    public static boolean launcherManagedLoginEnabled(@NotNull Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(FEATURE_PREFS, Context.MODE_PRIVATE);
            String json = prefs.getString(FEATURE_JSON, null);
            if (json == null) return false;
            return new JSONObject(json).optBoolean(LAUNCHER_MANAGED_LOGIN, false);
        } catch (Throwable unreadable) {
            // Fail closed to "not managed", but say so: silently reading the wrong store is what
            // produced a sign-in prompt next to a signed-in launcher.
            Log.w(TAG, "Could not read the launcher-managed login setting", unreadable);
            return false;
        }
    }

    public static boolean launcherDataPresent(@NotNull Context context) {
        return XalStorageContract.launcherDataPresent(xalRoot(context));
    }

    private static File xalRoot(@NotNull Context context) {
        return new File(context.getApplicationContext().getFilesDir(), XalStorageContract.XAL_DIR);
    }
}
