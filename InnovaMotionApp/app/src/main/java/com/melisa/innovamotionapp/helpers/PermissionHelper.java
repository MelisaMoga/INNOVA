package com.melisa.innovamotionapp.helpers;

import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;

public class PermissionHelper {
    public static boolean checkAndRequestPermissions(AppCompatActivity activity, String[] permissions, int requestCode) {
        for (String permission : permissions) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(permissions, requestCode);
                return false;
            }
        }
        return true;
    }
}
