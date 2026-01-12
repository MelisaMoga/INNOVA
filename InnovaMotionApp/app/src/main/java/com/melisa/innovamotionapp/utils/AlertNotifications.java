package com.melisa.innovamotionapp.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.activities.MainActivity;

/**
 * Utility class for creating and showing alert notifications.
 */
public final class AlertNotifications {
    private AlertNotifications() {}

    /**
     * Show a fall detection alert notification.
     * 
     * The notification includes the person's name in the title for easy identification
     * in multi-user monitoring scenarios.
     * 
     * @param ctx         Application context
     * @param personName  The display name of the person who fell (e.g., "Ion Popescu" or sensor ID)
     * @param msg         Optional message body (uses default if null)
     */
    public static void notifyFall(@NonNull Context ctx, @NonNull String personName, String msg) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent openIntent = new Intent(ctx, MainActivity.class)
                .setAction(NotificationConfig.ACTION_VIEW_FALL)
                .putExtra("from_notification", true)
                .putExtra("fall_person_name", personName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPI = TaskStackBuilder.create(ctx)
                .addNextIntentWithParentStack(openIntent)
                .getPendingIntent(
                        NotificationConfig.RC_OPEN_FROM_FALL,
                        android.os.Build.VERSION.SDK_INT >= 23
                                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                : PendingIntent.FLAG_UPDATE_CURRENT
                );

        // Title includes person name for multi-user monitoring
        String title = ctx.getString(R.string.notif_fall_title, personName);
        String body = msg != null ? msg : ctx.getString(R.string.notif_fall_text_generic);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, NotificationConfig.CHANNEL_FALL_ALERTS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentPI);

        // Use unique notification ID per person to avoid overwriting
        int notificationId = NotificationConfig.NOTIF_ID_FALL_BASE + personName.hashCode();
        nm.notify(notificationId, b.build());
    }
}