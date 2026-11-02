package org.chimeramc.client.core.mods.update;

import android.content.Context;

import org.chimeramc.client.core.mods.Mod;
import org.chimeramc.client.core.mods.ModManager;
import org.chimeramc.client.settings.LowLatencyNetworkManager;
import org.chimeramc.client.util.AppExecutors;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.util.MainThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Runs the mod-update check and reports it as one summary (item 273).
 *
 * <p>The check is throttled to once a day ({@link ModUpdateStore#CHECK_INTERVAL_MS}), skips while a
 * game session is live (the session gets the network, per {@link
 * LowLatencyNetworkManager#isGameSessionActive()}), and only calls back when the set of updates has
 * actually changed since the last announcement. That last rule is what makes it "one notification":
 * a launcher opened twenty times in a day does one check and one message, not twenty.
 *
 * <p>Fail-closed: no feed URL, no network, a malformed document or an unreadable mod list all mean
 * "nothing to report", never a fabricated update.
 */
public final class ModUpdateChecker {

    private static final String TAG = "ModUpdateChecker";
    private static final int TIMEOUT_MS = 8000;

    /** Receives the updates to surface, already de-duplicated against the last announcement. */
    public interface Callback {
        void onUpdates(List<ModUpdatePlan.ModUpdate> updates);
    }

    private final AtomicBoolean busy = new AtomicBoolean(false);

    /**
     * Checks for mod updates if the throttle allows it.
     *
     * <p>Callbacks run on the main thread. A null callback still performs the check so the throttle
     * advances; pass one to be told about a change.
     */
    public void check(Context context, Callback callback) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        String url = ModUpdateStore.configuredUrl();
        if (url.isEmpty()) return;
        if (LowLatencyNetworkManager.isGameSessionActive()) return;
        long now = System.currentTimeMillis();
        if (!ModUpdateStore.isDue(app, now)) return;
        if (!busy.compareAndSet(false, true)) return;

        AppExecutors.run(AppExecutors.Kind.NETWORK, "mod-update-check", () -> {
            try {
                ModUpdateStore.recordCheck(app, now);
                String body = fetch(url);
                if (body == null) return;
                Map<String, String> feed = ModUpdateFeed.parse(body);
                if (feed.isEmpty()) return;

                List<ModUpdatePlan.InstalledMod> installed = installedMods();
                if (installed.isEmpty()) return;

                List<ModUpdatePlan.ModUpdate> updates = ModUpdatePlan.compute(installed, feed);
                Set<String> fingerprint = ModUpdatePlan.fingerprint(updates);
                if (fingerprint.isEmpty()) return;
                if (fingerprint.equals(ModUpdateStore.announced(app))) return;

                ModUpdateStore.setAnnounced(app, fingerprint);
                if (callback != null) {
                    MainThread.post(() -> callback.onUpdates(updates));
                }
            } catch (RuntimeException e) {
                AppLog.warn(TAG, "mod update check failed", e);
            } finally {
                busy.set(false);
            }
        });
    }

    /** Stops the worker. Safe to call more than once. The shared pool outlives us. */
    public void shutdown() {
        busy.set(false);
    }

    /** The installed mods for the currently selected version, or an empty list. */
    private static List<ModUpdatePlan.InstalledMod> installedMods() {
        List<ModUpdatePlan.InstalledMod> result = new ArrayList<>();
        try {
            ModManager manager = ModManager.getInstance();
            if (manager == null) return result;
            List<Mod> mods = manager.getMods();
            if (mods == null) return result;
            for (Mod mod : mods) {
                if (mod == null) continue;
                result.add(new ModUpdatePlan.InstalledMod(
                        mod.getId(), mod.getDisplayName(), mod.getVersion()));
            }
        } catch (RuntimeException e) {
            AppLog.warn(TAG, "could not read the installed mods", e);
        }
        return result;
    }

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
        } catch (Throwable e) {
            AppLog.warn(TAG, "mod update fetch failed", e);
            return null;
        }
    }
}
