package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.List;

/**
 * SessionGate manages the authentication and session loading flow.
 * Ensures all sync operations only start after user is authenticated and session is loaded.
 */
public class SessionGate {
    private static final String TAG = "SessionGate";
    
    private static SessionGate instance;
    private final Context context;
    private final FirebaseAuth auth;
    private final UserSession userSession;
    private final FirestoreSyncService syncService;
    
    // Session state
    private boolean isSessionReady = false;
    private String currentUserUid;
    private String currentUserRole;
    private List<String> supervisedUserIds;
    
    // Callback interface for session ready
    public interface SessionReadyCallback {
        void onSessionReady(String userId, String role, List<String> supervisedUserIds);
        void onSessionError(String error);
    }
    
    private SessionGate(Context context) {
        this.context = context.getApplicationContext();
        this.auth = FirebaseAuth.getInstance();
        this.userSession = UserSession.getInstance(context);
        this.syncService = FirestoreSyncService.getInstance(context);
        
        // Set up auth state listener
        setupAuthStateListener();
    }
    
    public static synchronized SessionGate getInstance(Context context) {
        if (instance == null) {
            instance = new SessionGate(context);
        }
        return instance;
    }
    
    /**
     * Set up Firebase Auth state listener to detect sign-in/sign-out
     */
    private void setupAuthStateListener() {
        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                Log.d(TAG, "User authenticated: " + user.getUid());
                handleUserAuthenticated(user);
            } else {
                Log.d(TAG, "User signed out");
                handleUserSignedOut();
            }
        });
    }
    
    /**
     * Handle user authentication - load session and start appropriate pipeline
     */
    private void handleUserAuthenticated(FirebaseUser user) {
        Log.d(TAG, "Loading user session for authenticated user: " + user.getUid());
        
        userSession.loadUserSession(new UserSession.SessionLoadCallback() {
            @Override
            public void onSessionLoaded(String userId, String role, List<String> supervisedUserIds) {
                Log.i("SESSION", "ready user=" + userId + " role=" + role + " supervised=" + supervisedUserIds);
                Log.i(TAG, "Session loaded - User: " + userId + ", Role: " + role + ", Supervised: " + supervisedUserIds);
                
                // Cache session data
                SessionGate.this.currentUserUid = userId;
                SessionGate.this.currentUserRole = role;
                SessionGate.this.supervisedUserIds = supervisedUserIds;
                SessionGate.this.isSessionReady = true;
                
                // Update GlobalData with session info
                GlobalData.getInstance().setCurrentUserUid(userId);
                GlobalData.getInstance().setCurrentUserRole(role);
                GlobalData.getInstance().setSupervisedUserIds(supervisedUserIds);
                
                // If role==supervisor, log each childUid you'll backfill + mirror
                if ("supervisor".equals(role)) {
                    for (String child : supervisedUserIds) {
                        Log.i("SESSION", "Supervisor will monitor child=" + child);
                    }
                }
                
                // Start appropriate pipeline based on role
                runPostAuthBootstrap(role, supervisedUserIds);
            }
            
            @Override
            public void onSessionLoadError(String error) {
                Log.e(TAG, "Failed to load user session: " + error);
                isSessionReady = false;
            }
        });
    }
    
    /**
     * Handle user sign-out - cleanup and reset state
     */
    private void handleUserSignedOut() {
        Log.d(TAG, "User signed out, cleaning up session");
        
        // Stop all mirrors and clear data
        syncService.stopAllMirrors();
        syncService.clearLocalData();
        
        // Reset GlobalData
        GlobalData.getInstance().resetSessionData();
        
        // Reset session state
        isSessionReady = false;
        currentUserUid = null;
        currentUserRole = null;
        supervisedUserIds = null;
    }
    
    /**
     * Run post-authentication bootstrap based on user role
     */
    private void runPostAuthBootstrap(String role, List<String> supervisedUserIds) {
        Log.i(TAG, "Running post-auth bootstrap for role: " + role);
        
        if ("supervised".equals(role)) {
            runSupervisedPipeline();
        } else if ("supervisor".equals(role)) {
            runSupervisorPipeline(supervisedUserIds);
        } else {
            Log.w(TAG, "Unknown role: " + role);
        }
    }
    
    /**
     * Run supervised user pipeline
     */
    private void runSupervisedPipeline() {
        Log.i(TAG, "Starting supervised user pipeline");
        
        // Backfill current user's data from cloud
        syncService.backfillLocalFromCloudForCurrentUser(new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "Supervised backfill completed: " + message);
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Supervised backfill failed: " + error);
            }
            
            @Override
            public void onProgress(int current, int total) {
                Log.d(TAG, "Supervised backfill progress: " + current + "/" + total);
            }
        });
    }
    
    /**
     * Run supervisor pipeline
     */
    private void runSupervisorPipeline(List<String> supervisedUserIds) {
        Log.i(TAG, "Starting supervisor pipeline for " + supervisedUserIds.size() + " supervised users");
        
        if (supervisedUserIds.isEmpty()) {
            Log.w(TAG, "No supervised users found for supervisor");
            return;
        }
        
        // Purge old data and backfill for each supervised user
        syncService.purgeAndBackfillForSupervisor(supervisedUserIds, new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "Supervisor pipeline completed: " + message);
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Supervisor pipeline failed: " + error);
            }
            
            @Override
            public void onProgress(int current, int total) {
                Log.d(TAG, "Supervisor pipeline progress: " + current + "/" + total);
            }
        });
    }
    
    /**
     * Check if session is ready
     */
    public boolean isSessionReady() {
        return isSessionReady;
    }
    
    /**
     * Get current user UID
     */
    public String getCurrentUserUid() {
        return currentUserUid;
    }
    
    /**
     * Get current user role
     */
    public String getCurrentUserRole() {
        return currentUserRole;
    }
    
    /**
     * Get supervised user IDs
     */
    public List<String> getSupervisedUserIds() {
        return supervisedUserIds;
    }
    
    /**
     * Wait for session to be ready and execute callback
     */
    public void waitForSessionReady(SessionReadyCallback callback) {
        if (isSessionReady) {
            callback.onSessionReady(currentUserUid, currentUserRole, supervisedUserIds);
        } else {
            // For now, just return error - in a real implementation you might want to queue callbacks
            callback.onSessionError("Session not ready");
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        Log.i(TAG, "Cleaning up SessionGate");
        // Auth state listener is automatically cleaned up when FirebaseAuth is destroyed
    }
}
