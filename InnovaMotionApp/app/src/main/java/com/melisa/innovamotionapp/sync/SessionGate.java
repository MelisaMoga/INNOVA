package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.ArrayList;
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
    private final SensorInventoryService sensorInventoryService;
    
    // Session state
    private boolean isSessionReady = false;
    private boolean hasBootstrapped = false; // Track if bootstrap has run
    private String currentUserUid;
    private List<String> currentUserRoles;
    private List<String> supervisedSensorIds;
    
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
        this.sensorInventoryService = SensorInventoryService.getInstance(context);
        this.currentUserRoles = new ArrayList<>();
        this.supervisedSensorIds = new ArrayList<>();
        
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
     * @param roles User roles list
     */
    private void updateSessionCache(String userId, List<String> roles) {
        // Cache session data locally
        this.currentUserUid = userId;
        this.currentUserRoles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
        this.isSessionReady = true;
        
        // Update GlobalData singleton with session info
        GlobalData.getInstance().setCurrentUserUid(userId);
        
        // Preserve existing role if explicitly set (e.g., by RoleSelectionActivity)
        // Only set default role if GlobalData.currentUserRole is null or empty
        String existingRole = GlobalData.getInstance().currentUserRole;
        if (existingRole == null || existingRole.isEmpty()) {
            // No role explicitly set - default to first role in list for backward compatibility
            String primaryRole = !this.currentUserRoles.isEmpty() ? this.currentUserRoles.get(0) : null;
            GlobalData.getInstance().setCurrentUserRole(primaryRole);
            Log.d(TAG, "Set default role from profile: " + primaryRole);
        } else {
            // Role was explicitly set (user selected in RoleSelectionActivity) - preserve it
            Log.d(TAG, "Preserving explicitly selected role: " + existingRole);
        }
    }
    
    /**
     * Load session data without triggering bootstrap
     * This is called by the auth listener to cache user info
     */
    private void loadSessionDataOnly(FirebaseUser user) {
        Log.d(TAG, "Loading session data (without bootstrap) for: " + user.getUid());
        
        userSession.loadUserSession(new UserSession.SessionLoadCallback() {
            @Override
            public void onSessionLoaded(String userId, List<String> roles) {
                Log.i(TAG, "Session data loaded - User: " + userId + ", Roles: " + roles);
                Log.i(TAG, "⚠️ Bootstrap will NOT run automatically - waiting for explicit trigger");
                
                // Cache session data
                updateSessionCache(userId, roles);
                
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
            public void onSessionLoaded(String userId, List<String> roles) {
                Log.i("SESSION", "✅ Session reloaded - user=" + userId + " roles=" + roles);
                
                // Update cached session data
                updateSessionCache(userId, roles);
                
                // Determine primary role for bootstrap
                String primaryRole = !roles.isEmpty() ? roles.get(0) : null;
                
                // Now run bootstrap with correct data
                Log.i(TAG, "🚀 Running post-auth bootstrap with roles: " + roles);
                runPostAuthBootstrap(roles);
                hasBootstrapped = true;
                
                if (callback != null) {
                    // Callback uses legacy signature - pass primary role and empty sensor list
                    // Sensor IDs are now fetched dynamically
                    callback.onSessionReady(userId, primaryRole, new ArrayList<>());
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
        currentUserRoles = new ArrayList<>();
        supervisedSensorIds = new ArrayList<>();
    }
    
    /**
     * Run post-authentication bootstrap based on user roles
     */
    private void runPostAuthBootstrap(List<String> roles) {
        Log.i(TAG, "Running post-auth bootstrap for roles: " + roles);
        // #region agent log
        android.util.Log.w("DBG_SUP", "runPostAuthBootstrap: roles=" + roles);
        // #endregion
        
        // Check for aggregator role
        if (roles.contains("aggregator")) {
            runAggregatorPipeline();
        }
        
        // Check for supervisor role
        if (roles.contains("supervisor")) {
            runSupervisorPipeline();
        }
        
        if (roles.isEmpty()) {
            Log.w(TAG, "No roles found for user");
        }
    }
    
    /**
     * Run aggregator user pipeline (backfill their own data from cloud)
     */
    private void runAggregatorPipeline() {
        Log.i(TAG, "Starting aggregator pipeline");
        // #region agent log
        android.util.Log.w("DBG_SUP", "runAggregatorPipeline: starting backfill for current user");
        // #endregion
        
        // Backfill current user's data from cloud
        syncService.backfillLocalFromCloudForCurrentUser(new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "Aggregator backfill completed: " + message);
                // #region agent log
                android.util.Log.w("DBG_SUP", "runAggregatorPipeline: backfill success - " + message);
                // #endregion
                
                // Also download sensor names from the sensors collection
                downloadAggregatorSensorNames();
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Aggregator backfill failed: " + error);
                // #region agent log
                android.util.Log.w("DBG_SUP", "runAggregatorPipeline: backfill error - " + error);
                // #endregion
                
                // Still try to download sensor names even if message backfill failed
                downloadAggregatorSensorNames();
            }
            
            @Override
            public void onProgress(int current, int total) {
                Log.d(TAG, "Aggregator backfill progress: " + current + "/" + total);
            }
        });
    }
    
    /**
     * Download sensor names for the current aggregator from Firestore.
     * This ensures display names are restored after account switch.
     */
    private void downloadAggregatorSensorNames() {
        if (currentUserUid == null) {
            Log.w(TAG, "Cannot download sensor names: currentUserUid is null");
            return;
        }
        
        Log.i(TAG, "Downloading sensor names for aggregator: " + currentUserUid);
        sensorInventoryService.getOwnedSensors(new SensorInventoryService.SensorListCallback() {
            @Override
            public void onResult(List<com.melisa.innovamotionapp.data.models.Sensor> sensors) {
                Log.i(TAG, "Fetched " + sensors.size() + " sensor names from Firestore");
                
                // Save fetched sensors to local Room database
                if (!sensors.isEmpty()) {
                    sensorInventoryService.saveSensorsToLocal(sensors, new SensorInventoryService.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.i(TAG, "Aggregator sensor names saved to local: " + message);
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.w(TAG, "Failed to save aggregator sensor names locally: " + error);
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to download sensor names: " + error);
            }
        });
    }
    
    /**
     * Run supervisor pipeline - dynamically fetches assigned sensors and syncs data
     */
    private void runSupervisorPipeline() {
        Log.i(TAG, "Starting supervisor pipeline - fetching assigned sensors");
        
        // Dynamically fetch assigned sensor IDs from the 'assignments' collection
        userSession.fetchAssignedSensorIds(new UserSession.AssignedSensorsCallback() {
            @Override
            public void onSensorsLoaded(List<String> sensorIds) {
                // #region agent log
                android.util.Log.w("DBG_SUP", "runSupervisorPipeline: fetched sensorIds=" + sensorIds);
                // #endregion
                
                // Cache for later use
                supervisedSensorIds = new ArrayList<>(sensorIds);
                GlobalData.getInstance().setSupervisedSensorIds(sensorIds);
                
                if (sensorIds.isEmpty()) {
                    Log.w(TAG, "No sensors assigned to supervisor");
                    // #region agent log
                    android.util.Log.e("DBG_SUP", "runSupervisorPipeline: EMPTY sensorIds!");
                    // #endregion
                    return;
                }
                
                // First, sync existing data from Firestore for these sensors
                syncService.syncFromSupervisedSensors(sensorIds, new FirestoreSyncService.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.i(TAG, "Supervisor initial sync completed: " + message);
                        // #region agent log
                        android.util.Log.w("DBG_SUP", "runSupervisorPipeline: initial sync success - " + message);
                        // #endregion
                        
                        // Now start real-time mirrors for ongoing updates
                        syncService.startSupervisorMirrors(sensorIds);
                        
                        // Also download sensor names for display
                        downloadSupervisorSensorNames(sensorIds);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Supervisor initial sync failed: " + error);
                        // #region agent log
                        android.util.Log.e("DBG_SUP", "runSupervisorPipeline: initial sync error - " + error);
                        // #endregion
                        
                        // Still try to start mirrors even if initial sync failed
                        syncService.startSupervisorMirrors(sensorIds);
                    }
                    
                    @Override
                    public void onProgress(int current, int total) {
                        Log.d(TAG, "Supervisor sync progress: " + current + "/" + total);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to fetch assigned sensors: " + error);
            }
        });
    }
    
    /**
     * Download sensor names for supervisor from sensors collection.
     */
    private void downloadSupervisorSensorNames(List<String> sensorIds) {
        Log.i(TAG, "Downloading sensor names for supervisor");
        sensorInventoryService.downloadToLocal(sensorIds, new SensorInventoryService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "Supervisor sensor names downloaded: " + message);
            }
            
            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to download supervisor sensor names: " + error);
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
     * Get current user roles.
     */
    public List<String> getCurrentUserRoles() {
        return currentUserRoles != null ? new ArrayList<>(currentUserRoles) : new ArrayList<>();
    }
    
    /**
     * Get current user primary role (for backward compatibility).
     */
    public String getCurrentUserRole() {
        return !currentUserRoles.isEmpty() ? currentUserRoles.get(0) : null;
    }
    
    /**
     * Get supervised sensor IDs (for supervisor role).
     * These are cached from the last call to runSupervisorPipeline.
     */
    public List<String> getSupervisedSensorIds() {
        return supervisedSensorIds != null ? new ArrayList<>(supervisedSensorIds) : new ArrayList<>();
    }
    
    /**
     * @deprecated Use {@link #getSupervisedSensorIds()} instead.
     */
    @Deprecated
    public List<String> getSupervisedUserIds() {
        return getSupervisedSensorIds();
    }
    
    /**
     * Wait for session to be ready and execute callback
     */
    public void waitForSessionReady(SessionReadyCallback callback) {
        if (isSessionReady) {
            String primaryRole = !currentUserRoles.isEmpty() ? currentUserRoles.get(0) : null;
            callback.onSessionReady(currentUserUid, primaryRole, supervisedSensorIds);
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
