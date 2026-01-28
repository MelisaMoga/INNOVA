package com.melisa.innovamotionapp.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * Utility class for checking Bluetooth permissions.
 * Centralizes permission checking logic for use in BtSettingsActivity.
 */
public class PermissionHelper {
    
    /**
     * Checks if the app has Bluetooth scan permission.
     * On Android S+ (API 31+), checks BLUETOOTH_SCAN permission.
     * On older versions, checks ACCESS_FINE_LOCATION (legacy requirement).
     * 
     * @param context The context to check permissions
     * @return true if permission is granted, false otherwise
     */
    public static boolean hasBluetoothScanPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Legacy: BLUETOOTH + BLUETOOTH_ADMIN + ACCESS_FINE_LOCATION
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Checks if the app has Bluetooth connect permission.
     * On Android S+ (API 31+), checks BLUETOOTH_CONNECT permission.
     * On older versions, returns true (no runtime permission needed pre-S).
     * 
     * @param context The context to check permissions
     * @return true if permission is granted or not required, false otherwise
     */
    public static boolean hasBluetoothConnectPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Legacy: BLUETOOTH + BLUETOOTH_ADMIN (no runtime permission needed pre-S)
            return true; // No runtime permission needed pre-S
        }
    }
}
