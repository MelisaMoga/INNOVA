package com.melisa.innovamotionapp.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.melisa.innovamotionapp.databinding.MainActivityBinding;
import com.melisa.innovamotionapp.utils.Logger;


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
        navigateToActivity(BtSettingsActivity.class, null);
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