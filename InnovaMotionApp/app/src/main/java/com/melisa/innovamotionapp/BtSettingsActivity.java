package com.melisa.innovamotionapp;

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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.databinding.BtSettingsActivityBinding;
import com.melisa.innovamotionapp.receivers.DeviceDiscoveryCallback;
import com.melisa.innovamotionapp.receivers.DiscoveryReceiver;
import com.melisa.innovamotionapp.uistuff.DeviceDataUIAdapter;

import java.util.ArrayList;


public class BtSettingsActivity extends AppCompatActivity implements DeviceDiscoveryCallback {

    private BluetoothAdapter mBluetoothAdapter;

    private BtSettingsActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //when discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onReceive: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver discoveryReceiver = new DiscoveryReceiver(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = BtSettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Keep UI
        Button btConnection = binding.btConnection;
        btConnection.setText(R.string.bt_start_connect_text);
        btConnection.setOnClickListener(this::StartConnecting);
        EditText inputChildName = binding.inputChildName;
        inputChildName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Get the trimmed version of the input
                String trimmedInput = s.toString().trim();
                // Enable the button if the trimmed input is at least 3 characters
                btConnection.setEnabled(trimmedInput.length() >= 3);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Register receivers
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, BTIntent);

        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, discoverDevicesIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean requirePermissions = doesNeedRequestPermission(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT});
        if (requirePermissions) {
            // Safe return in case permissions are not allowed.
            return;
        }
    }

    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(discoveryReceiver);
    }

    public void StartConnecting(View view) {
        boolean requirePermissions = doesNeedRequestPermission(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT});
        if (requirePermissions) {
            // Safe return in case permissions are not allowed.
            return;
        }

        // Update UI
        binding.inputChildName.setEnabled(false);
//        binding.btConnection.setEnabled(false);
        binding.btConnection.setText(R.string.bt_connecting_text);

        // Save child name
        // globalData.childName = binding.inputChildName.getText();

        log("Enabling bluetooth..");
        BtEnable();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            log("Starting discovery..");
            BtStartDiscovery();
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void BtStartDiscovery() {
        log("Am trecut de perms in discovery..");
        globalData.nearbyBtDevices.clear();


        if (mBluetoothAdapter.isDiscovering()) {
            log("Canceling discovery...");
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
        log("Start discovery...");
    }

    public void log(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    @SuppressLint("MissingPermission")
    public void BtEnable() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }

        if (!mBluetoothAdapter.isEnabled()) {  //if Bluetooth is disabled
            Log.d(TAG, "enableDisableBT: Enabling BT.");

            boolean requirePermissions = doesNeedRequestPermission(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT});
            if (requirePermissions) {
                return;
            }
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
        } else {
            log("Bluetooth already enabled.");
        }
//        if(mBluetoothAdapter.isEnabled()){   //if Bluetooth is enabled
//            Log.d(TAG, "enableDisableBT: Disabling BT.");
//            Intent intentBtDisabled = new Intent("android.bluetooth.adapter.action.REQUEST_DISABLE");
//            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//
//                BtSettingsActivity.this.requestPermissions( new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
//            }
//            startActivity(intentBtDisabled);
//        }
    }

    private boolean doesNeedRequestPermission(String[] perms) {
        for (String perm : perms) {
            if (this.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {

                this.requestPermissions(new String[]{perm}, 2);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDeviceFound(BluetoothDevice bluetoothDevice) {
        globalData.nearbyBtDevices.add(bluetoothDevice);
        // Convert set to list
        ArrayList<BluetoothDevice> devices = new ArrayList<>(globalData.nearbyBtDevices);
        DeviceDataUIAdapter mDeviceListAdapter = new DeviceDataUIAdapter(this, R.layout.nearby_device_layout, devices);
        binding.deviceListRecyclerView.setAdapter(mDeviceListAdapter);

    }
}