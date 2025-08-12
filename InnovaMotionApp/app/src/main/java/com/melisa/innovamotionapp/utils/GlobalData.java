package com.melisa.innovamotionapp.utils;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melisa.innovamotionapp.bluetooth.DeviceCommunicationManager;
import com.melisa.innovamotionapp.data.posture.Posture;

import java.util.LinkedHashSet;

public class GlobalData extends Application {
    private static GlobalData instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        userDeviceSettingsStorage = new UserDeviceSettingsStorage(this);
    }
    public static GlobalData getInstance() {
        return instance;
    }

    public final DeviceCommunicationManager deviceCommunicationManager = new DeviceCommunicationManager(this);

    public LinkedHashSet<BluetoothDevice> nearbyBtDevices = new LinkedHashSet<>();

    public UserDeviceSettingsStorage userDeviceSettingsStorage;
    private final MutableLiveData<Posture> receivedPosture = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnectedDevice = new MutableLiveData<>();
    public String currentUserRole = null; // "admin" or "simple_user"
    public String currentUserUid = null;

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
    public String userName = "Popescu Mihaita";
}
