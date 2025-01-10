package com.melisa.innovamotionapp.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class DeviceCommunicationThread extends Thread {
    private final BluetoothDevice device;
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final DataCallback callback;

    public BluetoothDevice getDevice() {
        return device;
    }

    public interface DataCallback {
        void onConnectionEstablished();
        void onDataReceived(String data);
        void onConnectionDisconnected();
    }
    byte[] receiveBuffer = new byte[1024];
    private static final String TAG = "MY_APP_DEBUG_TAG";
    public static UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("MissingPermission")
    public DeviceCommunicationThread(BluetoothDevice device, DataCallback callback) throws IOException {
        this.device = device;
        this.socket = device.createRfcommSocketToServiceRecord(APP_UUID);
        this.inputStream = socket.getInputStream();
        this.callback = callback;

//        sendMessageToMainThread(MessageHandler.STATE_CONNECTING, "");
    }


    @SuppressLint("MissingPermission")
    public void run() {
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socket.connect();
            callback.onConnectionEstablished();
//            sendMessageToMainThread(MessageHandler.STATE_CONNECTED, "");
        } catch (IOException connectException) {
            // Unable to connect; close the socket
//            sendMessageToMainThread(MessageHandler.STATE_CONNECTION_FAILED, "");
            Log.d(TAG, "Error at socket connect", connectException);
        }
        startReceiving();
    }

    private void startReceiving() {
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                bytes = inputStream.read(receiveBuffer);
                String receivedData = new String(receiveBuffer, 0, bytes);

                // Notify callback
                Log.d(TAG, "[Thread] MSG: " + receivedData);
                callback.onDataReceived(receivedData);
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
//                sendMessageToMainThread(MessageHandler.STATE_DISCONNECTED, ":(");
                cancel();
                break;
            }
        }
    }



//    // Function where the write operation is performed
//    public void write(byte[] bytes) {
//        try {
//            // Write the outputStream value
//            outStream.write(bytes);
//        } catch (IOException e) {
//            // Print the error
//            Log.d(TAG, "Error on writing to connected socket", e);
//        }
//    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            socket.close();
            callback.onConnectionDisconnected();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}