package org.chimeramc.pojavcontrols;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

public final class PojavControls {
    public static final String ACTION_PROFILE_CHANGED = "org.chimeramc.pojavcontrols.PROFILE_CHANGED";

    /** SharedPreferences the touch-control toggles live in (item 222). */
    public static final String PREFS = "pojav_controls_prefs";
    private static final String KEY_LEFT_HANDED = "left_handed";
    private static final String KEY_TABLET_SCALE = "tablet_scale";
    private static final String KEY_POINTER_SPEED = "pointer_speed";

    private static WeakReference<Activity> attachedActivity = new WeakReference<>(null);
    private static PojavControlOverlay overlay;
    private static WeakReference<Activity> editorActivity = new WeakReference<>(null);
    private static PojavControlsEditorView editor;
    private static volatile boolean lowLatencyMode = false;

    private PojavControls() {}

    public static synchronized void setEnabled(Activity activity, PojavControlsHost host, boolean enabled) {
        if (enabled) attach(activity, host);
        else detach();
    }

    public static synchronized boolean isEnabled() {
        return overlay != null && overlay.isAttachedToWindow();
    }

    public static void launchEditor(Activity activity) {
        activity.runOnUiThread(() -> showEditor(activity));
    }

    public static synchronized boolean closeEditor() {
        if (editor == null) return false;
        editor.close();
        return true;
    }

    public static synchronized boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return editor != null && editor.handleActivityResult(requestCode, resultCode, data);
    }

    public static void setLowLatencyMode(boolean enabled) {
        lowLatencyMode = enabled;
        if (overlay != null) {
            overlay.onLowLatencyModeChanged();
        }
    }

    public static boolean isLowLatencyMode() {
        return lowLatencyMode;
    }

    /**
     * Whether the controls are mirrored for a left-handed player (item 222).
     *
     * <p>Read from the shared preferences on every layout pass rather than cached in a static, so a
     * toggle in the editor takes effect immediately with no reload path to keep in step.
     */
    public static boolean isLeftHanded() {
        Activity activity = attachedActivity.get();
        if (activity == null) return false;
        return preferences(activity).getBoolean(KEY_LEFT_HANDED, false);
    }

    /** Turns the left-handed mirror on or off and re-lays the overlay out. */
    public static void setLeftHanded(Activity activity, boolean leftHanded) {
        preferences(activity).edit().putBoolean(KEY_LEFT_HANDED, leftHanded).apply();
        if (overlay != null) {
            overlay.setLeftHanded(leftHanded);
        }
        activity.sendBroadcast(new Intent(ACTION_PROFILE_CHANGED));
    }

    /**
     * Whether the tablet size scale is on (item 222).
     *
     * <p>Off by default: scaling a profile the player already tuned would silently change the feel,
     * so it is an explicit choice rather than a width threshold that fires on its own.
     */
    public static boolean isTabletScaleEnabled(Context context) {
        return preferences(context).getBoolean(KEY_TABLET_SCALE, false);
    }

    /** Turns the tablet size scale on or off and reloads the active profile. */
    public static void setTabletScaleEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_TABLET_SCALE, enabled).apply();
        if (overlay != null) {
            overlay.reloadProfile();
        }
    }

    /** The smallest width in dp the window reports, which is the tablet rule's input. */
    public static int smallestWidthDp(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        return configuration.smallestScreenWidthDp;
    }

    /** The calibrated pointer-speed multiplier (item 223). */
    public static float getPointerSpeedMultiplier(Context context) {
        return preferences(context).getFloat(KEY_POINTER_SPEED, PointerCalibration.DEFAULT_MULTIPLIER);
    }

    /** Stores a calibrated pointer-speed multiplier, clamped to the playable range. */
    public static void setPointerSpeedMultiplier(Context context, float multiplier) {
        preferences(context).edit()
                .putFloat(KEY_POINTER_SPEED, PointerCalibration.clamp(multiplier))
                .apply();
        if (overlay != null) overlay.requestLayout();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized boolean ownsTouchInput() {
        View target = editor != null && editor.isAttachedToWindow() ? editor : overlay;
        return target != null && target.isAttachedToWindow() && target.getVisibility() == View.VISIBLE;
    }

    public static synchronized void reload() {
        if (overlay != null) overlay.reloadProfile();
    }

    private static void attach(Activity activity, PojavControlsHost host) {
        Activity current = attachedActivity.get();
        if (overlay != null && current == activity && overlay.isAttachedToWindow()) {
            overlay.reloadProfile();
            overlay.bringToFront();
            return;
        }
        detach();
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        overlay = new PojavControlOverlay(activity, host);
        ((ViewGroup) content).addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.bringToFront();
        attachedActivity = new WeakReference<>(activity);
    }

    private static synchronized void showEditor(Activity activity) {
        if (editor != null) {
            if (editorActivity.get() == activity && editor.isAttachedToWindow()) {
                editor.bringToFront();
                return;
            }
            finishEditor();
        }
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        if (overlay != null) {
            overlay.releaseAll();
            overlay.setVisibility(View.GONE);
        }
        editor = new PojavControlsEditorView(activity, PojavControls::finishEditor);
        ((ViewGroup) content).addView(editor, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        editor.bringToFront();
        editorActivity = new WeakReference<>(activity);
    }

    private static synchronized void finishEditor() {
        if (editor != null && editor.getParent() instanceof ViewGroup) {
            ((ViewGroup) editor.getParent()).removeView(editor);
        }
        editor = null;
        editorActivity.clear();
        if (overlay != null) {
            overlay.reloadProfile();
            overlay.setVisibility(View.VISIBLE);
            overlay.bringToFront();
        }
    }

    private static void detach() {
        if (editor != null) editor.close();
        if (overlay != null) {
            overlay.releaseAll();
            if (overlay.getParent() instanceof ViewGroup) ((ViewGroup) overlay.getParent()).removeView(overlay);
            overlay.dispose();
        }
        overlay = null;
        attachedActivity.clear();
    }
}
