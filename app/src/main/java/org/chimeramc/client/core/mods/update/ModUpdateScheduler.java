package org.chimeramc.client.core.mods.update;

import android.content.Context;

import org.chimeramc.client.settings.FeatureSettings;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The one place that decides when the mod-update check runs.
 *
 * <p>Both entry points (launcher startup and the Settings "turn it on" action) funnel through here
 * so the throttle, the feature switch and the single-flight rule are applied in one place. Without
 * this the two call sites would each have to remember the same three checks and would drift.
 *
 * <p>The checker is a process singleton because it owns a worker thread: creating a second one per
 * call would leak a thread per launch. It is never shut down, because the check is a one-shot
 * background task whose thread is a daemon and exits with the process.
 */
public final class ModUpdateScheduler {

    private static final AtomicReference<ModUpdateChecker> CHECKER = new AtomicReference<>();

    private ModUpdateScheduler() {
    }

    /** Runs the check if the feature is on and the throttle allows it. */
    public static void requestCheck(Context context) {
        if (context == null) return;
        if (!FeatureSettings.getInstance().isModUpdateCheckEnabled()) return;
        if (ModUpdateStore.configuredUrl().isEmpty()) return;
        Context app = context.getApplicationContext();
        checker().check(app, updates -> ModUpdateNotifier.notify(app, updates));
    }

    /** Runs the check once, with a caller-supplied sink (the Settings "check now" path). */
    public static void requestCheck(Context context, ModUpdateChecker.Callback callback) {
        if (context == null) return;
        checker().check(context.getApplicationContext(), callback);
    }

    private static ModUpdateChecker checker() {
        ModUpdateChecker existing = CHECKER.get();
        if (existing != null) return existing;
        ModUpdateChecker created = new ModUpdateChecker();
        return CHECKER.compareAndSet(null, created) ? created : CHECKER.get();
    }
}
