package org.chimeramc.client.core.cosmetics;

import android.content.Context;

import org.chimeramc.client.util.AppLog;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The published list of xuids that are known to run CaramelClient.
 *
 * <p>The badge used to depend on having heard an account on a Caramel-only protocol (voice or
 * cosmetic sync), which is honest but only ever lights for somebody you have already stood next to
 * in a world. A friend from your Xbox list who plays on Caramel in another world therefore never
 * showed the badge, which is the half of the friends screen that was missing.
 *
 * <p>This document closes that: one small file in the same repository as the other remote documents,
 * read and cached at startup. An id only belongs in it with its owner's consent - a badge is a claim
 * about another person - so publishing to it is an operator action, never something this client does
 * on its own.
 *
 * <p>Pure parse + cache, no device needed to test: the rule is "an id in the published list is a
 * Caramel user", and that rule is exercised by a JVM test.
 */
public final class CaramelUserRegistry {

    private static final String TAG = "CaramelUserRegistry";

    /** Where the list is published. Same repository, same branch, as the other remote documents. */
    public static final String SOURCE_URL =
            "https://raw.githubusercontent.com/ChimeraAnt-DEV/CaramelClient/main/resources/launcher/caramel_users.json";

    private static final String CACHE_FILE = "caramel_users.json";
    private static final int TIMEOUT_MS = 10_000;
    private static final AtomicBoolean refreshRunning = new AtomicBoolean(false);

    private CaramelUserRegistry() {
    }

    /**
     * The ids in a published document, lower-cased and trimmed.
     *
     * <p>Accepts both shapes the document may take — `{"xuids": [...]}` and a bare array — because a
     * list of ids is small enough that a maintainer will edit it by hand and will not always copy
     * the wrapper. Anything that is not an id is dropped rather than stored.
     */
    public static Set<String> parse(String json) {
        Set<String> out = new LinkedHashSet<>();
        if (json == null || json.trim().isEmpty()) return out;
        try {
            String trimmed = json.trim();
            JSONArray ids = trimmed.startsWith("[") ? new JSONArray(trimmed) : null;
            if (ids == null) {
                JSONObject root = new JSONObject(trimmed);
                ids = root.optJSONArray("xuids");
            }
            if (ids == null) return out;
            for (int i = 0; i < ids.length(); i++) {
                // Only a string is an id: a number or a nested object that snuck into the list must
                // not become "42" or "{\"xuid\":\"1\"}" and then be compared against real ids.
                Object raw = ids.opt(i);
                if (raw instanceof String) add(out, (String) raw);
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "parse", "The Caramel user document could not be parsed", t);
        }
        return out;
    }

    /** The last successfully fetched list, or an empty set when none was ever fetched. */
    public static Set<String> ids(Context context) {
        if (context == null) return new LinkedHashSet<>();
        String json = readCache(context);
        if (json == null) return new LinkedHashSet<>();
        return parse(json);
    }

    /** Whether the published list names {@code xuid}. */
    public static boolean contains(Context context, String xuid) {
        if (xuid == null || xuid.trim().isEmpty()) return false;
        return ids(context).contains(xuid.trim().toLowerCase(Locale.ROOT));
    }

    /** Reads the published document once, in the background, and caches it. */
    public static void refresh(final Context context) {
        if (context == null) return;
        if (!refreshRunning.compareAndSet(false, true)) return;
        final Context appContext = context.getApplicationContext();
        org.chimeramc.client.util.AppExecutors.run(
                org.chimeramc.client.util.AppExecutors.Kind.NETWORK, "caramel-users", () -> {
            try {
                String body = httpGet(SOURCE_URL);
                // A failed or empty fetch keeps the cached list: an outage must not silently turn
                // every badge off.
                if (body != null && !body.trim().isEmpty()) {
                    writeCache(appContext, body);
                }
            } catch (Throwable t) {
                AppLog.warnOnce(TAG, "refresh", "The Caramel user list could not be fetched", t);
            } finally {
                refreshRunning.set(false);
            }
        });
    }

    private static void add(Set<String> out, String value) {
        if (value == null) return;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return;
        out.add(trimmed.toLowerCase(Locale.ROOT));
    }

    private static String readCache(Context context) {
        File file = new File(context.getFilesDir(), CACHE_FILE);
        if (!file.exists()) return null;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int read = in.read(buffer);
            return read > 0 ? new String(buffer, 0, read, StandardCharsets.UTF_8) : null;
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "cache:read", "The cached Caramel user list could not be read", t);
            return null;
        }
    }

    private static void writeCache(Context context, String body) {
        File file = new File(context.getFilesDir(), CACHE_FILE);
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "cache:write", "The Caramel user list could not be cached", t);
        }
    }

    private static String httpGet(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) return null;
            try (InputStream in = connection.getInputStream()) {
                return readAll(in);
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "http", "The Caramel user list request failed", t);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readAll(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read;
        while ((read = in.read(buf)) > 0) out.write(buf, 0, read);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
