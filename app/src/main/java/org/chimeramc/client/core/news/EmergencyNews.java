package org.chimeramc.client.core.news;

import android.content.Context;

import org.chimeramc.client.ui.compose.caramel.CaramelNewsInfo;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Announcements that can be published without shipping a build.
 *
 * <p>The News tab reads its posts from the bundled changelog feed, so anything a maintainer wants to
 * say - a server outage, a broken release, a date for a beta - has to wait for a release. This adds
 * one small document on the same repository the client already fetches its signed rule set from, read
 * at startup and cached, so a post appears on the next launch after it is published.
 *
 * <p><b>Nothing here is a trust boundary.</b> The text is displayed, never executed, and the only URL
 * a post can carry is opened through the launcher's own in-app browser. A failed fetch keeps the last
 * good post rather than emptying the tab: an outage must not erase the notice that explains it.
 */
public final class EmergencyNews {

    private static final String TAG = "EmergencyNews";

    /** Where a post is published. Same repository, same branch, as the other remote documents. */
    public static final String SOURCE_URL =
            "https://raw.githubusercontent.com/ChimeraAnt-DEV/CaramelClient/main/resources/launcher/emergency_news.json";

    private static final String CACHE_FILE = "emergency_news.json";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_POSTS = 3;

    private EmergencyNews() {
    }

    /** One published announcement. */
    public static final class Post {
        public final String id;
        public final String title;
        public final String body;
        public final String meta;
        public final String url;

        Post(String id, String title, String body, String meta, String url) {
            this.id = id;
            this.title = title;
            this.body = body;
            this.meta = meta;
            this.url = url;
        }
    }

    /** Wakes when a refresh lands, so an open screen can redraw with the new post. */
    public interface Listener {
        void onEmergencyNewsUpdated();
    }

    private static final Set<Listener> LISTENERS = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean refreshRunning = new AtomicBoolean(false);

    public static void addListener(Listener listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        if (listener != null) LISTENERS.remove(listener);
    }

    /**
     * Parses the document into the posts that may be shown at {@code nowMs}.
     *
     * <p>Pure and unit-tested: a document from a remote source is the one input here that can be
     * anything, so what it is allowed to produce is a rule rather than a set of hoping.
     */
    public static List<Post> parse(String json, long nowMs) {
        List<Post> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return out;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray posts = root.optJSONArray("posts");
            if (posts == null) return out;
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < posts.length() && out.size() < MAX_POSTS; i++) {
                JSONObject raw = posts.optJSONObject(i);
                if (raw == null) continue;
                String title = raw.optString("title", "").trim();
                String body = raw.optString("body", "").trim();
                if (title.isEmpty() || body.isEmpty()) continue;
                long expiresAt = parseInstant(raw.optString("expiresAt", ""));
                if (expiresAt > 0L && nowMs >= expiresAt) continue;
                String id = raw.optString("id", "").trim();
                if (id.isEmpty()) id = title;
                if (!seen.add(id)) continue;
                out.add(new Post(id, title, body, raw.optString("meta", "").trim(),
                        raw.optString("url", "").trim()));
            }
        } catch (Throwable t) {
            // A malformed document is not a reason to clear the tab: it yields no posts, and the
            // cached copy from the last good fetch is still what load() returns.
            AppLog.warnOnce(TAG, "parse", "The emergency news document could not be parsed", t);
        }
        return out;
    }

    /** An ISO-8601 instant in milliseconds, or 0 when absent or unparseable. */
    static long parseInstant(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        try {
            return Instant.parse(value.trim()).toEpochMilli();
        } catch (Throwable t) {
            return 0L;
        }
    }

    /** The cached posts, or an empty list when none were ever fetched successfully. */
    public static List<Post> posts(Context context) {
        if (context == null) return new ArrayList<>();
        String json = readCache(context);
        if (json == null) return new ArrayList<>();
        return parse(json, System.currentTimeMillis());
    }

    /**
     * The cached posts as News-tab rows.
     *
     * <p>The banner is left to the screen's gradient: a remote document cannot ship a drawable, and a
     * post with no picture is honest about having none.
     */
    public static List<CaramelNewsInfo> newsItems(Context context) {
        List<CaramelNewsInfo> out = new ArrayList<>();
        for (Post p : posts(context)) {
            out.add(new CaramelNewsInfo(p.title, p.body, p.meta, true, "", p.url, 0));
        }
        return out;
    }

    /**
     * Reads the published document once, in the background, and caches it.
     *
     * <p>One refresh at a time process-wide: startup and a manual refresh can race, and two identical
     * downloads of a few hundred bytes are not worth the extra socket.
     */
    public static void refresh(final Context context, final Runnable onFinished) {
        if (context == null) {
            if (onFinished != null) onFinished.run();
            return;
        }
        if (!refreshRunning.compareAndSet(false, true)) {
            if (onFinished != null) onFinished.run();
            return;
        }
        final Context appContext = context.getApplicationContext();
        org.chimeramc.client.util.AppExecutors.run(
                org.chimeramc.client.util.AppExecutors.Kind.NETWORK, "emergency-news", () -> {
            try {
                String body = httpGet(SOURCE_URL);
                if (body != null && !parse(body, System.currentTimeMillis()).isEmpty()) {
                    writeCache(appContext, body);
                    notifyListeners();
                }
            } catch (Throwable t) {
                AppLog.warnOnce(TAG, "refresh", "Could not read the emergency news", t);
            } finally {
                refreshRunning.set(false);
                if (onFinished != null) onFinished.run();
            }
        });
    }

    private static void notifyListeners() {
        for (Listener listener : LISTENERS) {
            try {
                listener.onEmergencyNewsUpdated();
            } catch (Throwable t) {
                AppLog.warnOnce(TAG, "listener", "An emergency news listener threw", t);
            }
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
            AppLog.warnOnce(TAG, "http", "The emergency news request failed", t);
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

    private static File cacheFile(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), CACHE_FILE);
    }

    private static String readCache(Context context) {
        File file = cacheFile(context);
        if (!file.isFile()) return null;
        try (InputStream in = new FileInputStream(file)) {
            return readAll(in);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "cacheRead", "Could not read the cached emergency news", t);
            return null;
        }
    }

    private static void writeCache(Context context, String json) {
        File file = cacheFile(context);
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "cacheWrite", "Could not cache the emergency news", t);
        }
    }
}
