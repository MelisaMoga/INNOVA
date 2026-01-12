package com.melisa.innovamotionapp.sync;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;

import java.util.List;

/**
 * Sample activity demonstrating how to use the sync functionality.
 * This can be integrated into your existing activities or used as a standalone sync screen.
 * 
 * MANUAL TEST CHECKLIST:
 * =====================
 * 
 * 1. AGGREGATOR USER TESTS:
 *    □ Sign in as aggregator user
 *    □ Connect Bluetooth device and receive messages
 *    □ Verify messages appear in local Room database
 *    □ Verify messages sync to Firestore when online
 *    □ Test offline scenario: disconnect internet, receive messages, reconnect
 *    □ Verify offline messages sync to Firestore after reconnection
 *    □ Check no duplicate messages in Room after sync
 * 
 * 2. SUPERVISOR USER TESTS:
 *    □ Sign in as supervisor with aggregator email
 *    □ Verify supervisor can see aggregator's sensor messages in local Room
 *    □ Test real-time sync: aggregator sends message, supervisor sees it
 *    □ Test offline supervisor: disconnect internet, reconnect
 *    □ Verify supervisor resumes downloading when back online
 *    □ Check no duplicate messages in Room after sync
 * 
 * 3. SUPERVISOR BYPASS TESTS:
 *    □ Sign in as supervisor and tap "Monitoring" button
 *    □ Verify direct navigation to BtConnectedActivity (skips scanning)
 *    □ Check that latest posture from Room is displayed immediately
 *    □ Verify real-time updates as new sensor data arrives
 *    □ Test that supervisors don't exit when Bluetooth disconnects
 *    □ Verify BtSettingsActivity redirects supervisors automatically
 *    □ Test supervisor can monitor specific sensors (extended feature)
 * 
 * 4. CONNECTIVITY TESTS:
 *    □ Toggle airplane mode on/off
 *    □ Switch between WiFi and mobile data
 *    □ Test with poor network connection
 *    □ Verify sync resumes automatically when connectivity restored
 * 
 * 5. ERROR HANDLING TESTS:
 *    □ Test with invalid user role
 *    □ Test with missing sensors
 *    □ Test with Firestore permission errors
 *    □ Verify graceful degradation (app doesn't crash)
 * 
 * 6. DATA INTEGRITY TESTS:
 *    □ Verify deterministic document IDs in Firestore
 *    □ Check unique constraints prevent duplicates
 *    □ Verify messageExists() DAO method works correctly
 *    □ Test with large batches of messages
 * 
 * 6. PERFORMANCE TESTS:
 *    □ Test with 100+ messages
 *    □ Verify batch operations work efficiently
 *    □ Check memory usage during sync
 *    □ Test concurrent Bluetooth and sync operations
 */
public class SyncActivity extends AppCompatActivity implements SyncManager.SyncProgressListener {
    private static final String TAG = "SyncActivity";
    
    private SyncManager syncManager;
    private FirestoreSyncService firestoreSyncService;
    private UserSession userSession;
    private Button btnFullSync, btnSyncToFirestore, btnSyncToLocal, btnShowUserInfo, btnShowLocalData;
    private TextView txtSyncStatus, txtSyncProgress, txtUserInfo;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: You'll need to create this layout file
        // setContentView(R.layout.activity_sync);
        
        syncManager = new SyncManager(this);
        firestoreSyncService = FirestoreSyncService.getInstance(this);
        userSession = UserSession.getInstance(this);
        initializeViews();
        updateSyncStatus();
        showUserInfo();
    }

    private void initializeViews() {
        // Initialize your UI components here
        // Example implementation:
        /*
        btnFullSync = findViewById(R.id.btn_full_sync);
        btnSyncToFirestore = findViewById(R.id.btn_sync_to_firestore);
        btnSyncToLocal = findViewById(R.id.btn_sync_to_local);
        btnShowUserInfo = findViewById(R.id.btn_show_user_info);
        btnShowLocalData = findViewById(R.id.btn_show_local_data);
        txtSyncStatus = findViewById(R.id.txt_sync_status);
        txtSyncProgress = findViewById(R.id.txt_sync_progress);
        txtUserInfo = findViewById(R.id.txt_user_info);
        progressBar = findViewById(R.id.progress_bar);

        btnFullSync.setOnClickListener(v -> performFullSync());
        btnSyncToFirestore.setOnClickListener(v -> syncLocalToFirestore());
        btnSyncToLocal.setOnClickListener(v -> syncFirestoreToLocal());
        btnShowUserInfo.setOnClickListener(v -> showUserInfo());
        btnShowLocalData.setOnClickListener(v -> showLocalData());
        // Note: Add backfill buttons to layout for testing
        // findViewById(R.id.btn_backfill).setOnClickListener(v -> performBackfill());
        // findViewById(R.id.btn_backfill_paged).setOnClickListener(v -> performPagedBackfill());
        */
    }

    private void updateSyncStatus() {
        SyncManager.SyncStatus status = syncManager.getSyncStatus();
        
        String statusText = "Connection: " + (status.isConnected() ? "Online" : "Offline") + "\n" +
                           "Authentication: " + (status.isAuthenticated() ? "Signed In" : "Not Signed In") + "\n" +
                           "User Session Loaded: " + (firestoreSyncService.isUserSessionLoaded() ? "Yes" : "No") + "\n" +
                           "User Role: " + (firestoreSyncService.getCurrentUserRole() != null ? firestoreSyncService.getCurrentUserRole() : "Unknown") + "\n" +
                           "Can Sync: " + (status.canSync() ? "Yes" : "No");
        
        if (txtSyncStatus != null) {
            txtSyncStatus.setText(statusText);
        }
        
        // Enable/disable sync buttons based on status
        boolean canSync = status.canSync();
        if (btnFullSync != null) btnFullSync.setEnabled(canSync);
        if (btnSyncToFirestore != null) btnSyncToFirestore.setEnabled(canSync);
        if (btnSyncToLocal != null) btnSyncToLocal.setEnabled(canSync);
    }
    
    /**
     * Show detailed user session information for debugging
     */
    private void showUserInfo() {
        if (userSession.isLoaded()) {
            String userInfo = "User ID: " + userSession.getCurrentUserId() + "\n" +
                             "Role: " + userSession.getRole() + "\n" +
                             "Supervised Sensor IDs: " + userSession.getSupervisedSensorIds().toString();
            
            if (txtUserInfo != null) {
                txtUserInfo.setText(userInfo);
            }
            
            Log.d(TAG, "User Info: " + userInfo);
        } else {
            Log.d(TAG, "User session not loaded yet");
            if (txtUserInfo != null) {
                txtUserInfo.setText("Loading user session...");
            }
            
            // Try to reload session
            userSession.loadUserSession(new UserSession.SessionLoadCallback() {
                @Override
                public void onSessionLoaded(String userId, String role, List<String> supervisedUserIds) {
                    runOnUiThread(() -> showUserInfo());
                }

                @Override
                public void onSessionLoadError(String error) {
                    runOnUiThread(() -> {
                        if (txtUserInfo != null) {
                            txtUserInfo.setText("Error loading session: " + error);
                        }
                    });
                }
            });
        }
    }
    
    /**
     * Show local database data for debugging
     */
    private void showLocalData() {
        new Thread(() -> {
            try {
                ReceivedBtDataDao dao = InnovaDatabase.getInstance(this).receivedBtDataDao();
                List<ReceivedBtDataEntity> allData = dao.getAllDataSync();
                
                runOnUiThread(() -> {
                    String dataInfo = "Local Database Messages: " + allData.size() + "\n";
                    if (allData.size() > 0) {
                        dataInfo += "Latest message: " + allData.get(allData.size() - 1).getReceivedMsg() + "\n";
                        dataInfo += "Timestamp: " + allData.get(allData.size() - 1).getTimestamp();
                    }
                    
                    Toast.makeText(this, dataInfo, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Local Data Info: " + dataInfo);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error getting local data", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error getting local data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void performFullSync() {
        syncManager.performFullSync(this);
    }

    private void syncLocalToFirestore() {
        syncManager.syncLocalToFirestore(this);
    }

    private void syncFirestoreToLocal() {
        syncManager.syncFirestoreToLocal(this);
    }
    
    /**
     * Manual backfill trigger for testing (aggregator users only)
     */
    private void performBackfill() {
        firestoreSyncService.backfillLocalFromCloudForCurrentUser(new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(SyncActivity.this, "Backfill completed: " + message, Toast.LENGTH_LONG).show();
                    showLocalData(); // Show updated data count
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SyncActivity.this, "Backfill failed: " + error, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onProgress(int current, int total) {
                Log.d(TAG, "Backfill progress: " + current + "/" + total);
            }
        });
    }
    
    /**
     * Manual paged backfill trigger for testing (aggregator users only)
     */
    private void performPagedBackfill() {
        firestoreSyncService.backfillLocalFromCloudForCurrentUserPaged(new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(SyncActivity.this, "Paged backfill completed: " + message, Toast.LENGTH_LONG).show();
                    showLocalData(); // Show updated data count
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SyncActivity.this, "Paged backfill failed: " + error, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onProgress(int current, int total) {
                Log.d(TAG, "Paged backfill progress: " + current + "/" + (total == -1 ? "?" : total));
            }
        });
    }

    // SyncProgressListener implementation
    @Override
    public void onSyncStarted(String message) {
        runOnUiThread(() -> {
            if (txtSyncProgress != null) {
                txtSyncProgress.setText(message);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
            }
            setButtonsEnabled(false);
        });
    }

    @Override
    public void onSyncProgress(int current, int total) {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setIndeterminate(false);
                progressBar.setMax(total);
                progressBar.setProgress(current);
            }
            if (txtSyncProgress != null) {
                txtSyncProgress.setText("Syncing " + current + "/" + total + " messages...");
            }
        });
    }

    @Override
    public void onSyncCompleted(String message) {
        runOnUiThread(() -> {
            if (txtSyncProgress != null) {
                txtSyncProgress.setText("Sync completed: " + message);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            setButtonsEnabled(true);
            updateSyncStatus();
            Toast.makeText(this, "Sync completed successfully", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onSyncError(String error) {
        runOnUiThread(() -> {
            if (txtSyncProgress != null) {
                txtSyncProgress.setText("Sync error: " + error);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            setButtonsEnabled(true);
            updateSyncStatus();
            Toast.makeText(this, "Sync failed: " + error, Toast.LENGTH_LONG).show();
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        SyncManager.SyncStatus status = syncManager.getSyncStatus();
        boolean canSync = enabled && status.canSync();
        
        if (btnFullSync != null) btnFullSync.setEnabled(canSync);
        if (btnSyncToFirestore != null) btnSyncToFirestore.setEnabled(canSync);
        if (btnSyncToLocal != null) btnSyncToLocal.setEnabled(canSync);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSyncStatus();
    }
}
