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
import androidx.core.content.ContextCompat;
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
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.LayoutInflater;
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
import java.util.List;
import java.util.ArrayList;

/**
 * LoginActivity with online-only sign-in flow that pre-fills user preferences:
 * 
 * - For SUPERVISORS: Pre-fills the supervised email field from server data
 * - For SUPERVISED accounts: Pre-fills the radio button selection from server data  
 * - Users can edit any pre-filled values before proceeding
 * - All preferences are fetched fresh from server on each sign-in (no local caching)
 */
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

    // Autocomplete functionality
    private PopupWindow suggestionPopup;
    private ListView suggestionListView;
    private ArrayAdapter<String> emailAdapter;
    private List<String> supervisedEmails;
    private Handler searchHandler;
    private Runnable searchRunnable;
    private boolean isSettingTextProgrammatically = false;

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

        setupAutocomplete();
    }

    private void setupAutocomplete() {
        Log.d(TAG, "Setting up email autocomplete functionality");
        
        // Initialize autocomplete components
        supervisedEmails = new ArrayList<>();
        emailAdapter = new ArrayAdapter<>(this, R.layout.autocomplete_item, supervisedEmails);
        searchHandler = new Handler(Looper.getMainLooper());
        
        if (supervisedEmailInput != null) {
            createSuggestionPopup();
            
            // Add text change listener for real-time search
            supervisedEmailInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Skip search if we're setting text programmatically
                    if (isSettingTextProgrammatically) {
                        return;
                    }
                    
                    // Cancel previous search
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    
                    // Schedule new search with delay to avoid too many requests
                    if (s.length() > 1) {
                        searchRunnable = () -> searchSupervisedUsers(s.toString());
                        searchHandler.postDelayed(searchRunnable, 300); // 300ms delay
                    } else {
                        // Hide popup when input is too short
                        hideSuggestionPopup();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
            
            // Hide popup when focus is lost
            supervisedEmailInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    hideSuggestionPopup();
                }
            });
            
            Log.d(TAG, "Autocomplete setup completed");
        }
    }

    private void createSuggestionPopup() {
        // Create ListView for suggestions
        suggestionListView = new ListView(this);
        suggestionListView.setAdapter(emailAdapter);
        suggestionListView.setDividerHeight(1);
        suggestionListView.setDivider(ContextCompat.getDrawable(this, android.R.color.transparent));
        suggestionListView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        suggestionListView.setElevation(8f);
        
        // Handle item clicks
        suggestionListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedEmail = supervisedEmails.get(position);
            Log.d(TAG, "Selected supervised email from suggestions: " + selectedEmail);
            
            // Set flag to prevent triggering search when setting text programmatically
            isSettingTextProgrammatically = true;
            supervisedEmailInput.setText(selectedEmail);
            supervisedEmailInput.setSelection(selectedEmail.length());
            isSettingTextProgrammatically = false;
            
            supervisedEmailLayout.setError(null);
            hideSuggestionPopup();
        });
        
        // Create popup window
        suggestionPopup = new PopupWindow(suggestionListView, 
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT);
        suggestionPopup.setOutsideTouchable(true);
        suggestionPopup.setFocusable(false);
        suggestionPopup.setElevation(8f);
        suggestionPopup.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.drawable.editbox_dropdown_light_frame));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy - Cleaning up resources");
        
        // Clean up search handler
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        
        // Clean up popup
        hideSuggestionPopup();
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
                        Log.d(TAG, "User found with role: " + (role != null ? role : "none"));
                        // Always show role selection UI for online-only flow with pre-filling
                        showRoleSelectionUI(user);
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

    private void fetchAndPreFillUserPreferences(FirebaseUser user) {
        Log.d(TAG, "Fetching user preferences from server for UID: " + user.getUid());
        showLoading(getString(R.string.loading_preferences));
        
        DocumentReference userRef = db.collection("users").document(user.getUid());
        userRef.get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        String supervisedEmail = document.getString("supervisedEmail");
                        String lastSelectedRole = document.getString("lastSelectedRole");
                        
                        Log.d(TAG, "Retrieved preferences - role: " + role + 
                               ", supervisedEmail: " + supervisedEmail + 
                               ", lastSelectedRole: " + lastSelectedRole);
                        
                        runOnUiThread(() -> {
                            hideLoading();
                            
                            // Pre-fill based on user type
                            if ("supervisor".equals(role) && supervisedEmail != null && !supervisedEmail.isEmpty()) {
                                // Pre-fill supervised email for supervisors
                                preFillSupervisedEmail(supervisedEmail);
                                roleSupervisor.setChecked(true);
                                supervisedEmailLayout.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Pre-filled supervised email for supervisor: " + supervisedEmail);
                            } else if ("supervised".equals(role)) {
                                // Pre-fill role selection for supervised accounts
                                roleSupervised.setChecked(true);
                                supervisedEmailLayout.setVisibility(View.GONE);
                                Log.d(TAG, "Pre-filled supervised role selection");
                            } else if (lastSelectedRole != null) {
                                // Pre-fill last selected role for users without a set role
                                if ("supervisor".equals(lastSelectedRole)) {
                                    roleSupervisor.setChecked(true);
                                    supervisedEmailLayout.setVisibility(View.VISIBLE);
                                    if (supervisedEmail != null && !supervisedEmail.isEmpty()) {
                                        preFillSupervisedEmail(supervisedEmail);
                                    }
                                } else {
                                    roleSupervised.setChecked(true);
                                    supervisedEmailLayout.setVisibility(View.GONE);
                                }
                                Log.d(TAG, "Pre-filled last selected role: " + lastSelectedRole);
                            } else {
                                // Default to supervised if no preferences found
                                roleSupervised.setChecked(true);
                                supervisedEmailLayout.setVisibility(View.GONE);
                                Log.d(TAG, "No preferences found, defaulting to supervised role");
                            }
                        });
                    } else {
                        Log.d(TAG, "No user document found, using defaults");
                        runOnUiThread(() -> {
                            hideLoading();
                            roleSupervised.setChecked(true);
                            supervisedEmailLayout.setVisibility(View.GONE);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error fetching user preferences: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        hideLoading();
                        // Continue with defaults if fetch fails
                        roleSupervised.setChecked(true);
                        supervisedEmailLayout.setVisibility(View.GONE);
                        showErrorToast(getString(R.string.preferences_load_failed));
                    });
                });
    }

    private void preFillSupervisedEmail(String email) {
        if (supervisedEmailInput != null && email != null && !email.isEmpty()) {
            // Set flag to prevent triggering search when setting text programmatically
            isSettingTextProgrammatically = true;
            supervisedEmailInput.setText(email);
            supervisedEmailInput.setSelection(email.length());
            isSettingTextProgrammatically = false;
            supervisedEmailLayout.setError(null);
            Log.d(TAG, "Pre-filled supervised email field with: " + email);
        }
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
            googleSignInButton.setVisibility(View.GONE);
            signedInSection.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.VISIBLE);
            
            signedInAsText.setText("Welcome " + user.getDisplayName() + "!\nPlease select your role:");
        });
        
        // Fetch and pre-fill user preferences from server
        fetchAndPreFillUserPreferences(user);
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
        signOutButton.setEnabled(false);

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
                signOutButton.setEnabled(true);
                return;
            }
            
            if (!isValidGmail(supervisedEmail)) {
                supervisedEmailLayout.setError("Please enter a valid Gmail address");
                supervisedEmailLayout.requestFocus();
                hideLoading();
                proceedButton.setEnabled(true);
                signOutButton.setEnabled(true);
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
        updates.put("lastSelectedRole", role); // Save for future pre-filling
        updates.put("lastSignIn", System.currentTimeMillis());
        if (supervisedEmail != null && !supervisedEmail.isEmpty()) {
            updates.put("supervisedEmail", supervisedEmail);
        }

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User role and preferences saved successfully");
                    showSuccessToast("Role set successfully!");
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user role", e);
                    runOnUiThread(() -> {
                        hideLoading();
                        proceedButton.setEnabled(true);
                        signOutButton.setEnabled(true);
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

    private void searchSupervisedUsers(String query) {
        if (query.trim().isEmpty() || query.length() < 2) {
            return;
        }

        Log.d(TAG, "Searching for supervised users matching: '" + query + "'");
        
        // Search for users with role 'supervised' and email containing the query
        db.collection("users")
                .whereEqualTo("role", "supervised")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> matchingEmails = new ArrayList<>();
                    String lowerQuery = query.toLowerCase();
                    
                    queryDocumentSnapshots.forEach(document -> {
                        String email = document.getString("email");
                        if (email != null && email.toLowerCase().contains(lowerQuery)) {
                            matchingEmails.add(email);
                            Log.d(TAG, "Found matching supervised user: " + email);
                        }
                    });
                    
                    Log.d(TAG, "Search completed: " + matchingEmails.size() + " supervised users found matching '" + query + "'");
                    
                    runOnUiThread(() -> {
                        supervisedEmails.clear();
                        supervisedEmails.addAll(matchingEmails);
                        emailAdapter.notifyDataSetChanged();
                        
                        if (!matchingEmails.isEmpty() && supervisedEmailInput.hasFocus()) {
                            Log.d(TAG, "Showing popup with " + matchingEmails.size() + " suggestions");
                            showSuggestionPopup();
                        } else if (matchingEmails.isEmpty()) {
                            Log.d(TAG, "No supervised users found matching '" + query + "' - user can enter manually");
                            hideSuggestionPopup();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error searching supervised users: " + e.getMessage(), e);
                    // Don't show error to user, just continue with manual entry
                    // This allows the flow to continue even if database is unavailable
                });
    }

    private void showSuggestionPopup() {
        if (suggestionPopup != null && !suggestionPopup.isShowing() && supervisedEmailInput != null) {
            try {
                // Calculate popup width to match the TextInputLayout
                int width = supervisedEmailLayout != null ? supervisedEmailLayout.getWidth() : supervisedEmailInput.getWidth();
                suggestionPopup.setWidth(width);
                
                // Show popup below the input field
                suggestionPopup.showAsDropDown(supervisedEmailInput, 0, 0);
                Log.d(TAG, "Suggestion popup shown with " + supervisedEmails.size() + " items");
            } catch (Exception e) {
                Log.w(TAG, "Error showing suggestion popup", e);
            }
        }
    }

    private void hideSuggestionPopup() {
        if (suggestionPopup != null && suggestionPopup.isShowing()) {
            try {
                suggestionPopup.dismiss();
                Log.d(TAG, "Suggestion popup hidden");
            } catch (Exception e) {
                Log.w(TAG, "Error hiding suggestion popup", e);
            }
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
            if (proceedButton != null) {
                proceedButton.setEnabled(true);
            }
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