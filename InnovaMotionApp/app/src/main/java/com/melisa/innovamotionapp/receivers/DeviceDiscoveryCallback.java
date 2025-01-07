package com.melisa.innovamotionapp.receivers;

import android.bluetooth.BluetoothDevice;

public interface DeviceDiscoveryCallback {
    void onDeviceFound(BluetoothDevice bluetoothDevice);
}
