package com.melisa.innovamotionapp._login.login;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.activities.AggregatorMenuActivity;
import com.melisa.innovamotionapp.activities.RoleSelectionActivity;
import com.melisa.innovamotionapp.activities.SupervisorDashboardActivity;
import com.melisa.innovamotionapp.data.models.UserProfile;
import com.melisa.innovamotionapp.sync.FirestoreSyncService;
import com.melisa.innovamotionapp.sync.SessionGate;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

/**
 * LoginActivity handles Google Sign-In authentication.
 * 
 * After successful authentication, users are directed to RoleSelectionActivity
 * to choose between Collector (Aggregator) or Supervisor roles.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private FirebaseFirestore db;

    // UI Elements
    private MaterialButton googleSignInButton;
    private MaterialButton signOutButton;
    private View signedInSection;
    private TextView signedInAsText;
    private View loadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "LoginActivity onCreate - Initializing components");
        
        initializeFirebase();
        initializeCredentialManager();
        initializeViews();
        setupClickListeners();
    }

    private void initializeFirebase() {
        Log.d(TAG, "Initializing Firebase Auth and Firestore");
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initializeCredentialManager() {
        try {
            credentialManager = CredentialManager.create(this);
            Log.d(TAG, "CredentialManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize CredentialManager", e);
            showErrorToast("Error initializing authentication services");
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing UI components");
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        signOutButton = findViewById(R.id.sign_out_button);
        signedInAsText = findViewById(R.id.signed_in_as_text);
        signedInSection = findViewById(R.id.signed_in_section);
        loadingProgress = findViewById(R.id.loading);
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        signOutButton.setOnClickListener(v -> signOut());
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - Checking current user authentication state");
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already authenticated: " + currentUser.getEmail());
            
            // Check for user switch and clear local data if needed
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String lastUid = prefs.getString("last_logged_in_uid", null);
            
            if (lastUid != null && !lastUid.equals(currentUser.getUid())) {
                // User switch detected - must clear local data BEFORE proceeding
                // to prevent race conditions between clear and backfill
                Log.i(TAG, "User switched from " + lastUid + " to " + currentUser.getUid() + " - clearing local DB");
                showLoading("Switching accounts...");
                
                FirestoreSyncService.getInstance(this).clearLocalData(() -> {
                    // Callback runs on main thread after clear completes
                    Log.i(TAG, "Local DB cleared, now proceeding with new user");
                    prefs.edit().putString("last_logged_in_uid", currentUser.getUid()).apply();
                    checkUserInFirestore(currentUser);
                });
            } else {
                // No user switch - proceed immediately
                prefs.edit().putString("last_logged_in_uid", currentUser.getUid()).apply();
                checkUserInFirestore(currentUser);
            }
        } else {
            Log.d(TAG, "No authenticated user found");
            showSignInUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy - Cleaning up resources");
    }

    private void signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-In process");
        showLoading("Signing in...");

        if (credentialManager == null) {
            Log.e(TAG, "CredentialManager is null");
            showErrorToast("Authentication service not available");
            hideLoading();
            return;
        }

        String webClientId = getString(R.string.default_web_client_id);
        if (webClientId.startsWith("YOUR_")) {
            Log.e(TAG, "Web Client ID not configured");
            showErrorToast("Please configure your Web Client ID in strings.xml");
            hideLoading();
            return;
        }

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Log.d(TAG, "Credential request successful");
                        handleSignInResult(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Sign-in failed: " + e.getLocalizedMessage(), e);
                        runOnUiThread(() -> {
                            hideLoading();
                            String errorMsg = e.getLocalizedMessage();
                            if (errorMsg != null && errorMsg.contains("Unknown calling package")) {
                                showErrorToast("Google Play Services issue detected");
                            } else {
                                showErrorToast("Sign-in failed: " + errorMsg);
                            }
                        });
                    }
                }
        );
    }

    private void handleSignInResult(Credential credential) {
        if (credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            
            try {
                GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(
                        ((CustomCredential) credential).getData());
                authenticateWithFirebase(googleCredential.getIdToken());
            } catch (Exception e) {
                Log.e(TAG, "Invalid Google ID token", e);
                runOnUiThread(() -> {
                        hideLoading();
                    showErrorToast("Invalid Google ID token");
                });
            }
        } else {
            Log.w(TAG, "Invalid credential type");
            runOnUiThread(() -> {
                    hideLoading();
                showErrorToast("Invalid credential type");
            });
        }
    }

    private void authenticateWithFirebase(String idToken) {
        Log.d(TAG, "Authenticating with Firebase using Google token");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase authentication successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            showSuccessToast("Welcome " + user.getDisplayName() + "!");
                            checkUserInFirestore(user);
                        }
                    } else {
                        Log.w(TAG, "Firebase authentication failed", task.getException());
                        hideLoading();
                        showErrorToast("Authentication failed");
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser user) {
        Log.d(TAG, "Checking user data in Firestore for UID: " + user.getUid());
        showLoading("Loading profile...");
        
        DocumentReference userRef = db.collection("users").document(user.getUid());
        userRef.get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Parse user profile with new roles array support
                        UserProfile profile = UserProfile.fromDocument(document);
                        List<String> roles = profile != null ? profile.getRoles() : new ArrayList<>();
                        
                        // Check for LOCAL role preference (per-device, not Firestore)
                        String localRolePref = GlobalData.getInstance().userDeviceSettingsStorage
                                .getLastSelectedRole(user.getUid());
                        Log.d(TAG, "User found with roles: " + roles + ", localRolePref: " + localRolePref);
                        
                        // Check if user has confirmed roles
                        if (!roles.isEmpty()) {
                            // Check if local role preference exists and is valid
                            if (localRolePref != null && roles.contains(localRolePref)) {
                                // Auto-redirect: user has a saved local role preference
                                Log.d(TAG, "Auto-redirecting to " + localRolePref + " (local preference)");
                                autoRedirectWithRole(localRolePref);
                            } else {
                                // No local preference or invalid - show role selection
                                Log.d(TAG, "No valid local role preference, showing RoleSelectionActivity");
                                hideLoading();
                                navigateToRoleSelection();
                            }
                        } else {
                            // No roles yet - check legacy 'role' field
                            String legacyRole = document.getString("role");
                            if (legacyRole != null && !legacyRole.isEmpty()) {
                                // User has legacy role - show role selection to migrate
                                Log.d(TAG, "Found legacy role field: " + legacyRole + ", showing RoleSelectionActivity");
                                hideLoading();
                                navigateToRoleSelection();
                            } else {
                                // No role yet - show initial role selection UI
                                showRoleSelectionUI(user);
                            }
                        }
                    } else {
                        Log.d(TAG, "User not found in Firestore - creating profile");
                        createUserProfile(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user in Firestore", e);
                    handleFirestoreError(e);
                });
    }
    
    /**
     * Auto-redirect to the appropriate dashboard based on the saved role preference.
     * Sets up GlobalData and runs SessionGate bootstrap before navigating.
     * 
     * @param role The saved role ("aggregator" or "supervisor")
     */
    private void autoRedirectWithRole(String role) {
        showLoading("Restoring session...");
        
        // Set the role in GlobalData BEFORE bootstrap
        GlobalData.getInstance().currentUserRole = role;
        Log.d(TAG, "Set GlobalData.currentUserRole to: " + role);
        
        // Run SessionGate bootstrap to initialize sync services
        SessionGate.getInstance(this).reloadSessionAndBootstrap(
            new SessionGate.SessionReadyCallback() {
                @Override
                public void onSessionReady(String userId, String sessionRole, List<String> supervisedUserIds) {
                    Log.i(TAG, "Session ready, navigating to dashboard for role: " + role);
                    hideLoading();
                    navigateBasedOnRole(role);
                }
                
                @Override
                public void onSessionError(String error) {
                    Log.w(TAG, "Session error during auto-redirect: " + error + ", proceeding anyway");
                    hideLoading();
                    navigateBasedOnRole(role);
                }
            }
        );
    }
    
    /**
     * Navigate to RoleSelectionActivity for users with both roles.
     */
    private void navigateToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void createUserProfile(FirebaseUser user) {
        Log.d(TAG, "Creating user profile for: " + user.getEmail());
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("displayName", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        userData.put("roles", new ArrayList<>()); // Empty roles array - will be set after role selection
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("lastSignIn", System.currentTimeMillis());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created successfully");
                    showRoleSelectionUI(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user profile", e);
                    handleFirestoreError(e);
                });
    }

    private void showRoleSelectionUI(FirebaseUser user) {
        Log.d(TAG, "New user - navigating to role selection for: " + user.getEmail());
        hideLoading();
        navigateToRoleSelection();
    }

    private void showSignInUI() {
        Log.d(TAG, "Showing sign-in UI");
        googleSignInButton.setVisibility(View.VISIBLE);
        signedInSection.setVisibility(View.GONE);
        signOutButton.setVisibility(View.GONE);
        hideLoading();
    }

    private void signOut() {
        Log.d(TAG, "Starting sign-out process");
        showLoading("Signing out...");
        
        // Clear local role preference BEFORE signing out
        // This ensures role selection appears on next login
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            GlobalData.getInstance().userDeviceSettingsStorage
                    .clearLastSelectedRole(currentUser.getUid());
            Log.d(TAG, "Cleared local role preference for user: " + currentUser.getUid());
        }
        
        completeSignOut();
    }
    
    /**
     * Completes the sign-out process.
     */
    private void completeSignOut() {
        // Reset GlobalData session state
        GlobalData.getInstance().resetSessionData();
        
        mAuth.signOut();

        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(@NonNull Void result) {
                        Log.d(TAG, "Credentials cleared successfully");
                        runOnUiThread(() -> {
                            showSignInUI();
                            showSuccessToast("Signed out successfully");
                        });
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.w(TAG, "Error clearing credentials: " + e.getLocalizedMessage());
                        runOnUiThread(() -> {
                            showSignInUI();
                            showSuccessToast("Sign-out completed");
                        });
                    }
                });
    }

    /**
     * Navigate to the appropriate activity based on user role.
     * Supervisors go directly to SupervisorDashboardActivity.
     * Aggregators go directly to AggregatorMenuActivity.
     * 
     * @param role The confirmed user role ("supervisor" or "aggregator")
     */
    private void navigateBasedOnRole(String role) {
        Log.d(TAG, "Navigating based on role: " + role);
        Intent intent;
        
        if ("supervisor".equals(role)) {
            intent = new Intent(this, SupervisorDashboardActivity.class);
            Log.d(TAG, "Routing supervisor to SupervisorDashboardActivity");
        } else {
            // Aggregator (default)
            intent = new Intent(this, AggregatorMenuActivity.class);
            Log.d(TAG, "Routing aggregator to AggregatorMenuActivity");
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleFirestoreError(Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
        
        if (errorMessage.contains("PERMISSION_DENIED")) {
            Log.w(TAG, "Permission denied - signing out user");
            showErrorToast("Permission denied. Please sign in again.");
            signOut();
        } else {
            Log.e(TAG, "Firestore error: " + errorMessage);
            showErrorToast("Database error: " + errorMessage);
        }
    }

    private void showLoading(String message) {
        Log.d(TAG, "Showing loading: " + message);
        runOnUiThread(() -> {
            loadingProgress.setVisibility(View.VISIBLE);
            googleSignInButton.setEnabled(false);
            if (signOutButton != null) {
                signOutButton.setEnabled(false);
            }
        });
    }

    private void hideLoading() {
        Log.d(TAG, "Hiding loading");
        runOnUiThread(() -> {
            loadingProgress.setVisibility(View.GONE);
            googleSignInButton.setEnabled(true);
            if (signOutButton != null) {
                signOutButton.setEnabled(true);
            }
        });
    }

    private void showSuccessToast(String message) {
        Log.i(TAG, "Success: " + message);
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void showErrorToast(String message) {
        Log.e(TAG, "Error: " + message);
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
}