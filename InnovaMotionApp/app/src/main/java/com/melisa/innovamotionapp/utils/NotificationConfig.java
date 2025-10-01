package com.melisa.innovamotionapp.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.melisa.innovamotionapp.R;

public final class NotificationConfig {
    private NotificationConfig() {}

    // Channel IDs (stable identifiers)
    public static final String CHANNEL_BT_SERVICE = "bluetooth_service_channel";
    public static final String CHANNEL_FALL_ALERTS = "fall_alerts";

    // Notification IDs (stable integers)
    public static final int NOTIF_ID_BT_SERVICE = 1;      // foreground service
    public static final int NOTIF_ID_FALL_BASE   = 9000;  // can reuse or offset if needed

    // PendingIntent request codes
    public static final int RC_OPEN_FROM_SERVICE = 2001;
    public static final int RC_OPEN_FROM_FALL    = 1001;

    // Intent actions
    public static final String ACTION_OPEN_FROM_SERVICE = "com.melisa.ACTION_OPEN_FROM_SERVICE";
    public static final String ACTION_VIEW_FALL        = "com.melisa.ACTION_VIEW_FALL";

    /** Call once at app startup (e.g., in Application.onCreate). */
    public static void initAllChannels(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);

        // Bluetooth Service channel (quiet/low)
        NotificationChannel bt = new NotificationChannel(
                CHANNEL_BT_SERVICE,
                ctx.getString(R.string.ch_bt_service_name),
                NotificationManager.IMPORTANCE_MIN
        );
        bt.setDescription(ctx.getString(R.string.ch_bt_service_desc));
        nm.createNotificationChannel(bt);

        // Fall Alerts (high/important)
        NotificationChannel fall = new NotificationChannel(
                CHANNEL_FALL_ALERTS,
                ctx.getString(R.string.ch_fall_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        fall.setDescription(ctx.getString(R.string.ch_fall_alerts_desc));
        nm.createNotificationChannel(fall);
    }
}