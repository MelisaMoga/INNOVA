package com.melisa.innovamotionapp.utils;

import android.util.Log;
import java.util.List;

public final class OwnerSource {
    private OwnerSource() {}

    /** 
     * For supervised: read currentUserUid.
     * For supervisor: read the FIRST supervised child (or your selected child if you later add UI).
     */
    public static String resolveOwnerUidForUI(GlobalData global) {
        String role = global.currentUserRole;
        if ("supervised".equals(role)) {
            Log.i("UI/Owner", "role=supervised -> owner=" + global.currentUserUid);
            return global.currentUserUid;
        }
        if ("supervisor".equals(role)) {
            List<String> sensorIds = global.supervisedSensorIds;
            if (sensorIds != null && !sensorIds.isEmpty()) {
                Log.i("UI/Owner", "role=supervisor -> sensorId=" + sensorIds.get(0));
                return sensorIds.get(0);
            }
            Log.e("UI/Owner", "role=supervisor but no supervisedSensorIds!");
            return null;
        }
        Log.e("UI/Owner", "UNKNOWN role; cannot resolve owner");
        return null;
    }
}
