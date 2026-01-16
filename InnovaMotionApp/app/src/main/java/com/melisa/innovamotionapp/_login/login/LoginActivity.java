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
import com.melisa.innovamotionapp.activities.AggregatorMenuActivity;
import com.melisa.innovamotionapp.activities.SupervisorDashboardActivity;
import com.melisa.innovamotionapp.sync.FirestoreSyncService;
import com.melisa.innovamotionapp.sync.SessionGate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

/**
 * LoginActivity with online-only sign-in flow that pre-fills user preferences:
 * 
 * - For SUPERVISORS: Pre-fills the aggregator email field from server data
 * - For AGGREGATOR accounts: Pre-fills the radio button selection from server data  
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
    private TextInputEditText aggregatorEmailInput;
    private TextInputLayout aggregatorEmailLayout;
    private RadioGroup roleGroup;
    private RadioButton roleSupervisor;
    private RadioButton roleAggregator;
    private TextView signedInAsText;
    private View loadingProgress;

    // Autocomplete functionality
    private PopupWindow suggestionPopup;
    private ListView suggestionListView;
    private ArrayAdapter<String> emailAdapter;
    private List<String> aggregatorEmails;
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
        aggregatorEmailInput = findViewById(R.id.aggregator_email);
        aggregatorEmailLayout = findViewById(R.id.aggregator_email_layout);
        roleGroup = findViewById(R.id.role_group);
        roleSupervisor = findViewById(R.id.role_supervisor);
        roleAggregator = findViewById(R.id.role_aggregator);
        signedInAsText = findViewById(R.id.signed_in_as_text);
        signedInSection = findViewById(R.id.signed_in_section);
        loadingProgress = findViewById(R.id.loading);

        setupAutocomplete();
    }

    private void setupAutocomplete() {
        Log.d(TAG, "Setting up email autocomplete functionality");
        
        // Initialize autocomplete components
        aggregatorEmails = new ArrayList<>();
        emailAdapter = new ArrayAdapter<>(this, R.layout.autocomplete_item, aggregatorEmails);
        searchHandler = new Handler(Looper.getMainLooper());
        
        if (aggregatorEmailInput != null) {
            createSuggestionPopup();
            
            // Add text change listener for real-time search
            aggregatorEmailInput.addTextChangedListener(new TextWatcher() {
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
                        searchRunnable = () -> searchAggregatorUsers(s.toString());
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
            aggregatorEmailInput.setOnFocusChangeListener((v, hasFocus) -> {
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
            String selectedEmail = aggregatorEmails.get(position);
            Log.d(TAG, "Selected aggregator email from suggestions: " + selectedEmail);
            
            // Set flag to prevent triggering search when setting text programmatically
            isSettingTextProgrammatically = true;
            aggregatorEmailInput.setText(selectedEmail);
            aggregatorEmailInput.setSelection(selectedEmail.length());
            isSettingTextProgrammatically = false;
            
            aggregatorEmailLayout.setError(null);
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
        // Note: Supervisor no longer needs to enter aggregator email - sensors are pre-assigned in Firestore
        if (roleGroup != null) {
            roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.role_supervisor) {
                    Log.d(TAG, "Supervisor role selected - no email input needed (sensors pre-assigned)");
                    aggregatorEmailLayout.setVisibility(View.GONE);
                    aggregatorEmailLayout.setError(null);
                } else if (checkedId == R.id.role_aggregator) {
                    Log.d(TAG, "Aggregator role selected - hiding email input");
                    aggregatorEmailLayout.setVisibility(View.GONE);
                    aggregatorEmailLayout.setError(null);
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
                        String aggregatorEmail = document.getString("aggregatorEmail");
                        String lastSelectedRole = document.getString("lastSelectedRole");
                        
                        Log.d(TAG, "Retrieved preferences - role: " + role + 
                               ", aggregatorEmail: " + aggregatorEmail + 
                               ", lastSelectedRole: " + lastSelectedRole);
                        
                        runOnUiThread(() -> {
                            hideLoading();
                            
                            // Pre-fill based on user type
                            // Note: Supervisors no longer need email input - sensors are pre-assigned in Firestore
                            if ("supervisor".equals(role)) {
                                roleSupervisor.setChecked(true);
                                aggregatorEmailLayout.setVisibility(View.GONE);
                                Log.d(TAG, "Pre-filled supervisor role (sensors pre-assigned in Firestore)");
                            } else if ("aggregator".equals(role)) {
                                // Pre-fill role selection for aggregator accounts
                                roleAggregator.setChecked(true);
                                aggregatorEmailLayout.setVisibility(View.GONE);
                                Log.d(TAG, "Pre-filled aggregator role selection");
                            } else if (lastSelectedRole != null) {
                                // Pre-fill last selected role for users without a set role
                                if ("supervisor".equals(lastSelectedRole)) {
                                    roleSupervisor.setChecked(true);
                                    aggregatorEmailLayout.setVisibility(View.GONE);
                                } else {
                                    roleAggregator.setChecked(true);
                                    aggregatorEmailLayout.setVisibility(View.GONE);
                                }
                                Log.d(TAG, "Pre-filled last selected role: " + lastSelectedRole);
                            } else {
                                // Default to aggregator if no preferences found
                                roleAggregator.setChecked(true);
                                aggregatorEmailLayout.setVisibility(View.GONE);
                                Log.d(TAG, "No preferences found, defaulting to aggregator role");
                            }
                        });
                    } else {
                        Log.d(TAG, "No user document found, using defaults");
                        runOnUiThread(() -> {
                            hideLoading();
                            roleAggregator.setChecked(true);
                            aggregatorEmailLayout.setVisibility(View.GONE);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error fetching user preferences: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        hideLoading();
                        // Continue with defaults if fetch fails
                        roleAggregator.setChecked(true);
                        aggregatorEmailLayout.setVisibility(View.GONE);
                        showErrorToast(getString(R.string.preferences_load_failed));
                    });
                });
    }

    private void preFillAggregatorEmail(String email) {
        if (aggregatorEmailInput != null && email != null && !email.isEmpty()) {
            // Set flag to prevent triggering search when setting text programmatically
            isSettingTextProgrammatically = true;
            aggregatorEmailInput.setText(email);
            aggregatorEmailInput.setSelection(email.length());
            isSettingTextProgrammatically = false;
            aggregatorEmailLayout.setError(null);
            Log.d(TAG, "Pre-filled aggregator email field with: " + email);
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

        String role = (selectedId == R.id.role_supervisor) ? "supervisor" : "aggregator";
        
        // Supervisor no longer requires email input - sensors are pre-assigned in Firestore by aggregator
        // Just save the role and proceed
        saveUserRole(currentUser, role, null);
    }

    private void saveUserRole(FirebaseUser user, String role, String aggregatorEmail) {
        Log.d(TAG, "Saving role '" + role + "' for user: " + user.getUid() + 
               (aggregatorEmail != null ? ", aggregator email: " + aggregatorEmail : ""));

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", role);
        updates.put("lastSelectedRole", role); // Save for future pre-filling
        updates.put("lastSignIn", System.currentTimeMillis());
        if (aggregatorEmail != null && !aggregatorEmail.isEmpty()) {
            updates.put("aggregatorEmail", aggregatorEmail);
        }

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User role and preferences saved to Firestore");
                    
                    // Show loading while we reload session
                    showLoading("Loading your account...");
                    
                    // NOW reload session and trigger bootstrap with confirmed role
                    SessionGate.getInstance(this).reloadSessionAndBootstrap(
                        new SessionGate.SessionReadyCallback() {
                            @Override
                            public void onSessionReady(String userId, String confirmedRole, List<String> supervisedUserIds) {
                                Log.i(TAG, "✅ Session ready with confirmed role: " + confirmedRole);
                                runOnUiThread(() -> {
                                    hideLoading();
                                    showSuccessToast("Role set successfully!");
                                    navigateBasedOnRole(confirmedRole);
                                });
                            }
                            
                            @Override
                            public void onSessionError(String error) {
                                Log.e(TAG, "⚠️ Session reload failed: " + error);
                                runOnUiThread(() -> {
                                    hideLoading();
                                    showErrorToast("Session error: " + error);
                                    // Proceed anyway using the role that was being saved
                                    navigateBasedOnRole(role);
                                });
                            }
                        }
                    );
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

    private void searchAggregatorUsers(String query) {
        if (query.trim().isEmpty() || query.length() < 2) {
            return;
        }

        Log.d(TAG, "Searching for aggregator users matching: '" + query + "'");
        
        // Search for users with role 'aggregator' and email containing the query
        db.collection("users")
                .whereEqualTo("role", "aggregator")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> matchingEmails = new ArrayList<>();
                    String lowerQuery = query.toLowerCase();
                    
                    queryDocumentSnapshots.forEach(document -> {
                        String email = document.getString("email");
                        if (email != null && email.toLowerCase().contains(lowerQuery)) {
                            matchingEmails.add(email);
                            Log.d(TAG, "Found matching aggregator user: " + email);
                        }
                    });
                    
                    Log.d(TAG, "Search completed: " + matchingEmails.size() + " aggregator users found matching '" + query + "'");
                    
                    runOnUiThread(() -> {
                        aggregatorEmails.clear();
                        aggregatorEmails.addAll(matchingEmails);
                        emailAdapter.notifyDataSetChanged();
                        
                        if (!matchingEmails.isEmpty() && aggregatorEmailInput.hasFocus()) {
                            Log.d(TAG, "Showing popup with " + matchingEmails.size() + " suggestions");
                            showSuggestionPopup();
                        } else if (matchingEmails.isEmpty()) {
                            Log.d(TAG, "No aggregator users found matching '" + query + "' - user can enter manually");
                            hideSuggestionPopup();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error searching aggregator users: " + e.getMessage(), e);
                    // Don't show error to user, just continue with manual entry
                    // This allows the flow to continue even if database is unavailable
                });
    }

    private void showSuggestionPopup() {
        if (suggestionPopup != null && !suggestionPopup.isShowing() && aggregatorEmailInput != null) {
            try {
                // Calculate popup width to match the TextInputLayout
                int width = aggregatorEmailLayout != null ? aggregatorEmailLayout.getWidth() : aggregatorEmailInput.getWidth();
                suggestionPopup.setWidth(width);
                
                // Show popup below the input field
                suggestionPopup.showAsDropDown(aggregatorEmailInput, 0, 0);
                Log.d(TAG, "Suggestion popup shown with " + aggregatorEmails.size() + " items");
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