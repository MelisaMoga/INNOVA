package com.melisa.innovamotionapp.activities;

import static android.content.ContentValues.TAG;
import static com.melisa.innovamotionapp.ui.viewmodels.BtSettingsViewModel.BtSettingsState;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.BtSettingsActivityBinding;
import com.melisa.innovamotionapp.ui.adapters.NearbyDeviceDataAdapter;
import com.melisa.innovamotionapp.ui.viewmodels.BtSettingsViewModel;
import com.melisa.innovamotionapp.utils.GlobalData;

public class BtSettingsActivity extends AppCompatActivity {

    private BtSettingsActivityBinding binding;
    private BtSettingsViewModel viewModel;
    //    private final GlobalData globalData = GlobalData.getInstance();
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::processEnableBluetoothResponse);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = BtSettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(BtSettingsViewModel.class);


        // Observe LiveData from ViewModel
        setupObservers();
        setupUIListeners();

        viewModel.checkBluetoothState();
        updateUI(BtSettingsState.BEFORE_BTN_PRESSED);
    }

    private void setupObservers() {
        // Observe Bluetooth UI state
        viewModel.getUIState().observe(this, this::updateUI);

        // Observe nearby devices
        viewModel.getNearbyDevices().observe(this, devices -> {
            String userName = binding.inputChildName.getText().toString();


            // Display nearby devices
            NearbyDeviceDataAdapter adapter = new NearbyDeviceDataAdapter(
                    this,
                    R.layout.nearby_device_layout,
                    devices
            );
            adapter.setOnDeviceClickListener(device -> {
                viewModel.connectToDevice(device, userName);
            });
            binding.deviceListRecyclerView.setAdapter(adapter);
        });

        GlobalData.getInstance().getIsConnectedDevice().observe(this, isConnected -> {

            if (isConnected) {
                String deviceConnected = GlobalData.getInstance().deviceCommunicationManager.getDeviceToConnect().getAddress();
                GlobalData.getInstance().userDeviceSettingsStorage.saveLatestDeviceAddress(deviceConnected);

                // Navigate to the next screen or update UI
                launchBtConnectedActivity();
            } else {
                // Handle the disconnected state if necessary
                Log.d("ServiceState", "Service is disconnected.");
            }
        });
    }


    private void setupUIListeners() {
        // UserName text input logic
        binding.inputChildName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Get the trimmed version of the input
                String trimmedInput = s.toString().trim();
                // Enable the button if the trimmed input is at least 3 characters
                binding.btConnection.setEnabled(trimmedInput.length() >= 3);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        // Button click logic
        binding.btConnection.setOnClickListener(this::onStartBtnClicked);
    }

    private void updateUI(BtSettingsState state) {
        switch (state) {
            case BEFORE_BTN_PRESSED: // On activity initialisation
                binding.inputChildName.setEnabled(true);
                binding.btConnection.setEnabled(false);
                binding.inputChildName.setText(GlobalData.getInstance().userDeviceSettingsStorage.getLatestUser());
                break;

            case AFTER_BTN_PRESSED:
                binding.inputChildName.setEnabled(false);
                binding.btConnection.setEnabled(false);
                break;

            case BLUETOOTH_OFF:
                // Bluetooth needs to be enabled
                binding.btConnection.setText(R.string.bt_start_to_enable_text);
                break;

            case ENABLING_BLUETOOTH:
                binding.btConnection.setText(R.string.bt_enabling_text);

                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetoothLauncher.launch(enableBTIntent);
                break;

            case READY_TO_CONNECT:
                // Ready to connect
                binding.btConnection.setText(R.string.bt_start_to_connect_text);
                break;

            case SCANNING:
                binding.btConnection.setText(R.string.bt_scanning_text);
                binding.btConnection.setEnabled(true);
                break;

            case SCAN_FINISHED:
                binding.btConnection.setText(R.string.bt_stopped_scan_text);
                binding.btConnection.setEnabled(true);
                break;

            case CONNECTING:
                binding.btConnection.setText(R.string.bt_connecting_text);
                binding.btConnection.setEnabled(false);
                break;
        }
    }

    private void onStartBtnClicked(View view) {
        updateUI(BtSettingsState.AFTER_BTN_PRESSED);

        boolean requirePermissions = doesNeedRequestPermission(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT});
        if (requirePermissions) {
            // Safe return in case permissions are not allowed.
            updateUI(BtSettingsState.BEFORE_BTN_PRESSED);
            return;
        }


        // Start Bluetooth discovery
        viewModel.startBluetoothDiscovery();
    }

    private void processEnableBluetoothResponse(ActivityResult activityResult) {
        if (activityResult.getResultCode() == AppCompatActivity.RESULT_OK) {

            log("Bluetooth enabled successfully");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                viewModel.startBluetoothDiscovery();
            }
        } else {
            log("Failed to enable Bluetooth");
        }
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

    public void log(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    private void launchBtConnectedActivity() {
        // Launch the new Activity
        Intent i = new Intent(this, BtConnectedActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register broadcast receivers
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(viewModel.getBluetoothStateReceiver(), filter);

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(viewModel.getNearbyDeviceDiscoveryReceiver(), filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(viewModel.getDiscoveryFinishedReceiver(), filter);


        boolean requirePermissions = doesNeedRequestPermission(new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT});
        if (requirePermissions) {
            // Safe return in case permissions are not allowed.
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister broadcast receivers
        unregisterReceiver(viewModel.getBluetoothStateReceiver());
        unregisterReceiver(viewModel.getNearbyDeviceDiscoveryReceiver());
        unregisterReceiver(viewModel.getDiscoveryFinishedReceiver());
    }
}
