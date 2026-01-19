package com.melisa.innovamotionapp.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class UserDeviceSettingsStorage {
    private static final String PREF_NAME = "UserDevicePrefs";
    private static final String LATEST_USER_KEY = "latestUser";
    private static final String LATEST_DEVICE_ADDRESS = "lastDeviceAddress";
    private static final String LAST_SELECTED_ROLE_PREFIX = "lastRole_";
    private final SharedPreferences sharedPreferences;

    public UserDeviceSettingsStorage(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save the latest username
    public void saveLatestUser(String userName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LATEST_USER_KEY, userName);
        editor.apply();
    }

    // Save the latest device address
    public void saveLatestDeviceAddress(String deviceAddress) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LATEST_DEVICE_ADDRESS, deviceAddress);
        editor.apply();
    }

    // Retrieve the latest username
    public String getLatestUser() {
        return sharedPreferences.getString(LATEST_USER_KEY, "");
    }

    // Retrieve the latest device address
    public String getLatestDeviceAddress() {
        return sharedPreferences.getString(LATEST_DEVICE_ADDRESS, "");
    }
    
    /**
     * Save the last selected role for a specific user on this device.
     * This enables auto-redirect to the user's preferred dashboard on app restart.
     * 
     * @param uid The Firebase user UID
     * @param role The selected role ("aggregator" or "supervisor")
     */
    public void saveLastSelectedRole(String uid, String role) {
        if (uid == null || uid.isEmpty()) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LAST_SELECTED_ROLE_PREFIX + uid, role);
        editor.apply();
    }
    
    /**
     * Retrieve the last selected role for a specific user on this device.
     * 
     * @param uid The Firebase user UID
     * @return The saved role, or null if no preference exists
     */
    public String getLastSelectedRole(String uid) {
        if (uid == null || uid.isEmpty()) return null;
        String role = sharedPreferences.getString(LAST_SELECTED_ROLE_PREFIX + uid, null);
        return role;
    }
    
    /**
     * Clear the last selected role for a specific user.
     * Called on sign-out to ensure RoleSelectionActivity appears on next login.
     * 
     * @param uid The Firebase user UID
     */
    public void clearLastSelectedRole(String uid) {
        if (uid == null || uid.isEmpty()) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(LAST_SELECTED_ROLE_PREFIX + uid);
        editor.apply();
    }

}