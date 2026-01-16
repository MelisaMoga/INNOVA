package com.melisa.innovamotionapp.utils;

import android.content.Context;

import com.melisa.innovamotionapp.sync.UserSession;

public final class TargetUserResolver {
    private TargetUserResolver() {}

    /**
     * Resolves the target user ID for data access.
     * 
     * For aggregators: returns their own user ID.
     * For supervisors: returns their own user ID (sensor-specific data is now filtered by sensorId).
     * 
     * Note: With the new architecture, supervisors access data by sensorId, not userId.
     * This method returns the current user ID for session-based data access.
     */
    public static String resolveTargetUserId(Context context) {
        UserSession session = UserSession.getInstance(context.getApplicationContext());
        if (!session.isLoaded()) return null;
        
        // Both aggregators and supervisors use their own user ID for session purposes
        // Sensor-specific data filtering now uses sensorId directly
        return session.getCurrentUserId();
    }
}


