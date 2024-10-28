package com.melisa.pedonovation.BluetoothCore;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;


//import com.melisa.pedonovation.MessageHandlers.MessageHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ConnectionThread extends Thread {
    protected final BluetoothSocket socket;
    protected final BluetoothDevice device;
    private static final String TAG = "MY_APP_DEBUG_TAG";
    private Handler handler; // handler that gets info from Bluetooth service
    InputStream inStream;
    OutputStream outStream;
    byte[] receiveBuffer = new byte[1024];


    @SuppressLint("MissingPermission")
    public ConnectionThread(BluetoothDevice device, Handler handler) {
        this.handler = handler;
        this.device = device;

        //sendMessageToMainThread(MessageHandler.STATE_CONNECTING, "");

        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            tmp = device.createRfcommSocketToServiceRecord(BluetoothHelper.APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        socket = tmp;
    }


    @SuppressLint("MissingPermission")
    public void run() {

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socket.connect();
            //sendMessageToMainThread(MessageHandler.STATE_CONNECTED, "");

            startReceiving();
        } catch (IOException connectException) {
            // Unable to connect; close the socket
            //sendMessageToMainThread(MessageHandler.STATE_CONNECTION_FAILED, "");
            cancel();
        }
    }

    private void startReceiving() {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        inStream = tmpIn;
        outStream = tmpOut;

        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = inStream.read(receiveBuffer);
                String receivedData = new String(receiveBuffer, 0, numBytes);

                // Process received data
                processReceivedData(receivedData);
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                //sendMessageToMainThread(MessageHandler.STATE_DISCONNECTED, ":(");
                break;
            }
        }
    }

    private void processReceivedData(String receivedData) {
        processAndSend(receivedData);
    }

    public void sendMessageToMainThread(int what, String data) {
        //BluetoothDeviceReceivedData bluetoothDeviceReceivedData = new BluetoothDeviceReceivedData(device, pedometricData, data);
     //   Message message = handler.obtainMessage(what, bluetoothDeviceReceivedData);
        //handler.sendMessage(message);
    }


    private void processAndSend(String receivedData) {

       /* List<String> lines = pedometricHelper.textToArrayList(receivedData);
        for (String line : lines) {
            Pair<SensorTypeData, SensorTypeData> result = pedometricHelper.processPedometricDataLine(line);
            SensorTypeData accData = result.first;
            SensorTypeData capData = result.second;

            pedometricData = new PedometricData(accData, capData);
            sendMessageToMainThread(MessageHandler.STATE_MESSAGE_RECEIVED, line + '\n');

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
*/

    }

    // Function where the write operation is performed
    public void write(byte[] bytes) {
        try {
            // Write the outputStream value
            outStream.write(bytes);
        } catch (IOException e) {
            // Print the error
            Log.d(TAG, "Error on writing to connected socket", e);
        }
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }
}
