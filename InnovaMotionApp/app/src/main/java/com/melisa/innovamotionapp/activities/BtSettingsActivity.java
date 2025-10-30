package com.melisa.innovamotionapp.activities;

import static android.content.ContentValues.TAG;
import static com.melisa.innovamotionapp.ui.viewmodels.BtSettingsViewModel.BtSettingsState;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.BtSettingsActivityBinding;
import com.melisa.innovamotionapp.sync.SessionGate;
import com.melisa.innovamotionapp.ui.adapters.NearbyDeviceDataAdapter;
import com.melisa.innovamotionapp.ui.viewmodels.BtSettingsViewModel;
import com.melisa.innovamotionapp.utils.Logger;
import com.melisa.innovamotionapp.utils.RoleProvider;

import java.util.List;

public class BtSettingsActivity extends BaseActivity {

    private BtSettingsActivityBinding binding;
    private BtSettingsViewModel viewModel;
    private MaterialButton signOutButton;
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::processEnableBluetoothResponse);
    private AlertDialog locationDialog = null;


    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        // Early exit for supervisors: they shouldn't need to scan
        SessionGate.getInstance(this).waitForSessionReady(new SessionGate.SessionReadyCallback() {
            @Override
            public void onSessionReady(String userId, String role, List<String> supervisedUserIds) {
                runOnUiThread(() -> {
                    if ("supervisor".equals(role)) {
                        Logger.d(TAG, "Supervisor detected in BtSettingsActivity: redirecting to SupervisorDashboard");
                        Intent intent = new Intent(BtSettingsActivity.this, SupervisorDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                        return;
                    }
                    
                    // Continue with normal aggregator user flow
                    initializeAggregatorUserUI();
                });
            }
            
            @Override
            public void onSessionError(String error) {
                Logger.e(TAG, "Session not ready: " + error);
                // Continue with normal flow if session not ready
                initializeAggregatorUserUI();
            }
        });
    }
    
    private void initializeAggregatorUserUI() {
        binding = BtSettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(BtSettingsViewModel.class);

        // Initialize sign out button
        signOutButton = findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(v -> signOut());

        // Observe LiveData from ViewModel
        setupObservers();
        setupUIListeners();

        viewModel.checkBluetoothState();
        updateUI(BtSettingsState.BEFORE_BTN_PRESSED);
        
        Logger.i(TAG, "BtSettingsActivity UI initialized successfully");
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

        globalData.getIsConnectedDevice().observe(this, isConnected -> {

            if (isConnected) {
                String deviceConnected = globalData.deviceCommunicationManager.getDeviceToConnect().getAddress();
                globalData.userDeviceSettingsStorage.saveLatestDeviceAddress(deviceConnected);
                
                Logger.bluetooth(TAG, deviceConnected, "Device connected successfully");

                // Navigate to the next screen or update UI
                launchBtConnectedActivity();
            } else {
                // Handle the disconnected state if necessary
                Logger.d(TAG, "Service is disconnected");
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
                binding.inputChildName.setText(globalData.userDeviceSettingsStorage.getLatestUser());
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

        if (checkAllRequirements()) {
            // Safe return in case permissions are not allowed.
            updateUI(BtSettingsState.BEFORE_BTN_PRESSED);
            return;
        }


        // Start Bluetooth discovery
        viewModel.startBluetoothDiscovery();
    }

    private void processEnableBluetoothResponse(ActivityResult activityResult) {
        if (activityResult.getResultCode() == RESULT_OK) {
            logAndToast("Bluetooth enabled successfully");
            viewModel.startBluetoothDiscovery();
        } else {
            logErrorAndNotifyUser("Failed to enable Bluetooth", "Bluetooth is required for device connection", null);
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




    private void launchBtConnectedActivity() {
        // Route based on user role
        RoleProvider.Role role = RoleProvider.getCurrentRole();
        
        if (role == RoleProvider.Role.AGGREGATOR) {
            Logger.userAction(TAG, "Launching DataAggregatorActivity (aggregator role)");
            navigateToActivityAndFinish(DataAggregatorActivity.class, null);
        } else if (role == RoleProvider.Role.SUPERVISOR) {
            // Supervisors shouldn't normally reach here, but handle it gracefully
            Logger.w(TAG, "Supervisor reached BT connection flow, routing to dashboard");
            navigateToActivityAndFinish(SupervisorDashboardActivity.class, null);
        } else {
            // Unknown role: default to aggregator behavior
            Logger.w(TAG, "Unknown role, defaulting to DataAggregatorActivity");
            navigateToActivityAndFinish(DataAggregatorActivity.class, null);
        }
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


        if (checkAllRequirements()) {
            // Safe return in case permissions are not allowed.
            return;
        }
    }

    private String[] getRequiredPermissions() {
        String[] requiredPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions = new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            requiredPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }
        return requiredPermissions;
    }

    /**
     * Checks if all requirements (permissions + location for older OS) are met.
     * If not, it requests them or shows a location dialog. Returns `false` if everything
     * is good, or `true` otherwise.
     */
    private boolean checkAllRequirements() {
        String[] requiredPermissions = getRequiredPermissions();


        boolean requirePermissions = doesNeedRequestPermission(requiredPermissions);

        // 2. If we are on an older device (below S), ensure location is enabled
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!isLocationEnabled) {
                showLocationDisabledDialog();
                return true;
            }
        }

        return requirePermissions;
    }

    private void showLocationDisabledDialog() {
        // If the dialog is already showing, just return
        if (locationDialog != null && locationDialog.isShowing()) {
            return;
        }

        // Otherwise, build a new one
        locationDialog = new AlertDialog.Builder(this)
                .setTitle("Location Services Required")
                .setMessage("Please enable location services for Bluetooth scanning.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(settingsIntent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .create();

        locationDialog.show();
    }



    @Override
    protected void onPause() {
        super.onPause();

        // Unregister broadcast receivers
        unregisterReceiver(viewModel.getBluetoothStateReceiver());
        unregisterReceiver(viewModel.getNearbyDeviceDiscoveryReceiver());
        unregisterReceiver(viewModel.getDiscoveryFinishedReceiver());

        if (locationDialog != null && locationDialog.isShowing()) {
            locationDialog.dismiss();
            locationDialog = null;
        }
    }
}
