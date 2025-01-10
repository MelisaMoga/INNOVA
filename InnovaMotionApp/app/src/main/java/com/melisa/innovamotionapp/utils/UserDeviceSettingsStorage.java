package com.melisa.innovamotionapp.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class UserDeviceSettingsStorage {
    private static final String PREF_NAME = "UserDevicePrefs";
    private static final String LATEST_USER_KEY = "latestUser";
    private static final String LATEST_DEVICE_ADDRESS = "lastDeviceAddress";
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

}