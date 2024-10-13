package com.melisa.pedonovation.MessageHandlers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import com.melisa.pedonovation.Interfaces.ICurrentActivityManager;
import com.melisa.pedonovation.BluetoothCore.BluetoothDeviceReceivedData;

public class MessageHandler implements Handler.Callback {

    private ICurrentActivityManager iCurrentActivityManager;

    // Constant value for STATE_LISTENING
    public static final int STATE_LISTENING = 1;

    // Constant value for STATE_CONNECTING
    public static final int STATE_CONNECTING = 2;

    // Constant value for STATE_CONNECTED
    public static final int STATE_CONNECTED = 3;

    // Constant value for STATE_CONNECTION_FAILED
    public static final int STATE_CONNECTION_FAILED = 4;

    // Constant value for STATE_MESSAGE_RECEIVED
    public static final int STATE_MESSAGE_RECEIVED = 5;

    public static final int STATE_DISCONNECTED = 6;

    public MessageHandler(ICurrentActivityManager iCurrentActivityManager) {
        this.iCurrentActivityManager = iCurrentActivityManager;
    }


    @SuppressLint({"SetTextI18n", "MissingPermission"})
    @Override
    public boolean handleMessage(Message msg) {
        // If there is no currentActivityManager set then do not handleMessage
        if (iCurrentActivityManager == null) {
            return false;
        }

        BluetoothDeviceReceivedData bluetoothDeviceReceivedData = (BluetoothDeviceReceivedData) msg.obj;
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;
        String data = bluetoothDeviceReceivedData.receivedData;

        // Update Main UI elements
        iCurrentActivityManager.handleAnyState(bluetoothDeviceReceivedData);

        // Handler: A class that allows communication with the UI thread.
        switch (msg.what) {
            // If the value is STATE_LISTENING
            case STATE_LISTENING:
                break;

            // If the value is STATE_CONNECTING
            case STATE_CONNECTING:
                iCurrentActivityManager.handleConnecting(bluetoothDeviceReceivedData);
                break;

            // If the value is STATE_CONNECTED
            case STATE_CONNECTED:
                iCurrentActivityManager.handleConnected(bluetoothDeviceReceivedData);
                break;

            // If the value is STATE_CONNECTION_FAILED
            case STATE_CONNECTION_FAILED:
                iCurrentActivityManager.handleConnectionFailed(bluetoothDeviceReceivedData);
                break;

            // If the value is STATE_MESSAGE_RECEIVED
            case STATE_MESSAGE_RECEIVED:
                iCurrentActivityManager.handleMsgReceived(bluetoothDeviceReceivedData);
                break;

            case STATE_DISCONNECTED:
                iCurrentActivityManager.handleDisconnected(bluetoothDeviceReceivedData);
                break;
        }
        // Return true value
        return true;
    }


}
