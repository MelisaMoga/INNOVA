package com.melisa.innovamotionapp.utils;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.util.ArraySet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melisa.innovamotionapp.bluetooth.DeviceCommunicationManager;

public class GlobalData extends Application {
    private static GlobalData instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    public static GlobalData getInstance() {
        return instance;
    }

    public final DeviceCommunicationManager deviceCommunicationManager = new DeviceCommunicationManager(this);

    public ArraySet<BluetoothDevice> nearbyBtDevices = new ArraySet<>();

    private final MutableLiveData<String> receivedData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnectedDevice = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsConnectedDevice() {
        return isConnectedDevice;
    }
    public LiveData<String> getReceivedData() {
        return receivedData;
    }

    public void setReceivedData(String data) {
        receivedData.postValue(data);
    }
    public void setIsConnectedDevice(boolean connectionEstablished) {
        isConnectedDevice.postValue(connectionEstablished);
    }
    public String childName = "Popescu Mihaita";

}
