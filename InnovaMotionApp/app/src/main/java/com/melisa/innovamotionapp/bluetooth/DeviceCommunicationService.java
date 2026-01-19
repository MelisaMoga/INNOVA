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
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import android.content.pm.ServiceInfo;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.activities.AggregatorMenuActivity;
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
import com.melisa.innovamotionapp.utils.PersonNameManager;

import java.io.File;
import java.util.List;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeviceCommunicationService extends Service {
    private FileOutputStream fileOutputStream;


    /**
     * @noinspection BusyWait
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Init database with current context
        database = InnovaDatabase.getInstance(this);
        
        // Initialize Firestore sync service and user session
        firestoreSyncService = FirestoreSyncService.getInstance(this);
        userSession = UserSession.getInstance(this);
        personNameManager = PersonNameManager.getInstance(this);

        // Start thread that will save receivedData on each second
        // Start the batch-saving thread
        new Thread(() -> {
            while (isBatchSavingRunning) {
                try {
                    // Wait for 5 seconds
                    Thread.sleep(Constants.COUNTDOWN_TIMER_IN_MILLISECONDS_FOR_MESSAGE_SAVE);

                    // Copy and clear the list in a thread-safe manner
                    List<ReceivedBtDataEntity> currentBatch;
                    synchronized (lock) {
                        currentBatch = new ArrayList<>(temporaryReceivedBtDataListToSave);
                        temporaryReceivedBtDataListToSave.clear();
                    }

                    // Save the batch to the database
                    if (!currentBatch.isEmpty()) {
                        database.receivedBtDataDao().insertAll(currentBatch);
                        
                        // Sync entire batch to Firestore in a single network call (if aggregator and online)
                        // This is more efficient than individual writes - single round-trip per batch
                        firestoreSyncService.syncPacketBatch(currentBatch, new FirestoreSyncService.SyncCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Log.d(TAG, "Batch synced: " + message);
                            }

                            @Override
                            public void onError(String error) {
                                Log.w(TAG, "Batch sync error: " + error);
                            }

                            @Override
                            public void onProgress(int current, int total) {
                                Log.d(TAG, "Batch sync progress: " + current + "/" + total);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    break;
                }
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private DeviceCommunicationThread deviceCommunicationThread; // The connection thread that handles communication with a device
    private int attemptToReconnectCounter = 0;
    private final int MAX_NUM_CONNECTING_CONSECUTIVE_ATTEMPTS = 2;
    
    // Flag to prevent auto-reconnection during intentional disconnect/shutdown
    private volatile boolean isStopping = false;

    // Database operations
    private InnovaDatabase database;
    private final List<ReceivedBtDataEntity> temporaryReceivedBtDataListToSave = new ArrayList<>();
    private final Object lock = new Object(); // For thread-safe access to the list
    private volatile boolean isBatchSavingRunning = true; // Flag to stop the batch-saving thread
    
    // Firestore sync service and user session
    private FirestoreSyncService firestoreSyncService;
    private UserSession userSession;
    
    // Person name manager (sensor ID to display name mapping)
    private PersonNameManager personNameManager;
    
    // Multi-user protocol parser
    private final PacketParser packetParser = new PacketParser();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Reset stopping flag when service is (re)started
        isStopping = false;
        
        // Foreground start with a neutral/starting state
        Notification initial = new androidx.core.app.NotificationCompat.Builder(this, NotificationConfig.CHANNEL_BT_SERVICE)
                .setSmallIcon(R.drawable.baseline_bluetooth_connected_24)
                .setContentTitle(getString(R.string.notif_bt_title_init))
                .setContentText(getString(R.string.notif_bt_text_starting))
                .setOngoing(true)
                .build();

        // Use ServiceCompat for proper foreground service type declaration (Android 14+ requirement)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires explicit foreground service type
            ServiceCompat.startForeground(
                    this,
                    NotificationConfig.NOTIF_ID_BT_SERVICE,
                    initial,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE | ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-13: service type supported but not required
            ServiceCompat.startForeground(
                    this,
                    NotificationConfig.NOTIF_ID_BT_SERVICE,
                    initial,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE | ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        } else {
            // Pre-Android 10: no service type concept
            startForeground(NotificationConfig.NOTIF_ID_BT_SERVICE, initial);
        }

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

            // Create a new connection thread to connect to the Bluetooth device
            deviceCommunicationThread = new DeviceCommunicationThread(device, new DeviceCommunicationThread.DataCallback() {

                @SuppressLint("MissingPermission")
                @Override
                public void onConnectionEstablished(BluetoothDevice device) {
                    GlobalData.getInstance().setIsConnectedDevice(true);

                    // Reset consecutive attempts
                    attemptToReconnectCounter = 0;

                    // Update the existing foreground notification (don’t create a new one)
                    updateServiceNotification(
                            getString(R.string.notif_bt_title_connected, device.getName()),
                            getString(R.string.notif_bt_text_connected),
                            R.drawable.baseline_bluetooth_connected_24
                    );
                }

                @Override
                public void onDataReceived(BluetoothDevice device, String receivedData) {
                    Log.d(TAG, "[Service] MSG: " + receivedData);

                    try {
                        fileOutputStream.write((receivedData + "\n").getBytes());
                    } catch (IOException e) {
                        Log.d(TAG, "ERROR writing posture file", e);
                    }

                    // Feed line to multi-user protocol parser
                    List<ParsedReading> readings = packetParser.feedLine(receivedData);
                    
                    // If null, the parser is still accumulating (not END_PACKET yet)
                    if (readings == null) {
                        return;
                    }
                    
                    // END_PACKET received - process all readings in this packet
                    if (readings.isEmpty()) {
                        Log.d(TAG, "[Service] Empty packet received");
                        return;
                    }

                    // App policy: user is signed in; Firebase caches UID offline. Fetch UID when aggregator.
                    String ownerUid = null;
                    if (userSession.isLoaded() && userSession.isAggregator()) {
                        ownerUid = firestoreSyncService.getCurrentUserId(); // cached UID even offline
                    }
                    
                    // If not aggregator/signed-in, we can't store data (need owner)
                    if (ownerUid == null) {
                        Log.w(TAG, "[Service] Ignoring packet - no authenticated user");
                        return;
                    }

                    // Process each reading from the packet
                    for (ParsedReading reading : readings) {
                        final ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                                device.getAddress(),
                                reading.getReceivedTimestamp(),
                                reading.getHexCode(),
                                ownerUid,
                                reading.getSensorId()
                        );

                        // Register sensor if new (async, creates with sensorId as default name)
                        personNameManager.ensureSensorExists(reading.getSensorId());

                        // Enqueue for local persistence (batch thread will insertAll with IGNORE)
                        synchronized (lock) {
                            temporaryReceivedBtDataListToSave.add(entity);
                        }

                        // Keep existing LiveData/UI updates (use the last reading's posture)
                        Posture posture = PostureFactory.createPosture(reading.getHexCode());
                        GlobalData.getInstance().setReceivedPosture(posture);

                        // Notify fall locally (aggregator device)
                        if (posture instanceof com.melisa.innovamotionapp.data.posture.types.FallingPosture) {
                            // Get display name asynchronously and show notification
                            final String sensorId = reading.getSensorId();
                            personNameManager.getDisplayNameAsync(sensorId, personName -> {
                                AlertNotifications.notifyFall(
                                        DeviceCommunicationService.this,
                                        personName,
                                        getString(R.string.notif_fall_text_generic)
                                );
                            });
                        }
                    }
                    
                    Log.d(TAG, "[Service] Processed packet with " + readings.size() + " readings");
                }

                @Override
                public void onConnectionDisconnected() {
                    GlobalData.getInstance().setIsConnectedDevice(false);
                    try {
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "ERROR closing file output stream", e);
                    }

                    // If we're intentionally stopping, don't attempt reconnection or update notifications
                    if (isStopping) {
                        Log.i(TAG, "onConnectionDisconnected - stopping flag set, skipping reconnection");
                        return;
                    }

                    // Update the same foreground notification to show we’re reconnecting
                    updateServiceNotification(
                            getString(R.string.notif_bt_title_disconnected),
                            getString(R.string.notif_bt_text_reconnecting),
                            R.drawable.baseline_bluetooth_disabled_24
                    );


                    if (attemptToReconnectCounter >= MAX_NUM_CONNECTING_CONSECUTIVE_ATTEMPTS) {
                        // Max reconnection attempts reached - stop current service
                        Log.w(TAG, "Max reconnection attempts reached, stopping service");
                        GlobalData.getInstance().deviceCommunicationManager.stopService();
                        ServiceCompat.stopForeground(DeviceCommunicationService.this, ServiceCompat.STOP_FOREGROUND_REMOVE);
                        stopSelf();
                    } else {
                        attemptToReconnectCounter += 1;
                        Log.i(TAG, "Attempting to reconnect, attempt " + attemptToReconnectCounter);
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
     * Disconnect the currently connected device by properly closing the Bluetooth socket.
     * This ensures the Classic Bluetooth connection is fully terminated.
     */
    public void disconnectDevice() {
        if (deviceCommunicationThread != null) {
            // Cancel properly closes the BluetoothSocket, which is the correct way
            // to terminate a Classic Bluetooth connection (not just interrupt())
            deviceCommunicationThread.cancel();
            deviceCommunicationThread = null;
        }
    }
    
    /**
     * Disconnects the device and stops the foreground service completely.
     * This is the proper shutdown sequence for intentional disconnects (user-initiated).
     * 
     * Sequence:
     * 1. Set stopping flag to prevent auto-reconnection in onConnectionDisconnected
     * 2. Disconnect device (closes BluetoothSocket)
     * 3. Remove foreground notification
     * 4. Stop the service
     */
    public void disconnectAndStop() {
        Log.i(TAG, "disconnectAndStop() - initiating graceful shutdown");
        
        // Set flag FIRST to prevent onConnectionDisconnected from auto-reconnecting
        isStopping = true;
        
        // Disconnect the Bluetooth socket
        disconnectDevice();
        
        // Update global connection state
        GlobalData.getInstance().setIsConnectedDevice(false);
        
        // Stop foreground and remove notification
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        
        // Stop the service itself
        stopSelf();
        
        Log.i(TAG, "disconnectAndStop() - shutdown complete");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isBatchSavingRunning = false; // Signal the batch-saving thread to stop
        
        // Note: Do NOT cleanup firestoreSyncService or userSession here - they are shared
        // singletons that should outlive this service. Cleanup happens during app termination
        // or is handled by SessionGate when Firebase auth state changes.
    }

    /**
     * Check if a device is currently connected.
     */
    public boolean isDeviceConnected() {
        return deviceCommunicationThread != null && deviceCommunicationThread.isAlive();
    }

    private void updateServiceNotification(String title, String text, int iconRes) {
        Intent openIntent = new Intent(this, AggregatorMenuActivity.class)
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

