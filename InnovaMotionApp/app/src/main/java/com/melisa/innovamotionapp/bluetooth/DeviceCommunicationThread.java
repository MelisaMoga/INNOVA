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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.melisa.innovamotionapp.utils.MessageParser;

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

    public interface DataCallback {
        void onConnectionEstablished(BluetoothDevice device);
        
        /**
         * Called when a complete packet is received (END_PACKET delimiter detected).
         * @param device The Bluetooth device
         * @param packetLines List of message lines in the packet (excluding END_PACKET)
         */
        void onPacketReceived(BluetoothDevice device, List<String> packetLines);
        
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
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String receivedLine;
        
        // Packet accumulator for multi-user messages
        List<String> packetLines = new ArrayList<>();

        Log.d(TAG, "Starting to receive data from Bluetooth device (packet mode).");

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
                        receivedLine = receivedLine.substring(0, MAX_CHARS_PER_LINE);
                    }

                    Log.d(TAG, "[Thread] Received line: \"" + receivedLine + "\"");
                    
                    // Check if this is the packet end delimiter
                    if (MessageParser.isPacketEnd(receivedLine)) {
                        Log.i(TAG, "[Thread] END_PACKET detected, processing packet with " + packetLines.size() + " lines");
                        
                        // Trigger packet processing callback
                        if (!packetLines.isEmpty()) {
                            callback.onPacketReceived(device, new ArrayList<>(packetLines));
                        } else {
                            Log.w(TAG, "[Thread] Empty packet received (no lines before END_PACKET)");
                        }
                        
                        // Clear accumulator for next packet
                        packetLines.clear();
                    } else {
                        // Accumulate line for current packet
                        packetLines.add(receivedLine);
                    }
                } else {
                    // readLine() returns null if the stream is closed gracefully
                    Log.i(TAG, "Input stream closed gracefully by remote device or system.");
                    
                    // If we have accumulated lines, process them as a partial packet
                    if (!packetLines.isEmpty()) {
                        Log.w(TAG, "[Thread] Connection closed with " + packetLines.size() + " accumulated lines (incomplete packet)");
                        // Discard incomplete packet
                        packetLines.clear();
                    }
                    
                    break; // Exit the receiving loop
                }
            } catch (IOException e) {
                // This catches read errors or if the stream is unexpectedly disconnected
                Log.e(TAG, "I/O error during data reception (stream likely disconnected)", e);
                
                // Discard incomplete packet
                if (!packetLines.isEmpty()) {
                    Log.w(TAG, "[Thread] I/O error with " + packetLines.size() + " accumulated lines (discarding incomplete packet)");
                    packetLines.clear();
                }
                
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