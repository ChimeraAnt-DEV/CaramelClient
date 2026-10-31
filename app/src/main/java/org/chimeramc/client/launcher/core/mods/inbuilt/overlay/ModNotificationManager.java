package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.chimeramc.client.R;

import java.util.ArrayList;
import java.util.List;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.util.MainThread;
import org.chimeramc.client.util.OverlayLayers;

public class ModNotificationManager {

    private static final String TAG = "ModNotificationManager";
    private static final int DISPLAY_DURATION = 2500;
    private static final int SLIDE_DURATION = 200;
    private static final int NOTIFICATION_HEIGHT_DP = 70;
    private static final int NOTIFICATION_SPACING_DP = 8;
    private static final int BASE_MARGIN_DP = 16;
    private static final int BOTTOM_OFFSET_DP = 60;
    /**
     * The most notifications shown at once (item 201).
     *
     * <p>A burst of events (a loadout applying a dozen modules, or a script toggling a set) would
     * otherwise stack past the top of the screen and keep a dozen windows alive. Past the cap the
     * oldest is dismissed, so the newest — the one the player just caused — is always the visible
     * one.
     */
    private static final int MAX_NOTIFICATIONS = 3;

    private final Activity activity;
    private final Handler handler = MainThread.handler();
    private final WindowManager windowManager;
    private final List<NotificationEntry> activeNotifications = new ArrayList<>();
    private final float density;

    private static class NotificationEntry {
        View view;
        WindowManager.LayoutParams params;
        Runnable dismissRunnable;
        ObjectAnimator progressAnimator;
        int index;
    }

    public ModNotificationManager(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
        this.density = activity.getResources().getDisplayMetrics().density;
    }

    public void show(String modName, String modId) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        handler.post(() -> showInternal(modName, modId));
    }

    private void showInternal(String modName, String modId) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        shiftNotificationsUp();

        try {
            View notificationView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_notification, null);
            setupViews(notificationView, modName, modId);

            int margin = (int) (BASE_MARGIN_DP * density);
            int bottomOffset = (int) (BOTTOM_OFFSET_DP * density);

            WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.BOTTOM | Gravity.END;
            wmParams.x = margin;
            wmParams.y = margin + bottomOffset;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            NotificationEntry entry = new NotificationEntry();
            entry.view = notificationView;
            entry.params = wmParams;
            entry.index = 0;

            windowManager.addView(notificationView, wmParams);
            activeNotifications.add(0, entry);

            // Enforce the cap before animating in, so the queue never briefly holds more windows
            // than the cap allows. The "which to drop" decision is the pure, tested
            // ModNotificationQueue; the manager only applies it.
            for (int idx : ModNotificationQueue.evictionIndices(
                    activeNotifications.size(), MAX_NOTIFICATIONS)) {
                if (idx >= 0 && idx < activeNotifications.size()) {
                    dismissNotification(activeNotifications.get(idx));
                }
            }

            animateIn(notificationView);
            startProgressAnimation(notificationView, entry);

            entry.dismissRunnable = () -> dismissNotification(entry);
            handler.postDelayed(entry.dismissRunnable, DISPLAY_DURATION);
        } catch (Exception e) {
            AppLog.warn(TAG, "Showing a mod notification failed", e);
        }
    }

    private void shiftNotificationsUp() {
        int shiftAmount = (int) ((NOTIFICATION_HEIGHT_DP + NOTIFICATION_SPACING_DP) * density);
        for (NotificationEntry entry : activeNotifications) {
            entry.index++;
            entry.params.y += shiftAmount;
            try {
                entry.view.animate()
                    .translationY(-shiftAmount)
                    .setDuration(150)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        entry.view.setTranslationY(0);
                        try {
                            windowManager.updateViewLayout(entry.view, entry.params);
                        } catch (Exception ignored) {
                            AppLog.warnOnce(TAG, "notif-relayout", "Relaying out a notification failed", ignored);
                        }
                    })
                    .start();
            } catch (Exception ignored) {
                AppLog.warnOnce(TAG, "notif-relayout2", "Relaying out a notification failed", ignored);
            }
        }
    }

    private void setupViews(View notificationView, String modName, String modId) {
        ImageView icon = notificationView.findViewById(R.id.notification_icon);
        TextView title = notificationView.findViewById(R.id.notification_title);
        TextView message = notificationView.findViewById(R.id.notification_message);

        icon.setImageResource(ModIconHelper.getModIcon(modId));
        icon.setImageTintList(null);
        icon.setColorFilter(null);
        title.setText(modName);
        message.setText(activity.getString(R.string.mod_status_enabled));
    }

    private void animateIn(View view) {
        view.setTranslationX(300);
        view.setAlpha(0f);
        ViewPropertyAnimator animator = view.animate()
            .translationX(0)
            .alpha(1f)
            .setDuration(SLIDE_DURATION)
            .setInterpolator(new DecelerateInterpolator());
        // A hardware layer pays for itself while the slide/alpha runs and is pure overhead after,
        // so it is promoted for the animation and demoted on its end or cancel (item 189).
        OverlayLayers.hardwareDuring(view, animator);
        animator.start();
    }

    private void dismissNotification(NotificationEntry entry) {
        if (!activeNotifications.contains(entry)) return;

        ViewPropertyAnimator animator = entry.view.animate()
            .translationX(300)
            .alpha(0f)
            .setDuration(SLIDE_DURATION)
            .setInterpolator(new AccelerateInterpolator())
            .withEndAction(() -> removeNotification(entry));
        OverlayLayers.hardwareDuring(entry.view, animator);
        animator.start();
    }

    private void removeNotification(NotificationEntry entry) {
        if (entry.dismissRunnable != null) {
            handler.removeCallbacks(entry.dismissRunnable);
        }
        // The progress bar's animator holds the view; cancelling it here is what lets the view be
        // collected after removal instead of ticking until the display duration elapses.
        if (entry.progressAnimator != null) {
            entry.progressAnimator.cancel();
            entry.progressAnimator = null;
        }
        try {
            windowManager.removeView(entry.view);
        } catch (Exception ignored) {
            AppLog.warnOnce(TAG, "notif-remove", "Removing a notification view failed", ignored);
        }
        activeNotifications.remove(entry);
    }

    private void startProgressAnimation(View notificationView, NotificationEntry entry) {
        ProgressBar progress = notificationView.findViewById(R.id.notification_progress);
        ObjectAnimator animator = ObjectAnimator.ofInt(progress, "progress", 100, 0);
        animator.setDuration(DISPLAY_DURATION);
        animator.setInterpolator(new AccelerateInterpolator(0.5f));
        entry.progressAnimator = animator;
        animator.start();
    }


    public void hideAll() {
        handler.post(() -> {
            for (NotificationEntry entry : new ArrayList<>(activeNotifications)) {
                removeNotification(entry);
            }
            activeNotifications.clear();
        });
    }
}
