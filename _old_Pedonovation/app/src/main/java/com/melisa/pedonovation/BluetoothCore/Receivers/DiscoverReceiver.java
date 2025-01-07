package com.melisa.pedonovation.BluetoothCore.Receivers;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.melisa.pedonovation.AppActivities.Managers.BtSettingsManager;

public class DiscoverReceiver extends BroadcastReceiver {
    private final BtSettingsManager btSettingsManager;

    public DiscoverReceiver(BtSettingsManager btSettingsManager) {
        this.btSettingsManager = btSettingsManager;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive: ACTION FOUND.");

        if (action.equals(BluetoothDevice.ACTION_FOUND)){
            BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());

            btSettingsManager.deviceFound(device);
        }
    }
}
