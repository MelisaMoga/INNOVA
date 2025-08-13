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
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.sync.FirestoreSyncService;
import com.melisa.innovamotionapp.sync.UserSession;
import com.melisa.innovamotionapp.utils.Constants;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.io.File;
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
                        
                        // Sync each message to Firestore if user is supervised and online
                        // NOTE: syncNewMessage does NOT insert into Room (we already did that above)
                        for (ReceivedBtDataEntity entity : currentBatch) {
                            firestoreSyncService.syncNewMessage(entity, new FirestoreSyncService.SyncCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    Log.d(TAG, "Message synced: " + message);
                                }

                                @Override
                                public void onError(String error) {
                                    Log.w(TAG, "Sync error: " + error);
                                }

                                @Override
                                public void onProgress(int current, int total) {
                                    // Not used for single message sync
                                }
                            });
                        }
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
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "bluetooth_service_channel";
    private int attemptToReconnectCounter = 0;
    private final int MAX_NUM_CONNECTING_CONSECUTIVE_ATTEMPTS = 2;

    // Database operations
    private InnovaDatabase database;
    private final List<ReceivedBtDataEntity> temporaryReceivedBtDataListToSave = new ArrayList<>();
    private final Object lock = new Object(); // For thread-safe access to the list
    private volatile boolean isBatchSavingRunning = true; // Flag to stop the batch-saving thread
    
    // Firestore sync service and user session
    private FirestoreSyncService firestoreSyncService;
    private UserSession userSession;

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
                .setContentTitle("InnovaMotion Service")
                .setContentText("Bluetooth communication service is running")
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
            fileOutputStream = new FileOutputStream(new File(getFilesDir(), String.format(Constants.POSTURE_DATA_SAVE_FILE_NAME, device.getAddress())));

            // Create a new connection thread to connect to the Bluetooth device
            deviceCommunicationThread = new DeviceCommunicationThread(device, new DeviceCommunicationThread.DataCallback() {

                @Override
                public void onConnectionEstablished(BluetoothDevice device) {
                    GlobalData.getInstance().setIsConnectedDevice(true);

                    // Reset consecutive attempts
                    attemptToReconnectCounter = 0;

                    // Update the foreground notification to indicate the device is connected
                    @SuppressLint("MissingPermission") Notification notification = new NotificationCompat.Builder(DeviceCommunicationService.this, CHANNEL_ID)
                            .setContentTitle(device.getName() + " Connected")
                            .setContentText("Communication is ongoing.")
                            .setSmallIcon(R.drawable.baseline_bluetooth_connected_24)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .build();

                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(NOTIFICATION_ID + 1, notification);
                }

                @Override
                public void onDataReceived(BluetoothDevice device, String receivedData) {
                    Log.d(TAG, "[Service] MSG: " + receivedData);

                    try {
                        fileOutputStream.write((receivedData + "\n").getBytes());
                    } catch (IOException e) {
                        Log.d(TAG, "ERROR writing posture file", e);
                    }

                    final long now = System.currentTimeMillis();

                    // App policy: user is signed in; Firebase caches UID offline. Fetch UID when supervised.
                    String ownerUid = null;
                    if (userSession.isLoaded() && userSession.isSupervised()) {
                        ownerUid = firestoreSyncService.getCurrentUserId(); // cached UID even offline
                    }

                    // IMPORTANT: for supervised users, ensure owner_user_id is set at creation time
                    final ReceivedBtDataEntity entity = (ownerUid != null)
                            ? new ReceivedBtDataEntity(device.getAddress(), now, receivedData, ownerUid)
                            : new ReceivedBtDataEntity(device.getAddress(), now, receivedData);

                    // Always enqueue for local persistence (batch thread will insertAll with IGNORE)
                    synchronized (lock) {
                        temporaryReceivedBtDataListToSave.add(entity);
                    }

                    // Keep existing LiveData/UI updates
                    Posture posture = PostureFactory.createPosture(receivedData);
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        isBatchSavingRunning = false; // Signal the batch-saving thread to stop
        
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
}

