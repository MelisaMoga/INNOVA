package com.melisa.pedonovation.Interfaces;

import com.melisa.pedonovation.BluetoothCore.BluetoothDeviceReceivedData;

public interface ICurrentActivityManager {

    void handleAnyState(BluetoothDeviceReceivedData bluetoothDeviceReceivedData);

    void handleConnecting(BluetoothDeviceReceivedData bluetoothDeviceReceivedData);

    void handleConnected(BluetoothDeviceReceivedData bluetoothDeviceReceivedData);

    void handleConnectionFailed(BluetoothDeviceReceivedData bluetoothDeviceReceivedData);

    void handleMsgReceived(BluetoothDeviceReceivedData bluetoothDeviceReceivedData);

    void handleDisconnected(BluetoothDeviceReceivedData bluetoothDeviceReceivedData);
}
