package com.melisa.innovamotionapp.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets; // Recommended for explicit encoding
import java.util.List;
import java.util.UUID;

public class DeviceCommunicationThread extends Thread {
    private final BluetoothDevice device;
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final DataCallback callback;

    // Define the maximum number of characters allowed per line
    // Adjust this value based on the expected maximum length of your messages.
    // For "0xAB3311" (8 chars), something like 64 or 128 provides ample buffer.
    private static final int MAX_CHARS_PER_LINE = 64;

    public BluetoothDevice getDevice() {
        return device;
    }

    /**
     * Callback interface for Bluetooth device communication events.
     * 
     * Implementations can choose between two approaches:
     * 1. Single-line mode: Override only onDataReceived() to receive raw lines
     * 2. Packet mode: Override onPacketReceived() to receive complete parsed packets
     * 
     * For backward compatibility, onPacketReceived() has a default empty implementation.
     */
    public interface DataCallback {
        void onConnectionEstablished(BluetoothDevice device);
        
        /**
         * Called when a single line of data is received from the device.
         * This is the legacy single-line format (e.g., "0xAB3311").
         * For multi-user protocol, this is called for each line including "END_PACKET".
         * 
         * @param device The connected Bluetooth device
         * @param data The received line (without trailing newline)
         */
        void onDataReceived(BluetoothDevice device, String data);
        
        /**
         * Called when a complete packet is received (multi-user protocol).
         * A packet consists of multiple sensor readings terminated by "END_PACKET".
         * 
         * Default implementation does nothing for backward compatibility.
         * 
         * @param device The connected Bluetooth device
         * @param readings List of parsed readings from this packet
         */
        default void onPacketReceived(BluetoothDevice device, List<ParsedReading> readings) {
            // Default: no-op for backward compatibility
        }
        
        void onConnectionDisconnected();
    }

    private static final String TAG = "MY_APP_DEBUG_TAG";
    public static UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("MissingPermission")
    public DeviceCommunicationThread(BluetoothDevice device, DataCallback callback) throws IOException {
        this.device = device;
        this.socket = device.createRfcommSocketToServiceRecord(APP_UUID);
        this.inputStream = socket.getInputStream();
        this.callback = callback;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        try {
            Log.d(TAG, "Attempting to connect to Bluetooth device: " + device.getName() + " (" + device.getAddress() + ")");
            socket.connect();
            Log.i(TAG, "Bluetooth connection established with " + device.getName());
            callback.onConnectionEstablished(device);
        } catch (IOException connectException) {
            Log.e(TAG, "Error connecting to Bluetooth device", connectException);
            cancel(); // Close socket and notify disconnect
            return; // Exit thread if connection fails
        }
        startReceiving();
    }

    private void startReceiving() {
        // Using BufferedReader to read line by line.
        // It's generally more efficient than reading byte-by-byte for text streams.
        // Explicitly specifying StandardCharsets.UTF_8 is recommended for robust text handling.
        // If your device strictly sends ASCII, you could use StandardCharsets.US_ASCII.
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String receivedLine;

        Log.d(TAG, "Starting to receive data from Bluetooth device.");

        // Keep listening to the InputStream until an exception occurs or stream closes.
        while (true) {
            try {
                // This reads until a newline character (\n), a carriage return (\r),
                // or a carriage return followed immediately by a newline (\r\n).
                receivedLine = bufferedReader.readLine();

                if (receivedLine != null) {
                    // Implement the character limit check here
                    if (receivedLine.length() > MAX_CHARS_PER_LINE) {
                        Log.w(TAG, "Received line exceeded max length (" + MAX_CHARS_PER_LINE + " chars). Original length: " + receivedLine.length() + ". Truncating data.");
                        // Truncate the string to the maximum allowed length.
                        // The truncated part is lost, but prevents oversized strings.
                        receivedLine = receivedLine.substring(0, MAX_CHARS_PER_LINE);
                    }

                    Log.d(TAG, "[Thread] Received MSG: \"" + receivedLine + "\"");
                    callback.onDataReceived(device, receivedLine);
                } else {
                    // readLine() returns null if the stream is closed gracefully
                    Log.i(TAG, "Input stream closed gracefully by remote device or system.");
                    break; // Exit the receiving loop
                }
            } catch (IOException e) {
                // This catches read errors or if the stream is unexpectedly disconnected
                Log.e(TAG, "I/O error during data reception (stream likely disconnected)", e);
                break; // Exit loop on error
            } catch (Exception e) {
                // Catch any other unexpected exceptions during processing
                Log.e(TAG, "An unexpected error occurred during data reception", e);
                break;
            }
        }
        // Ensure the socket is closed and callback notified when the receiving loop exits
        cancel();
        Log.i(TAG, "Bluetooth data reception thread terminated.");
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        if (socket == null) {
            Log.d(TAG, "Socket is null, no need to close.");
            return;
        }
        try {
            Log.i(TAG, "Attempting to close Bluetooth client socket.");
            socket.close();
            Log.i(TAG, "Bluetooth client socket closed.");
            callback.onConnectionDisconnected();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}