package com.melisa.innovamotionapp.utils;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melisa.innovamotionapp.bluetooth.DeviceCommunicationManager;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.sync.FirestoreSyncService;
import com.melisa.innovamotionapp.sync.SessionGate;
import com.melisa.innovamotionapp.sync.UserSession;

import java.util.LinkedHashSet;

public class GlobalData extends Application {
    private static GlobalData instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        userDeviceSettingsStorage = new UserDeviceSettingsStorage(this);
        
        // Seed a safe default so observers never see null
        receivedPosture.setValue(new UnknownPosture());

        // Single source of truth: init all channels here (both BT + Fall)
        NotificationConfig.initAllChannels(this);
    }
    
    public static GlobalData getInstance() {
        return instance;
    }

    public final DeviceCommunicationManager deviceCommunicationManager = new DeviceCommunicationManager(this);

    public LinkedHashSet<BluetoothDevice> nearbyBtDevices = new LinkedHashSet<>();

    public UserDeviceSettingsStorage userDeviceSettingsStorage;
    private final MutableLiveData<Posture> receivedPosture = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnectedDevice = new MutableLiveData<>();
    public String currentUserRole = null; // "aggregator" or "supervisor"
    public String currentUserUid = null;
    public java.util.List<String> supervisedSensorIds = new java.util.ArrayList<>();
    
    // LiveData for reactive session data (ViewModels observe these to react to async updates)
    private final MutableLiveData<java.util.List<String>> supervisedSensorIdsLiveData = new MutableLiveData<>(new java.util.ArrayList<>());
    private final MutableLiveData<String> currentUserUidLiveData = new MutableLiveData<>(null);

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
    
    /**
     * Set current user UID (also updates LiveData for reactive observers)
     */
    public void setCurrentUserUid(String uid) {
        this.currentUserUid = uid;
        this.currentUserUidLiveData.postValue(uid);
    }
    
    /**
     * Get current user UID as LiveData (reactive - updates when UID changes)
     */
    public LiveData<String> getCurrentUserUidLive() {
        return currentUserUidLiveData;
    }
    
    /**
     * Set current user role
     */
    public void setCurrentUserRole(String role) {
        this.currentUserRole = role;
    }
    
    /**
     * Set supervised sensor IDs (also updates LiveData for reactive observers)
     */
    public void setSupervisedSensorIds(java.util.List<String> sensorIds) {
        this.supervisedSensorIds = sensorIds != null ? new java.util.ArrayList<>(sensorIds) : new java.util.ArrayList<>();
        this.supervisedSensorIdsLiveData.postValue(this.supervisedSensorIds);
    }
    
    /**
     * Get supervised sensor IDs as LiveData (reactive - updates when list changes)
     */
    public LiveData<java.util.List<String>> getSupervisedSensorIdsLive() {
        return supervisedSensorIdsLiveData;
    }
    
    /**
     * Reset session data (called on sign-out)
     */
    public void resetSessionData() {
        android.util.Log.d("GlobalData", "Resetting session data");
        currentUserUid = null;
        currentUserRole = null;
        supervisedSensorIds.clear();
        
        // Reset LiveData to neutral state
        receivedPosture.postValue(null);
        isConnectedDevice.postValue(false);
        
        // Reset session LiveData (triggers ViewModel updates)
        supervisedSensorIdsLiveData.postValue(new java.util.ArrayList<>());
        currentUserUidLiveData.postValue(null);
    }
    
    public String userName = "Popescu Mihaita";
}
