package com.melisa.innovamotionapp.data.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "received_bt_data")
public class ReceivedBtDataEntity {
    @PrimaryKey(autoGenerate = true)
    private int id; // Primary key, auto-generated

    @ColumnInfo(name = "device_address")
    private String deviceAddress; // Bluetooth device address

    @ColumnInfo(name = "timestamp")
    private long timestamp; // Timestamp for sorting and querying


    @ColumnInfo(name = "received_msg")
    private String receivedMsg; // The actual received message from the device

    public ReceivedBtDataEntity(String deviceAddress, long timestamp, String receivedMsg) {
        this.deviceAddress = deviceAddress;
        this.timestamp = timestamp;
        this.receivedMsg = receivedMsg;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReceivedMsg() {
        return receivedMsg;
    }

    public void setReceivedMsg(String receivedMsg) {
        this.receivedMsg = receivedMsg;
    }
}


