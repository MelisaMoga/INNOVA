package com.melisa.innovamotionapp.bluetooth;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
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
import com.melisa.innovamotionapp.activities.MainActivity;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.sync.FirestoreSyncService;
import com.melisa.innovamotionapp.sync.UserSession;
import com.melisa.innovamotionapp.utils.AlertNotifications;
import com.melisa.innovamotionapp.utils.Constants;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.NotificationConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceCommunicationService extends Service {
    private FileOutputStream fileOutputStream;
    
    // Packet processing
    private PacketProcessor packetProcessor;
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();

        // Init database with current context
        database = InnovaDatabase.getInstance(this);
        
        // Initialize Firestore sync service and user session
        firestoreSyncService = FirestoreSyncService.getInstance(this);
        userSession = UserSession.getInstance(this);
        
        // Initialize executor service for packet processing
        executorService = Executors.newFixedThreadPool(2);
        
        Log.i(TAG, "DeviceCommunicationService created with packet-based architecture");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private DeviceCommunicationThread deviceCommunicationThread; // The connection thread that handles communication with a device
    private int attemptToReconnectCounter = 0;
    private final int MAX_NUM_CONNECTING_CONSECUTIVE_ATTEMPTS = 2;

    // Database operations
    private InnovaDatabase database;
    
    // Firestore sync service and user session
    private FirestoreSyncService firestoreSyncService;
    private UserSession userSession;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Foreground start with a neutral/starting state
        Notification initial = new androidx.core.app.NotificationCompat.Builder(this, NotificationConfig.CHANNEL_BT_SERVICE)
                .setSmallIcon(R.drawable.baseline_bluetooth_connected_24)
                .setContentTitle(getString(R.string.notif_bt_title_init))
                .setContentText(getString(R.string.notif_bt_text_starting))
                .setOngoing(true)
                .build();

        startForeground(NotificationConfig.NOTIF_ID_BT_SERVICE, initial);

        return START_STICKY; // or START_REDELIVER_INTENT
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
            fileOutputStream = new FileOutputStream(new File(getFilesDir(), String.format(Constants.POSTURE_DATA_SAVE_FILE_NAME, device.getAddress())));
            
            // Create packet processor for this device
            packetProcessor = new PacketProcessor(this, device.getAddress(), executorService);

            // Create a new connection thread to connect to the Bluetooth device
            deviceCommunicationThread = new DeviceCommunicationThread(device, new DeviceCommunicationThread.DataCallback() {

                @SuppressLint("MissingPermission")
                @Override
                public void onConnectionEstablished(BluetoothDevice device) {
                    GlobalData.getInstance().setIsConnectedDevice(true);

                    // Reset consecutive attempts
                    attemptToReconnectCounter = 0;

                    // Update the existing foreground notification (don't create a new one)
                    updateServiceNotification(
                            getString(R.string.notif_bt_title_connected, device.getName()),
                            getString(R.string.notif_bt_text_connected),
                            R.drawable.baseline_bluetooth_connected_24
                    );
                }
                
                @Override
                public void onPacketReceived(BluetoothDevice device, List<String> packetLines) {
                    Log.i(TAG, "[Service] Packet received with " + packetLines.size() + " lines");
                    
                    // Write packet lines to file for debugging
                    try {
                        for (String line : packetLines) {
                            fileOutputStream.write((line + "\n").getBytes());
                        }
                        fileOutputStream.write("END_PACKET\n".getBytes());
                        fileOutputStream.flush();
                    } catch (IOException e) {
                        Log.w(TAG, "ERROR writing packet to file", e);
                    }
                    
                    // Process packet (parsing, Room insert, Firestore upload)
                    // This happens on background thread inside PacketProcessor
                    packetProcessor.processPacket(packetLines);
                }

                @Override
                public void onConnectionDisconnected() {
                    GlobalData.getInstance().setIsConnectedDevice(false);
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        Log.d(TAG, "ERROR closing input stream", e);
                    }

                    // Update the same foreground notification to show we're reconnecting
                    updateServiceNotification(
                            getString(R.string.notif_bt_title_disconnected),
                            getString(R.string.notif_bt_text_reconnecting),
                            R.drawable.baseline_bluetooth_disabled_24
                    );


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

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdown();
        }
        
        // Clean up sync service and user session
        if (firestoreSyncService != null) {
            firestoreSyncService.cleanup();
        }
        if (userSession != null) {
            userSession.cleanup();
        }
    }

    /**
     * Check if a device is currently connected.
     */
    public boolean isDeviceConnected() {
        return deviceCommunicationThread != null && deviceCommunicationThread.isAlive();
    }

    private void updateServiceNotification(String title, String text, int iconRes) {
        Intent openIntent = new Intent(this, MainActivity.class)
                .setAction(NotificationConfig.ACTION_OPEN_FROM_SERVICE);

        android.app.PendingIntent contentPI =
                androidx.core.app.TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(openIntent)
                        .getPendingIntent(
                                NotificationConfig.RC_OPEN_FROM_SERVICE,
                                android.os.Build.VERSION.SDK_INT >= 23
                                        ? android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                                        : android.app.PendingIntent.FLAG_UPDATE_CURRENT
                        );

        Notification notification = new androidx.core.app.NotificationCompat.Builder(this, NotificationConfig.CHANNEL_BT_SERVICE)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(contentPI)
                .build();

        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NotificationConfig.NOTIF_ID_BT_SERVICE, notification); // same ID → replaces in place
    }
}

