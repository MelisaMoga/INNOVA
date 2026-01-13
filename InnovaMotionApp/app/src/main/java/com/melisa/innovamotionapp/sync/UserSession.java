package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages user session information including role and supervised targets.
 * 
 * Architecture:
 * - Aggregator: Collects data from sensors, uploads to Firestore with sensorId
 * - Supervisor: Monitors specific sensors assigned by aggregator
 * 
 * Supervisor's Firestore document contains:
 * - supervisedSensorIds: List of sensor IDs this supervisor can monitor
 * - aggregatorUid: The aggregator that assigned these sensors (for reference)
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
    private String role;
    private List<String> supervisedSensorIds;  // Sensor IDs this supervisor can monitor
    private String aggregatorUid;              // The aggregator that assigned sensors
    private boolean isLoaded = false;
    
    // Callback interface for session loading
    public interface SessionLoadCallback {
        void onSessionLoaded(String userId, String role, List<String> supervisedSensorIds);
        void onSessionLoadError(String error);
    }
    
    private UserSession(Context context) {
        this.context = context.getApplicationContext();
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        
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
            firestore.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
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
                        }
                    });
        });
    }
    
    /**
     * Load user data from Firestore document
     */
    private void loadUserDataFromDocument(String userId, DocumentSnapshot document, SessionLoadCallback callback) {
        currentUserId = userId;
        role = document.getString("role");
        
        Log.d(TAG, "Loaded user role: " + role + " for user: " + userId);
        
        if ("supervisor".equals(role)) {
            loadSupervisedSensorIds(document, callback);
        } else {
            // For aggregator users, no additional data needed
            supervisedSensorIds = new ArrayList<>();
            aggregatorUid = null;
            isLoaded = true;
            
            if (callback != null) {
                callback.onSessionLoaded(userId, role, supervisedSensorIds);
            }
        }
    }
    
    /**
     * Load supervised sensor IDs for supervisor role.
     * 
     * Firestore document structure:
     * - supervisedSensorIds: ['sensor001', 'sensor002', ...]
     * - aggregatorUid: 'uid_of_aggregator' (optional, for reference)
     */
    private void loadSupervisedSensorIds(DocumentSnapshot document, SessionLoadCallback callback) {
        supervisedSensorIds = new ArrayList<>();
        
        // Load aggregator UID (for reference/ownership tracking)
        aggregatorUid = document.getString("aggregatorUid");
        if (aggregatorUid != null) {
            Log.d(TAG, "Found aggregatorUid: " + aggregatorUid);
        }
        
        // Try to get supervisedSensorIds array first (new architecture)
        @SuppressWarnings("unchecked")
        List<String> sensorIds = (List<String>) document.get("supervisedSensorIds");
        
        // #region agent log
        // H1: Log what supervisedSensorIds were found in Firestore
        android.util.Log.w("DBG_H1", "loadSupervisedSensorIds: userId=" + currentUserId + ", role=" + role + ", sensorIdsFromFirestore=" + sensorIds + ", aggregatorUid=" + aggregatorUid);
        // #endregion
        
        if (sensorIds != null && !sensorIds.isEmpty()) {
            supervisedSensorIds.addAll(sensorIds);
            Log.d(TAG, "Found supervisedSensorIds: " + supervisedSensorIds);
            isLoaded = true;
            
            if (callback != null) {
                callback.onSessionLoaded(currentUserId, role, supervisedSensorIds);
            }
            return;
        }
        
        // Fallback: try legacy supervisedUserIds field (backward compatibility)
        @SuppressWarnings("unchecked")
        List<String> legacyUserIds = (List<String>) document.get("supervisedUserIds");
        if (legacyUserIds != null && !legacyUserIds.isEmpty()) {
            // In legacy mode, these were aggregator UIDs - treat them as sensor IDs for now
            Log.w(TAG, "Using legacy supervisedUserIds field - migration recommended");
            supervisedSensorIds.addAll(legacyUserIds);
            isLoaded = true;
            
            if (callback != null) {
                callback.onSessionLoaded(currentUserId, role, supervisedSensorIds);
            }
            return;
        }
        
        // If no sensor IDs found, log warning
        Log.w(TAG, "No supervised sensors found for supervisor");
        isLoaded = true;
        
        if (callback != null) {
            callback.onSessionLoaded(currentUserId, role, supervisedSensorIds);
        }
    }
    
    /**
     * Check if current user is aggregator role (collects data from sensors, uploads to cloud).
     */
    public boolean isAggregator() {
        return "aggregator".equals(role);
    }
    
    /**
     * Check if current user is supervisor role
     */
    public boolean isSupervisor() {
        return "supervisor".equals(role);
    }
    
    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * Get current user role
     */
    public String getRole() {
        return role;
    }
    
    /**
     * Get supervised sensor IDs (for supervisor role).
     * These are the sensor IDs that this supervisor is allowed to monitor.
     */
    public List<String> getSupervisedSensorIds() {
        return supervisedSensorIds != null ? new ArrayList<>(supervisedSensorIds) : new ArrayList<>();
    }
    
    /**
     * @deprecated Use {@link #getSupervisedSensorIds()} instead.
     * Kept for backward compatibility during migration.
     */
    @Deprecated
    public List<String> getSupervisedUserIds() {
        return getSupervisedSensorIds();
    }
    
    /**
     * Get the aggregator UID that assigned sensors to this supervisor.
     * May be null if not set in Firestore.
     */
    public String getAggregatorUid() {
        return aggregatorUid;
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
        role = null;
        supervisedSensorIds = null;
        aggregatorUid = null;
        loadUserSession(callback);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        executorService.shutdown();
    }
}
