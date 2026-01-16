package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.melisa.innovamotionapp.data.models.UserProfile;
import com.melisa.innovamotionapp.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages user session information including roles.
 * 
 * Architecture:
 * - Users can have one or both roles: aggregator, supervisor
 * - Aggregator: Collects data from sensors, uploads to Firestore
 * - Supervisor: Monitors sensors assigned via the 'assignments' collection
 * 
 * NOTE: Supervised sensor IDs are now fetched dynamically from the 'assignments'
 * collection, NOT stored on the user profile.
 */
public class UserSession {
    private static final String TAG = "UserSession";
    
    private static UserSession instance;
    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;
    
    // Cached user data
    private String currentUserId;
    private List<String> roles;
    private UserProfile userProfile;
    private boolean isLoaded = false;
    
    // Callback interface for session loading
    public interface SessionLoadCallback {
        void onSessionLoaded(String userId, List<String> roles);
        void onSessionLoadError(String error);
    }
    
    /**
     * Callback for fetching assigned sensor IDs from the 'assignments' collection.
     */
    public interface AssignedSensorsCallback {
        void onSensorsLoaded(List<String> sensorIds);
        void onError(String error);
    }
    
    private UserSession(Context context) {
        this.context = context.getApplicationContext();
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        this.roles = new ArrayList<>();
        
        // Load session data on initialization
        loadUserSession(null);
    }
    
    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context);
        }
        return instance;
    }
    
    /**
     * Load user session data from Firestore
     */
    public void loadUserSession(SessionLoadCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user found");
            if (callback != null) {
                callback.onSessionLoadError("User not authenticated");
            }
            return;
        }
        
        executorService.execute(() -> {
            firestore.collection(Constants.FIRESTORE_COLLECTION_USERS)
                    .document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    loadUserDataFromDocument(user.getUid(), document, callback);
                                } else {
                                    Log.w(TAG, "User document not found in Firestore");
                                    if (callback != null) {
                                        callback.onSessionLoadError("User profile not found");
                                    }
                                }
                            } else {
                                Log.e(TAG, "Failed to load user session", task.getException());
                                if (callback != null) {
                                    callback.onSessionLoadError("Failed to load user data: " + 
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                            }
                        }
                    });
        });
    }
    
    /**
     * Load user data from Firestore document
     */
    private void loadUserDataFromDocument(String userId, DocumentSnapshot document, SessionLoadCallback callback) {
        currentUserId = userId;
        userProfile = UserProfile.fromDocument(document);
        
        if (userProfile != null) {
            roles = userProfile.getRoles();
        } else {
            roles = new ArrayList<>();
        }
        
        Log.d(TAG, "Loaded user roles: " + roles + " for user: " + userId);
        
            isLoaded = true;
            
            if (callback != null) {
            callback.onSessionLoaded(userId, roles);
        }
    }
    
    /**
     * Fetch assigned sensor IDs for the current supervisor from the 'assignments' collection.
     * This is the new way - we query the assignments collection instead of storing on user profile.
     */
    public void fetchAssignedSensorIds(AssignedSensorsCallback callback) {
        if (currentUserId == null) {
            callback.onError("User not loaded");
            return;
        }
        
        firestore.collection(Constants.FIRESTORE_COLLECTION_ASSIGNMENTS)
                .whereEqualTo("supervisorUid", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> sensorIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String sensorId = doc.getString("sensorId");
                        if (sensorId != null) {
                            sensorIds.add(sensorId);
                        }
                    }
                    Log.d(TAG, "Fetched " + sensorIds.size() + " assigned sensors for supervisor " + currentUserId);
                    callback.onSensorsLoaded(sensorIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch assigned sensors", e);
                    callback.onError("Failed to fetch assigned sensors: " + e.getMessage());
                });
    }
    
    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    /**
     * Check if current user is aggregator role (collects data from sensors, uploads to cloud).
     */
    public boolean isAggregator() {
        return hasRole(Constants.ROLE_AGGREGATOR);
    }
    
    /**
     * Check if current user is supervisor role
     */
    public boolean isSupervisor() {
        return hasRole(Constants.ROLE_SUPERVISOR);
    }
    
    /**
     * Check if user has both aggregator and supervisor roles.
     */
    public boolean hasBothRoles() {
        return isAggregator() && isSupervisor();
    }
    
    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * Get all user roles.
     */
    public List<String> getRoles() {
        return roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }
    
    /**
     * Get the first role (for legacy compatibility).
     * Returns null if no roles.
     */
    public String getRole() {
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }
        return null;
    }
    
    /**
     * Get cached user profile.
     */
    public UserProfile getUserProfile() {
        return userProfile;
    }
    
    /**
     * Check if session data is loaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Force reload session data
     */
    public void reloadSession(SessionLoadCallback callback) {
        isLoaded = false;
        currentUserId = null;
        roles = new ArrayList<>();
        userProfile = null;
        loadUserSession(callback);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        executorService.shutdown();
    }
}
