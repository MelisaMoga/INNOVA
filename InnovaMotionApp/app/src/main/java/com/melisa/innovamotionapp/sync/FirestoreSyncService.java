package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for role-aware synchronization between local Room database and Firestore.
 * 
 * For supervised users: Uploads local messages to Firestore
 * For supervisor users: Downloads messages from supervised users to local Room
 */
public class FirestoreSyncService {
    private static final String TAG = "FirestoreSyncService";
    private static final String COLLECTION_BT_DATA = "bluetooth_messages";
    private static final int BATCH_SIZE = 500; // Firestore batch write limit
    private static final int SUPERVISOR_SYNC_INTERVAL_SECONDS = 10; // Polling interval for supervisor
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final InnovaDatabase localDatabase;
    private final ReceivedBtDataDao dao;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final NetworkConnectivityMonitor connectivityMonitor;
    private final UserSession userSession;
    
    // Supervisor download listeners with proper management
    private final Map<String, ListenerRegistration> mirrorByUid = new HashMap<>();
    private final Set<String> startingUids = new HashSet<>();
    private boolean isSupervisorSyncActive = false;
    
    // Callbacks for sync operations
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
        void onProgress(int current, int total);
    }

    private static FirestoreSyncService instance;

    public static synchronized FirestoreSyncService getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreSyncService(context);
        }
        return instance;
    }

    private FirestoreSyncService(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.localDatabase = InnovaDatabase.getInstance(context);
        this.dao = localDatabase.receivedBtDataDao();
        this.executorService = Executors.newFixedThreadPool(2);
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.connectivityMonitor = new NetworkConnectivityMonitor(context);
        this.userSession = UserSession.getInstance(context);
        
        // Start monitoring connectivity for automatic sync
        setupConnectivityMonitoring();
    }

    /**
     * Setup connectivity monitoring for automatic sync when online
     */
    private void setupConnectivityMonitoring() {
        connectivityMonitor.addListener(isConnected -> {
            if (isConnected) {
                Log.i(TAG, "Network connectivity restored, checking user role for sync");
                handleConnectivityRestored();
            } else {
                Log.i(TAG, "Network connectivity lost, stopping supervisor sync");
                stopAllMirrors();
            }
        });
        connectivityMonitor.startMonitoring();
    }

    /**
     * Handle connectivity restored based on user role
     */
    private void handleConnectivityRestored() {
        if (!userSession.isLoaded()) {
            Log.d(TAG, "User session not loaded yet, loading...");
            userSession.loadUserSession(new UserSession.SessionLoadCallback() {
                @Override
                public void onSessionLoaded(String userId, String role, List<String> supervisedUserIds) {
                    handleConnectivityRestored();
                }

                @Override
                public void onSessionLoadError(String error) {
                    Log.e(TAG, "Failed to load user session: " + error);
                }
            });
            return;
        }

        if (userSession.isSupervised()) {
            Log.i(TAG, "Supervised user: syncing local data to Firestore");
            syncLocalDataToFirestore(new SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.i(TAG, "Supervised user sync completed: " + message);
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Supervised user sync failed: " + error);
                }

                @Override
                public void onProgress(int current, int total) {
                    Log.d(TAG, "Supervised user sync progress: " + current + "/" + total);
                }
            });
        } else if (userSession.isSupervisor()) {
            Log.i(TAG, "Supervisor user: starting download sync");
            List<String> supervisedUserIds = userSession.getSupervisedUserIds();
            if (!supervisedUserIds.isEmpty()) {
                startSupervisorMirrors(supervisedUserIds);
            }
        }
    }

    /**
     * Sync a new message immediately if user is supervised and online
     * NOTE: This method does NOT insert into Room - it only syncs to Firestore
     */
    public void syncNewMessage(ReceivedBtDataEntity entity, SyncCallback callback) {
        if (!isUserAuthenticated()) {
            callback.onError("User not authenticated");
            return;
        }

        if (!userSession.isLoaded()) {
            Log.w(TAG, "User session not loaded, skipping sync");
            callback.onError("User session not loaded");
            return;
        }

        if (!userSession.isSupervised()) {
            Log.d(TAG, "User is not supervised, skipping Firestore sync");
            callback.onSuccess("User is not supervised, no sync needed");
            return;
        }

        if (!connectivityMonitor.isConnected()) {
            Log.d(TAG, "No connectivity, message will be synced when online");
            callback.onSuccess("Message saved locally, will sync when online");
            return;
        }

        executorService.execute(() -> {
            syncSingleMessageToFirestore(entity, callback);
        });
    }

    /**
     * Sync a single message to Firestore
     */
    private void syncSingleMessageToFirestore(ReceivedBtDataEntity entity, SyncCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // Defensive guard: we never upload if no authenticated user; data is already in Room.
            callback.onError("User not authenticated (offline token unavailable); kept locally");
            return;
        }

        FirestoreDataModel firestoreModel = new FirestoreDataModel(
            entity.getDeviceAddress(),
            entity.getTimestamp(),
            entity.getReceivedMsg(),
            user.getUid()
        );

        String documentId = firestoreModel.getDocumentId();
        firestore.collection(COLLECTION_BT_DATA)
                .document(documentId)
                .set(firestoreModel.toFirestoreDocument())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message synced to Firestore: " + documentId);
                    callback.onSuccess("Message synced successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync message to Firestore", e);
                    callback.onError("Failed to sync to Firestore: " + e.getMessage());
                });
    }

    /**
     * Sync all local data that hasn't been synced to Firestore yet (supervised users only)
     */
    public void syncLocalDataToFirestore(SyncCallback callback) {
        if (!isUserAuthenticated()) {
            callback.onError("User not authenticated");
            return;
        }

        if (!userSession.isLoaded() || !userSession.isSupervised()) {
            callback.onError("User is not supervised or session not loaded");
            return;
        }

        if (!connectivityMonitor.isConnected()) {
            callback.onError("No internet connection");
            return;
        }

        executorService.execute(() -> {
            try {
                FirebaseUser user = auth.getCurrentUser();
                List<ReceivedBtDataEntity> localData = getAllLocalData();
                
                if (localData.isEmpty()) {
                    callback.onSuccess("No local data to sync");
                    return;
                }

                Log.i(TAG, "Starting sync of " + localData.size() + " local messages to Firestore");
                
                // Check which documents already exist in Firestore
                checkExistingDocumentsAndSync(user.getUid(), localData, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error during local to Firestore sync", e);
                callback.onError("Sync failed: " + e.getMessage());
            }
        });
    }

    /**
     * Check existing documents in Firestore and sync only missing ones
     */
    private void checkExistingDocumentsAndSync(String userId, List<ReceivedBtDataEntity> localData, SyncCallback callback) {
        List<String> documentIds = new ArrayList<>();
        Map<String, ReceivedBtDataEntity> localDataMap = new HashMap<>();
        
        // Prepare document IDs and mapping
        for (ReceivedBtDataEntity entity : localData) {
            String docId = FirestoreDataModel.generateDocumentId(userId, entity.getDeviceAddress(), entity.getTimestamp());
            documentIds.add(docId);
            localDataMap.put(docId, entity);
        }

        // Query Firestore to check which documents already exist
        firestore.collection(COLLECTION_BT_DATA)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Find documents that don't exist in Firestore
                    List<ReceivedBtDataEntity> documentsToSync = new ArrayList<>();
                    
                    for (String docId : documentIds) {
                        boolean exists = false;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (docId.equals(doc.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            documentsToSync.add(localDataMap.get(docId));
                        }
                    }

                    if (documentsToSync.isEmpty()) {
                        callback.onSuccess("All local data already synced");
                        return;
                    }

                    Log.i(TAG, "Found " + documentsToSync.size() + " documents to sync");
                    batchWriteToFirestore(userId, documentsToSync, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check existing documents", e);
                    callback.onError("Failed to check existing data: " + e.getMessage());
                });
    }

    /**
     * Batch write documents to Firestore
     */
    private void batchWriteToFirestore(String userId, List<ReceivedBtDataEntity> entities, SyncCallback callback) {
        int totalBatches = (int) Math.ceil((double) entities.size() / BATCH_SIZE);
        int completedBatches = 0;

        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, entities.size());
            List<ReceivedBtDataEntity> batch = entities.subList(i, endIndex);
            
            WriteBatch writeBatch = firestore.batch();
            
            for (ReceivedBtDataEntity entity : batch) {
                FirestoreDataModel firestoreModel = new FirestoreDataModel(
                    entity.getDeviceAddress(),
                    entity.getTimestamp(),
                    entity.getReceivedMsg(),
                    userId
                );
                
                String documentId = firestoreModel.getDocumentId();
                writeBatch.set(firestore.collection(COLLECTION_BT_DATA).document(documentId),
                              firestoreModel.toFirestoreDocument());
            }

            final int batchNumber = (i / BATCH_SIZE) + 1;
            writeBatch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Batch " + batchNumber + "/" + totalBatches + " synced successfully");
                        callback.onProgress(batchNumber, totalBatches);
                        
                        if (batchNumber == totalBatches) {
                            callback.onSuccess("All " + entities.size() + " messages synced successfully");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to sync batch " + batchNumber, e);
                        callback.onError("Failed to sync batch " + batchNumber + ": " + e.getMessage());
                    });
        }
    }

    /**
     * Start supervisor sync - downloads messages from supervised users
     * @deprecated Use startSupervisorMirrors() instead
     */
    @Deprecated
    private void startSupervisorSync() {
        if (isSupervisorSyncActive) {
            Log.d(TAG, "Supervisor sync already active");
            return;
        }

        if (!userSession.isSupervisor()) {
            Log.w(TAG, "User is not supervisor, cannot start supervisor sync");
            return;
        }

        List<String> supervisedUserIds = userSession.getSupervisedUserIds();
        if (supervisedUserIds.isEmpty()) {
            Log.w(TAG, "No supervised users found, cannot start supervisor sync");
            return;
        }

        Log.i(TAG, "Starting supervisor sync for " + supervisedUserIds.size() + " supervised users");
        isSupervisorSyncActive = true;

        // Start mirrors for each supervised user
        startSupervisorMirrors(supervisedUserIds);
    }

    /**
     * Download missing data from Firestore to local database (supervisor users)
     */
    public void syncFirestoreDataToLocal(SyncCallback callback) {
        if (!isUserAuthenticated()) {
            callback.onError("User not authenticated");
            return;
        }

        if (!userSession.isLoaded() || !userSession.isSupervisor()) {
            callback.onError("User is not supervisor or session not loaded");
            return;
        }

        if (!connectivityMonitor.isConnected()) {
            callback.onError("No internet connection");
            return;
        }

        executorService.execute(() -> {
            List<String> supervisedUserIds = userSession.getSupervisedUserIds();
            if (supervisedUserIds.isEmpty()) {
                callback.onSuccess("No supervised users to sync");
                return;
            }

            // For manual sync, do a one-time fetch from all supervised users
            syncFromSupervisedUsers(supervisedUserIds, callback);
        });
    }

    /**
     * Sync data from all supervised users (one-time fetch)
     */
    private void syncFromSupervisedUsers(List<String> supervisedUserIds, SyncCallback callback) {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        Map<Task<QuerySnapshot>, String> taskToUserId = new HashMap<>();

        for (String supervisedUserId : supervisedUserIds) {
            Task<QuerySnapshot> task = firestore.collection(COLLECTION_BT_DATA)
                    .whereEqualTo("userId", supervisedUserId)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get();
            tasks.add(task);
            taskToUserId.put(task, supervisedUserId);
        }

        // Wait for all queries to complete
        Task<Void> allTasks = com.google.android.gms.tasks.Tasks.whenAll(tasks);
        allTasks.addOnSuccessListener(aVoid -> {
            List<ReceivedBtDataEntity> allEntities = new ArrayList<>();
            
            // Process each completed task
            for (Task<QuerySnapshot> task : tasks) {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        String ownerUid = taskToUserId.get(task);
                        for (QueryDocumentSnapshot document : snapshot) {
                            try {
                                FirestoreDataModel firestoreModel = FirestoreDataModel.fromFirestoreDocument(document.getData());
                                
                                // Check if message already exists locally
                                int exists = dao.messageExistsOwned(
                                    ownerUid,
                                    firestoreModel.getDeviceAddress(),
                                    firestoreModel.getTimestamp(),
                                    firestoreModel.getReceivedMsg()
                                );

                                if (exists == 0) {
                                    ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                                        firestoreModel.getDeviceAddress(),
                                        firestoreModel.getTimestamp(),
                                        firestoreModel.getReceivedMsg(),
                                        ownerUid
                                    );
                                    allEntities.add(entity);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing supervised user data", e);
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "One of the supervised user queries failed", task.getException());
                }
            }

            if (allEntities.isEmpty()) {
                callback.onSuccess("Local database is up to date");
            } else {
                dao.insertAll(allEntities);
                callback.onSuccess("Added " + allEntities.size() + " missing messages to local database");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to sync from supervised users", e);
            callback.onError("Failed to sync from supervised users: " + e.getMessage());
        });
    }

    /**
     * Backfill local Room database from Firestore for supervised users
     * This method fetches historical data from Firestore and materializes it into Room
     * Used after app reinstall/clear data or first sign-in to restore local data
     */
    public void backfillLocalFromCloudForCurrentUser(SyncCallback callback) {
        Log.i(TAG, "Starting backfill from Firestore to local Room");
        
        if (!isUserAuthenticated()) {
            callback.onError("User not authenticated");
            return;
        }

        if (!userSession.isLoaded()) {
            Log.w(TAG, "User session not loaded, skipping backfill");
            callback.onError("User session not loaded");
            return;
        }

        if (!userSession.isSupervised()) {
            Log.d(TAG, "User is not supervised, skipping backfill");
            callback.onSuccess("User is not supervised, no backfill needed");
            return;
        }

        // One-time repair/migration: patch legacy rows with NULL owner to current supervised user's uid
        executorService.execute(() -> {
            try {
                String uid = auth.getCurrentUser().getUid();
                int fixed = dao.migrateSetOwnerForNull(uid);
                Log.i("DB/Migrate", "Assigned owner_user_id to " + fixed + " legacy rows for " + uid);
            } catch (Exception e) {
                Log.e("DB/Migrate", "Failed to run owner migration", e);
            }
        });

        if (!connectivityMonitor.isConnected()) {
            callback.onError("No internet connection");
            return;
        }

        executorService.execute(() -> {
            try {
                FirebaseUser user = auth.getCurrentUser();
                String userId = user.getUid();
                
                // Get the maximum timestamp from local database
                // If DB is empty (after reinstall), this returns 0 to fetch all data
                long localMaxTimestamp = dao.getMaxTimestampSync();
                Log.d(TAG, "Local max timestamp: " + localMaxTimestamp);
                
                // Query Firestore for messages newer than local max timestamp
                firestore.collection(COLLECTION_BT_DATA)
                        .whereEqualTo("userId", userId)
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .whereGreaterThan("timestamp", localMaxTimestamp)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<ReceivedBtDataEntity> entitiesToInsert = new ArrayList<>();
                            
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                try {
                                    FirestoreDataModel firestoreModel = FirestoreDataModel.fromFirestoreDocument(document.getData());
                                    Long ts = firestoreModel.getTimestamp();
                                    String msg = firestoreModel.getReceivedMsg();

                                    // ✅ set owner_user_id = current supervised user’s uid
                                    ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                                        firestoreModel.getDeviceAddress(),
                                        ts != null ? ts : 0L,
                                        msg != null ? msg : "",
                                        userId
                                    );
                                    entitiesToInsert.add(entity);
                                    
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing backfill document", e);
                                }
                            }

                            if (entitiesToInsert.isEmpty()) {
                                callback.onSuccess("Local database is up to date, no backfill needed");
                                return;
                            }

                            // Insert entities into Room database (idempotent due to unique index + IGNORE)
                            executorService.execute(() -> {
                                try {
                                    dao.insertAll(entitiesToInsert);
                                    Log.i(TAG, "Backfill completed: inserted " + entitiesToInsert.size() + " messages");
                                    // Verify owner rows now visible for supervised user
                                    int countMine = dao.dbgCountForOwner(userId);
                                    Log.i("VERIFY/AfterBackfill", "rows for owner=" + userId + " -> " + countMine);
                                    callback.onSuccess("Backfill completed: restored " + entitiesToInsert.size() + " messages");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error inserting backfill data", e);
                                    callback.onError("Failed to insert backfill data: " + e.getMessage());
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to fetch backfill data from Firestore", e);
                            callback.onError("Failed to fetch backfill data: " + e.getMessage());
                        });
                        
            } catch (Exception e) {
                Log.e(TAG, "Error during backfill", e);
                callback.onError("Backfill failed: " + e.getMessage());
            }
        });
    }

    /**
     * Paged backfill for large datasets - fetches data in chunks to avoid memory issues
     * and provides progress updates for better user experience
     */
    public void backfillLocalFromCloudForCurrentUserPaged(SyncCallback callback) {
        Log.i(TAG, "Starting paged backfill from Firestore to local Room");
        
        if (!isUserAuthenticated()) {
            callback.onError("User not authenticated");
            return;
        }

        if (!userSession.isLoaded()) {
            Log.w(TAG, "User session not loaded, skipping paged backfill");
            callback.onError("User session not loaded");
            return;
        }

        if (!userSession.isSupervised()) {
            Log.d(TAG, "User is not supervised, skipping paged backfill");
            callback.onSuccess("User is not supervised, no paged backfill needed");
            return;
        }

        if (!connectivityMonitor.isConnected()) {
            callback.onError("No internet connection");
            return;
        }

        executorService.execute(() -> {
            try {
                FirebaseUser user = auth.getCurrentUser();
                String userId = user.getUid();
                
                // Get the maximum timestamp from local database
                long localMaxTimestamp = dao.getMaxTimestampSync();
                Log.d(TAG, "Local max timestamp for paged backfill: " + localMaxTimestamp);
                
                // Start paged backfill
                fetchPagedBackfillData(userId, localMaxTimestamp, null, 0, callback);
                        
            } catch (Exception e) {
                Log.e(TAG, "Error during paged backfill", e);
                callback.onError("Paged backfill failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Recursively fetch paged data from Firestore
     */
    private void fetchPagedBackfillData(String userId, long localMaxTimestamp, 
                                       DocumentSnapshot lastDoc, int totalProcessed, 
                                       SyncCallback callback) {
        final int PAGE_SIZE = 500;
        
        Query query = firestore.collection(COLLECTION_BT_DATA)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .whereGreaterThan("timestamp", localMaxTimestamp)
                .limit(PAGE_SIZE);
        
        // Add pagination if we have a last document
        if (lastDoc != null) {
            query = query.startAfter(lastDoc);
        }
        
        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ReceivedBtDataEntity> entitiesToInsert = new ArrayList<>();
                    DocumentSnapshot finalLastDocument = null;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            FirestoreDataModel firestoreModel = FirestoreDataModel.fromFirestoreDocument(document.getData());
                            
                            ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                                firestoreModel.getDeviceAddress(),
                                firestoreModel.getTimestamp(),
                                firestoreModel.getReceivedMsg()
                            );
                            entitiesToInsert.add(entity);
                            finalLastDocument = document;
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing paged backfill document", e);
                        }
                    }

                    final DocumentSnapshot lastDocumentForRecursion = finalLastDocument;

                    if (entitiesToInsert.isEmpty()) {
                        // No more data to fetch
                        Log.i(TAG, "Paged backfill completed: processed " + totalProcessed + " total messages");
                        callback.onSuccess("Paged backfill completed: restored " + totalProcessed + " messages");
                        return;
                    }

                    // Insert current page into Room database
                    executorService.execute(() -> {
                        try {
                            dao.insertAll(entitiesToInsert);
                            int newTotal = totalProcessed + entitiesToInsert.size();
                            Log.d(TAG, "Paged backfill: inserted " + entitiesToInsert.size() + " messages, total: " + newTotal);
                            
                            // Update progress
                            callback.onProgress(newTotal, -1); // -1 indicates unknown total
                            
                            // Continue with next page if we got a full page
                            if (entitiesToInsert.size() == PAGE_SIZE) {
                                fetchPagedBackfillData(userId, localMaxTimestamp, lastDocumentForRecursion, newTotal, callback);
                            } else {
                                // Last page
                                callback.onSuccess("Paged backfill completed: restored " + newTotal + " messages");
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error inserting paged backfill data", e);
                            callback.onError("Failed to insert paged backfill data: " + e.getMessage());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch paged backfill data from Firestore", e);
                    callback.onError("Failed to fetch paged backfill data: " + e.getMessage());
                });
    }

    /**
     * Perform bidirectional sync - both directions
     */
    public void performFullSync(SyncCallback callback) {
        Log.i(TAG, "Starting full bidirectional sync");
        
        if (!userSession.isLoaded()) {
            callback.onError("User session not loaded");
            return;
        }

        if (userSession.isSupervisor()) {
            // For supervisors, sync from Firestore to local
            syncFirestoreDataToLocal(callback);
        } else if (userSession.isSupervised()) {
            // For supervised users, sync from local to Firestore
            syncLocalDataToFirestore(callback);
        } else {
            callback.onError("Unknown user role");
        }
    }

    /**
     * Get all local data (this should be done on background thread)
     */
    private List<ReceivedBtDataEntity> getAllLocalData() {
        return dao.getAllDataSync();
    }

    /**
     * Check if user is authenticated
     */
    private boolean isUserAuthenticated() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null;
    }

    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Check if currently connected to internet
     */
    public boolean isConnected() {
        return connectivityMonitor.isConnected();
    }

    /**
     * Get current user role
     */
    public String getCurrentUserRole() {
        return userSession.getRole();
    }

    /**
     * Check if user session is loaded
     */
    public boolean isUserSessionLoaded() {
        return userSession.isLoaded();
    }

    /**
     * Purge old data and backfill for supervisor with supervised users
     */
    public void purgeAndBackfillForSupervisor(List<String> supervisedUserIds, SyncCallback callback) {
        Log.i(TAG, "Starting supervisor purge and backfill for " + supervisedUserIds.size() + " users");
        
        if (!isUserAuthenticated()) {
            callback.onError("User not authenticated");
            return;
        }

        if (!userSession.isLoaded() || !userSession.isSupervisor()) {
            callback.onError("User is not supervisor or session not loaded");
            return;
        }

        if (!connectivityMonitor.isConnected()) {
            callback.onError("No internet connection");
            return;
        }

        executorService.execute(() -> {
            try {
                // First, purge old data that doesn't belong to current supervised users
                int deletedRows = dao.deleteWhereOwnerNotIn(supervisedUserIds);
                Log.i(TAG, "Purged " + deletedRows + " old data rows");
                
                // Then backfill for each supervised user
                backfillForSupervisedUsers(supervisedUserIds, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error during supervisor purge and backfill", e);
                callback.onError("Purge and backfill failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Backfill data for multiple supervised users
     */
    private void backfillForSupervisedUsers(List<String> supervisedUserIds, SyncCallback callback) {
        int totalUsers = supervisedUserIds.size();
        AtomicInteger completedUsers = new AtomicInteger(0);
        
        for (String supervisedUserId : supervisedUserIds) {
            backfillLocalFromCloudForSupervisedUser(supervisedUserId, new FirestoreSyncService.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    int currentCompleted = completedUsers.incrementAndGet();
                    Log.i(TAG, "Backfill completed for " + supervisedUserId + ": " + message);
                    
                    if (currentCompleted == totalUsers) {
                        // Start mirrors for all supervised users
                        startSupervisorMirrors(supervisedUserIds);
                        callback.onSuccess("Supervisor pipeline completed for " + totalUsers + " users");
                    }
                }
                
                @Override
                public void onError(String error) {
                    int currentCompleted = completedUsers.incrementAndGet();
                    Log.w(TAG, "Backfill failed for " + supervisedUserId + ": " + error);
                    
                    if (currentCompleted == totalUsers) {
                        // Start mirrors even if some backfills failed
                        startSupervisorMirrors(supervisedUserIds);
                        callback.onSuccess("Supervisor pipeline completed with some errors");
                    }
                }
                
                @Override
                public void onProgress(int current, int total) {
                    Log.d(TAG, "Backfill progress for " + supervisedUserId + ": " + current + "/" + total);
                }
            });
        }
    }
    
    /**
     * Backfill local data from cloud for a specific supervised user
     */
    public void backfillLocalFromCloudForSupervisedUser(String supervisedUserId, SyncCallback callback) {
        Log.i(TAG, "Starting backfill for supervised user: " + supervisedUserId);
        
        if (!isUserAuthenticated()) {
            callback.onError("User not authenticated");
            return;
        }

        if (!userSession.isLoaded() || !userSession.isSupervisor()) {
            callback.onError("User is not supervisor or session not loaded");
            return;
        }

        if (!connectivityMonitor.isConnected()) {
            callback.onError("No internet connection");
            return;
        }

        executorService.execute(() -> {
            try {
                // Get the maximum timestamp from local database for this supervised user
                long localMaxTimestamp = dao.getMaxTimestampForOwner(supervisedUserId);
                Log.i("SYNC/Backfill", "Start backfill for childUid=" + supervisedUserId + " sinceTs=" + localMaxTimestamp);
                
                // Query Firestore for messages newer than local max timestamp
                firestore.collection(COLLECTION_BT_DATA)
                        .whereEqualTo("userId", supervisedUserId)
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .whereGreaterThan("timestamp", localMaxTimestamp)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            List<ReceivedBtDataEntity> entitiesToInsert = new ArrayList<>();
                            
                            Log.i("SYNC/Backfill", "Fetched docs=" + queryDocumentSnapshots.size());
                            
                            long minTs = Long.MAX_VALUE, maxTs = Long.MIN_VALUE;
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                try {
                                    FirestoreDataModel firestoreModel = FirestoreDataModel.fromFirestoreDocument(document.getData());
                                    
                                    Long ts = firestoreModel.getTimestamp();
                                    String msg = firestoreModel.getReceivedMsg();
                                    String userId = supervisedUserId; // This is the supervised user ID
                                    minTs = Math.min(minTs, ts == null ? Long.MAX_VALUE : ts);
                                    maxTs = Math.max(maxTs, ts == null ? Long.MIN_VALUE : ts);
                                    
                                    // Build entity — **ALWAYS** assign owner_user_id = supervisedUserId
                                    ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                                        firestoreModel.getDeviceAddress(),
                                        ts != null ? ts : 0L,
                                        msg != null ? msg : "",
                                        supervisedUserId // <- THIS IS CRITICAL
                                    );
                                    
                                    // OWNER MAPPING VERIFICATION
                                    if (entity.getOwnerUserId() == null || !supervisedUserId.equals(entity.getOwnerUserId())) {
                                        Log.e("SYNC/BackfillMap", "BAD OWNER: " + entity.getOwnerUserId() + " expected=" + supervisedUserId);
                                    }
                                    
                                    Log.d("SYNC/BackfillMap",
                                        "doc=" + document.getId() +
                                        " userId=" + userId +
                                        " ts=" + ts +
                                        " owner(set)=" + entity.getOwnerUserId() +
                                        " msg=" + entity.getReceivedMsg());
                                    entitiesToInsert.add(entity);
                                    
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing backfill document for " + supervisedUserId, e);
                                }
                            }
                            
                            Log.i("SYNC/Backfill", "Snapshot ts range: [" + minTs + ", " + maxTs + "], will insert=" + entitiesToInsert.size());

                            if (entitiesToInsert.isEmpty()) {
                                callback.onSuccess("No new data for " + supervisedUserId);
                                return;
                            }

                            // Insert entities into Room database
                            executorService.execute(() -> {
                                try {
                                    dao.insertAll(entitiesToInsert);
                                    
                                    // POST-BACKFILL VERIFICATION
                                    int total = dao.dbgCountAll();
                                    int mine = dao.dbgCountForOwner(supervisedUserId);
                                    List<ReceivedBtDataEntity> latest3 = dao.dbgLatestForOwner(supervisedUserId, 3);

                                    Log.i("SYNC/BackfillVerify",
                                          "owner=" + supervisedUserId + " inserted=" + entitiesToInsert.size() +
                                          " | Room total=" + total + " mine=" + mine);

                                    for (ReceivedBtDataEntity e : latest3) {
                                        Log.d("SYNC/BackfillVerifyRow",
                                              "ts=" + e.getTimestamp() + " msg=" + e.getReceivedMsg() +
                                              " dev=" + e.getDeviceAddress() + " ownerCol=" + e.getOwnerUserId());
                                    }

                                    if (mine == 0) {
                                        Log.e("SYNC/BackfillVerify", "FAIL: No rows for owner after backfill. Check owner_user_id mapping & DB instance.");
                                    }
                                    
                                    Log.i(TAG, "Backfill completed for " + supervisedUserId + ": inserted " + entitiesToInsert.size() + " messages");
                                    callback.onSuccess("Backfill completed for " + supervisedUserId + ": " + entitiesToInsert.size() + " messages");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error inserting backfill data for " + supervisedUserId, e);
                                    callback.onError("Failed to insert backfill data for " + supervisedUserId + ": " + e.getMessage());
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to fetch backfill data for " + supervisedUserId, e);
                            callback.onError("Failed to fetch backfill data for " + supervisedUserId + ": " + e.getMessage());
                        });
                        
            } catch (Exception e) {
                Log.e(TAG, "Error during backfill for " + supervisedUserId, e);
                callback.onError("Backfill failed for " + supervisedUserId + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Start supervisor mirrors for all supervised users
     */
    private void startSupervisorMirrors(List<String> supervisedUserIds) {
        Log.i(TAG, "Starting supervisor mirrors for " + supervisedUserIds.size() + " users");
        
        for (String supervisedUserId : supervisedUserIds) {
            startSupervisorMirror(supervisedUserId);
        }
    }
    
    /**
     * Start a single supervisor mirror for a specific supervised user
     */
    public void startSupervisorMirror(String supervisedUserId) {
        // Check if already active
        if (mirrorByUid.containsKey(supervisedUserId)) {
            Log.d(TAG, "Mirror already active for " + supervisedUserId);
            return;
        }
        
        // Check if start is in flight
        if (startingUids.contains(supervisedUserId)) {
            Log.d(TAG, "Mirror start already in flight for " + supervisedUserId);
            return;
        }
        
        startingUids.add(supervisedUserId);
        Log.i("SYNC/Mirror", "Attach listener for childUid=" + supervisedUserId + " query=userId==childUid orderBy timestamp");
        
        ListenerRegistration listener = firestore.collection(COLLECTION_BT_DATA)
                .whereEqualTo("userId", supervisedUserId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("SYNC/Mirror", "Listen error for " + supervisedUserId + ": " + e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots == null) {
                        Log.w("SYNC/Mirror", "Null snapshot for " + supervisedUserId);
                        return;
                    }
                    Log.i("SYNC/Mirror", "Event for " + supervisedUserId + ": docs=" + queryDocumentSnapshots.size() + " fromCache=" + queryDocumentSnapshots.getMetadata().isFromCache());

                    executorService.execute(() -> {
                        handleSupervisorDocumentChanges(queryDocumentSnapshots.getDocumentChanges(), supervisedUserId);
                    });
                });

        mirrorByUid.put(supervisedUserId, listener);
        startingUids.remove(supervisedUserId);
        Log.i(TAG, "Supervisor mirror started for " + supervisedUserId);
    }
    
    /**
     * Stop a single supervisor mirror
     */
    public void stopSupervisorMirror(String supervisedUserId) {
        ListenerRegistration listener = mirrorByUid.remove(supervisedUserId);
        if (listener != null) {
            listener.remove();
            Log.d("SYNC/Mirror", "Detach listener for childUid=" + supervisedUserId + " reason=manual_stop");
            Log.i(TAG, "Stopped supervisor mirror for " + supervisedUserId);
        }
        startingUids.remove(supervisedUserId);
    }
    
    /**
     * Stop all supervisor mirrors
     */
    public void stopAllMirrors() {
        Log.i(TAG, "Stopping all supervisor mirrors");
        
        for (Map.Entry<String, ListenerRegistration> entry : mirrorByUid.entrySet()) {
            entry.getValue().remove();
        }
        mirrorByUid.clear();
        startingUids.clear();
        isSupervisorSyncActive = false;
    }
    
    /**
     * Clear all local data (for sign-out)
     */
    public void clearLocalData() {
        Log.i(TAG, "Clearing all local data");
        executorService.execute(() -> {
            int deletedRows = dao.clearAllData();
            Log.i(TAG, "Cleared " + deletedRows + " rows from local database");
        });
    }
    
    /**
     * Handle document changes from supervisor listeners with owner assignment
     */
    private void handleSupervisorDocumentChanges(List<DocumentChange> documentChanges, String supervisedUserId) {
        List<ReceivedBtDataEntity> entitiesToInsert = new ArrayList<>();
        long now = System.currentTimeMillis();
        final long RECENT_MS = 60_000 * 15; // alert only if within 15 minutes

        for (DocumentChange change : documentChanges) {
            if (change.getType() == DocumentChange.Type.ADDED || change.getType() == DocumentChange.Type.MODIFIED) {
                try {
                    FirestoreDataModel firestoreModel = FirestoreDataModel.fromFirestoreDocument(change.getDocument().getData());
                    
                    // Check if message already exists locally
                    int exists = dao.messageExistsOwned(
                        supervisedUserId,
                        firestoreModel.getDeviceAddress(),
                        firestoreModel.getTimestamp(),
                        firestoreModel.getReceivedMsg()
                    );

                    if (exists == 0) {
                        Long ts = firestoreModel.getTimestamp();
                        String msg = firestoreModel.getReceivedMsg();
                        
                        // Build entity — **ensure owner_user_id = supervisedUserId**
                        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                            firestoreModel.getDeviceAddress(),
                            ts != null ? ts : 0L,
                            msg != null ? msg : "",
                            supervisedUserId
                        );
                        entitiesToInsert.add(entity);
                        Log.d(TAG, "New message from supervised user " + supervisedUserId + ": " + entity.getReceivedMsg());

                        // If this looks like a fall AND it's recent, notify on supervisor phone
                        if (msg != null && ts != null && (now - ts) <= RECENT_MS) {
                            Posture p = com.melisa.innovamotionapp.data.posture.PostureFactory.createPosture(msg);
                            if (p instanceof com.melisa.innovamotionapp.data.posture.types.FallingPosture) {
                                // TODO: optionally resolve a friendly name for supervisedUserId
                                String who = "Supervised user";

                                // Use your centralized strings + new 3-arg signature
                                String body = who + " (" + supervisedUserId + ") "
                                        + context.getString(com.melisa.innovamotionapp.R.string.notif_fall_text_generic);

                                com.melisa.innovamotionapp.utils.AlertNotifications.notifyFall(
                                        context,
                                        who,
                                        body
                                );
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing supervisor document change for " + supervisedUserId, e);
                }
            }
        }

        // Insert new messages into local Room database
        if (!entitiesToInsert.isEmpty()) {
            dao.insertAll(entitiesToInsert);
            
            // MIRROR VERIFICATION
            executorService.execute(() -> {
                int mine = dao.dbgCountForOwner(supervisedUserId);
                Log.i("SYNC/MirrorVerify", "owner=" + supervisedUserId + " applied=" + entitiesToInsert.size() + " now mine=" + mine);
            });
            
            Log.i(TAG, "Inserted " + entitiesToInsert.size() + " new messages from supervised user " + supervisedUserId);
        }
    }

    /**
     * One-shot "cloud vs local" verifier (manual button or dev menu)
     * Run this after login to compare Firestore count vs Room count for a child
     */
    public void verifyCloudVsLocal(String childUid) {
        Log.i("VERIFY", "Start for childUid=" + childUid);

        // 1) Firestore one-shot count
        firestore.collection(COLLECTION_BT_DATA)
            .whereEqualTo("userId", childUid)
            .get()
            .addOnSuccessListener(snap -> {
                int cloud = snap.size();
                Log.i("VERIFY", "Cloud count=" + cloud);

                // 2) Room count
                executorService.execute(() -> {
                    int local = dao.countForOwner(childUid);
                    Log.i("VERIFY", "Local count=" + local + " (owner=" + childUid + ")");

                    if (local < cloud) {
                        Log.w("VERIFY", "Local is behind cloud by " + (cloud - local));
                    } else if (local > cloud) {
                        Log.w("VERIFY", "Local has MORE than cloud (duplicates or extra owners?)");
                    } else {
                        Log.i("VERIFY", "Local matches cloud ✅");
                    }
                });
            })
            .addOnFailureListener(e -> Log.e("VERIFY", "Cloud read failed: " + e));
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        Log.i(TAG, "Cleaning up FirestoreSyncService");
        stopAllMirrors();
        connectivityMonitor.cleanup();
        userSession.cleanup();
        executorService.shutdown();
        scheduledExecutor.shutdown();
    }
}