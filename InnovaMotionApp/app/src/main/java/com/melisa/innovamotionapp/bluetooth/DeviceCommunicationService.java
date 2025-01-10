package com.melisa.innovamotionapp.bluetooth;

import static android.content.ContentValues.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.PostureFactory;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DeviceCommunicationService extends Service {
    private FileOutputStream fileOutputStream;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private DeviceCommunicationThread deviceCommunicationThread; // The connection thread that handles communication with a device
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "bluetooth_service_channel";
    private int attemptToReconnectCounter = 0;
    private final int MAX_NUM_CONNECTING_CONSECUTIVE_ATTEMPTS = 2;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1. Create Notification Channel (if needed for Android 8.0+)
        createNotificationChannel();

        // 2. Create Notification
        Notification notification = createNotification();

        // 3. Start Foreground *immediately*
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY; // or START_REDELIVER_INTENT
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Bluetooth Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {
        // Build your notification here
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth Service")
                .setContentText("Bluetooth communication in progress")
                .setSmallIcon(R.drawable.baseline_bluetooth_connected_24)
                .build();
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public DeviceCommunicationService getService() {
            return DeviceCommunicationService.this;
        }
    }


    /**
     * Starts the connection to a Bluetooth device by initializing the connection thread.
     */
    public void connectToDevice(BluetoothDevice device) {
        // If a device is already connected, cancel the previous thread
        disconnectDevice();

        try {
            // Prepare the file to store data from the device
            fileOutputStream = new FileOutputStream(new File(getFilesDir(), device.getAddress() + "_data.txt"));

            // Create a new connection thread to connect to the Bluetooth device
            deviceCommunicationThread = new DeviceCommunicationThread(device, new DeviceCommunicationThread.DataCallback() {

                @Override
                public void onConnectionEstablished() {
                    GlobalData.getInstance().setIsConnectedDevice(true);

                    // Reset consecutive attempts
                    attemptToReconnectCounter = 0;

                    // Update the foreground notification to indicate the device is connected
                    Notification notification = new NotificationCompat.Builder(DeviceCommunicationService.this, CHANNEL_ID)
                            .setContentTitle("Bluetooth Device Connected")
                            .setContentText("Communication with your device is ongoing.")
                            .setSmallIcon(R.drawable.baseline_bluetooth_connected_24)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .build();

                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(NOTIFICATION_ID+1, notification);
                }

                @Override
                public void onDataReceived(String receivedData) {
                    Log.d(TAG, "[Service] MSG: " + receivedData);
                    // Save receivedData into file
                    try {
                        fileOutputStream.write((receivedData + "\n").getBytes());
                    } catch (IOException e) {
                        Log.d(TAG, "ERROR closing input stream", e);
                    }
                    // Translate receivedData into Posture
                    Posture posture = PostureFactory.createPosture(receivedData);

                    // Update LiveData with received data
                    GlobalData.getInstance().setReceivedPosture(posture);
                }

                @Override
                public void onConnectionDisconnected() {
                    GlobalData.getInstance().setIsConnectedDevice(false);
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        Log.d(TAG, "ERROR closing input stream", e);
                    }

                    // Send notification on disconnect
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    Notification notification = new NotificationCompat.Builder(DeviceCommunicationService.this, CHANNEL_ID)
                            .setContentTitle("Bluetooth Device Disconnected")
                            .setContentText("Your Bluetooth device has been disconnected.")
                            .setSmallIcon(R.drawable.baseline_bluetooth_disabled_24)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .build();
                    manager.notify(NOTIFICATION_ID + 1, notification);


                    if (attemptToReconnectCounter >= MAX_NUM_CONNECTING_CONSECUTIVE_ATTEMPTS) {
                        // Stop current service
                        GlobalData.getInstance().deviceCommunicationManager.stopService();
                        stopForeground(true);
                        stopSelf();
                    } else {
                        attemptToReconnectCounter += 1;
                        BluetoothDevice deviceToConnect = GlobalData.getInstance().deviceCommunicationManager.getDeviceToConnect();
                        connectToDevice(deviceToConnect);
                    }

                }
            });

            // Start the connection thread
            deviceCommunicationThread.start();
        } catch (IOException e) {
            Log.e(TAG, "[Service] Error at connectToDevice", e);
        }
    }

    /**
     * Disconnect the currently connected device.
     */
    public void disconnectDevice() {
        if (deviceCommunicationThread != null) {
            if (isDeviceConnected()) {
                deviceCommunicationThread.interrupt();
            }
            deviceCommunicationThread = null;
        }
    }

    /**
     * Check if a device is currently connected.
     */
    public boolean isDeviceConnected() {
        return deviceCommunicationThread != null && deviceCommunicationThread.isAlive();
    }
}

