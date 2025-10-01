package com.melisa.innovamotionapp.utils;

import android.content.Context;

import com.melisa.innovamotionapp.sync.UserSession;

public final class TargetUserResolver {
    private TargetUserResolver() {}

    public static String resolveTargetUserId(Context context) {
        UserSession session = UserSession.getInstance(context.getApplicationContext());
        if (!session.isLoaded()) return null;
        if (session.isSupervised()) {
            return session.getCurrentUserId();
        } else if (session.isSupervisor()) {
            java.util.List<String> kids = session.getSupervisedUserIds();
            return (kids == null || kids.isEmpty()) ? null : kids.get(0);
        }
        return null;
    }
}


