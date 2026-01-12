package com.melisa.innovamotionapp.data.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing a received Bluetooth message from the multi-user protocol.
 * 
 * Each reading includes a sensorId identifying the monitored person (e.g., "sensor001", UUID, etc.)
 * per the protocol format: "sensorId;hexCode\n"
 * 
 * The unique composite index ensures idempotent inserts across users and sensors.
 */
@Entity(
    tableName = "received_bt_data",
    indices = {
        // Prevent duplicates: same owner, device, sensor, timestamp, and message
        @Index(value = {"owner_user_id", "device_address", "sensor_id", "timestamp", "received_msg"}, unique = true),
        // Query optimization indexes
        @Index(value = {"owner_user_id", "timestamp"}),
        @Index(value = {"sensor_id"}),
        @Index(value = {"owner_user_id", "sensor_id"})
    }
)
public class ReceivedBtDataEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "device_address")
    private String deviceAddress;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @NonNull
    @ColumnInfo(name = "received_msg")
    private String receivedMsg;

    @NonNull
    @ColumnInfo(name = "owner_user_id")
    private String ownerUserId;

    /**
     * The sensor/person ID from the hardware protocol (e.g., "sensor001", UUID).
     * Mandatory field - every reading must identify the monitored person.
     */
    @NonNull
    @ColumnInfo(name = "sensor_id")
    private String sensorId;

    /**
     * Primary constructor for multi-user protocol data.
     *
     * @param deviceAddress Bluetooth MAC address of the hardware device
     * @param timestamp     When the reading was received (epoch millis)
     * @param receivedMsg   The hex code payload (e.g., "0xAB3311")
     * @param ownerUserId   The aggregator user ID who owns this data
     * @param sensorId      The monitored person's ID from hardware (e.g., "sensor001")
     */
    public ReceivedBtDataEntity(
            @NonNull String deviceAddress,
            long timestamp,
            @NonNull String receivedMsg,
            @NonNull String ownerUserId,
            @NonNull String sensorId) {
        this.deviceAddress = deviceAddress;
        this.timestamp = timestamp;
        this.receivedMsg = receivedMsg;
        this.ownerUserId = ownerUserId;
        this.sensorId = sensorId;
    }

    // ========== Getters ==========

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getDeviceAddress() {
        return deviceAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public String getReceivedMsg() {
        return receivedMsg;
    }

    @NonNull
    public String getOwnerUserId() {
        return ownerUserId;
    }

    @NonNull
    public String getSensorId() {
        return sensorId;
    }
}


