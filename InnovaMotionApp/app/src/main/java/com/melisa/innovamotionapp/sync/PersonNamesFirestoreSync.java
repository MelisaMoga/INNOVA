package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.data.database.MonitoredPersonDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles Firestore sync for monitored person names.
 * 
 * Aggregators upload person names to their user document subcollection.
 * Supervisors download person names from their linked aggregator.
 * 
 * Firestore structure:
 * users/{aggregatorUid}/monitored_persons/{sensorId}
 *   - sensorId: String
 *   - displayName: String
 *   - updatedAt: Long
 * 
 * @deprecated Use {@link SensorInventoryService} instead. 
 * This class uses a subcollection structure that is being phased out
 * in favor of a root-level 'sensors' collection.
 */
@Deprecated
public class PersonNamesFirestoreSync {
    private static final String TAG = "PersonNamesSync";
    private static final String COLLECTION_MONITORED_PERSONS = "monitored_persons";

    private static volatile PersonNamesFirestoreSync instance;
    
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final MonitoredPersonDao dao;
    private final ExecutorService executor;

    private PersonNamesFirestoreSync(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.dao = InnovaDatabase.getInstance(context.getApplicationContext()).monitoredPersonDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Get the singleton instance.
     */
    public static PersonNamesFirestoreSync getInstance(Context context) {
        if (instance == null) {
            synchronized (PersonNamesFirestoreSync.class) {
                if (instance == null) {
                    instance = new PersonNamesFirestoreSync(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Upload all person names to Firestore.
     * Collection path: users/{currentUserUid}/monitored_persons/{sensorId}
     * 
     * @param callback Callback for success/error
     */
    public void uploadAllNames(@NonNull SyncCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        String aggregatorUid = user.getUid();
        
        executor.execute(() -> {
            try {
                List<MonitoredPerson> persons = dao.getAllMonitoredPersonsSync();
                
                if (persons.isEmpty()) {
                    callback.onSuccess("No person names to upload");
                    return;
                }

                WriteBatch batch = firestore.batch();
                
                for (MonitoredPerson person : persons) {
                    DocumentReference docRef = firestore
                            .collection("users")
                            .document(aggregatorUid)
                            .collection(COLLECTION_MONITORED_PERSONS)
                            .document(person.getSensorId());

                    Map<String, Object> data = new HashMap<>();
                    data.put("sensorId", person.getSensorId());
                    data.put("displayName", person.getDisplayName());
                    data.put("updatedAt", person.getUpdatedAt());

                    batch.set(docRef, data);
                }

                batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Uploaded " + persons.size() + " person names");
                            callback.onSuccess("Uploaded " + persons.size() + " names");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to upload person names", e);
                            callback.onError("Upload failed: " + e.getMessage());
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error preparing upload", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Upload a single person name to Firestore.
     * 
     * @param sensorId    The sensor ID
     * @param displayName The display name
     * @param callback    Callback for success/error
     */
    public void uploadSingleName(@NonNull String sensorId, @NonNull String displayName, @NonNull SyncCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        String aggregatorUid = user.getUid();
        
        DocumentReference docRef = firestore
                .collection("users")
                .document(aggregatorUid)
                .collection(COLLECTION_MONITORED_PERSONS)
                .document(sensorId);

        Map<String, Object> data = new HashMap<>();
        data.put("sensorId", sensorId);
        data.put("displayName", displayName);
        data.put("updatedAt", System.currentTimeMillis());

        docRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Uploaded name for " + sensorId);
                    callback.onSuccess("Name uploaded");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload name for " + sensorId, e);
                    callback.onError("Upload failed: " + e.getMessage());
                });
    }

    /**
     * Download person names from a specific aggregator (for supervisors).
     * 
     * @param aggregatorUid The aggregator's user ID
     * @param callback      Callback for success/error
     */
    public void downloadNamesFromAggregator(@NonNull String aggregatorUid, @NonNull SyncCallback callback) {
        firestore.collection("users")
                .document(aggregatorUid)
                .collection(COLLECTION_MONITORED_PERSONS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    executor.execute(() -> {
                        int count = 0;
                        for (DocumentSnapshot doc : querySnapshot) {
                            try {
                                String sensorId = doc.getString("sensorId");
                                String displayName = doc.getString("displayName");
                                
                                if (sensorId != null && displayName != null) {
                                    dao.upsertByName(sensorId, displayName, System.currentTimeMillis());
                                    count++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing document: " + doc.getId(), e);
                            }
                        }
                        
                        Log.d(TAG, "Downloaded " + count + " person names from aggregator " + aggregatorUid);
                        final int finalCount = count;
                        callback.onSuccess("Downloaded " + finalCount + " names");
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to download person names", e);
                    callback.onError("Download failed: " + e.getMessage());
                });
    }

    /**
     * Delete a person name from Firestore.
     * 
     * @param sensorId The sensor ID to delete
     * @param callback Callback for success/error
     */
    public void deleteName(@NonNull String sensorId, @NonNull SyncCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("User not authenticated");
            return;
        }

        String aggregatorUid = user.getUid();
        
        firestore.collection("users")
                .document(aggregatorUid)
                .collection(COLLECTION_MONITORED_PERSONS)
                .document(sensorId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deleted name for " + sensorId);
                    callback.onSuccess("Name deleted");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete name for " + sensorId, e);
                    callback.onError("Delete failed: " + e.getMessage());
                });
    }

    /**
     * Callback interface for sync operations.
     */
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
