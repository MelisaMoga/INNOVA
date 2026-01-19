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
     * Disconnect the currently connected device and stop the foreground service.
     * 
     * This properly shuts down the Bluetooth connection by:
     * 1. Closing the BluetoothSocket (via service.disconnectAndStop())
     * 2. Removing the foreground notification
     * 3. Stopping the service
     * 4. Unbinding from the service connection
     */
    public void disconnectDevice() {
        if (deviceCommunicationService != null) {
            // Use disconnectAndStop() for proper shutdown sequence
            // This sets the stopping flag, closes the socket, removes foreground, and stops the service
            deviceCommunicationService.disconnectAndStop();
        }

        // Unbind from the service to prevent leaks
        unbindFromService();
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
     * Unbinds from the service connection to prevent memory leaks.
     * Should be called after the service has been stopped.
     */
    private void unbindFromService() {
        if (deviceCommunicationService != null) {
            try {
                context.unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                // Service was already unbound or not bound
            }
            deviceCommunicationService = null;
        }
    }
    
    /**
     * Stop the service when no device is connected.
     * This is primarily called from the service's onConnectionDisconnected callback
     * when auto-reconnection fails.
     */
    public void stopService() {
        if (deviceCommunicationService != null) {
            // The service will handle stopping itself via disconnectAndStop()
            // We just need to unbind
            unbindFromService();
            
            // Also explicitly stop the service in case it wasn't stopped internally
            Intent serviceIntent = new Intent(context, DeviceCommunicationService.class);
            context.stopService(serviceIntent);
        }
    }
}
