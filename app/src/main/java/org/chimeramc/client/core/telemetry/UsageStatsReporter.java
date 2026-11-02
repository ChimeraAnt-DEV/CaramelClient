package org.chimeramc.client.core.telemetry;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.core.stats.LaunchStats;
import org.chimeramc.client.core.stats.LaunchStatsStore;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.AppLog;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends the opt-in anonymous usage counter (item 281).
 *
 * <p>Three gates, all of which must pass, and all of which are checked before any request is built:
 * the user's switch ({@link FeatureSettings#isUsageStatsEnabled()}), a collector address for this
 * build ({@code BuildConfig.USAGE_STATS_ENDPOINT}), and a non-empty payload. The shipped source
 * configures no address, so the feature is inert until an operator sets one -- the same shape the
 * crash uploader and the voice relay address use.
 *
 * <p>The report is built by {@link UsageStatsPayload}, which is also what the in-app preview
 * renders, so the preview cannot describe a smaller payload than the request carries.
 *
 * <p>Sent at most once per day, on a daemon worker, and never on a launch path that could block:
 * telemetry must never make the launcher slower or less reliable than not having it.
 */
public final class UsageStatsReporter {

    private static final String TAG = "UsageStatsReporter";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final long MIN_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final String PREFS = "usage_stats";
    private static final String KEY_LAST_SENT = "last_sent_at";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "usage-stats");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);

    private UsageStatsReporter() {
    }

    /** True when this build has a collector configured. */
    public static boolean isConfigured() {
        String endpoint = BuildConfig.USAGE_STATS_ENDPOINT;
        return endpoint != null && !endpoint.trim().isEmpty();
    }

    /** The fields a report would carry right now, for the consent preview. */
    public static Map<String, String> currentFields(Context context) {
        Context app = context == null ? null : context.getApplicationContext();
        String deviceClass = "unknown";
        if (app != null) {
            try {
                ActivityManager manager =
                        (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
                if (manager != null) {
                    ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
                    manager.getMemoryInfo(info);
                    deviceClass = UsageStatsPayload.deviceClassFor(info.totalMem);
                }
            } catch (RuntimeException e) {
                AppLog.warn(TAG, "could not read the device memory class", e);
            }
        }
        LaunchStats stats = app == null ? new LaunchStats() : LaunchStatsStore.load(app);
        return UsageStatsPayload.fields(
                BuildConfig.VERSION_NAME,
                deviceClass,
                Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown",
                stats.launches(),
                stats.completedLaunches(),
                stats.crashes());
    }

    /** The preview text for the Settings dialog. Never throws. */
    public static String describe(Context context) {
        try {
            return UsageStatsPayload.describe(currentFields(context));
        } catch (RuntimeException e) {
            return "Could not build the report preview: " + e.getClass().getSimpleName();
        }
    }

    /**
     * Sends a report if every gate allows it.
     *
     * <p>Call from a background thread. Never throws: a failed counter is not a failure the user
     * should ever see.
     */
    public static void reportIfDue(Context context) {
        if (context == null) return;
        if (!isConfigured()) return;
        if (!FeatureSettings.getInstance().isUsageStatsEnabled()) return;
        final Context app = context.getApplicationContext();
        if (!isDue(app)) return;
        if (!BUSY.compareAndSet(false, true)) return;
        EXECUTOR.execute(() -> {
            try {
                send(app);
            } catch (Throwable e) {
                AppLog.warn(TAG, "usage report failed", e);
            } finally {
                BUSY.set(false);
            }
        });
    }

    private static void send(Context app) {
        String body = UsageStatsPayload.toJson(currentFields(app));
        Request request = new Request.Builder()
                .url(BuildConfig.USAGE_STATS_ENDPOINT.trim())
                .post(RequestBody.create(body, JSON))
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                recordSent(app);
            } else {
                AppLog.warn(TAG, "usage report refused: HTTP " + response.code());
            }
        } catch (Throwable e) {
            AppLog.warn(TAG, "usage report request failed", e);
        }
    }

    private static boolean isDue(Context app) {
        long last = lastSentAt(app);
        long now = System.currentTimeMillis();
        return last <= 0L || now - last >= MIN_INTERVAL_MS || now < last;
    }

    private static long lastSentAt(Context app) {
        try {
            return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getLong(KEY_LAST_SENT, 0L);
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private static void recordSent(Context app) {
        try {
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putLong(KEY_LAST_SENT, System.currentTimeMillis()).apply();
        } catch (RuntimeException ignored) {
        }
    }
}
