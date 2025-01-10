package com.melisa.innovamotionapp.utils;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.util.ArraySet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melisa.innovamotionapp.bluetooth.DeviceCommunicationManager;
import com.melisa.innovamotionapp.data.posture.Posture;

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

    private final MutableLiveData<Posture> receivedPosture = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnectedDevice = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsConnectedDevice() {
        return isConnectedDevice;
    }
    public LiveData<Posture> getReceivedPosture() {
        return receivedPosture;
    }

    public void setReceivedPosture(Posture receivedPosture) {
        this.receivedPosture.postValue(receivedPosture);
    }
    public void setIsConnectedDevice(boolean connectionEstablished) {
        isConnectedDevice.postValue(connectionEstablished);
    }
    public String childName = "Popescu Mihaita";

}
