package com.melisa.innovamotionapp.ui.viewmodels;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.melisa.innovamotionapp.bluetooth.DeviceCommunicationManager;
import com.melisa.innovamotionapp.receivers.BluetoothStateReceiver;
import com.melisa.innovamotionapp.receivers.DiscoveryFinishedReceiver;
import com.melisa.innovamotionapp.receivers.NearbyDeviceDiscoveryReceiver;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.ArrayList;

public class BtSettingsViewModel extends ViewModel {

    private final GlobalData globalData = GlobalData.getInstance();
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ;
    private final MutableLiveData<BtSettingsState> uiState = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<BluetoothDevice>> nearbyDevices = new MutableLiveData<>();
    private final MutableLiveData<Boolean> needRequestBluetoothEnable = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isBluetoothEnabled = new MutableLiveData<>();


    private final BluetoothStateReceiver bluetoothStateReceiver = new BluetoothStateReceiver(this::onBtStateChanged);
    private final DiscoveryFinishedReceiver discoveryFinishedReceiver = new DiscoveryFinishedReceiver(this::onDiscoveryFinished);
    private final BroadcastReceiver nearbyDeviceDiscoveryReceiver = new NearbyDeviceDiscoveryReceiver(this::onNearbyDeviceFound);
    private boolean isConnecting = false;
    private DeviceCommunicationManager deviceCommunicationManager;


    // Exposed LiveData for UI binding
    public LiveData<BtSettingsState> getUIState() {
        return uiState;
    }

    public LiveData<ArrayList<BluetoothDevice>> getNearbyDevices() {
        return nearbyDevices;
    }

    public MutableLiveData<Boolean> getNeedRequestBluetoothEnable() {
        return needRequestBluetoothEnable;
    }


    public BroadcastReceiver getBluetoothStateReceiver() {
        return bluetoothStateReceiver;
    }

    public BroadcastReceiver getNearbyDeviceDiscoveryReceiver() {
        return nearbyDeviceDiscoveryReceiver;
    }

    public BroadcastReceiver getDiscoveryFinishedReceiver() {
        return discoveryFinishedReceiver;
    }

    public MutableLiveData<Boolean> getIsBluetoothEnabled() {
        return isBluetoothEnabled;
    }

    public void onBtStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                Log.d(TAG, "onReceive: STATE OFF");
                isBluetoothEnabled.setValue(false);
                break;
            case BluetoothAdapter.STATE_ON:
                Log.d(TAG, "onReceive: STATE ON");
                isBluetoothEnabled.setValue(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                Log.d(TAG, "onReceive: STATE TURNING OFF");
                // Handle turning off
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                Log.d(TAG, "onReceive: STATE TURNING ON");
                // Handle turning on
                break;
        }
    }

    public void checkBluetoothState() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            uiState.setValue(BtSettingsState.BLUETOOTH_OFF);
        } else {
            uiState.setValue(BtSettingsState.READY_TO_CONNECT);
        }
    }

    public void enableBluetooth() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "Does not have BT capabilities.");
        }
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            uiState.setValue(BtSettingsState.ENABLING_BLUETOOTH);
        }
    }

    @SuppressLint("MissingPermission")
    public void startBluetoothDiscovery() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                globalData.nearbyBtDevices.clear();

                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                mBluetoothAdapter.startDiscovery();

                uiState.setValue(BtSettingsState.SCANNING);
            } else {
                enableBluetooth();
            }
        }
    }


    public void onNearbyDeviceFound(BluetoothDevice device) {
        String lastDeviceAddress = GlobalData.getInstance().userDeviceSettingsStorage.getLatestDeviceAddress();
        // Try to reconnect to it, if is the same address as last one
        if (device.getAddress().equals(lastDeviceAddress)) {
            connectToDevice(device);
        } else {
            globalData.nearbyBtDevices.add(device);
            nearbyDevices.setValue(new ArrayList<>(globalData.nearbyBtDevices));
        }
    }

    private void onDiscoveryFinished() {
        // Discovery has finished, update UI text
        if (!isConnecting) {
            uiState.setValue(BtSettingsState.SCAN_FINISHED);
        }
    }

    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice device) {
        isConnecting = true;

        uiState.setValue(BtSettingsState.CONNECTING);
        // Cancel discovery because it otherwise slows down the connection.
        this.mBluetoothAdapter.cancelDiscovery();

        deviceCommunicationManager = globalData.deviceCommunicationManager;
        deviceCommunicationManager.connectDevice(device);
    }


    public enum BtSettingsState {
        BEFORE_BTN_PRESSED,
        AFTER_BTN_PRESSED,
        BLUETOOTH_OFF,
        ENABLING_BLUETOOTH,
        READY_TO_CONNECT,
        SCANNING,
        SCAN_FINISHED,
        CONNECTING
    }
}
