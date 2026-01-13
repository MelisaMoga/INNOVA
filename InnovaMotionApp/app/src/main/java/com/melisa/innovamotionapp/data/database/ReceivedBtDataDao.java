package com.melisa.innovamotionapp.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO for local persistence of received Bluetooth messages.
 * Inserts are idempotent thanks to IGNORE + unique composite index.
 */
@Dao
public interface ReceivedBtDataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ReceivedBtDataEntity entity);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<ReceivedBtDataEntity> entities);

    // Fetch all data for a specific device address, ordered by timestamp
    @Query("SELECT * FROM received_bt_data WHERE device_address = :deviceAddress ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getDataForDevice(String deviceAddress);

    // Fetch data for a specific device within a timestamp range
    @Query("SELECT * FROM received_bt_data WHERE device_address = :deviceAddress AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getDataForDeviceInRange(String deviceAddress, long startTime, long endTime);

    // Fetch all data from all devices (if needed)
    @Query("SELECT * FROM received_bt_data ORDER BY timestamp DESC")
    LiveData<List<ReceivedBtDataEntity>> getAllData();

    // Synchronous version for background sync operations
    @Query("SELECT * FROM received_bt_data ORDER BY timestamp ASC")
    List<ReceivedBtDataEntity> getAllDataSync();

    // Check if a specific message exists (for conflict resolution)
    @Query("SELECT COUNT(*) FROM received_bt_data WHERE device_address = :deviceAddress AND timestamp = :timestamp AND received_msg = :receivedMsg")
    int messageExists(String deviceAddress, long timestamp, String receivedMsg);

    // Owner-aware existence check matching the unique index
    @Query("SELECT COUNT(*) FROM received_bt_data WHERE owner_user_id = :owner AND device_address = :deviceAddress AND timestamp = :timestamp AND received_msg = :receivedMsg")
    int messageExistsOwned(String owner, String deviceAddress, long timestamp, String receivedMsg);

    // Get the maximum timestamp from local database for incremental backfill
    // Returns 0 if database is empty (for initial backfill)
    @Query("SELECT COALESCE(MAX(timestamp), 0) FROM received_bt_data")
    long getMaxTimestampSync();

    // Get the latest message from any device (for supervisor feed)
    // Observes the most recent message regardless of device address
    @Query("SELECT * FROM received_bt_data ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReceivedBtDataEntity> getLatestMessage();

    // Get the latest message for a specific device (extended feature for supervisors)
    // Useful when supervisors want to monitor a specific supervised user
    @Query("SELECT * FROM received_bt_data WHERE device_address = :deviceAddress ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReceivedBtDataEntity> getLatestForDevice(String deviceAddress);

    // Supervisor-specific queries with owner filtering
    
    // Delete data that doesn't belong to current supervised users (for data cleanup)
    @Query("DELETE FROM received_bt_data WHERE owner_user_id NOT IN (:uids)")
    int deleteWhereOwnerNotIn(List<String> uids);
    
    // Get max timestamp for a specific owner (for backfill)
    @Query("SELECT COALESCE(MAX(timestamp), 0) FROM received_bt_data WHERE owner_user_id = :uid")
    long getMaxTimestampForOwner(String uid);
    
    // Get latest message for a specific owner (supervisor feed)
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUserId ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReceivedBtDataEntity> getLatestForOwner(String ownerUserId);
    
    // Get latest message from any of the supervised users (supervisor feed)
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id IN (:ownerUserIds) ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReceivedBtDataEntity> getLatestForOwners(List<String> ownerUserIds);
    
    // Get all data for a specific owner (for supervisor monitoring)
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUserId ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getDataForOwner(String ownerUserId);
    
    // Get all data for multiple owners (for supervisor monitoring)
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id IN (:ownerUserIds) ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getDataForOwners(List<String> ownerUserIds);
    
    // Clear all data (for sign-out cleanup)
    @Query("DELETE FROM received_bt_data")
    int clearAllData();

    // One-time repair: set owner for legacy NULL rows
    @Query("UPDATE received_bt_data SET owner_user_id = :owner WHERE owner_user_id IS NULL")
    int migrateSetOwnerForNull(String owner);
    
    // Debug helpers for sync verification
    @Query("SELECT COUNT(*) FROM received_bt_data")
    int countAll();
    
    @Query("SELECT COUNT(*) FROM received_bt_data WHERE owner_user_id = :ownerUserId")
    int countForOwner(String ownerUserId);
    
    @Query("SELECT owner_user_id, COUNT(*) as c FROM received_bt_data GROUP BY owner_user_id")
    List<OwnerCount> countByOwner();
    
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUserId ORDER BY timestamp DESC LIMIT :limit")
    List<ReceivedBtDataEntity> latestForOwner(String ownerUserId, int limit);
    
    // Helper class for countByOwner query
    class OwnerCount {
        @androidx.room.ColumnInfo(name = "owner_user_id") 
        public String owner;
        @androidx.room.ColumnInfo(name = "c") 
        public int count;
    }
    
    // -------- Owner-filtered readers used by Activities --------
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUid ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getAllForOwnerLive(String ownerUid);
    
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUid AND timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getRangeForOwnerLive(String ownerUid, long start, long end);
    
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUid ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReceivedBtDataEntity> getLatestForOwnerLive(String ownerUid);
    
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUid AND device_address = :device ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getAllForOwnerAndDeviceLive(String ownerUid, String device);

    // -------- User-centric aliases (unified naming: user == owner) --------
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :userId ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getAllForUserLive(String userId);

    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :userId AND timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getRangeForUserLive(String userId, long start, long end);

    // ======== SENSOR-SPECIFIC QUERIES (Multi-User Protocol) ========
    
    /**
     * Get the latest reading for a specific sensor/person.
     * Used to show current posture of a monitored person.
     */
    @Query("SELECT * FROM received_bt_data WHERE sensor_id = :sensorId ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReceivedBtDataEntity> getLatestForSensor(String sensorId);

    /**
     * Get all readings for a specific sensor/person, ordered by timestamp.
     * Used for history/timeline view of a single person.
     */
    @Query("SELECT * FROM received_bt_data WHERE sensor_id = :sensorId ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getAllForSensor(String sensorId);

    /**
     * Get readings for a specific sensor within a timestamp range.
     * Used for date-filtered statistics/energy view of a single person.
     */
    @Query("SELECT * FROM received_bt_data WHERE sensor_id = :sensorId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getRangeForSensor(String sensorId, long startTime, long endTime);

    /**
     * Get all distinct sensor IDs in the database.
     * Used to populate dropdown lists of monitored persons.
     */
    @Query("SELECT DISTINCT sensor_id FROM received_bt_data")
    LiveData<List<String>> getDistinctSensorIds();

    /**
     * Get the latest reading for each sensor (one row per person).
     * Used for dashboard view showing all monitored persons with their current posture.
     * 
     * QUERY OPTIMIZATION NOTE:
     * This query is efficient because it:
     * 1. Uses a subquery to find max timestamp per sensor (single pass)
     * 2. Joins back to get full row data
     * 3. Avoids N+1 queries (one query for all sensors, not one per sensor)
     * 
     * Performance: O(n) where n = total rows, regardless of sensor count.
     * Much better than N separate queries where N = sensor count.
     */
    @Query("SELECT r.* FROM received_bt_data r " +
           "INNER JOIN (SELECT sensor_id, MAX(timestamp) as max_ts FROM received_bt_data GROUP BY sensor_id) latest " +
           "ON r.sensor_id = latest.sensor_id AND r.timestamp = latest.max_ts")
    LiveData<List<ReceivedBtDataEntity>> getLatestForEachSensor();

    /**
     * Get the latest reading for a specific owner and sensor combination.
     * Used when supervisor wants to see a specific person from a specific aggregator.
     */
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUid AND sensor_id = :sensorId ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReceivedBtDataEntity> getLatestForOwnerAndSensor(String ownerUid, String sensorId);

    /**
     * Get all distinct sensor IDs for a specific owner/aggregator.
     * Used to list all monitored persons belonging to a specific aggregator.
     */
    @Query("SELECT DISTINCT sensor_id FROM received_bt_data WHERE owner_user_id = :ownerUid")
    LiveData<List<String>> getDistinctSensorIdsForOwner(String ownerUid);

    /**
     * Get the latest reading for each sensor belonging to a specific owner.
     * Dashboard view for supervisor showing all persons from one aggregator.
     */
    @Query("SELECT r.* FROM received_bt_data r " +
           "INNER JOIN (SELECT sensor_id, MAX(timestamp) as max_ts FROM received_bt_data WHERE owner_user_id = :ownerUid GROUP BY sensor_id) latest " +
           "ON r.sensor_id = latest.sensor_id AND r.timestamp = latest.max_ts " +
           "WHERE r.owner_user_id = :ownerUid")
    LiveData<List<ReceivedBtDataEntity>> getLatestForEachSensorByOwner(String ownerUid);

    /**
     * Get all readings for a specific owner and sensor, ordered by timestamp.
     * Used for detailed history of one person from one aggregator.
     */
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUid AND sensor_id = :sensorId ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getAllForOwnerAndSensor(String ownerUid, String sensorId);

    /**
     * Count readings per sensor (for debugging/stats).
     */
    @Query("SELECT sensor_id, COUNT(*) as c FROM received_bt_data GROUP BY sensor_id")
    List<SensorCount> countBySensor();

    /**
     * Helper class for countBySensor query.
     */
    class SensorCount {
        @androidx.room.ColumnInfo(name = "sensor_id")
        public String sensorId;
        @androidx.room.ColumnInfo(name = "c")
        public int count;
    }

    // ======== MESSAGE LOG UI QUERIES ========
    
    /**
     * Get recent messages across all sensors, ordered by timestamp descending.
     * Used for the message log UI.
     */
    @Query("SELECT * FROM received_bt_data ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<ReceivedBtDataEntity>> getRecentMessages(int limit);

    /**
     * Get recent messages for a specific sensor, ordered by timestamp descending.
     * Used for filtered message log UI.
     */
    @Query("SELECT * FROM received_bt_data WHERE sensor_id = :sensorId ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<ReceivedBtDataEntity>> getMessagesForSensor(String sensorId, int limit);

    // TEMPORARY DEBUG HELPERS (remove later)
    @Query("SELECT COUNT(*) FROM received_bt_data")
    int dbgCountAll();
    
    @Query("SELECT COUNT(*) FROM received_bt_data WHERE owner_user_id = :ownerUserId")
    int dbgCountForOwner(String ownerUserId);
    
    @Query("SELECT owner_user_id AS owner, COUNT(*) AS c FROM received_bt_data GROUP BY owner_user_id")
    List<DbgOwnerCount> dbgCountByOwner();
    
    @Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUserId ORDER BY timestamp DESC LIMIT :limit")
    List<ReceivedBtDataEntity> dbgLatestForOwner(String ownerUserId, int limit);
    
    // Helper class for dbgCountByOwner query
    class DbgOwnerCount {
        @androidx.room.ColumnInfo(name = "owner") 
        public String owner;
        @androidx.room.ColumnInfo(name = "c") 
        public int count;
    }
}