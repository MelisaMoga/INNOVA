package com.melisa.pedonovation.BluetoothCore;

import android.bluetooth.BluetoothDevice;

import com.melisa.pedonovation.PedometricSole.PedometricData;

public class BluetoothDeviceReceivedData {
    public final BluetoothDevice device;
    public final PedometricData pedometricData;
    public final String receivedData;

    public BluetoothDeviceReceivedData(BluetoothDevice device, PedometricData pedometricData, String receivedData) {
        this.device = device;
        this.pedometricData = pedometricData;
        this.receivedData = receivedData;
    }
}
