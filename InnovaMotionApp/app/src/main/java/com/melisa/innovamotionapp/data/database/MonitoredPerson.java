package com.melisa.innovamotionapp.data.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing a monitored person.
 * 
 * Maps sensor IDs (like "sensor001" or UUIDs) to human-readable display names
 * (like "Ion Popescu") for better user experience.
 * 
 * The sensor_id is unique - each sensor can only have one display name.
 */
@Entity(
    tableName = "monitored_persons",
    indices = {
        @Index(value = {"sensor_id"}, unique = true)
    }
)
public class MonitoredPerson {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    /**
     * The unique sensor/person ID from the hardware (e.g., "sensor001", UUID).
     */
    @NonNull
    @ColumnInfo(name = "sensor_id")
    private String sensorId;

    /**
     * The human-readable display name for this person (e.g., "Ion Popescu").
     * Defaults to the sensorId if not set.
     */
    @NonNull
    @ColumnInfo(name = "display_name")
    private String displayName;

    /**
     * When this person was first registered (epoch millis).
     */
    @ColumnInfo(name = "created_at")
    private long createdAt;

    /**
     * When this person's name was last updated (epoch millis).
     */
    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    /**
     * Primary constructor for Room.
     *
     * @param sensorId    The unique sensor ID from hardware
     * @param displayName The human-readable display name
     * @param createdAt   When first registered
     * @param updatedAt   When last updated
     */
    public MonitoredPerson(@NonNull String sensorId, @NonNull String displayName, long createdAt, long updatedAt) {
        this.sensorId = sensorId;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ========== Getters ==========

    public long getId() {
        return id;
    }

    @NonNull
    public String getSensorId() {
        return sensorId;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    // ========== Setters ==========

    public void setId(long id) {
        this.id = id;
    }

    public void setSensorId(@NonNull String sensorId) {
        this.sensorId = sensorId;
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ========== Factory Methods ==========

    /**
     * Create a new MonitoredPerson with sensorId as the default display name.
     * Timestamps are set to current time.
     */
    public static MonitoredPerson createNew(@NonNull String sensorId) {
        long now = System.currentTimeMillis();
        return new MonitoredPerson(sensorId, sensorId, now, now);
    }

    /**
     * Create a new MonitoredPerson with a custom display name.
     * Timestamps are set to current time.
     */
    public static MonitoredPerson createNew(@NonNull String sensorId, @NonNull String displayName) {
        long now = System.currentTimeMillis();
        return new MonitoredPerson(sensorId, displayName, now, now);
    }
}
