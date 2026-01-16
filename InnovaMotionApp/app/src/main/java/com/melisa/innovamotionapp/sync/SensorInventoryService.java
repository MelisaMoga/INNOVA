package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.data.database.MonitoredPersonDao;
import com.melisa.innovamotionapp.data.models.Sensor;
import com.melisa.innovamotionapp.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the sensor inventory in Firestore.
 * 
 * Collection: sensors
 * Document ID: {sensorId}
 * 
 * This replaces the old PersonNamesFirestoreSync which used a subcollection under users.
 * Sensors are now stored at the root level for easier querying by supervisors.
 */
public class SensorInventoryService {
    private static final String TAG = "SensorInventoryService";
    private static final String COLLECTION_SENSORS = Constants.FIRESTORE_COLLECTION_SENSORS;

    private static volatile SensorInventoryService instance;
    
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final MonitoredPersonDao dao;
    private final ExecutorService executor;

    private SensorInventoryService(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.dao = InnovaDatabase.getInstance(context.getApplicationContext()).monitoredPersonDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Get the singleton instance.
     */
    public static SensorInventoryService getInstance(Context context) {
        if (instance == null) {
            synchronized (SensorInventoryService.class) {
                if (instance == null) {
                    instance = new SensorInventoryService(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Callback interface for sync operations.
     */
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Callback for fetching sensors.
     */
    public interface SensorListCallback {
        void onResult(List<Sensor> sensors);
        void onError(String error);
    }

    /**
     * Register or update a sensor in the inventory.
     * 
     * @param sensorId      The sensor ID (from hardware)
     * @param deviceAddress The Bluetooth MAC address
     * @param displayName   Human-readable name for the sensor
     * @param callback      Callback for success/error
     */
    public void registerSensor(@NonNull String sensorId, String deviceAddress, 
                               @NonNull String displayName, @NonNull SyncCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        // #region agent log
        try { java.io.FileWriter fw = new java.io.FileWriter("/mnt/d/Proiecte/INNOVA/InnovaMotionApp/.cursor/debug.log", true); fw.write("{\"hypothesisId\":\"H4\",\"location\":\"SensorInventoryService.java:registerSensor\",\"message\":\"registerSensor called\",\"data\":{\"sensorId\":\"" + sensorId + "\",\"displayName\":\"" + displayName + "\",\"userNull\":" + (user == null) + "},\"timestamp\":" + System.currentTimeMillis() + "}\n"); fw.close(); } catch (Exception e) {}
        // #endregion
        
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        String ownerUid = user.getUid();
        Sensor sensor = new Sensor(sensorId, deviceAddress, ownerUid, displayName);

        firestore.collection(COLLECTION_SENSORS)
                .document(sensorId)
                .set(sensor.toFirestoreDocument())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Registered sensor: " + sensorId);
                    // #region agent log
                    try { java.io.FileWriter fw = new java.io.FileWriter("/mnt/d/Proiecte/INNOVA/InnovaMotionApp/.cursor/debug.log", true); fw.write("{\"hypothesisId\":\"H4\",\"location\":\"SensorInventoryService.java:registerSensor:onSuccess\",\"message\":\"Firestore write SUCCESS\",\"data\":{\"sensorId\":\"" + sensorId + "\",\"ownerUid\":\"" + ownerUid + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n"); fw.close(); } catch (Exception e) {}
                    // #endregion
                    
                    // Also update local Room database
                    executor.execute(() -> {
                        dao.upsertByName(sensorId, displayName, System.currentTimeMillis());
                    });
                    
                    callback.onSuccess("Sensor registered");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to register sensor: " + sensorId, e);
                    // #region agent log
                    try { java.io.FileWriter fw = new java.io.FileWriter("/mnt/d/Proiecte/INNOVA/InnovaMotionApp/.cursor/debug.log", true); fw.write("{\"hypothesisId\":\"H4\",\"location\":\"SensorInventoryService.java:registerSensor:onFailure\",\"message\":\"Firestore write FAILED\",\"data\":{\"sensorId\":\"" + sensorId + "\",\"error\":\"" + e.getMessage().replace("\"", "'") + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n"); fw.close(); } catch (Exception e2) {}
                    // #endregion
                    callback.onError("Failed to register sensor: " + e.getMessage());
                });
    }

    /**
     * Update the display name of a sensor.
     * Uses set with merge (upsert) to create the sensor document if it doesn't exist.
     * 
     * @param sensorId    The sensor ID
     * @param displayName New display name
     * @param callback    Callback for success/error
     */
    public void updateSensorName(@NonNull String sensorId, @NonNull String displayName, 
                                 @NonNull SyncCallback callback) {
        // #region agent log
        try { java.io.FileWriter fw = new java.io.FileWriter("/mnt/d/Proiecte/INNOVA/InnovaMotionApp/.cursor/debug.log", true); fw.write("{\"hypothesisId\":\"H2\",\"location\":\"SensorInventoryService.java:updateSensorName\",\"message\":\"updateSensorName called (UPSERT)\",\"data\":{\"sensorId\":\"" + sensorId + "\",\"displayName\":\"" + displayName + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n"); fw.close(); } catch (Exception e) {}
        // #endregion
        
        // Build data map for upsert
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", displayName);
        
        // If user is authenticated, also set ownerUid to claim ownership
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            data.put("ownerUid", user.getUid());
        }
        
        // Use set with merge to create-or-update (upsert)
        firestore.collection(COLLECTION_SENSORS)
                .document(sensorId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Upserted sensor name: " + sensorId + " -> " + displayName);
                    // #region agent log
                    try { java.io.FileWriter fw = new java.io.FileWriter("/mnt/d/Proiecte/INNOVA/InnovaMotionApp/.cursor/debug.log", true); fw.write("{\"hypothesisId\":\"H2\",\"location\":\"SensorInventoryService.java:updateSensorName:onSuccess\",\"message\":\"Firestore UPSERT SUCCESS\",\"data\":{\"sensorId\":\"" + sensorId + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n"); fw.close(); } catch (Exception e) {}
                    // #endregion
                    
                    // Also update local Room database
                    executor.execute(() -> {
                        dao.upsertByName(sensorId, displayName, System.currentTimeMillis());
                    });
                    
                    callback.onSuccess("Name updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upsert sensor name: " + sensorId, e);
                    // #region agent log
                    try { java.io.FileWriter fw = new java.io.FileWriter("/mnt/d/Proiecte/INNOVA/InnovaMotionApp/.cursor/debug.log", true); fw.write("{\"hypothesisId\":\"H2\",\"location\":\"SensorInventoryService.java:updateSensorName:onFailure\",\"message\":\"Firestore UPSERT FAILED\",\"data\":{\"sensorId\":\"" + sensorId + "\",\"error\":\"" + e.getMessage().replace("\"", "'") + "\"},\"timestamp\":" + System.currentTimeMillis() + "}\n"); fw.close(); } catch (Exception e2) {}
                    // #endregion
                    callback.onError("Failed to update: " + e.getMessage());
                });
    }

    /**
     * Get a sensor by ID.
     * 
     * @param sensorId The sensor ID
     * @param callback Callback with the sensor (or null if not found)
     */
    public void getSensor(@NonNull String sensorId, @NonNull SensorListCallback callback) {
        firestore.collection(COLLECTION_SENSORS)
                .document(sensorId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<Sensor> result = new ArrayList<>();
                    if (doc.exists()) {
                        Sensor sensor = Sensor.fromDocument(doc);
                        if (sensor != null) {
                            result.add(sensor);
                        }
                    }
                    callback.onResult(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get sensor: " + sensorId, e);
                    callback.onError("Failed to get sensor: " + e.getMessage());
                });
    }

    /**
     * Get all sensors owned by the current user.
     * 
     * @param callback Callback with the list of sensors
     */
    public void getOwnedSensors(@NonNull SensorListCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        String ownerUid = user.getUid();
        
        firestore.collection(COLLECTION_SENSORS)
                .whereEqualTo("ownerUid", ownerUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Sensor> sensors = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Sensor sensor = Sensor.fromDocument(doc);
                        if (sensor != null) {
                            sensors.add(sensor);
                        }
                    }
                    Log.d(TAG, "Found " + sensors.size() + " owned sensors");
                    callback.onResult(sensors);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get owned sensors", e);
                    callback.onError("Failed to get sensors: " + e.getMessage());
                });
    }

    /**
     * Get sensors by a list of sensor IDs.
     * Useful for supervisors to resolve names for their assigned sensors.
     * 
     * @param sensorIds List of sensor IDs to fetch
     * @param callback  Callback with the list of sensors
     */
    public void getSensorsByIds(@NonNull List<String> sensorIds, @NonNull SensorListCallback callback) {
        if (sensorIds.isEmpty()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        // Firestore whereIn has a limit of 10, so we may need to batch
        List<Sensor> allSensors = new ArrayList<>();
        int batchSize = Constants.FIRESTORE_WHERE_IN_LIMIT;
        int totalBatches = (int) Math.ceil((double) sensorIds.size() / batchSize);
        final int[] completedBatches = {0};
        final boolean[] hasError = {false};

        for (int i = 0; i < sensorIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, sensorIds.size());
            List<String> batch = sensorIds.subList(i, endIndex);

            firestore.collection(COLLECTION_SENSORS)
                    .whereIn("__name__", batch.stream()
                            .map(id -> firestore.collection(COLLECTION_SENSORS).document(id))
                            .collect(java.util.stream.Collectors.toList()))
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        synchronized (allSensors) {
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                Sensor sensor = Sensor.fromDocument(doc);
                                if (sensor != null) {
                                    allSensors.add(sensor);
                                }
                            }
                            completedBatches[0]++;
                            if (completedBatches[0] >= totalBatches && !hasError[0]) {
                                callback.onResult(allSensors);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        synchronized (allSensors) {
                            if (!hasError[0]) {
                                hasError[0] = true;
                                Log.e(TAG, "Failed to get sensors by IDs", e);
                                callback.onError("Failed to get sensors: " + e.getMessage());
                            }
                        }
                    });
        }
    }

    /**
     * Upload all local person names to Firestore sensors collection.
     * Migration helper from old PersonNamesFirestoreSync.
     * 
     * @param callback Callback for success/error
     */
    public void uploadAllFromLocal(@NonNull SyncCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        String ownerUid = user.getUid();
        
        executor.execute(() -> {
            try {
                List<MonitoredPerson> persons = dao.getAllMonitoredPersonsSync();
                
                if (persons.isEmpty()) {
                    callback.onSuccess("No sensors to upload");
                    return;
                }

                WriteBatch batch = firestore.batch();
                
                for (MonitoredPerson person : persons) {
                    Sensor sensor = new Sensor(
                            person.getSensorId(),
                            null, // deviceAddress not stored in MonitoredPerson
                            ownerUid,
                            person.getDisplayName()
                    );
                    
                    batch.set(
                            firestore.collection(COLLECTION_SENSORS).document(person.getSensorId()),
                            sensor.toFirestoreDocument()
                    );
                }

                batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Uploaded " + persons.size() + " sensors");
                            callback.onSuccess("Uploaded " + persons.size() + " sensors");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to upload sensors", e);
                            callback.onError("Upload failed: " + e.getMessage());
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error preparing upload", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Save a list of sensors to the local Room database.
     * This is a synchronous operation that should be called from a background thread,
     * or use the async version with callback.
     * 
     * @param sensors List of sensors to save
     * @return Number of sensors saved
     */
    public int saveSensorsToLocalSync(@NonNull List<Sensor> sensors) {
        int count = 0;
        for (Sensor sensor : sensors) {
            if (sensor.getSensorId() != null) {
                String displayName = sensor.getDisplayName() != null 
                        ? sensor.getDisplayName() 
                        : sensor.getSensorId();
                dao.upsertByName(sensor.getSensorId(), displayName, System.currentTimeMillis());
                count++;
            }
        }
        Log.d(TAG, "Saved " + count + " sensors to local database");
        return count;
    }

    /**
     * Save a list of sensors to the local Room database asynchronously.
     * 
     * @param sensors  List of sensors to save
     * @param callback Callback for success/error
     */
    public void saveSensorsToLocal(@NonNull List<Sensor> sensors, @NonNull SyncCallback callback) {
        if (sensors.isEmpty()) {
            callback.onSuccess("No sensors to save");
            return;
        }
        
        executor.execute(() -> {
            try {
                int count = saveSensorsToLocalSync(sensors);
                callback.onSuccess("Saved " + count + " sensors to local database");
            } catch (Exception e) {
                Log.e(TAG, "Failed to save sensors to local", e);
                callback.onError("Failed to save sensors: " + e.getMessage());
            }
        });
    }

    /**
     * Download sensors to local Room database.
     * For supervisors to cache sensor names locally.
     * 
     * @param sensorIds List of sensor IDs to download
     * @param callback  Callback for success/error
     */
    public void downloadToLocal(@NonNull List<String> sensorIds, @NonNull SyncCallback callback) {
        getSensorsByIds(sensorIds, new SensorListCallback() {
            @Override
            public void onResult(List<Sensor> sensors) {
                saveSensorsToLocal(sensors, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Delete a sensor from the inventory.
     * 
     * @param sensorId The sensor ID to delete
     * @param callback Callback for success/error
     */
    public void deleteSensor(@NonNull String sensorId, @NonNull SyncCallback callback) {
        firestore.collection(COLLECTION_SENSORS)
                .document(sensorId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deleted sensor: " + sensorId);
                    callback.onSuccess("Sensor deleted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete sensor: " + sensorId, e);
                    callback.onError("Delete failed: " + e.getMessage());
                });
    }
}
