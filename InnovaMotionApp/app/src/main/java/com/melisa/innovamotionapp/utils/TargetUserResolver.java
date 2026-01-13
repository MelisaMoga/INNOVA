package com.melisa.innovamotionapp.utils;

import android.content.Context;

import com.melisa.innovamotionapp.sync.UserSession;

public final class TargetUserResolver {
    private TargetUserResolver() {}

    public static String resolveTargetUserId(Context context) {
        UserSession session = UserSession.getInstance(context.getApplicationContext());
        if (!session.isLoaded()) return null;
        if (session.isAggregator()) {
            return session.getCurrentUserId();
        } else if (session.isSupervisor()) {
            // For supervisors, return first supervised sensor ID (for data access)
            java.util.List<String> sensorIds = session.getSupervisedSensorIds();
            return (sensorIds == null || sensorIds.isEmpty()) ? null : sensorIds.get(0);
        }
        return null;
    }
}


