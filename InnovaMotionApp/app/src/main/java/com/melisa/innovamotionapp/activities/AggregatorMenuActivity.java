package com.melisa.innovamotionapp.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.databinding.ActivityAggregatorMenuBinding;
import com.melisa.innovamotionapp.utils.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Aggregator Menu Activity.
 * 
 * Provides a simple menu screen for aggregator users with two navigation options:
 * - Bluetooth Settings: Always enabled, for configuring BT connection
 * - Dashboard: Enabled only when local sensor data exists
 * 
 * This activity serves as the main hub for aggregator users after login,
 * routing them to either BT setup or the dashboard for monitoring.
 */
public class AggregatorMenuActivity extends BaseActivity {

    private ActivityAggregatorMenuBinding binding;
    private ReceivedBtDataDao receivedBtDataDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = ActivityAggregatorMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Logger.i(TAG, "AggregatorMenuActivity created");

        // Initialize DAO for data availability check
        receivedBtDataDao = InnovaDatabase.getInstance(this).receivedBtDataDao();

        setupClickListeners();
        
        // Initial dashboard button state (will be updated in onBaseResume)
        updateDashboardButtonState(false);
    }

    /**
     * Sets up click listeners for all interactive elements.
     */
    private void setupClickListeners() {
        // Sign out button
        binding.signOutButton.setOnClickListener(v -> {
            Logger.userAction(TAG, "Sign out button clicked");
            signOut();
        });

        // Bluetooth Settings button
        binding.bluetoothSettingsButton.setOnClickListener(v -> {
            Logger.userAction(TAG, "Bluetooth Settings button clicked");
            navigateToActivity(BtSettingsActivity.class, null);
        });

        // Dashboard button
        binding.dashboardButton.setOnClickListener(v -> {
            Logger.userAction(TAG, "Dashboard button clicked");
            navigateToActivity(AggregatorDashboardActivity.class, null);
        });

        Logger.d(TAG, "Click listeners configured");
    }

    @Override
    protected void onBaseResume() {
        super.onBaseResume();
        Logger.d(TAG, "AggregatorMenuActivity resumed - checking data availability");
        
        // Refresh dashboard button state on resume
        // This handles the case where user returns from BT connection with new data
        checkDataAvailability();
    }

    /**
     * Checks if local sensor data is available and updates the Dashboard button state.
     * Runs on a background thread to avoid blocking the UI.
     */
    private void checkDataAvailability() {
        executor.execute(() -> {
            try {
                int count = receivedBtDataDao.countAll();
                Logger.d(TAG, "Data availability check: count=" + count);
                
                runOnUiThread(() -> {
                    boolean hasData = count > 0;
                    updateDashboardButtonState(hasData);
                    Logger.i(TAG, "Dashboard button enabled: " + hasData + " (count=" + count + ")");
                });
            } catch (Exception e) {
                Logger.e(TAG, "Error checking data availability", e);
                runOnUiThread(() -> updateDashboardButtonState(false));
            }
        });
    }

    /**
     * Updates the Dashboard button enabled state and hint visibility.
     * 
     * @param hasData true if sensor data is available, false otherwise
     */
    private void updateDashboardButtonState(boolean hasData) {
        binding.dashboardButton.setEnabled(hasData);
        binding.dashboardHint.setVisibility(hasData ? View.GONE : View.VISIBLE);
        
        // Update button alpha for visual feedback when disabled
        binding.dashboardButton.setAlpha(hasData ? 1.0f : 0.5f);
    }

    @Override
    protected void onBaseDestroy() {
        super.onBaseDestroy();
        
        // Clean up executor to prevent memory leaks
        if (!executor.isShutdown()) {
            executor.shutdown();
            Logger.d(TAG, "Executor shutdown");
        }
        
        binding = null;
        Logger.d(TAG, "AggregatorMenuActivity destroyed");
    }
}
