package com.melisa.innovamotionapp.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.data.database.MonitoredPersonDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager for monitored person names.
 * 
 * Maps sensor IDs (like "sensor001") to human-readable display names (like "Ion Popescu").
 * Provides a simple API for name lookup with fallback to sensorId if no name is set.
 * 
 * Thread-safe singleton - all database operations run on a background executor.
 */
public class PersonNameManager {
    private static final String TAG = "PersonNameManager";
    
    private static volatile PersonNameManager instance;
    private final MonitoredPersonDao dao;
    private final ExecutorService executor;

    private PersonNameManager(Context context) {
        this.dao = InnovaDatabase.getInstance(context.getApplicationContext()).monitoredPersonDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Get the singleton instance.
     */
    public static PersonNameManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PersonNameManager.class) {
                if (instance == null) {
                    instance = new PersonNameManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Get display name for a sensor ID.
     * Returns the sensorId itself if no friendly name is set.
     * 
     * WARNING: Must be called off the main thread!
     */
    @NonNull
    public String getDisplayName(@NonNull String sensorId) {
        String name = dao.getDisplayNameForSensor(sensorId);
        return (name != null && !name.isEmpty()) ? name : sensorId;
    }

    /**
     * Get display name asynchronously via callback.
     * Safe to call from main thread.
     */
    public void getDisplayNameAsync(@NonNull String sensorId, @NonNull DisplayNameCallback callback) {
        executor.execute(() -> {
            String name = getDisplayName(sensorId);
            callback.onResult(name);
        });
    }

    /**
     * Set display name for a sensor ID.
     * Creates entry if it doesn't exist, updates if it does.
     * Safe to call from any thread.
     */
    public void setDisplayName(@NonNull String sensorId, @NonNull String displayName) {
        executor.execute(() -> {
            dao.upsertByName(sensorId, displayName, System.currentTimeMillis());
            Log.d(TAG, "Set display name for " + sensorId + " -> " + displayName);
        });
    }

    /**
     * Ensure a sensor ID exists in the database.
     * If not, creates an entry with sensorId as the default display name.
     * Call this when a new sensor is first seen.
     * Safe to call from any thread.
     */
    public void ensureSensorExists(@NonNull String sensorId) {
        executor.execute(() -> {
            if (dao.sensorExists(sensorId) == 0) {
                dao.upsertByName(sensorId, sensorId, System.currentTimeMillis());
                Log.d(TAG, "Registered new sensor: " + sensorId);
            }
        });
    }

    /**
     * Get LiveData of all monitored persons for UI.
     * Safe to call from any thread.
     */
    public LiveData<List<MonitoredPerson>> getAllPersonsLive() {
        return dao.getAllMonitoredPersons();
    }

    /**
     * Get all monitored persons synchronously.
     * WARNING: Must be called off the main thread!
     */
    public List<MonitoredPerson> getAllPersonsSync() {
        return dao.getAllMonitoredPersonsSync();
    }

    /**
     * Get a specific monitored person by sensor ID.
     * Returns null if not found.
     * WARNING: Must be called off the main thread!
     */
    @Nullable
    public MonitoredPerson getPersonBySensorId(@NonNull String sensorId) {
        return dao.getPersonBySensorId(sensorId);
    }

    /**
     * Get a specific monitored person by sensor ID (LiveData).
     * Safe to call from any thread.
     */
    public LiveData<MonitoredPerson> getPersonBySensorIdLive(@NonNull String sensorId) {
        return dao.getPersonBySensorIdLive(sensorId);
    }

    /**
     * Delete a monitored person by sensor ID.
     * Safe to call from any thread.
     */
    public void deleteBySensorId(@NonNull String sensorId) {
        executor.execute(() -> {
            dao.deleteBySensorId(sensorId);
            Log.d(TAG, "Deleted sensor: " + sensorId);
        });
    }

    /**
     * Get count of all registered monitored persons.
     * WARNING: Must be called off the main thread!
     */
    public int getPersonCount() {
        return dao.countAll();
    }

    /**
     * Get all registered sensor IDs.
     * WARNING: Must be called off the main thread!
     */
    public List<String> getAllSensorIds() {
        return dao.getAllSensorIds();
    }

    /**
     * Callback interface for async operations.
     */
    public interface DisplayNameCallback {
        void onResult(@NonNull String displayName);
    }
}
