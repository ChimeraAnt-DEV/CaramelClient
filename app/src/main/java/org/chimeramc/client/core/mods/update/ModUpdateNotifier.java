package org.chimeramc.client.core.mods.update;

import org.chimeramc.client.util.AppLog;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import org.chimeramc.client.R;
import org.chimeramc.client.ui.activities.ModsFullscreenActivity;

import java.util.List;

/**
 * Posts the single mod-update notification.
 *
 * <p>One notification for the whole library, not one per mod: the message names the count and the
 * first mod, and tapping it opens the Mods screen. A per-mod prompt for a library of twenty mods is
 * twenty interruptions, which is the behaviour this deliberately avoids.
 *
 * <p>The notification is low importance and does not badge, because it is informational: a mod
 * update is not an alarm, and the user is already in the launcher when it is posted.
 */
public final class ModUpdateNotifier {

    private static final String CHANNEL_ID = "mod_updates";
    private static final int NOTIFICATION_ID = 0x4D0D;

    private ModUpdateNotifier() {
    }

    /** Posts the summary notification for [updates]. */
    public static void notify(Context context, List<ModUpdatePlan.ModUpdate> updates) {
        if (context == null || updates == null || updates.isEmpty()) return;
        Context app = context.getApplicationContext();
        NotificationManager manager =
                (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        ensureChannel(app, manager);

        String text = summaryText(app, updates);
        Intent open = new Intent(app, ModsFullscreenActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent = PendingIntent.getActivity(app, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_caramel_mono)
                .setContentTitle(app.getString(R.string.mod_update_notification_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build();
        try {
            manager.notify(NOTIFICATION_ID, notification);
        } catch (RuntimeException ignored) {
            AppLog.warnOnce("CaramelClient", "ModUpdateNotifier:65", "Ignored a failure at ModUpdateNotifier:65", ignored);
        }
    }

    /**
     * The notification body: "3 mods have updates · First, Second, and 1 more".
     *
     * <p>The named mods are capped at two so the line stays readable on a notification shade; the
     * count still tells the truth about how many there are.
     */
    static String summaryText(Context context, List<ModUpdatePlan.ModUpdate> updates) {
        int count = updates.size();
        String headline = context.getResources()
                .getQuantityString(R.plurals.mod_update_summary, count, count);
        StringBuilder names = new StringBuilder();
        int named = 0;
        for (ModUpdatePlan.ModUpdate update : updates) {
            if (update == null) continue;
            if (named == 2) {
                names.append(context.getString(R.string.mod_update_and_more, count - named));
                break;
            }
            if (named > 0) names.append(", ");
            names.append(update.name);
            named++;
        }
        if (names.length() == 0) return headline;
        return context.getString(R.string.mod_update_summary_detail, headline, names.toString());
    }

    private static void ensureChannel(Context app, NotificationManager manager) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, app.getString(R.string.mod_update_channel),
                NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }
}
