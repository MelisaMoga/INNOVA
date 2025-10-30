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
    
    // Packet statistics for aggregator UI
    private final MutableLiveData<Integer> packetCount = new MutableLiveData<>(0);
    private final MutableLiveData<Long> lastPacketTimestamp = new MutableLiveData<>(0L);
    private final MutableLiveData<String> lastRawMessage = new MutableLiveData<>("");
    
    public String currentUserRole = null; // "supervised" or "supervisor" (or "aggregator")
    public String currentUserUid = null;
    public java.util.List<String> supervisedUserIds = new java.util.ArrayList<>();

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
    
    // Packet statistics getters and setters
    public LiveData<Integer> getPacketCount() {
        return packetCount;
    }
    
    public LiveData<Long> getLastPacketTimestamp() {
        return lastPacketTimestamp;
    }
    
    public LiveData<String> getLastRawMessage() {
        return lastRawMessage;
    }
    
    /**
     * Set current user UID
     */
    public void setCurrentUserUid(String uid) {
        this.currentUserUid = uid;
    }
    
    /**
     * Set current user role
     */
    public void setCurrentUserRole(String role) {
        this.currentUserRole = role;
    }
    
    /**
     * Set supervised user IDs
     */
    public void setSupervisedUserIds(java.util.List<String> supervisedUserIds) {
        this.supervisedUserIds = supervisedUserIds != null ? new java.util.ArrayList<>(supervisedUserIds) : new java.util.ArrayList<>();
    }
    
    /**
     * Reset session data (called on sign-out)
     */
    public void resetSessionData() {
        android.util.Log.d("GlobalData", "Resetting session data");
        currentUserUid = null;
        currentUserRole = null;
        supervisedUserIds.clear();
        
        // Reset LiveData to neutral state
        receivedPosture.postValue(null);
        isConnectedDevice.postValue(false);
        
        // Reset packet statistics
        packetCount.postValue(0);
        lastPacketTimestamp.postValue(0L);
        lastRawMessage.postValue("");
    }
    
    public String userName = "Popescu Mihaita";
}
