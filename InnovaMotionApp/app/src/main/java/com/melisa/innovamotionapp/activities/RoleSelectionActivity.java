package com.melisa.innovamotionapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.sync.SessionGate;
import com.melisa.innovamotionapp.sync.UserSession;
import com.melisa.innovamotionapp.utils.Constants;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.List;

/**
 * Simple activity to let users with BOTH roles choose which mode to use.
 * 
 * Shown after login when the user has both "aggregator" and "supervisor" roles.
 */
public class RoleSelectionActivity extends AppCompatActivity {
    private static final String TAG = "RoleSelectionActivity";
    
    private MaterialButton btnContinueAsCollector;
    private MaterialButton btnContinueAsSupervisor;
    private TextView welcomeText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);
        
        initializeViews();
        setupClickListeners();
        updateWelcomeText();
    }
    
    private void initializeViews() {
        btnContinueAsCollector = findViewById(R.id.btn_continue_as_collector);
        btnContinueAsSupervisor = findViewById(R.id.btn_continue_as_supervisor);
        welcomeText = findViewById(R.id.welcome_text);
    }
    
    private void setupClickListeners() {
        btnContinueAsCollector.setOnClickListener(v -> {
            Log.d(TAG, "User selected Collector (Aggregator) mode");
            navigateToAggregator();
        });
        
        btnContinueAsSupervisor.setOnClickListener(v -> {
            Log.d(TAG, "User selected Supervisor mode");
            navigateToSupervisor();
        });
    }
    
    private void updateWelcomeText() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && welcomeText != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                welcomeText.setText("Welcome, " + name + "!\nChoose how you want to continue:");
            }
        }
    }
    
    private void navigateToAggregator() {
        // Set active role in GlobalData for the session
        GlobalData.getInstance().currentUserRole = Constants.ROLE_AGGREGATOR;
        
        // Start SessionGate bootstrap for aggregator
        SessionGate.getInstance(this).reloadSessionAndBootstrap(
            new SessionGate.SessionReadyCallback() {
                @Override
                public void onSessionReady(String userId, String role, List<String> supervisedUserIds) {
                    Intent intent = new Intent(RoleSelectionActivity.this, AggregatorMenuActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                
                @Override
                public void onSessionError(String error) {
                    Log.w(TAG, "Session error, proceeding anyway: " + error);
                    Intent intent = new Intent(RoleSelectionActivity.this, AggregatorMenuActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        );
    }
    
    private void navigateToSupervisor() {
        // Set active role in GlobalData for the session
        GlobalData.getInstance().currentUserRole = Constants.ROLE_SUPERVISOR;
        
        // Start SessionGate bootstrap for supervisor
        SessionGate.getInstance(this).reloadSessionAndBootstrap(
            new SessionGate.SessionReadyCallback() {
                @Override
                public void onSessionReady(String userId, String role, List<String> supervisedUserIds) {
                    Intent intent = new Intent(RoleSelectionActivity.this, SupervisorDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                
                @Override
                public void onSessionError(String error) {
                    Log.w(TAG, "Session error, proceeding anyway: " + error);
                    Intent intent = new Intent(RoleSelectionActivity.this, SupervisorDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        );
    }
}
