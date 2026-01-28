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

import java.util.Map;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.BtSettingsActivityBinding;
import com.melisa.innovamotionapp.ui.adapters.NearbyDeviceDataAdapter;
import com.melisa.innovamotionapp.ui.viewmodels.BtSettingsViewModel;
import com.melisa.innovamotionapp.utils.Logger;
import com.melisa.innovamotionapp.utils.PermissionHelper;

public class BtSettingsActivity extends BaseActivity {

    private BtSettingsActivityBinding binding;
    private BtSettingsViewModel viewModel;
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::processEnableBluetoothResponse);
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);
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
                // Check BLUETOOTH_CONNECT permission before connecting to device
                if (!PermissionHelper.hasBluetoothConnectPermission(this)) {
                    logErrorAndNotifyUser("Bluetooth permission required", 
                        "Bluetooth connect permission is required to connect to devices", null);
                    return;
                }
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
                // On Android S+ (API 31+), BLUETOOTH_CONNECT permission is required before enabling Bluetooth
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // Request permission first
                        requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT});
                        return; // Exit early, will proceed after permission is granted
                    }
                }
                // Permission granted (or Android < S), proceed with enabling Bluetooth
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
                // Check BLUETOOTH_CONNECT permission before getting device name
                String deviceName = null;
                if (PermissionHelper.hasBluetoothConnectPermission(this)) {
                    deviceName = viewModel.getConnectedDeviceName();
                } else {
                    // Permission denied - use device address as fallback
                    BluetoothDevice device = globalData.deviceCommunicationManager.getDeviceToConnect();
                    if (device != null) {
                        deviceName = device.getAddress();
                    }
                }
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
                // Start scanning - check BLUETOOTH_SCAN permission
                if (!PermissionHelper.hasBluetoothScanPermission(this)) {
                    logErrorAndNotifyUser("Bluetooth permission required", 
                        "Bluetooth scan permission is required to discover devices", null);
                    // Still check all requirements to request permissions if needed
                    checkAllRequirements();
                    break;
                }
                if (!checkAllRequirements()) {
                    viewModel.startBluetoothDiscovery();
                }
                break;
                
            case SCANNING:
                // Stop scanning - check BLUETOOTH_SCAN permission
                if (!PermissionHelper.hasBluetoothScanPermission(this)) {
                    logErrorAndNotifyUser("Bluetooth permission required", 
                        "Bluetooth scan permission is required to stop scanning", null);
                    // Still update UI state even if permission denied
                    viewModel.stopScanning();
                    break;
                }
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
            // Check BLUETOOTH_SCAN permission before starting discovery
            if (!PermissionHelper.hasBluetoothScanPermission(this)) {
                logErrorAndNotifyUser("Bluetooth permission required", 
                    "Bluetooth scan permission is required to discover devices", null);
                // Still check all requirements to request permissions if needed
                checkAllRequirements();
                return;
            }
            viewModel.startBluetoothDiscovery();
        } else {
            logErrorAndNotifyUser("Failed to enable Bluetooth", "Bluetooth is required for device connection", null);
            viewModel.checkBluetoothState(); // Refresh UI
        }
    }

    /**
     * Handles the result of permission requests.
     * When BLUETOOTH_CONNECT permission is granted and we're in ENABLING_BLUETOOTH state,
     * proceeds with enabling Bluetooth. Otherwise, just acknowledges the permission result.
     */
    private void onPermissionResult(Map<String, Boolean> result) {
        Boolean connectGranted = result.get(Manifest.permission.BLUETOOTH_CONNECT);
        
        // Only proceed with enabling Bluetooth if we're in ENABLING_BLUETOOTH state
        // (permission was requested specifically for enabling Bluetooth)
        if (currentState == BtSettingsState.ENABLING_BLUETOOTH) {
            if (connectGranted != null && connectGranted) {
                // Permission granted, proceed with enabling Bluetooth
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetoothLauncher.launch(enableBTIntent);
            } else {
                // Permission denied
                logErrorAndNotifyUser("Bluetooth permission denied", 
                    "Bluetooth permission is required to enable Bluetooth", null);
                viewModel.checkBluetoothState(); // Reset to BLUETOOTH_OFF
            }
        }
        // If we're not in ENABLING_BLUETOOTH state, permissions were likely requested for scanning
        // In that case, user can click scan again after granting permissions - no action needed here
    }

    /**
     * Checks if any of the required permissions are missing and requests them if needed.
     * Uses modern ActivityResultLauncher API instead of deprecated requestPermissions().
     * @param perms Array of permission strings to check
     * @return true if permissions were requested (and need to wait for result), false if all granted
     */
    private boolean doesNeedRequestPermission(String[] perms) {
        // Collect permissions that are not granted
        java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();
        for (String perm : perms) {
            if (this.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            // Request missing permissions using modern API
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
            return true;
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
     * 
     * Note: When permissions are requested, this returns `true` to indicate that
     * the operation should wait for permission result before proceeding.
     */
    private boolean checkAllRequirements() {
        String[] requiredPermissions = getRequiredPermissions();

        // Check and request permissions if needed (uses modern ActivityResultLauncher)
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
