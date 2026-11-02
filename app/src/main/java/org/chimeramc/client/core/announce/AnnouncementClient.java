package org.chimeramc.client.core.announce;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.chimeramc.client.settings.LowLatencyNetworkManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches the announcements feed and hands the chosen card to the Launch tab (item 255).
 *
 * <p>Fail-closed in every direction: with no configured URL, no network, a bad document or an HTTP
 * error the card simply does not appear — there is never a placeholder card claiming something the
 * feed did not say. The last good feed is cached so a card keeps showing offline, which is the point
 * of an announcement (a user with no data still learns about a known problem).
 *
 * <p>The fetch runs on one daemon thread and the callback is posted to the main thread, so the
 * Launch tab never blocks on the network. A session-quiet check ({@link
 * LowLatencyNetworkManager#isGameSessionActive()}) skips the request while a game runs, matching the
 * launcher's own rule that a running session gets the network to itself.
 */
public final class AnnouncementClient {

    /** Receives the card to show, or null when there is nothing to show. */
    public interface Callback {
        void onCard(AnnouncementCard card);
    }

    /** How long a fetch may take before it is abandoned. */
    private static final int TIMEOUT_MS = 8000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "announcements");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final Handler main = new Handler(Looper.getMainLooper());

    /**
     * Shows the cached card immediately (if any) and refreshes from the network in the background.
     *
     * <p>Calling with a null callback is allowed; the fetch then only warms the cache.
     */
    public void load(Context context, long versionCode, Callback callback) {
        if (context == null) return;
        Context app = context.getApplicationContext();

        // The cached feed is shown at once: the card is useful offline and there is no reason to
        // make the user wait for a request that may never complete.
        AnnouncementCard cached = pick(AnnouncementFeed.parse(AnnouncementStore.cachedFeed(app)),
                versionCode, AnnouncementStore.dismissedIds(app));
        if (callback != null) callback.onCard(cached);

        String url = AnnouncementStore.configuredUrl();
        if (url.isEmpty()) return;
        if (LowLatencyNetworkManager.isGameSessionActive()) return;
        if (!busy.compareAndSet(false, true)) return;

        executor.execute(() -> {
            String body = fetch(url);
            try {
                if (body == null) return;
                AnnouncementStore.cacheFeed(app, body);
                List<AnnouncementCard> cards = AnnouncementFeed.parse(body);
                AnnouncementCard card = pick(cards, versionCode, AnnouncementStore.dismissedIds(app));
                if (callback != null) {
                    main.post(() -> callback.onCard(card));
                }
            } finally {
                busy.set(false);
            }
        });
    }

    /** Stops the worker. Safe to call more than once. */
    public void shutdown() {
        executor.shutdownNow();
    }

    private static AnnouncementCard pick(List<AnnouncementCard> cards, long versionCode,
                                         Set<String> dismissed) {
        return AnnouncementFeed.firstMatching(cards, versionCode, dismissed);
    }

    /** Reads the feed, preferring OkHttp (shared connection pool) and falling back to a plain GET. */
    private static String fetch(String url) {
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if (LowLatencyNetworkManager.isEnabled()) {
                builder.socketFactory(LowLatencyNetworkManager.createSocketFactory());
            }
            builder.connectTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            builder.readTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            OkHttpClient client = builder.build();
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;
                ResponseBody body = response.body();
                return body != null ? body.string() : null;
            }
        } catch (Throwable ignored) {
            return fetchPlain(url);
        }
    }

    /** A plain {@link HttpURLConnection} GET, used when OkHttp is unavailable for any reason. */
    private static String fetchPlain(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            StringBuilder builder = new StringBuilder();
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            return builder.toString();
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
