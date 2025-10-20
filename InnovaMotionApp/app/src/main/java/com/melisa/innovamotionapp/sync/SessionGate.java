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
    private boolean hasBootstrapped = false; // Track if bootstrap has run
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
                // Only load session data, DO NOT run bootstrap automatically
                loadSessionDataOnly(user);
            } else {
                Log.d(TAG, "User signed out");
                handleUserSignedOut();
            }
        });
    }
    
    /**
     * Update cached session data and GlobalData instance
     * @param userId User ID
     * @param role User role (supervised or supervisor)
     * @param supervisedUserIds List of supervised user IDs (empty for supervised users)
     */
    private void updateSessionCache(String userId, String role, List<String> supervisedUserIds) {
        // Cache session data locally
        this.currentUserUid = userId;
        this.currentUserRole = role;
        this.supervisedUserIds = supervisedUserIds;
        this.isSessionReady = true;
        
        // Update GlobalData singleton with session info
        GlobalData.getInstance().setCurrentUserUid(userId);
        GlobalData.getInstance().setCurrentUserRole(role);
        GlobalData.getInstance().setSupervisedUserIds(supervisedUserIds);
    }
    
    /**
     * Load session data without triggering bootstrap
     * This is called by the auth listener to cache user info
     */
    private void loadSessionDataOnly(FirebaseUser user) {
        Log.d(TAG, "Loading session data (without bootstrap) for: " + user.getUid());
        
        userSession.loadUserSession(new UserSession.SessionLoadCallback() {
            @Override
            public void onSessionLoaded(String userId, String role, List<String> supervisedUserIds) {
                Log.i(TAG, "Session data loaded - User: " + userId + ", Role: " + role + ", Supervised: " + supervisedUserIds);
                Log.i(TAG, "⚠️ Bootstrap will NOT run automatically - waiting for explicit trigger");
                
                // Cache session data
                updateSessionCache(userId, role, supervisedUserIds);
                
                // Log supervised users for supervisor role
                if ("supervisor".equals(role)) {
                    for (String child : supervisedUserIds) {
                        Log.i("SESSION", "Supervisor's cached supervised user=" + child);
                    }
                }
                
                // DO NOT run bootstrap here - wait for explicit call
            }
            
            @Override
            public void onSessionLoadError(String error) {
                Log.e(TAG, "Failed to load user session: " + error);
                isSessionReady = false;
                hasBootstrapped = false;
            }
        });
    }
    
    /**
     * Reload session from Firestore and run bootstrap with fresh data
     * This should be called after the user confirms their role in LoginActivity
     */
    public void reloadSessionAndBootstrap(final SessionReadyCallback callback) {
        Log.i(TAG, "🔄 Reloading session from Firestore and running bootstrap...");
        
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot reload session: user not authenticated");
            if (callback != null) {
                callback.onSessionError("User not authenticated");
            }
            return;
        }
        
        // Force reload session to get latest data from Firestore
        userSession.reloadSession(new UserSession.SessionLoadCallback() {
            @Override
            public void onSessionLoaded(String userId, String role, List<String> supervisedUserIds) {
                Log.i("SESSION", "✅ Session reloaded - user=" + userId + " role=" + role + " supervised=" + supervisedUserIds);
                
                // Update cached session data
                updateSessionCache(userId, role, supervisedUserIds);
                
                // Now run bootstrap with correct data
                Log.i(TAG, "🚀 Running post-auth bootstrap with confirmed role: " + role);
                runPostAuthBootstrap(role, supervisedUserIds);
                hasBootstrapped = true;
                
                if (callback != null) {
                    callback.onSessionReady(userId, role, supervisedUserIds);
                }
            }
            
            @Override
            public void onSessionLoadError(String error) {
                Log.e(TAG, "Failed to reload session: " + error);
                if (callback != null) {
                    callback.onSessionError(error);
                }
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
        hasBootstrapped = false;
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
     * Check if bootstrap has already run
     */
    public boolean hasBootstrapped() {
        return hasBootstrapped;
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
