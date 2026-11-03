package org.chimeramc.client.core.content;

import org.chimeramc.client.util.AppExecutors;
import org.chimeramc.client.util.MainThread;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Computes {@link PackAssetSummary} off the main thread and caches the result.
 *
 * <p>Reading a pack's size and largest texture means walking its directory and opening every PNG
 * for a 24-byte header. That is cheap per file but unbounded in total, so doing it in a list row's
 * {@code onBindViewHolder} would hitch the list on a pack with thousands of textures. The scan
 * therefore runs on one background thread, keyed by the pack's path plus its modification stamp so
 * a changed pack is rescanned, and the row is repainted through the callback when the value lands.
 *
 * <p>The cache is bounded ({@link #MAX_ENTRIES}, least-recently-used eviction) so browsing a large
 * library cannot grow it without limit.
 */
public final class PackAssetSummaryCache {

    /** The number of pack summaries kept before the least-recently-used one is evicted. */
    public static final int MAX_ENTRIES = 128;

    /** Receives a pack's summary on the main thread. */
    public interface Callback {
        void onSummary(File pack, PackAssetSummary.Result summary);
    }

    private final Map<String, PackAssetSummary.Result> cache =
            new LinkedHashMap<String, PackAssetSummary.Result>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PackAssetSummary.Result> eldest) {
                    return size() > MAX_ENTRIES;
                }
            };
    private final Map<String, Boolean> inFlight = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /** The cached summary for a pack, or null when it has not been computed yet. */
    public PackAssetSummary.Result peek(File pack) {
        if (pack == null) return null;
        synchronized (cache) {
            return cache.get(key(pack));
        }
    }

    /**
     * Returns the cached summary, or schedules a scan and invokes {@code callback} on the main
     * thread when it completes.
     *
     * <p>A second request for a pack already being scanned does not start a duplicate scan; the
     * caller simply gets null and its callback fires from the first scan's completion only if it
     * registered. Rows are re-bound on notify, so a missed callback costs one frame, not a wrong
     * value.
     */
    public PackAssetSummary.Result get(File pack, Callback callback) {
        if (pack == null) return null;
        String key = key(pack);
        PackAssetSummary.Result cached;
        synchronized (cache) {
            cached = cache.get(key);
        }
        if (cached != null) return cached;
        if (shutdown.get()) return null;
        if (inFlight.putIfAbsent(key, Boolean.TRUE) == null) {
            AppExecutors.run(AppExecutors.Kind.DISK, "pack-asset-scan", () -> {
                PackAssetSummary.Result result = PackAssetSummary.scan(pack);
                synchronized (cache) {
                    cache.put(key, result);
                }
                inFlight.remove(key);
                if (callback != null && !shutdown.get()) {
                    MainThread.post(() -> {
                        if (!shutdown.get()) callback.onSummary(pack, result);
                    });
                }
            });
        }
        return null;
    }

    /** Drops a pack's cached value so the next request rescans it (after a reimport, say). */
    public void invalidate(File pack) {
        if (pack == null) return;
        synchronized (cache) {
            cache.remove(key(pack));
        }
    }

    /** Stops the worker and clears the cache. Safe to call more than once. */
    public void shutdown() {
        shutdown.set(true);
        synchronized (cache) {
            cache.clear();
        }
    }

    private static String key(File pack) {
        return pack.getAbsolutePath() + ':' + pack.lastModified();
    }
}
