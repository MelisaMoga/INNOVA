package com.melisa.pedonovation.BluetoothCore;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.pedonovation.Interfaces.UILogger;

import java.util.Set;
import java.util.UUID;

public class BluetoothHelper {
    public static UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final AppCompatActivity context;
    public BluetoothAdapter bluetoothAdapter;
    public BroadcastReceiver bluetoothStateReceiver;
    public BroadcastReceiver discoverReceiver;


    private UILogger uiLogger;

    public BluetoothHelper(AppCompatActivity context) {
        this.context = context;

        // Get phone bt adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getBondedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }

    @SuppressLint("MissingPermission")
    public void scanDevices() {
        if (validateBluetoothPerms(new String[]{Manifest.permission.BLUETOOTH_SCAN})) {
            // Safe return in case permissions are not allowed.
            return;
        }
        if (bluetoothAdapter.isDiscovering()) {
            log("Canceling discovery...");
            bluetoothAdapter.cancelDiscovery();
        }

        if (validateBluetoothPerms(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})) {
            // Safe return in case permissions are not allowed.
            return;
        }
        bluetoothAdapter.startDiscovery();
        log("Start discovery...");
    }

    public void tryTurnOnBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            turnOnBluetooth();
        } else {
            log("Bluetooth is already enabled");
        }
    }

    @SuppressLint("MissingPermission")
    private void turnOnBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (validateBluetoothPerms(new String[]{Manifest.permission.BLUETOOTH_CONNECT})) {
                // Safe return in case permissions are not allowed.
                return;
            }
        } else {
            Log.d(TAG, "No need to check permissions. SDK version < LOLLIPOP.");
        }
        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        context.startActivity(enableBTIntent);
        log("Turning On Bluetooth...");
    }

    protected boolean validateBluetoothPerms(String[] perms) {
        // Add up permissions check
        int permissionCheck = 0;
        for (String perm : perms) {
            permissionCheck += context.checkSelfPermission(perm);
        }

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            context.requestPermissions(perms, 1001);
            return true;
        } else {
            return false;
        }
    }

    private void log(String stringToLog) {
        if (uiLogger != null) {
            uiLogger.log_and_toast(stringToLog);
        }
    }

    public void registerReceivers(Context context) {
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(bluetoothStateReceiver, BTIntent);
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(discoverReceiver, discoverDevicesIntent);
    }

    public void unregisterReceivers(Context context) {
        context.unregisterReceiver(bluetoothStateReceiver);
        context.unregisterReceiver(discoverReceiver);
    }

    public void setLogger(UILogger uiLogger) {
        this.uiLogger = uiLogger;
    }
}
