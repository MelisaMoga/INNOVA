package com.melisa.innovamotionapp.utils;

import android.content.Context;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.MonitoredPersonDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.sync.FirestoreSyncService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generates and injects mock sensor data for developer testing.
 * 
 * Creates realistic test data that flows through the same Room database
 * as real Bluetooth data, enabling end-to-end testing without hardware.
 */
public class MockDataGenerator {
    
    private static final String TAG = "MockDataGenerator";
    
    // Posture hex codes matching the existing protocol
    public static final String HEX_STANDING = "0xAB3311";
    public static final String HEX_SITTING = "0xAC4312";
    public static final String HEX_WALKING = "0xBA3311";
    public static final String HEX_FALLING = "0xEF0112";
    public static final String HEX_UNUSED = "0x793248";
    
    // All non-fall postures for random selection
    private static final String[] NORMAL_POSTURES = {HEX_STANDING, HEX_SITTING, HEX_WALKING};
    
    // Mock sensor ID prefixes
    private static final String SENSOR_ID_PREFIX = "mock_sensor_";
    
    // Mock device address for all generated data
    private static final String MOCK_DEVICE_ADDRESS = "00:00:00:00:00:00";
    
    // Display names for name resolution testing
    private static final String[] MOCK_NAMES = {
            "Ion Popescu", "Maria Ionescu", "Gheorghe Marin",
            "Elena Dumitrescu", "Alexandru Popa", "Ana Radu",
            "Mihai Stoica", "Ioana Florescu", "Dan Gheorghe", "Sofia Nicolae"
    };
    
    private final Context context;
    private final ReceivedBtDataDao btDataDao;
    private final MonitoredPersonDao personDao;
    private final ExecutorService executor;
    private final Random random;
    
    // Owner ID for mock data (uses current user or mock owner)
    private String ownerUserId = "mock_aggregator";
    
    // Whether to sync generated data to Firestore
    private boolean syncToFirestore = false;
    
    /**
     * Callback for generation completion.
     */
    public interface GenerationCallback {
        void onComplete(int totalReadings, int totalSensors);
        void onError(Exception e);
        
        /**
         * Called when Firestore sync completes (if syncToFirestore is enabled).
         * Default implementation does nothing.
         */
        default void onFirestoreSyncComplete(int syncedReadings) {}
        
        /**
         * Called when Firestore sync fails.
         * Default implementation does nothing.
         */
        default void onFirestoreSyncError(Exception e) {}
    }
    
    /**
     * Create a new MockDataGenerator.
     * 
     * @param context Application context
     */
    public MockDataGenerator(Context context) {
        this.context = context.getApplicationContext();
        InnovaDatabase db = InnovaDatabase.getInstance(this.context);
        this.btDataDao = db.receivedBtDataDao();
        this.personDao = db.monitoredPersonDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.random = new Random();
        
        // Try to get actual user ID
        GlobalData global = GlobalData.getInstance();
        if (global != null && global.currentUserUid != null) {
            this.ownerUserId = global.currentUserUid;
        }
    }
    
    /**
     * Set the owner user ID for generated data.
     * Useful for testing multi-user scenarios.
     */
    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
    
    /**
     * Set whether to sync generated data to Firestore.
     * When enabled, data is uploaded to cloud after local insertion.
     * 
     * @param syncToFirestore true to sync to Firestore, false for local only
     */
    public void setSyncToFirestore(boolean syncToFirestore) {
        this.syncToFirestore = syncToFirestore;
    }
    
    /**
     * Check if Firestore sync is enabled.
     */
    public boolean isSyncToFirestoreEnabled() {
        return syncToFirestore;
    }
    
    /**
     * Run a test scenario asynchronously.
     * 
     * @param scenario The scenario to run
     * @param callback Completion callback (called on background thread)
     */
    public void runScenario(TestScenario scenario, GenerationCallback callback) {
        executor.execute(() -> {
            try {
                Logger.i(TAG, "Running scenario: " + scenario.getId());
                
                int sensorCount = scenario.getSensorCount();
                int readingsPerSensor = scenario.getReadingsPerSensor();
                boolean includeFall = scenario.shouldIncludeFall();
                boolean includeStale = scenario.shouldIncludeStale();
                
                // Generate sensor IDs
                List<String> sensorIds = generateSensorIds(sensorCount);
                
                // Create person entries for ALL scenarios to ensure Live Posture tab works
                createPersonEntries(sensorIds);
                
                // Generate readings
                List<ReceivedBtDataEntity> allReadings = new ArrayList<>();
                long baseTime = System.currentTimeMillis();
                
                for (int sensorIndex = 0; sensorIndex < sensorIds.size(); sensorIndex++) {
                    String sensorId = sensorIds.get(sensorIndex);
                    
                    // Determine time offset for stale data
                    long timeOffset = 0;
                    if (includeStale && sensorIndex % 2 == 1) {
                        // Make odd-indexed sensors stale (10+ minutes old)
                        timeOffset = -(10 * 60 * 1000 + random.nextInt(50 * 60 * 1000));
                    }
                    
                    for (int readingIndex = 0; readingIndex < readingsPerSensor; readingIndex++) {
                        // Calculate timestamp with realistic spacing
                        long timestamp = baseTime + timeOffset - (readingIndex * 3000L);
                        
                        // Select posture
                        String hexCode;
                        if (includeFall && sensorIndex == 0 && readingIndex == 0) {
                            // First reading of first sensor is a fall
                            hexCode = HEX_FALLING;
                        } else {
                            hexCode = NORMAL_POSTURES[random.nextInt(NORMAL_POSTURES.length)];
                        }
                        
                        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                                MOCK_DEVICE_ADDRESS,
                                timestamp,
                                hexCode,
                                ownerUserId,
                                sensorId
                        );
                        allReadings.add(entity);
                    }
                }
                
                // Insert all readings into database
                btDataDao.insertAll(allReadings);
                
                // #region agent log
                // H5: Verify data was inserted into Room
                int dbCount = btDataDao.dbgCountAll();
                android.util.Log.w("DBG_H5", "After insertAll: insertedCount=" + allReadings.size() + ", dbCountAfter=" + dbCount + ", ownerUserId=" + ownerUserId + ", sensorIds=" + sensorIds);
                // #endregion
                
                int totalReadings = allReadings.size();
                Logger.i(TAG, "Scenario complete: " + totalReadings + " readings for " + sensorCount + " sensors");
                
                if (callback != null) {
                    callback.onComplete(totalReadings, sensorCount);
                }
                
                // Sync to Firestore if enabled
                if (syncToFirestore && !allReadings.isEmpty()) {
                    syncToFirestore(allReadings, callback);
                }
                
            } catch (Exception e) {
                Logger.e(TAG, "Error running scenario: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Generate a list of mock sensor IDs.
     */
    private List<String> generateSensorIds(int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(SENSOR_ID_PREFIX + String.format("%03d", i + 1));
        }
        return ids;
    }
    
    /**
     * Create MonitoredPerson entries for name resolution testing.
     * Uses PersonNameManager which also syncs to Firestore.
     */
    private void createPersonEntries(List<String> sensorIds) {
        PersonNameManager nameManager = PersonNameManager.getInstance(context);
        for (int i = 0; i < sensorIds.size(); i++) {
            String sensorId = sensorIds.get(i);
            String displayName = MOCK_NAMES[i % MOCK_NAMES.length];
            
            // This also syncs to Firestore for backup/restore on account switch
            nameManager.setDisplayName(sensorId, displayName);
        }
        Logger.d(TAG, "Created " + sensorIds.size() + " person entries (synced to Firestore)");
    }
    
    /**
     * Inject a single mock reading (for continuous streaming simulation).
     * 
     * @param sensorId Sensor ID for the reading
     * @param hexCode Posture hex code
     */
    public void injectReading(String sensorId, String hexCode) {
        executor.execute(() -> {
            try {
                ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                        MOCK_DEVICE_ADDRESS,
                        System.currentTimeMillis(),
                        hexCode,
                        ownerUserId,
                        sensorId
                );
                btDataDao.insert(entity);
                Logger.v(TAG, "Injected reading: " + sensorId + " -> " + hexCode);
            } catch (Exception e) {
                Logger.e(TAG, "Error injecting reading: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Inject a fall posture for testing alerts.
     * 
     * @param sensorId Sensor ID for the fall
     */
    public void injectFall(String sensorId) {
        injectReading(sensorId, HEX_FALLING);
    }
    
    /**
     * Clear all data from the database.
     * 
     * @param callback Completion callback
     */
    public void clearAllData(Runnable callback) {
        executor.execute(() -> {
            try {
                btDataDao.clearAllData();
                personDao.clearAll();
                Logger.i(TAG, "All data cleared");
                if (callback != null) {
                    callback.run();
                }
            } catch (Exception e) {
                Logger.e(TAG, "Error clearing data: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Get database statistics.
     * 
     * @return Array with [messageCount, sensorCount]
     */
    public int[] getStatistics() {
        try {
            int messageCount = btDataDao.dbgCountAll();
            // Use countBySensor to get distinct sensor count
            List<ReceivedBtDataDao.SensorCount> sensorCounts = btDataDao.countBySensor();
            int sensorCount = sensorCounts != null ? sensorCounts.size() : 0;
            return new int[]{messageCount, sensorCount};
        } catch (Exception e) {
            Logger.e(TAG, "Error getting statistics: " + e.getMessage(), e);
            return new int[]{0, 0};
        }
    }
    
    /**
     * Generate a random posture hex code (excluding fall).
     */
    public String getRandomNormalPosture() {
        return NORMAL_POSTURES[random.nextInt(NORMAL_POSTURES.length)];
    }
    
    /**
     * Generate a random posture hex code (including fall with 10% probability).
     */
    public String getRandomPosture() {
        if (random.nextFloat() < 0.1f) {
            return HEX_FALLING;
        }
        return getRandomNormalPosture();
    }
    
    /**
     * Sync generated readings to Firestore.
     */
    private void syncToFirestore(List<ReceivedBtDataEntity> readings, GenerationCallback callback) {
        Logger.i(TAG, "Syncing " + readings.size() + " readings to Firestore");
        
        try {
            FirestoreSyncService syncService = FirestoreSyncService.getInstance(context);
            syncService.syncPacketBatch(readings, new FirestoreSyncService.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    Logger.i(TAG, "Firestore sync complete: " + message);
                    if (callback != null) {
                        callback.onFirestoreSyncComplete(readings.size());
                    }
                }
                
                @Override
                public void onError(String error) {
                    Logger.e(TAG, "Firestore sync error: " + error);
                    if (callback != null) {
                        callback.onFirestoreSyncError(new Exception(error));
                    }
                }
                
                @Override
                public void onProgress(int current, int total) {
                    // Progress updates not needed for mock data
                    Logger.v(TAG, "Firestore sync progress: " + current + "/" + total);
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Error initiating Firestore sync: " + e.getMessage(), e);
            if (callback != null) {
                callback.onFirestoreSyncError(e);
            }
        }
    }
}
