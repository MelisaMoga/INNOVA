package com.melisa.innovamotionapp.receivers;


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NearbyDeviceDiscoveryReceiver extends BroadcastReceiver {
    private final NearbyDeviceDiscoveryListener listener;

    public interface NearbyDeviceDiscoveryListener {
        void onDeviceFound(BluetoothDevice bluetoothDevice);
    }

    // Constructor accepts a callback
    public NearbyDeviceDiscoveryReceiver(NearbyDeviceDiscoveryListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            listener.onDeviceFound(device);
        }
    }


}
