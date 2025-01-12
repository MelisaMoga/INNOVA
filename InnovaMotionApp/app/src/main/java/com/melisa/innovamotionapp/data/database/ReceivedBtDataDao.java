package com.melisa.innovamotionapp.data.database;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReceivedBtDataDao {

    // Insert new data
    @Insert
    void insert(ReceivedBtDataEntity myData);

    @Insert
    void insertAll(List<ReceivedBtDataEntity> receivedBtDataEntities);

    // Fetch all data for a specific device address, ordered by timestamp
    @Query("SELECT * FROM received_bt_data WHERE device_address = :deviceAddress ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getDataForDevice(String deviceAddress);

    // Fetch data for a specific device within a timestamp range
    @Query("SELECT * FROM received_bt_data WHERE device_address = :deviceAddress AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    LiveData<List<ReceivedBtDataEntity>> getDataForDeviceInRange(String deviceAddress, long startTime, long endTime);

    // Fetch all data from all devices (if needed)
    @Query("SELECT * FROM received_bt_data ORDER BY timestamp DESC")
    LiveData<List<ReceivedBtDataEntity>> getAllData();
}