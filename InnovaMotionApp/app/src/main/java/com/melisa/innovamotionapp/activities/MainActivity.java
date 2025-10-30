package com.melisa.innovamotionapp.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.melisa.innovamotionapp.databinding.MainActivityBinding;
import com.melisa.innovamotionapp.sync.SessionGate;
import com.melisa.innovamotionapp.utils.Logger;
import com.melisa.innovamotionapp.utils.RoleProvider;

import java.util.List;


public class MainActivity extends BaseActivity {

    private MainActivityBinding binding;
    private MaterialButton signOutButton;

    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize sign out button
        signOutButton = findViewById(com.melisa.innovamotionapp.R.id.sign_out_button);
        signOutButton.setOnClickListener(v -> signOut());

        Logger.i(TAG, "MainActivity UI initialized successfully");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    public void LaunchMonitoring(View view) {
        Logger.userAction(TAG, "Launch Monitoring clicked");
        
        // Wait for SessionGate to be ready before routing
        SessionGate.getInstance(this).waitForSessionReady(new SessionGate.SessionReadyCallback() {
            @Override
            public void onSessionReady(String userId, String role, List<String> supervisedUserIds) {
                runOnUiThread(() -> {
                    if ("supervisor".equals(role)) {
                        Logger.d(TAG, "Supervisor detected: routing to supervisor dashboard");
                        // Supervisor: View all children from linked aggregator
                        Intent intent = new Intent(MainActivity.this, SupervisorDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        // do not call finish();
                    } else if ("aggregator".equals(role) || "supervised".equals(role)) {
                        Logger.d(TAG, "Aggregator detected: routing to BT settings for device scanning");
                        // Aggregator (formerly supervised): Scan and connect to BT device
                        // After connection, will route to DataAggregatorActivity
                        navigateToActivity(BtSettingsActivity.class, null);
                    } else {
                        Logger.w(TAG, "Unknown role: " + role + ", defaulting to BT settings");
                        navigateToActivity(BtSettingsActivity.class, null);
                    }
                });
            }
            
            @Override
            public void onSessionError(String error) {
                Logger.e(TAG, "Session not ready: " + error);
                // Fallback to aggregator flow if session not ready
                navigateToActivity(BtSettingsActivity.class, null);
            }
        });
    }

    public void LaunchStatistics(View view) {
        Logger.userAction(TAG, "Launch Statistics clicked");
        navigateToActivity(StatisticsActivity.class, null);
    }

    public void LaunchTimelapse(View view) {
        Logger.userAction(TAG, "Launch Timelapse clicked");
        navigateToActivity(TimeLapseActivity.class, null);
    }

    public void LaunchEnergyConsumption(View view) {
        Logger.userAction(TAG, "Launch Energy Consumption clicked");
        navigateToActivity(EnergyConsumptionActivity.class, null);
    }


}