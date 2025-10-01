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
 * Caches user role and supervised user IDs for efficient sync operations.
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
    private List<String> supervisedUserIds;
    private boolean isLoaded = false;
    
    // Callback interface for session loading
    public interface SessionLoadCallback {
        void onSessionLoaded(String userId, String role, List<String> supervisedUserIds);
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
            loadSupervisedUserIds(document, callback);
        } else {
            // For supervised users, no additional data needed
            supervisedUserIds = new ArrayList<>();
            isLoaded = true;
            
            if (callback != null) {
                callback.onSessionLoaded(userId, role, supervisedUserIds);
            }
        }
    }
    
    /**
     * Load supervised user IDs for supervisor role
     */
    private void loadSupervisedUserIds(DocumentSnapshot document, SessionLoadCallback callback) {
        supervisedUserIds = new ArrayList<>();
        
        // Try to get supervisedUserIds array first
        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) document.get("supervisedUserIds");
        if (userIds != null && !userIds.isEmpty()) {
            supervisedUserIds.addAll(userIds);
            Log.d(TAG, "Found supervisedUserIds: " + supervisedUserIds);
            isLoaded = true;
            
            if (callback != null) {
                callback.onSessionLoaded(currentUserId, role, supervisedUserIds);
            }
            return;
        }
        
        // If no supervisedUserIds array, try supervisedEmail
        String supervisedEmail = document.getString("supervisedEmail");
        if (supervisedEmail != null && !supervisedEmail.trim().isEmpty()) {
            Log.d(TAG, "Found supervisedEmail, resolving to UID: " + supervisedEmail);
            resolveEmailToUserId(supervisedEmail, callback);
        } else {
            Log.w(TAG, "No supervised users found for supervisor");
            isLoaded = true;
            
            if (callback != null) {
                callback.onSessionLoaded(currentUserId, role, supervisedUserIds);
            }
        }
    }
    
    /**
     * Resolve email to user ID via Firestore query
     */
    private void resolveEmailToUserId(String email, SessionLoadCallback callback) {
        firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    String supervisedUserId = doc.getId();
                                    supervisedUserIds.add(supervisedUserId);
                                    Log.d(TAG, "Resolved email " + email + " to UID: " + supervisedUserId);
                                }
                            } else {
                                Log.w(TAG, "No user found with email: " + email);
                            }
                        } else {
                            Log.e(TAG, "Failed to resolve email to UID", task.getException());
                        }
                        
                        isLoaded = true;
                        if (callback != null) {
                            callback.onSessionLoaded(currentUserId, role, supervisedUserIds);
                        }
                    }
                });
    }
    
    /**
     * Check if current user is supervised role
     */
    public boolean isSupervised() {
        return "supervised".equals(role);
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
     * Get supervised user IDs (for supervisor role)
     */
    public List<String> getSupervisedUserIds() {
        return supervisedUserIds != null ? new ArrayList<>(supervisedUserIds) : new ArrayList<>();
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
        supervisedUserIds = null;
        loadUserSession(callback);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        executorService.shutdown();
    }
}
