package com.melisa.innovamotionapp.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.activities.MainActivity;

public final class AlertNotifications {
    private AlertNotifications() {}

    /** Fall alert (high-priority). */
    public static void notifyFall(Context ctx, String who, String msg) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent openIntent = new Intent(ctx, MainActivity.class)
                .setAction(NotificationConfig.ACTION_VIEW_FALL)
                .putExtra("from_notification", true)
                .putExtra("fall_owner_uid", who)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPI = TaskStackBuilder.create(ctx)
                .addNextIntentWithParentStack(openIntent)
                .getPendingIntent(
                        NotificationConfig.RC_OPEN_FROM_FALL,
                        android.os.Build.VERSION.SDK_INT >= 23
                                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                : PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, NotificationConfig.CHANNEL_FALL_ALERTS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(ctx.getString(R.string.notif_fall_title))
                .setContentText(msg != null ? msg : ctx.getString(R.string.notif_fall_text_generic))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        msg != null ? msg : ctx.getString(R.string.notif_fall_text_generic)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentPI);

        nm.notify(NotificationConfig.NOTIF_ID_FALL_BASE, b.build());
    }
}