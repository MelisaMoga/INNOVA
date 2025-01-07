package com.melisa.innovamotionapp.receivers;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.melisa.innovamotionapp.GlobalData;
import com.melisa.innovamotionapp.uistuff.DeviceDataUIAdapter;

public class DiscoveryReceiver extends BroadcastReceiver {
    private final DeviceDiscoveryCallback callback;

    // Constructor accepts a callback
    public DiscoveryReceiver(DeviceDiscoveryCallback callback) {
        this.callback = callback;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive: ACTION FOUND.");

        if (action.equals(BluetoothDevice.ACTION_FOUND)){
            BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
            callback.onDeviceFound(device);
        }
    }
}
