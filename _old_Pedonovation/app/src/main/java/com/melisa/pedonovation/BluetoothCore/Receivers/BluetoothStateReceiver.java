package com.melisa.pedonovation.BluetoothCore.Receivers;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.melisa.pedonovation.AppActivities.Managers.BtSettingsManager;

/**
 * BroadcastReceiver to handle Bluetooth state changes.
 */
public class BluetoothStateReceiver extends BroadcastReceiver {
    private final BtSettingsManager btSettingsManager;

    /**
     * Constructor to initialize BluetoothStateReceiver with a BluetoothHelper instance.
     *
     * @param btSettingsManager The BluetoothHelper instance to manage Bluetooth operations.
     */
    public BluetoothStateReceiver(BtSettingsManager btSettingsManager) {
        this.btSettingsManager = btSettingsManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the action of the received intent
        String action = intent.getAction();

        // Check if the action is related to Bluetooth state change
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            // Get the current state of the Bluetooth adapter
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            // Update the UI based on the Bluetooth state
            btSettingsManager.updateBluetoothUI();

            // Handle different Bluetooth states
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    // Log and show a toast message for Bluetooth off state
                    btSettingsManager.log_and_toast("STATE OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    // Log the Bluetooth turning off state
                    Log.d(TAG, "STATE TURNING OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    // Log and show a toast message for Bluetooth on state
                    btSettingsManager.log_and_toast("STATE ON");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    // Log and show a toast message for Bluetooth turning on state
                    btSettingsManager.log_and_toast("STATE TURNING ON");
                    break;
            }
        }
    }
}
