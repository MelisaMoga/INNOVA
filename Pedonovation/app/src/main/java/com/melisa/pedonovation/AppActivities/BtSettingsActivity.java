package com.melisa.pedonovation.AppActivities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.pedonovation.BluetoothCore.BluetoothHelper;
import com.melisa.pedonovation.BluetoothCore.Receivers.BluetoothStateReceiver;
import com.melisa.pedonovation.BluetoothCore.Receivers.DiscoverReceiver;
import com.melisa.pedonovation.AppActivities.Managers.BtSettingsManager;
import com.melisa.pedonovation.GlobalData;
import com.melisa.pedonovation.databinding.ActivityBtsettingsBinding;

public class BtSettingsActivity extends AppCompatActivity {


    public ActivityBtsettingsBinding binding;
    private BtSettingsManager btSettingsManager;
    private BluetoothHelper bluetoothHelper;
    private GlobalData globalData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBtsettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve the BluetoothHelper instance from the global application context
        globalData = ((GlobalData) this.getApplicationContext());

        bluetoothHelper = new BluetoothHelper(this);

        BluetoothStateReceiver bluetoothStateReceiver = new BluetoothStateReceiver(btSettingsManager);

        btSettingsManager = new BtSettingsManager(this, bluetoothHelper, globalData);

        DiscoverReceiver discoverReceiver = new DiscoverReceiver(btSettingsManager);
        bluetoothHelper.bluetoothStateReceiver = bluetoothStateReceiver;
        bluetoothHelper.discoverReceiver = discoverReceiver;
        bluetoothHelper.setLogger(btSettingsManager);

    }


    @Override
    protected void onResume() {
        super.onResume();
        bluetoothHelper.registerReceivers(this);


        // Change the manager to btSettingsManager that handles the Thread messages
        globalData.setActivityManagerForHandler(btSettingsManager);

        // Update inApp bluetooth state
        btSettingsManager.updateBluetoothUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        bluetoothHelper.unregisterReceivers(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
