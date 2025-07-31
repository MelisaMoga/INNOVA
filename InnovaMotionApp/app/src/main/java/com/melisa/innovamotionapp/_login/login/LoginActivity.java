package com.melisa.innovamotionapp._login.login;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.activities.MainActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private FirebaseFirestore db;

    // UI Elements
    private MaterialButton googleSignInButton;
    private MaterialButton signOutButton;
    private MaterialButton proceedButton;
    private View signedInSection;
    private TextInputEditText supervisedEmailInput;
    private TextInputLayout supervisedEmailLayout;
    private RadioGroup roleGroup;
    private RadioButton roleSupervisor;
    private RadioButton roleSupervised;
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
        proceedButton = findViewById(R.id.proceed_button);
        supervisedEmailInput = findViewById(R.id.supervised_email);
        supervisedEmailLayout = findViewById(R.id.supervised_email_layout);
        roleGroup = findViewById(R.id.role_group);
        roleSupervisor = findViewById(R.id.role_supervisor);
        roleSupervised = findViewById(R.id.role_supervised);
        signedInAsText = findViewById(R.id.signed_in_as_text);
        signedInSection = findViewById(R.id.signed_in_section);
        loadingProgress = findViewById(R.id.loading);
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        signOutButton.setOnClickListener(v -> signOut());
        proceedButton.setOnClickListener(v -> onProceed());
        
        // Setup role selection listener
        if (roleGroup != null) {
            roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.role_supervisor) {
                    Log.d(TAG, "Supervisor role selected - showing email input");
                    supervisedEmailLayout.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.role_supervised) {
                    Log.d(TAG, "Supervised role selected - hiding email input");
                    supervisedEmailLayout.setVisibility(View.GONE);
                    supervisedEmailLayout.setError(null);
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - Checking current user authentication state");
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already authenticated: " + currentUser.getEmail());
            checkUserInFirestore(currentUser);
        } else {
            Log.d(TAG, "No authenticated user found");
            showSignInUI();
        }
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
                        String role = document.getString("role");
                        if (role != null && !role.isEmpty()) {
                            Log.d(TAG, "User found with role: " + role);
                            navigateToMainActivity();
                        } else {
                            Log.d(TAG, "User exists but no role set - showing role selection");
                            showRoleSelectionUI(user);
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

    private void createUserProfile(FirebaseUser user) {
        Log.d(TAG, "Creating user profile for: " + user.getEmail());
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("displayName", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
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
        Log.d(TAG, "Showing role selection UI for: " + user.getEmail());
        runOnUiThread(() -> {
            hideLoading();
            googleSignInButton.setVisibility(View.GONE);
            signedInSection.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.VISIBLE);
            
            signedInAsText.setText("Welcome " + user.getDisplayName() + "!\nPlease select your role:");
            
            // Set default selection
            if (roleSupervised != null) {
                roleSupervised.setChecked(true);
            }
            supervisedEmailLayout.setVisibility(View.GONE);
        });
    }

    private void showSignInUI() {
        Log.d(TAG, "Showing sign-in UI");
        googleSignInButton.setVisibility(View.VISIBLE);
        signedInSection.setVisibility(View.GONE);
        signOutButton.setVisibility(View.GONE);
        hideLoading();
    }

    private void onProceed() {
        Log.d(TAG, "Proceed button clicked");
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found during proceed");
            showErrorToast("Authentication required");
            return;
        }

        int selectedId = roleGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            showErrorToast("Please select a role");
            return;
        }

        showLoading("Saving your role...");
        proceedButton.setEnabled(false);

        String role = (selectedId == R.id.role_supervisor) ? "supervisor" : "supervised";
        String supervisedEmail = null;

        if (selectedId == R.id.role_supervisor) {
            supervisedEmail = supervisedEmailInput.getText() != null ? 
                    supervisedEmailInput.getText().toString().trim() : "";
            
            if (supervisedEmail.isEmpty()) {
                supervisedEmailLayout.setError("Supervised email is required");
                supervisedEmailLayout.requestFocus();
                hideLoading();
                proceedButton.setEnabled(true);
                return;
            }
            
            if (!isValidGmail(supervisedEmail)) {
                supervisedEmailLayout.setError("Please enter a valid Gmail address");
                supervisedEmailLayout.requestFocus();
                hideLoading();
                proceedButton.setEnabled(true);
                return;
            }
            
            supervisedEmailLayout.setError(null);
        }

        saveUserRole(currentUser, role, supervisedEmail);
    }

    private void saveUserRole(FirebaseUser user, String role, String supervisedEmail) {
        Log.d(TAG, "Saving role '" + role + "' for user: " + user.getUid() + 
               (supervisedEmail != null ? ", supervised email: " + supervisedEmail : ""));

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", role);
        updates.put("lastSignIn", System.currentTimeMillis());
        if (supervisedEmail != null) {
            updates.put("supervisedEmail", supervisedEmail);
        }

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User role saved successfully");
                    showSuccessToast("Role set successfully!");
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user role", e);
                    runOnUiThread(() -> {
                        hideLoading();
                        proceedButton.setEnabled(true);
                        handleFirestoreError(e);
                    });
                });
    }

    private void signOut() {
        Log.d(TAG, "Starting sign-out process");
        showLoading("Signing out...");
        
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

    private void navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
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

    private boolean isValidGmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@gmail\\.com$");
    }

    private void showLoading(String message) {
        Log.d(TAG, "Showing loading: " + message);
        runOnUiThread(() -> {
            loadingProgress.setVisibility(View.VISIBLE);
            googleSignInButton.setEnabled(false);
            if (proceedButton != null) {
                proceedButton.setEnabled(false);
            }
        });
    }

    private void hideLoading() {
        Log.d(TAG, "Hiding loading");
        runOnUiThread(() -> {
            loadingProgress.setVisibility(View.GONE);
            googleSignInButton.setEnabled(true);
            if (proceedButton != null) {
                proceedButton.setEnabled(true);
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