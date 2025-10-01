package com.melisa.innovamotionapp.data.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing a received Bluetooth message.
 * The unique composite index ensures idempotent inserts across users.
 */
@Entity(
    tableName = "received_bt_data",
    indices = {
        // Prevent duplicates across users with same payload
        @Index(value = {"owner_user_id", "device_address", "timestamp", "received_msg"}, unique = true),
        @Index(value = {"owner_user_id", "timestamp"})
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

    // NOTE: Keep nullable at schema level if you already shipped; code will ensure it's set on supervised path.
    @Nullable
    @ColumnInfo(name = "owner_user_id")
    private String ownerUserId;

    // Owner-aware constructor (preferred when supervised)
    public ReceivedBtDataEntity(@NonNull String deviceAddress, long timestamp, @NonNull String receivedMsg, @NonNull String ownerUserId) {
        this.deviceAddress = deviceAddress;
        this.timestamp = timestamp;
        this.receivedMsg = receivedMsg;
        this.ownerUserId = ownerUserId;
    }

    // Legacy constructor (avoid for supervised path; kept for compatibility)
    @Ignore
    public ReceivedBtDataEntity(@NonNull String deviceAddress, long timestamp, @NonNull String receivedMsg) {
        this.deviceAddress = deviceAddress;
        this.timestamp = timestamp;
        this.receivedMsg = receivedMsg;
    }

    // Getters (and setters if you need them)
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

    @Nullable 
    public String getOwnerUserId() {
        return ownerUserId;
    }
}


