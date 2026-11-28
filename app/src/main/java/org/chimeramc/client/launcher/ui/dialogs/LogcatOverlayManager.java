package org.chimeramc.client.ui.dialogs;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;

import org.chimeramc.client.core.minecraft.MinecraftLoadingActivity;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.settings.SettingsStorage;
import org.chimeramc.client.ui.activities.SplashActivity;
import org.chimeramc.client.util.MainThread;


public class LogcatOverlayManager {

    private static volatile LogcatOverlayManager instance;
    private final Application app;
    private LogcatOverlay overlay;
    private final Handler mainHandler = MainThread.handler();
    private volatile boolean onSplash = false;
    private volatile boolean onLoading = false;
    /** Cleared on pause, so it cannot outlive the screen it points at. */
    private volatile Activity resumedActivity;

    /**
     * Held in a field on purpose: {@code registerOnSharedPreferenceChangeListener} keeps only a
     * weak reference to its listener, so a listener passed inline is collected at the next GC and
     * the overlay silently stops answering the switch until the process restarts. That was the
     * "logcat needs a restart" report.
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener settingsListener;

    private LogcatOverlayManager(Application app) {
        this.app = app;
        settingsListener = (prefs, key) -> {
            if (SettingsStorage.KEY_SETTINGS_JSON.equals(key)) updateVisibility();
        };
        registerLifecycleCallbacks();
        registerSettingsListener();
    }

    public static void init(Application app) {
        if (instance == null) {
            synchronized (LogcatOverlayManager.class) {
                if (instance == null) instance = new LogcatOverlayManager(app);
            }
        }
    }

    public static LogcatOverlayManager getInstance() { return instance; }

    private void attachTo(Activity act) {
        if (overlay == null) overlay = new LogcatOverlay(act.getApplicationContext());
        ViewGroup root = act.findViewById(android.R.id.content);
        if (root == null) return;
        if (overlay.getParent() != root) {
            if (overlay.getParent() instanceof ViewGroup) ((ViewGroup) overlay.getParent()).removeView(overlay);
            root.addView(overlay);
        }
        updateVisibility();
    }

    /**
     * Re-reads the switch and shows or hides the strip.
     *
     * <p>The overlay is created lazily: a switch flipped before any activity attached one still has
     * to take effect, so the currently resumed activity is used when there is no overlay yet.
     */
    private void updateVisibility() {
        boolean show = LogcatOverlayVisibility.shouldShow(
                FeatureSettings.getInstance().isLogcatOverlayEnabled(), onSplash, onLoading);
        Activity activity = resumedActivity;
        mainHandler.post(() -> {
            if (overlay == null && activity != null && !activity.isFinishing()) attachTo(activity);
            if (overlay == null) return;
            if (show) overlay.show(); else overlay.hide();
        });
    }

    private void registerLifecycleCallbacks() {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity activity) {
                onSplash = activity instanceof SplashActivity;
                onLoading = activity instanceof MinecraftLoadingActivity;
                resumedActivity = onSplash || onLoading ? null : activity;
                attachTo(activity);
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {
                if (resumedActivity == a) resumedActivity = null;
            }
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    private void registerSettingsListener() {
        SharedPreferences sp = app.getSharedPreferences(SettingsStorage.SP_NAME, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(settingsListener);
    }

    public void refreshVisibility() { updateVisibility(); }
}
