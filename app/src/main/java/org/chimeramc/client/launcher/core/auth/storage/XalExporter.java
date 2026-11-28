package org.chimeramc.client.core.auth.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.microsoft.xal.androidjava.XalStorageContract;

import org.chimeramc.client.core.auth.MsftAccountStore;
import org.chimeramc.client.util.JsonIOUtils;

import java.io.File;
import java.util.Map;

/**
 * Writes the launcher's Microsoft account into the store the game's XAL reads.
 *
 * <p>The layout, the account directory and the "which account" pointer come from
 * {@link XalStorageContract}, which the game-side {@code Storage} also uses. That shared contract is
 * the point: the exporter used to write {@code files/xal/<user>/...} while the game read
 * {@code files/} unless a settings toggle was on, so a launcher that was signed in handed the game
 * nothing and the game asked for a sign-in that had already happened.
 */
public class XalExporter {
    private static final String TAG = "XalExporter";

    /**
     * Exports a specific account instead of the globally active one (F2).
     *
     * <p>Used when an instance is bound to an account: the launch must write <em>that</em> account's
     * tokens, not whichever account happens to be active in the launcher UI. The active flag is
     * deliberately left alone, so choosing an account for one instance does not silently change the
     * account the launcher shows everywhere else; the export pointer records the choice instead.
     *
     * <p>Returns false when the account cannot be resolved or carries no auth material, so the
     * caller can report it rather than launching signed out with no explanation. A false result
     * does <b>not</b> clear the XAL directory: the previous account's data is still the best
     * available, and wiping it would sign the user out mid-session.
     */
    public static boolean exportAccount(Context ctx, String accountId) {
        if (ctx == null || accountId == null || accountId.trim().isEmpty()) return false;
        try {
            MsftAccountStore.MsftAccount account = MsftAccountStore.find(ctx, accountId.trim());
            return write(ctx, account);
        } catch (Exception e) {
            Log.e(TAG, "Failed to export XAL data for a bound account", e);
            return false;
        }
    }

    /** Exports the account the launcher marks active. Returns false when there is none. */
    public static boolean exportActiveAccount(Context ctx) {
        if (ctx == null) return false;
        try {
            MsftAccountStore.MsftAccount active = activeAccount(ctx);
            if (active == null) {
                clearXalData(ctx);
                return false;
            }
            return write(ctx, active);
        } catch (Exception e) {
            Log.e(TAG, "Failed to export XAL data", e);
            return false;
        }
    }

    /**
     * Rewrites an account's exported tokens without moving the export pointer.
     *
     * <p>Called after a token refresh. A refresh can run for any stale account, including one this
     * launch is not going to use, so it must not change which account the game reads. When the game
     * is not pointed at this account there is nothing to update and this is a no-op.
     */
    public static boolean refreshExportedAccount(Context ctx, String accountId) {
        if (ctx == null || accountId == null) return false;
        if (!accountId.equals(currentAccountId(ctx))) return false;
        return exportAccount(ctx, accountId);
    }

    /** The account the last export pointed the game at, or null. */
    public static String currentAccountId(Context ctx) {
        if (ctx == null) return null;
        return XalStorageContract.readAccountPointer(xalRoot(ctx));
    }

    private static MsftAccountStore.MsftAccount activeAccount(Context ctx) {
        for (MsftAccountStore.MsftAccount account : MsftAccountStore.list(ctx)) {
            if (account.active) return account;
        }
        return null;
    }

    private static boolean write(Context ctx, MsftAccountStore.MsftAccount account) {
        if (account == null || account.serializedAuthManager == null) return false;
        if (account.msUserId == null || account.msUserId.trim().isEmpty()) return false;

        JsonObject authJson = JsonParser.parseString(account.serializedAuthManager).getAsJsonObject();
        XalExportBundle bundle = XalExportBundle.build(
                authJson, account.msUserId, account.xboxGamertag, account.xuid);

        File userDir = new File(xalRoot(ctx), XalStorageContract.encodeUserId(account.msUserId));
        if (!userDir.exists() && !userDir.mkdirs()) {
            Log.w(TAG, "Could not create the XAL account directory " + userDir.getAbsolutePath());
            return false;
        }

        for (Map.Entry<String, String> file : bundle.files.entrySet()) {
            JsonIOUtils.write(new File(userDir, file.getKey()), file.getValue());
        }
        writeCryptoPrefs(ctx, bundle.cryptoPrefs);
        writePointer(ctx, account.msUserId);
        logExport(bundle, userDir);
        return true;
    }

    private static void writePointer(Context ctx, String msUserId) {
        JsonObject pointer = new JsonObject();
        pointer.addProperty(XalStorageContract.CURRENT_ACCOUNT_KEY, msUserId);
        JsonIOUtils.write(new File(xalRoot(ctx), XalStorageContract.CURRENT_ACCOUNT_FILE),
                pointer.toString());
    }

    private static void writeCryptoPrefs(Context ctx, Map<String, String> prefs) {
        if (prefs.isEmpty()) return;
        SharedPreferences.Editor edit = ctx.getSharedPreferences(
                XalStorageContract.CRYPTO_PREFS_LAUNCHER, Context.MODE_PRIVATE).edit();
        for (Map.Entry<String, String> pref : prefs.entrySet()) {
            edit.putString(pref.getKey(), pref.getValue());
        }
        edit.apply();
    }

    /**
     * One line per export: which relying parties were written, when the soonest token expires, and
     * where. Deliberately no token, no account id and no gamertag, so the line is safe to paste into
     * a bug report.
     */
    private static void logExport(XalExportBundle bundle, File userDir) {
        Log.i(TAG, exportLogLine(bundle, userDir, System.currentTimeMillis()));
    }

    /** The export log line, built from counts and times only so it can never carry a token. */
    static String exportLogLine(XalExportBundle bundle, File userDir, long nowMs) {
        long remainingSeconds = bundle.soonestExpiryMs <= 0
                ? -1
                : (bundle.soonestExpiryMs - nowMs) / 1000L;
        return "Exported " + bundle.files.size() + " file(s) to " + userDir.getAbsolutePath()
                + "; relying parties " + bundle.distinctRelyingParties()
                + "; soonest token expiry "
                + (remainingSeconds < 0 ? "unknown" : remainingSeconds + "s");
    }

    private static File xalRoot(Context ctx) {
        return new File(ctx.getApplicationContext().getFilesDir(), XalStorageContract.XAL_DIR);
    }

    private static void clearXalData(Context ctx) {
        deleteDirectory(xalRoot(ctx));
        SharedPreferences.Editor edit = ctx.getSharedPreferences(
                XalStorageContract.CRYPTO_PREFS_LAUNCHER, Context.MODE_PRIVATE).edit();
        edit.clear();
        edit.apply();
    }

    private static boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return false;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return dir.delete();
    }
}
