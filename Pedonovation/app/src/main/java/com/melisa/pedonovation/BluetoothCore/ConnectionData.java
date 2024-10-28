package com.melisa.pedonovation.BluetoothCore;

import android.bluetooth.BluetoothDevice;

import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionData {
    public final BluetoothDevice device;
    public final ConnectionThread connectionThread;
//    public final LinkedBlockingQueue<BluetoothDeviceReceivedData> msgQueue = new LinkedBlockingQueue<>();

    public ConnectionData(BluetoothDevice device, ConnectionThread connectionThread) {
        this.device = device;
        this.connectionThread = connectionThread;
    }

    public void cancelConnection() {
        connectionThread.cancel();
    }

}
