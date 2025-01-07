package com.melisa.innovamotionapp;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.util.ArraySet;

public class GlobalData extends Application {
    private static GlobalData instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static GlobalData getInstance() {
        return instance;
    }
    public ArraySet<BluetoothDevice> nearbyBtDevices = new ArraySet<>();
}
