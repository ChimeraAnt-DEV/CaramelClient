package com.microsoft.xal.androidjava;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The on-disk layout of the launcher-managed XAL store.
 *
 * <p>The launcher writes this store ({@code XalExporter}) and this copy of XAL reads it
 * ({@link Storage}, {@code Ecdsa}). The names and the resolution rule live here so the two sides
 * cannot disagree: the exporter used to write {@code files/xal/<user>/...} unconditionally while
 * {@link Storage#getStoragePath} returned plain {@code files/} unless a settings toggle was on, so
 * on a default install the exported account was invisible to the game.
 */
public final class XalStorageContract {

    /** The launcher's XAL root, relative to the app's files directory. */
    public static final String XAL_DIR = "xal";

    /** The launcher's account list. Entries carry {@code msUserId} and {@code active}. */
    public static final String ACCOUNTS_FILE = "Xal.Accounts.json";

    /**
     * The account the next game start must read.
     *
     * <p>Written by every export. It exists because a bound instance exports an account that is
     * deliberately not the launcher's active one, and the active flag alone cannot express that.
     */
    public static final String CURRENT_ACCOUNT_FILE = "Xal.Current.json";

    public static final String CURRENT_ACCOUNT_KEY = "msUserId";

    /** The crypto preferences the launcher writes the device key pair into. */
    public static final String CRYPTO_PREFS_LAUNCHER = "org.chimeramc.xal.crypto";

    /** The preferences name this XAL copy used before the launcher managed its storage. */
    public static final String CRYPTO_PREFS_GAME = "com.microsoft.xal.crypto";

    private XalStorageContract() {
    }

    /** The per-account directory name: url-safe base64 of the MSA user id, unpadded. */
    public static String encodeUserId(String msUserId) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(msUserId.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The account the game must read.
     *
     * <p>The export pointer wins over the account list: a bound instance exports an account that is
     * not the launcher's active one, and the pointer is the only place that choice is recorded.
     * Falls back to the active entry, then to the first entry, then to null.
     */
    public static String resolveAccountId(String currentAccountJson, String accountsJson) {
        String pointed = optUserId(currentAccountJson, CURRENT_ACCOUNT_KEY);
        if (pointed != null) return pointed;

        JSONArray accounts = parseArray(accountsJson);
        if (accounts == null) return null;
        String first = null;
        for (int i = 0; i < accounts.length(); i++) {
            JSONObject account = accounts.optJSONObject(i);
            if (account == null) continue;
            String id = optUserId(account);
            if (id == null) continue;
            if (first == null) first = id;
            if (account.optBoolean("active", false)) return id;
        }
        return first;
    }

    /** Whether an exported account exists, i.e. whether the game should read the launcher's store. */
    public static boolean hasExportedAccount(String currentAccountJson, String accountsJson) {
        return resolveAccountId(currentAccountJson, accountsJson) != null;
    }

    /** The account named by the export pointer, or null when there is no readable pointer. */
    public static String readAccountPointer(File xalRoot) {
        return optUserId(read(new File(xalRoot, CURRENT_ACCOUNT_FILE)), CURRENT_ACCOUNT_KEY);
    }

    /** The account the game must read from this store, or null when the store holds none. */
    public static String resolveAccountId(File xalRoot) {
        return resolveAccountId(
                read(new File(xalRoot, CURRENT_ACCOUNT_FILE)),
                read(new File(xalRoot, ACCOUNTS_FILE)));
    }

    /** Whether this store holds an exported account. */
    public static boolean launcherDataPresent(File xalRoot) {
        return resolveAccountId(xalRoot) != null;
    }

    /**
     * The per-account directory the game must read, or null when the store holds no account.
     *
     * <p>The exporter creates the directory with {@link #encodeUserId} of the same id, so this is
     * where the writer and the reader meet: a change to either side shows up as a mismatch here.
     */
    public static File accountDirectory(File xalRoot) {
        String accountId = resolveAccountId(xalRoot);
        return accountId == null ? null : new File(xalRoot, encodeUserId(accountId));
    }

    /**
     * Which crypto preferences name to use.
     *
     * <p>The key pair must live where the exporter wrote it, which is the same decision as the
     * storage path: the launcher's store is in use when the user asked for launcher-managed login
     * or when an exported account is actually present.
     */
    public static String cryptoPrefsName(boolean launcherManagedLogin, boolean launcherDataPresent) {
        return launcherManagedLogin || launcherDataPresent
                ? CRYPTO_PREFS_LAUNCHER
                : CRYPTO_PREFS_GAME;
    }

    private static String optUserId(JSONObject json) {
        if (json == null) return null;
        String id = json.optString(CURRENT_ACCOUNT_KEY, "");
        return id.isEmpty() ? null : id;
    }

    /**
     * The user id in a JSON object, or null when the text is absent, empty or unreadable.
     *
     * <p>Unreadable text is treated as "no account" so the caller fails closed to a normal sign-in
     * instead of reading a half-written export; the caller that has a {@code Context} logs it.
     */
    private static String optUserId(String json, String key) {
        if (json == null) return null;
        try {
            JSONObject object = new JSONObject(json);
            String id = object.optString(key, "");
            return id.isEmpty() ? null : id;
        } catch (Throwable unreadable) {
            return null;
        }
    }

    private static JSONArray parseArray(String json) {
        if (json == null) return null;
        try {
            return new JSONArray(json);
        } catch (Throwable unreadable) {
            return null;
        }
    }

    private static String read(File file) {
        if (file == null || !file.isFile()) return null;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(file.length(), 1024 * 1024)];
            int read = in.read(buffer);
            return read <= 0 ? null : new String(buffer, 0, read, StandardCharsets.UTF_8);
        } catch (Throwable unreadable) {
            return null;
        }
    }
}
