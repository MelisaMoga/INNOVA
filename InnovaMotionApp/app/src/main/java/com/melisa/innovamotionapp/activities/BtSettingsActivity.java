package com.melisa.innovamotionapp.activities;

import static android.content.ContentValues.TAG;
import static com.melisa.innovamotionapp.ui.viewmodels.BtSettingsViewModel.BtSettingsState;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.BtSettingsActivityBinding;
import com.melisa.innovamotionapp.ui.adapters.NearbyDeviceDataAdapter;
import com.melisa.innovamotionapp.ui.viewmodels.BtSettingsViewModel;
import com.melisa.innovamotionapp.utils.Logger;

public class BtSettingsActivity extends BaseActivity {

    private BtSettingsActivityBinding binding;
    private BtSettingsViewModel viewModel;
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::processEnableBluetoothResponse);
    private AlertDialog locationDialog = null;
    private BtSettingsState currentState = BtSettingsState.DISCONNECTED;


    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = BtSettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(BtSettingsViewModel.class);

        // Initialize sign out button
        binding.signOutButton.setOnClickListener(v -> signOut());

        // Observe LiveData from ViewModel
        setupObservers();
        setupUIListeners();

        // Check initial state
        viewModel.checkBluetoothState();
        
        Logger.i(TAG, "BtSettingsActivity UI initialized successfully");
    }

    private void setupObservers() {
        // Observe Bluetooth UI state
        viewModel.getUIState().observe(this, this::updateUI);

        // Observe nearby devices
        viewModel.getNearbyDevices().observe(this, devices -> {
            // Display nearby devices
            NearbyDeviceDataAdapter adapter = new NearbyDeviceDataAdapter(
                    this,
                    R.layout.nearby_device_layout,
                    devices
            );
            adapter.setOnDeviceClickListener(device -> {
                viewModel.connectToDevice(device);
            });
            binding.deviceListView.setAdapter(adapter);
        });

        // Observe connection state changes
        globalData.getIsConnectedDevice().observe(this, isConnected -> {
            if (isConnected) {
                String deviceConnected = globalData.deviceCommunicationManager.getDeviceToConnect().getAddress();
                globalData.userDeviceSettingsStorage.saveLatestDeviceAddress(deviceConnected);
                
                Logger.bluetooth(TAG, deviceConnected, "Device connected successfully");
                
                // Notify ViewModel and update UI to connected state
                viewModel.onDeviceConnected();
            } else {
                Logger.d(TAG, "Service is disconnected");
                viewModel.onDeviceDisconnected();
            }
        });
    }


    private void setupUIListeners() {
        // Dynamic action button click logic
        binding.btnAction.setOnClickListener(this::onActionButtonClicked);
    }

    @SuppressLint("MissingPermission")
    private void updateUI(BtSettingsState state) {
        currentState = state;
        
        switch (state) {
            case BLUETOOTH_OFF:
                // Bluetooth needs to be enabled
                updateStatusCard(false, null);
                binding.btnAction.setText(R.string.bt_start_to_enable_text);
                binding.btnAction.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_bluetooth_disabled_24));
                binding.btnAction.setEnabled(true);
                binding.deviceListContainer.setVisibility(View.GONE);
                break;

            case ENABLING_BLUETOOTH:
                binding.btnAction.setText(R.string.bt_enabling_text);
                binding.btnAction.setEnabled(false);
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetoothLauncher.launch(enableBTIntent);
                break;

            case DISCONNECTED:
                // Ready to scan
                updateStatusCard(false, null);
                binding.btnAction.setText(R.string.bt_scan_for_devices);
                binding.btnAction.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_bluetooth_searching_24));
                binding.btnAction.setEnabled(true);
                setButtonPrimaryStyle();
                binding.deviceListContainer.setVisibility(View.GONE);
                break;

            case SCANNING:
                updateStatusCard(false, null);
                binding.statusText.setText(R.string.bt_status_scanning);
                binding.btnAction.setText(R.string.bt_stop_scan);
                binding.btnAction.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_bluetooth_searching_24));
                binding.btnAction.setEnabled(true);
                binding.deviceListContainer.setVisibility(View.VISIBLE);
                break;

            case SCAN_FINISHED:
                updateStatusCard(false, null);
                binding.btnAction.setText(R.string.bt_scan_for_devices);
                binding.btnAction.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_bluetooth_searching_24));
                binding.btnAction.setEnabled(true);
                // Keep device list visible so user can select a device
                binding.deviceListContainer.setVisibility(View.VISIBLE);
                break;

            case CONNECTING:
                binding.statusText.setText(R.string.bt_status_connecting);
                binding.btnAction.setText(R.string.bt_connecting_text);
                binding.btnAction.setEnabled(false);
                binding.deviceListContainer.setVisibility(View.GONE);
                break;

            case CONNECTED:
                // Show connected status with device name
                String deviceName = viewModel.getConnectedDeviceName();
                updateStatusCard(true, deviceName);
                binding.btnAction.setText(R.string.bt_disconnect);
                binding.btnAction.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_bluetooth_disabled_24));
                binding.btnAction.setEnabled(true);
                setButtonSecondaryStyle();
                binding.deviceListContainer.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Updates the status card appearance based on connection state.
     */
    private void updateStatusCard(boolean isConnected, @Nullable String deviceName) {
        if (isConnected) {
            // Connected state - green tint
            binding.statusIcon.setImageResource(R.drawable.baseline_bluetooth_connected_24);
            binding.statusIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.status_active));
            binding.statusText.setText(R.string.bt_status_connected);
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.status_active));
            
            if (deviceName != null) {
                binding.deviceNameText.setText(getString(R.string.bt_connected_to, deviceName));
                binding.deviceNameText.setVisibility(View.VISIBLE);
            } else {
                binding.deviceNameText.setVisibility(View.GONE);
            }
        } else {
            // Disconnected state - use theme-aware colors (works in both light and dark mode)
            binding.statusIcon.setImageResource(R.drawable.baseline_bluetooth_24);
            binding.statusIcon.setImageTintList(resolveThemeColorStateList(com.google.android.material.R.attr.colorOnSurfaceVariant));
            binding.statusText.setText(R.string.bt_status_disconnected);
            binding.statusText.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));
            binding.deviceNameText.setVisibility(View.GONE);
        }
    }

    /**
     * Resolves a color from the current theme.
     */
    private int resolveThemeColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }

    /**
     * Resolves a ColorStateList from the current theme.
     */
    private ColorStateList resolveThemeColorStateList(int attrResId) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attrResId, typedValue, true);
        return ColorStateList.valueOf(typedValue.data);
    }

    /**
     * Sets the action button to primary style (for Scan action).
     */
    private void setButtonPrimaryStyle() {
        binding.btnAction.setBackgroundTintList(resolveThemeColorStateList(com.google.android.material.R.attr.colorPrimary));
        binding.btnAction.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary));
        binding.btnAction.setIconTint(resolveThemeColorStateList(com.google.android.material.R.attr.colorOnPrimary));
    }

    /**
     * Sets the action button to secondary/error style (for Disconnect action).
     */
    private void setButtonSecondaryStyle() {
        binding.btnAction.setBackgroundTintList(resolveThemeColorStateList(com.google.android.material.R.attr.colorError));
        binding.btnAction.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnError));
        binding.btnAction.setIconTint(resolveThemeColorStateList(com.google.android.material.R.attr.colorOnError));
    }

    private void onActionButtonClicked(View view) {
        switch (currentState) {
            case BLUETOOTH_OFF:
                // Request to enable Bluetooth
                viewModel.enableBluetooth();
                break;
                
            case DISCONNECTED:
            case SCAN_FINISHED:
                // Start scanning
                if (!checkAllRequirements()) {
                    viewModel.startBluetoothDiscovery();
                }
                break;
                
            case SCANNING:
                // Stop scanning
                viewModel.stopScanning();
                break;
                
            case CONNECTED:
                // Disconnect
                viewModel.disconnectDevice();
                logAndToast("Disconnected from device");
                break;
                
            default:
                // Do nothing for other states (ENABLING_BLUETOOTH, CONNECTING)
                break;
        }
    }

    private void processEnableBluetoothResponse(ActivityResult activityResult) {
        if (activityResult.getResultCode() == RESULT_OK) {
            logAndToast("Bluetooth enabled successfully");
            viewModel.startBluetoothDiscovery();
        } else {
            logErrorAndNotifyUser("Failed to enable Bluetooth", "Bluetooth is required for device connection", null);
            viewModel.checkBluetoothState(); // Refresh UI
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

        // Check current connection state on resume (in case state changed externally)
        viewModel.checkConnectionState();
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

        // If we are on an older device (below S), ensure location is enabled
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
