package com.melisa.innovamotionapp.activities;

import android.content.Intent;
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
    }

    public void LaunchMonitoring(View view) {
        Logger.userAction(TAG, "Launch Monitoring clicked");
        
        // Wait for SessionGate to be ready before routing
        SessionGate.getInstance(this).waitForSessionReady(new SessionGate.SessionReadyCallback() {
            @Override
            public void onSessionReady(String userId, String role, List<String> supervisedUserIds) {
                runOnUiThread(() -> {
                    if ("supervisor".equals(role)) {
                        Logger.d(TAG, "Supervisor detected: routing directly to BtConnectedActivity");
                        // Supervisor: skip BtSettingsActivity; go straight to the live screen fed by Room
                        Intent intent = new Intent(MainActivity.this, BtConnectedActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        // do not call finish();
                    } else {
                        Logger.d(TAG, "Supervised user detected: routing to BtSettingsActivity for scanning");
                        // Supervised: normal flow (scan + connect)
                        navigateToActivity(BtSettingsActivity.class, null);
                    }
                });
            }
            
            @Override
            public void onSessionError(String error) {
                Logger.e(TAG, "Session not ready: " + error);
                // Fallback to supervised flow if session not ready
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