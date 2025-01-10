package com.melisa.innovamotionapp.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.core.content.ContextCompat;

public class DeviceCommunicationManager {
    private final Context context;
    private DeviceCommunicationService deviceCommunicationService;

    public BluetoothDevice getDeviceToConnect() {
        return deviceToConnect;
    }

    private BluetoothDevice deviceToConnect; // Store the device to connect
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DeviceCommunicationService.LocalBinder binder = (DeviceCommunicationService.LocalBinder) service;
            deviceCommunicationService = binder.getService();


            // Inform the service to connect to the device
            deviceCommunicationService.connectToDevice(deviceToConnect);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            deviceCommunicationService = null;
        }
    };

    public DeviceCommunicationManager(Context context) {
        this.context = context;
    }


    /**
     * Start the service and connect to a Bluetooth device.
     */
    public void connectDevice(BluetoothDevice device) {
        // Store the device
        deviceToConnect = device;

        // Start the service if it's not running yet

        if (deviceCommunicationService == null) {
            // Create an Intent to start the service
            Intent serviceIntent = new Intent(context, DeviceCommunicationService.class);
            // Start the service
            ContextCompat.startForegroundService(context, serviceIntent);
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        // Connection will be initiated in onServiceConnected()
    }

    /**
     * Disconnect the currently connected device and stop the service if no devices are connected.
     */
    public void disconnectDevice() {
        if (deviceCommunicationService != null) {
            deviceCommunicationService.disconnectDevice();
        }

        // Optionally stop the service if no device is connected
        if (!isDeviceConnected()) {
            stopService();
        }
    }

    /**
     * Check if the current device is connected.
     *
     * @return true if a device is connected, false otherwise.
     */
    public boolean isDeviceConnected() {
        return deviceCommunicationService != null && deviceCommunicationService.isDeviceConnected();
    }

    /**
     * Stop the service when no device is connected.
     */
    public void stopService() {
        if (deviceCommunicationService != null) {
            Intent serviceIntent = new Intent(context, DeviceCommunicationService.class);
            context.unbindService(serviceConnection);
            context.stopService(serviceIntent);
            deviceCommunicationService = null;
        }
    }
}
