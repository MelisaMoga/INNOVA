package com.melisa.innovamotionapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.button.MaterialButton;
import com.melisa.innovamotionapp._login.login.LoginActivity;
import com.melisa.innovamotionapp.databinding.MainActivityBinding;
import com.melisa.innovamotionapp.utils.GlobalData;
import android.util.Log;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MainActivityBinding binding;
    private MaterialButton signOutButton;

    private final GlobalData globalData = GlobalData.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize sign out button
        signOutButton = findViewById(com.melisa.innovamotionapp.R.id.sign_out_button);
        signOutButton.setOnClickListener(v -> signOut());

        Log.d(TAG, "MainActivity created successfully");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Require authentication
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
    }

    public void LunchMoniotring(View view) {
        Intent i = new Intent(this, BtSettingsActivity.class);
        startActivity(i);
    }

    public void LaunchStatistics(View view) {
        Intent i = new Intent(this, StatisticsActivity.class);
        startActivity(i);
    }

    public void LaunchTimelaps(View view) {
        Intent i = new Intent(this, TimeLapseActivity.class);
        startActivity(i);
    }

    public void LaunchEnergyConsumption(View view) {
        Intent i = new Intent(this, EnergyConsumptionActivity.class);
        startActivity(i);
    }

    /**
     * Signs out the current user and navigates back to login screen
     */
    private void signOut() {
        Log.d(TAG, "Sign out requested");
        
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Clear any global data
        if (globalData != null) {
            globalData.currentUserRole = null;
            globalData.currentUserUid = null;
        }
        
        // Show confirmation
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login activity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Log.d(TAG, "User signed out and redirected to login");
    }
}