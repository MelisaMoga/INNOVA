package com.melisa.pedonovation.AppActivities.Managers;

import static android.content.ContentValues.TAG;
import static com.melisa.pedonovation.Utilities.setVisibility;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.melisa.pedonovation.AppActivities.BtSettingsActivity;
import com.melisa.pedonovation.BluetoothCore.BluetoothDeviceReceivedData;
import com.melisa.pedonovation.BluetoothCore.BluetoothHelper;
import com.melisa.pedonovation.BluetoothCore.ConnectionData;
import com.melisa.pedonovation.BluetoothCore.ConnectionThread;
import com.melisa.pedonovation.BluetoothCore.DeviceListAdapter;
import com.melisa.pedonovation.GlobalData;
import com.melisa.pedonovation.Interfaces.ICurrentActivityManager;
import com.melisa.pedonovation.R;
import com.melisa.pedonovation.Interfaces.UILogger;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class BtSettingsManager implements UILogger, ICurrentActivityManager {
    private final BtSettingsActivity activity;
    private final BluetoothHelper bluetoothHelper;
    private final TextView bluetoothStatusTv, device1Tv, device2Tv;
    private final Button btnTurnOnBluetooth, btnDevice1, btnDevice2, btnScanDevices;
    public final ListView unconnectedLv;
    public ArraySet<BluetoothDevice> nearbyBtDevices = new ArraySet<>();
    public GlobalData globalData;
    public DeviceListAdapter mDeviceListAdapter;

    private final String BT_STATUS_NOT_SUPPORTED_TEXT = "Bluetooth not supported";
    private final String BT_STATUS_ON_TEXT = "Bluetooth Status: On";
    private final String BT_STATUS_OFF_TEXT = "Bluetooth Status: Off";

    public BtSettingsManager(BtSettingsActivity activity, BluetoothHelper bluetoothHelper, GlobalData globalData) {
        this.activity = activity;
        this.bluetoothHelper = bluetoothHelper;

        bluetoothStatusTv = activity.binding.bluetoothStatusTv;
        device1Tv = activity.binding.device1Tv;
        device2Tv = activity.binding.device2Tv;
        btnTurnOnBluetooth = activity.binding.btnTurnOnBluetooth;
        btnDevice1 = activity.binding.btnDevice1;
        btnDevice2 = activity.binding.btnDevice2;
        btnScanDevices = activity.binding.btnScanDevices;
        unconnectedLv = activity.binding.unconnectedLv;

        btnTurnOnBluetooth.setOnClickListener(v -> bluetoothHelper.tryTurnOnBluetooth());
        btnDevice1.setOnClickListener(v -> showDeviceListDialog(1));
        btnDevice2.setOnClickListener(v -> showDeviceListDialog(2));
        btnScanDevices.setOnClickListener(v -> scanDevices());

        this.globalData = globalData;
    }

    private void scanDevices() {
        // Do not keep bt nearby devices
        nearbyBtDevices.clear();
        bluetoothHelper.scanDevices();
    }


    @SuppressLint("MissingPermission")
    private void showDeviceListDialog(final int deviceNumber) {
        // Get paired devices
        final Set<BluetoothDevice> pairedDevices = bluetoothHelper.getBondedDevices();
        final ArrayList<String> deviceNames = new ArrayList<>();
        final ArrayList<BluetoothDevice> devices = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
            deviceNames.add(device.getName());
            devices.add(device);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select a Device");
        builder.setItems(deviceNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothDevice selectedDevice = devices.get(which);
                connectToDevice(selectedDevice, deviceNumber);
            }
        });
        builder.show();
    }

    /**
     * Connects to a specified Bluetooth device and assigns it to the specified device slot.
     *
     * @param device       The Bluetooth device to connect to.
     * @param deviceNumber The device slot number (1 for Device 1, 2 for Device 2).
     */
    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice device, int deviceNumber) {
        if (deviceNumber == 1) {
            // If the device selected to connect is already assigned as Device 2 -> skip
            if (device != null && globalData.connectionData2 != null) {
                if (Objects.equals(device.getAddress(), globalData.connectionData2.device.getAddress())) {
                    log_and_toast("Device already assigned!");
                    return;
                }
            }

            // Safely close the connection before replacing Device 1
            if (globalData.isConnection1Alive()) {
                globalData.connectionData1.cancelConnection();
            }

            // Initialize and start the connection thread for Device 1
            ConnectionThread connectionThread = new ConnectionThread(device, globalData.messageHandler);
            globalData.connectionData1 = new ConnectionData(device, connectionThread);

            // Cancel discovery because it otherwise slows down the connection.
            bluetoothHelper.bluetoothAdapter.cancelDiscovery();
            // Start thread execution for Device1
            globalData.connectionData1.connectionThread.start();
        } else if (deviceNumber == 2) {
            // If the device selected to connect is already assigned as Device 1 -> skip
            if (device != null && globalData.connectionData1 != null) {
                if (Objects.equals(device.getAddress(), globalData.connectionData1.device.getAddress())) {
                    log_and_toast("Device already assigned!");
                    return;
                }
            }

            // Safely close the connection before replacing Device 2
            if (globalData.isConnection2Alive()) {
                globalData.connectionData2.cancelConnection();
            }

            // Initialize and start the connection thread for Device 2
            ConnectionThread connectionThread = new ConnectionThread(device, globalData.messageHandler);
            globalData.connectionData2 = new ConnectionData(device, connectionThread);

            // Cancel discovery because it otherwise slows down the connection.
            bluetoothHelper.bluetoothAdapter.cancelDiscovery();
            // Start thread execution for Device1
            globalData.connectionData2.connectionThread.start();
        }
    }

    /**
     * Manages the connection to a Bluetooth device, assigning it to either Device 1 or Device 2.
     *
     * @param device The Bluetooth device to connect.
     */
    public void connectToDevice(BluetoothDevice device) {
        if (!globalData.isConnection1Alive()) {
            // If Device 1 is not assigned, connect to Device 1
            connectToDevice(device, 1);
        } else if (!globalData.isConnection2Alive()) {
            // If Device 2 is not assigned, connect to Device 2
            connectToDevice(device, 2);

        } else {
            // If both devices are selected, replace Device 1
            connectToDevice(device, 1);
        }
    }

    @SuppressLint("MissingPermission")
    public void updateBluetoothUI() {
        // Check if bt is available or not
        if (bluetoothHelper.bluetoothAdapter == null) {
            bluetoothStatusTv.setText(BT_STATUS_NOT_SUPPORTED_TEXT);
        } else {
            // Set UI elements according to bt status (on/off)
            if (bluetoothHelper.bluetoothAdapter.isEnabled()) {
                bluetoothStatusTv.setText(BT_STATUS_ON_TEXT);
                // Hide btn that turn on bluetooth
                btnTurnOnBluetooth.setVisibility(View.GONE);
                // Show UI elements, if bt is ON
                setVisibility(
                        View.VISIBLE,
                        device1Tv,
                        device2Tv,
                        btnDevice1,
                        btnDevice2,
                        btnScanDevices
                );

                if (globalData.isConnection1Alive()) {
                    device1Tv.setText("Device 1: " + globalData.connectionData1.device.getName());
                } else {
                    device1Tv.setText("Device 1: None");
                }

                if (globalData.isConnection2Alive()) {
                    device2Tv.setText("Device 2: " +  globalData.connectionData2.device.getName());
                } else {
                    device2Tv.setText("Device 2: None");
                }
            } else {
                bluetoothStatusTv.setText(BT_STATUS_OFF_TEXT);
                // Show btn that turn on bluetooth
                btnTurnOnBluetooth.setVisibility(View.VISIBLE);
                // Hide UI elements, if bt is Off
                setVisibility(
                        View.GONE,
                        device1Tv,
                        device2Tv,
                        btnDevice1,
                        btnDevice2,
                        btnScanDevices
                );
            }
        }
    }

    public void showToast(String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void log_and_toast(String stringToLog) {
        Log.d(TAG, String.format("[LOG] %s", stringToLog));
        showToast(stringToLog);
    }

    public void deviceFound(BluetoothDevice device) {
        nearbyBtDevices.add(device);
        // Convert set to list
        ArrayList<BluetoothDevice> devices = new ArrayList<>(nearbyBtDevices);
        mDeviceListAdapter = new DeviceListAdapter(activity, R.layout.device_adapter_view, devices, this);
        unconnectedLv.setAdapter(mDeviceListAdapter);
    }

    @Override
    public void handleAnyState(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        updateBluetoothUI();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void handleConnecting(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;
        log_and_toast(String.format("Trying to connect to... %s", bluetoothDevice.getName()));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void handleConnected(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;
        log_and_toast(String.format("Connection Established %s", bluetoothDevice.getName()));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void handleConnectionFailed(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;
        log_and_toast(String.format("Connection Failed %s", bluetoothDevice.getName()));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void handleMsgReceived(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;;
        String receivedData = bluetoothDeviceReceivedData.receivedData;

        log_and_toast(String.format("[READ][%s] %s", bluetoothDevice.getName(), receivedData));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void handleDisconnected(BluetoothDeviceReceivedData bluetoothDeviceReceivedData) {
        BluetoothDevice bluetoothDevice = bluetoothDeviceReceivedData.device;
        log_and_toast(String.format("Device Disconnected %s", bluetoothDevice.getName()));
    }
}
