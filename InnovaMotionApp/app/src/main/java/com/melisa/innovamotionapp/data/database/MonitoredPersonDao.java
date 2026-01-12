package com.melisa.innovamotionapp.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO for managing monitored persons (sensor ID to display name mapping).
 */
@Dao
public interface MonitoredPersonDao {

    /**
     * Insert a new monitored person. Replaces on conflict (sensor_id unique).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MonitoredPerson person);

    /**
     * Insert multiple monitored persons.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MonitoredPerson> persons);

    /**
     * Update an existing monitored person.
     */
    @Update
    void update(MonitoredPerson person);

    /**
     * Delete a monitored person.
     */
    @Delete
    void delete(MonitoredPerson person);

    /**
     * Get display name for a sensor ID.
     * Returns null if not found.
     */
    @Query("SELECT display_name FROM monitored_persons WHERE sensor_id = :sensorId LIMIT 1")
    String getDisplayNameForSensor(String sensorId);

    /**
     * Get all monitored persons ordered by display name (LiveData for UI).
     */
    @Query("SELECT * FROM monitored_persons ORDER BY display_name ASC")
    LiveData<List<MonitoredPerson>> getAllMonitoredPersons();

    /**
     * Get all monitored persons synchronously (for background operations).
     */
    @Query("SELECT * FROM monitored_persons ORDER BY display_name ASC")
    List<MonitoredPerson> getAllMonitoredPersonsSync();

    /**
     * Upsert a monitored person by sensor ID.
     * If exists, updates display_name and updated_at.
     * If not exists, creates new entry with created_at = updated_at = now.
     */
    @Query("INSERT OR REPLACE INTO monitored_persons (sensor_id, display_name, created_at, updated_at) " +
           "VALUES (:sensorId, :displayName, " +
           "COALESCE((SELECT created_at FROM monitored_persons WHERE sensor_id = :sensorId), :now), " +
           ":now)")
    void upsertByName(String sensorId, String displayName, long now);

    /**
     * Check if a sensor ID exists in the database.
     * Returns count (0 if not exists, 1 if exists).
     */
    @Query("SELECT COUNT(*) FROM monitored_persons WHERE sensor_id = :sensorId")
    int sensorExists(String sensorId);

    /**
     * Get the full MonitoredPerson entity by sensor ID.
     * Returns null if not found.
     */
    @Query("SELECT * FROM monitored_persons WHERE sensor_id = :sensorId LIMIT 1")
    MonitoredPerson getPersonBySensorId(String sensorId);

    /**
     * Get the full MonitoredPerson entity by sensor ID (LiveData for UI).
     */
    @Query("SELECT * FROM monitored_persons WHERE sensor_id = :sensorId LIMIT 1")
    LiveData<MonitoredPerson> getPersonBySensorIdLive(String sensorId);

    /**
     * Delete a monitored person by sensor ID.
     */
    @Query("DELETE FROM monitored_persons WHERE sensor_id = :sensorId")
    int deleteBySensorId(String sensorId);

    /**
     * Get count of all monitored persons.
     */
    @Query("SELECT COUNT(*) FROM monitored_persons")
    int countAll();

    /**
     * Clear all monitored persons (for testing/reset).
     */
    @Query("DELETE FROM monitored_persons")
    int clearAll();

    /**
     * Get all distinct sensor IDs.
     */
    @Query("SELECT sensor_id FROM monitored_persons ORDER BY display_name ASC")
    List<String> getAllSensorIds();

    /**
     * Update only the display name for a sensor ID.
     */
    @Query("UPDATE monitored_persons SET display_name = :displayName, updated_at = :now WHERE sensor_id = :sensorId")
    int updateDisplayName(String sensorId, String displayName, long now);
}
