package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

/**
 * Manager class for handling sync operations from UI components.
 * Provides a simple interface for activities and fragments to trigger sync operations.
 */
public class SyncManager {
    private static final String TAG = "SyncManager";
    
    private final FirestoreSyncService syncService;
    private final Context context;
    
    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.syncService = FirestoreSyncService.getInstance(context);
    }

    /**
     * Perform a full bidirectional sync
     */
    public void performFullSync(SyncProgressListener listener) {
        Log.i(TAG, "Starting full sync operation");
        
        if (!syncService.isConnected()) {
            if (listener != null) {
                listener.onSyncError("No internet connection available");
            }
            return;
        }

        if (syncService.getCurrentUserId() == null) {
            if (listener != null) {
                listener.onSyncError("User not authenticated");
            }
            return;
        }

        if (listener != null) {
            listener.onSyncStarted("Starting full synchronization...");
        }

        syncService.performFullSync(new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "Full sync completed successfully: " + message);
                if (listener != null) {
                    listener.onSyncCompleted(message);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Full sync failed: " + error);
                if (listener != null) {
                    listener.onSyncError(error);
                }
            }

            @Override
            public void onProgress(int current, int total) {
                if (listener != null) {
                    listener.onSyncProgress(current, total);
                }
            }
        });
    }

    /**
     * Sync local data to Firestore only
     */
    public void syncLocalToFirestore(SyncProgressListener listener) {
        Log.i(TAG, "Starting local to Firestore sync");
        
        if (!syncService.isConnected()) {
            if (listener != null) {
                listener.onSyncError("No internet connection available");
            }
            return;
        }

        if (listener != null) {
            listener.onSyncStarted("Syncing local data to cloud...");
        }

        syncService.syncLocalDataToFirestore(new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "Local to Firestore sync completed: " + message);
                if (listener != null) {
                    listener.onSyncCompleted(message);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Local to Firestore sync failed: " + error);
                if (listener != null) {
                    listener.onSyncError(error);
                }
            }

            @Override
            public void onProgress(int current, int total) {
                if (listener != null) {
                    listener.onSyncProgress(current, total);
                }
            }
        });
    }

    /**
     * Sync Firestore data to local database only
     */
    public void syncFirestoreToLocal(SyncProgressListener listener) {
        Log.i(TAG, "Starting Firestore to local sync");
        
        if (!syncService.isConnected()) {
            if (listener != null) {
                listener.onSyncError("No internet connection available");
            }
            return;
        }

        if (listener != null) {
            listener.onSyncStarted("Downloading cloud data...");
        }

        syncService.syncFirestoreDataToLocal(new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "Firestore to local sync completed: " + message);
                if (listener != null) {
                    listener.onSyncCompleted(message);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Firestore to local sync failed: " + error);
                if (listener != null) {
                    listener.onSyncError(error);
                }
            }

            @Override
            public void onProgress(int current, int total) {
                if (listener != null) {
                    listener.onSyncProgress(current, total);
                }
            }
        });
    }

    /**
     * Check sync status and connectivity
     */
    public SyncStatus getSyncStatus() {
        return new SyncStatus(
            syncService.isConnected(),
            syncService.getCurrentUserId() != null,
            syncService.getCurrentUserId()
        );
    }

    /**
     * Interface for sync progress callbacks
     */
    public interface SyncProgressListener {
        void onSyncStarted(String message);
        void onSyncProgress(int current, int total);
        void onSyncCompleted(String message);
        void onSyncError(String error);
    }

    /**
     * Sync status information
     */
    public static class SyncStatus {
        private final boolean isConnected;
        private final boolean isAuthenticated;
        private final String userId;

        public SyncStatus(boolean isConnected, boolean isAuthenticated, String userId) {
            this.isConnected = isConnected;
            this.isAuthenticated = isAuthenticated;
            this.userId = userId;
        }

        public boolean isConnected() {
            return isConnected;
        }

        public boolean isAuthenticated() {
            return isAuthenticated;
        }

        public String getUserId() {
            return userId;
        }

        public boolean canSync() {
            return isConnected && isAuthenticated;
        }

        @Override
        public String toString() {
            return "SyncStatus{" +
                    "connected=" + isConnected +
                    ", authenticated=" + isAuthenticated +
                    ", userId='" + userId + '\'' +
                    '}';
        }
    }
}
