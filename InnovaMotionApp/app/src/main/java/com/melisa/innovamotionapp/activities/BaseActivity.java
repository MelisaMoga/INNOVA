package com.melisa.innovamotionapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp._login.login.LoginActivity;
import com.melisa.innovamotionapp.ui.dialogs.DeveloperPanelDialog;
import com.melisa.innovamotionapp.utils.DevShakeDetector;
import com.melisa.innovamotionapp.utils.FeatureFlags;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.Logger;

/**
 * Base activity class that provides common functionality for all activities in the app.
 * This class implements DRY principles by centralizing:
 * - Authentication checks
 * - Sign-out functionality
 * - Common UI operations
 * - Logging setup
 * - Global data access
 * 
 * All activities should extend this class to inherit common functionality.
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    protected final String TAG = getClass().getSimpleName();
    protected GlobalData globalData;
    
    // Developer mode shake detector (only active when DEV_MODE_ENABLED)
    private DevShakeDetector shakeDetector;
    private boolean devModeToastShown = false;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize global data instance
        globalData = GlobalData.getInstance();
        
        // Initialize developer mode shake detector
        initDevMode();
        
        // Log activity creation
        Logger.d(TAG, "Activity created");
        
        // Perform any additional setup
        onBaseCreate(savedInstanceState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Logger.d(TAG, "Activity resumed");
        
        // Check authentication if required
        if (requiresAuthentication()) {
            checkAuthentication();
        }
        
        // Start shake detector for dev mode
        if (shakeDetector != null) {
            shakeDetector.start();
        }
        
        onBaseResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "Activity paused");
        
        // Stop shake detector
        if (shakeDetector != null) {
            shakeDetector.stop();
        }
        
        onBasePause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "Activity destroyed");
        onBaseDestroy();
    }
    
    /**
     * Override this method instead of onCreate to perform activity-specific setup
     * @param savedInstanceState The saved instance state
     */
    protected abstract void onBaseCreate(@Nullable Bundle savedInstanceState);
    
    /**
     * Override this method instead of onResume to perform activity-specific resume logic
     */
    protected void onBaseResume() {
        // Default implementation - override if needed
    }
    
    /**
     * Override this method instead of onPause to perform activity-specific pause logic
     */
    protected void onBasePause() {
        // Default implementation - override if needed
    }
    
    /**
     * Override this method instead of onDestroy to perform activity-specific cleanup
     */
    protected void onBaseDestroy() {
        // Default implementation - override if needed
    }
    
    /**
     * Override this method to specify if the activity requires authentication
     * @return true if authentication is required, false otherwise
     */
    protected boolean requiresAuthentication() {
        return true; // Most activities require authentication
    }
    
    /**
     * Checks if the user is authenticated and redirects to login if not
     */
    protected void checkAuthentication() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Logger.w(TAG, "User not authenticated, redirecting to login");
            navigateToLogin();
        }
    }
    
    /**
     * Signs out the current user and navigates back to login screen
     * This method centralizes the sign-out logic to follow DRY principles
     */
    protected void signOut() {
        Logger.userAction(TAG, "Sign out requested");
        
        try {
            // Stop all mirrors and clear data before signing out
            com.melisa.innovamotionapp.sync.FirestoreSyncService.getInstance(this).stopAllMirrors();
            com.melisa.innovamotionapp.sync.FirestoreSyncService.getInstance(this).clearLocalData();
            
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();
            
            // Clear any global data using the new reset method
            if (globalData != null) {
                globalData.resetSessionData();
                Logger.d(TAG, "Global user data cleared");
            }
            
            // Show confirmation
            showToast("Signed out successfully");
            
            // Navigate to login activity
            navigateToLogin();
            
            Logger.i(TAG, "User signed out successfully and redirected to login");
            
        } catch (Exception e) {
            Logger.e(TAG, "Error during sign out", e);
            showToast("Error signing out. Please try again.");
        }
    }
    
    /**
     * Navigates to the login activity and clears the activity stack
     */
    protected void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Navigates to a specific activity with optional extras
     * @param targetActivity The target activity class
     * @param extras Optional bundle of extras to pass
     */
    protected void navigateToActivity(@NonNull Class<?> targetActivity, @Nullable Bundle extras) {
        Logger.userAction(TAG, "Navigating to " + targetActivity.getSimpleName());
        
        Intent intent = new Intent(this, targetActivity);
        if (extras != null) {
            intent.putExtras(extras);
        }
        startActivity(intent);
    }
    
    /**
     * Navigates to a specific activity and finishes current activity
     * @param targetActivity The target activity class
     * @param extras Optional bundle of extras to pass
     */
    protected void navigateToActivityAndFinish(@NonNull Class<?> targetActivity, @Nullable Bundle extras) {
        navigateToActivity(targetActivity, extras);
        finish();
    }
    
    /**
     * Shows a toast message with consistent styling
     * @param message The message to display
     */
    protected void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Shows a long toast message
     * @param message The message to display
     */
    protected void showLongToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Logs a message and shows it as a toast (useful for debugging)
     * This replaces the scattered log() methods in various activities
     * @param message The message to log and display
     */
    protected void logAndToast(@NonNull String message) {
        Logger.d(TAG, message);
        showToast(message);
    }
    
    /**
     * Logs an error and shows user-friendly message
     * @param errorMessage The error message for logging
     * @param userMessage The user-friendly message to display
     * @param throwable Optional exception to log
     */
    protected void logErrorAndNotifyUser(@NonNull String errorMessage, @NonNull String userMessage, @Nullable Throwable throwable) {
        Logger.e(TAG, errorMessage, throwable);
        showToast(userMessage);
    }
    
    // ========== Developer Mode Support ==========
    
    /**
     * Initialize developer mode shake detector.
     * Only active when FeatureFlags.DEV_MODE_ENABLED is true.
     */
    private void initDevMode() {
        if (!FeatureFlags.DEV_MODE_ENABLED) {
            return;
        }
        
        shakeDetector = new DevShakeDetector(this, this::onDevShakeDetected);
    }
    
    /**
     * Called when a shake gesture is detected in developer mode.
     */
    private void onDevShakeDetected() {
        Logger.i(TAG, "Developer mode shake detected");
        
        // Show toast on first activation
        if (!devModeToastShown) {
            showToast(getString(R.string.dev_mode_activated));
            devModeToastShown = true;
        }
        
        showDeveloperPanel();
    }
    
    /**
     * Show the developer panel dialog.
     * Can also be called programmatically for testing.
     */
    protected void showDeveloperPanel() {
        if (!FeatureFlags.DEV_MODE_ENABLED) {
            Logger.w(TAG, "Developer mode is disabled");
            return;
        }
        
        DeveloperPanelDialog dialog = DeveloperPanelDialog.newInstance();
        dialog.show(getSupportFragmentManager(), "DeveloperPanel");
    }
}
