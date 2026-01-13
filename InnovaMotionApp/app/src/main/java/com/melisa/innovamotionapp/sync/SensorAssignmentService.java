package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.melisa.innovamotionapp.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing sensor-supervisor assignments.
 * 
 * Handles:
 * - Assigning a supervisor to a sensor (creates mapping + updates supervisor's sensorIds array)
 * - Unassigning a supervisor from a sensor
 * - Looking up which supervisor is assigned to a sensor
 * - Searching for supervisors by email
 */
public class SensorAssignmentService {
    private static final String TAG = "SensorAssignmentService";
    private static final String COLLECTION_ASSIGNMENTS = Constants.FIRESTORE_COLLECTION_SENSOR_ASSIGNMENTS;
    private static final String COLLECTION_USERS = Constants.FIRESTORE_COLLECTION_USERS;
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    
    private static SensorAssignmentService instance;
    
    /**
     * Represents a sensor-supervisor assignment.
     */
    public static class Assignment {
        public final String sensorId;
        public final String supervisorUid;
        public final String supervisorEmail;
        public final String aggregatorUid;
        public final long assignedAt;
        
        public Assignment(String sensorId, String supervisorUid, String supervisorEmail,
                          String aggregatorUid, long assignedAt) {
            this.sensorId = sensorId;
            this.supervisorUid = supervisorUid;
            this.supervisorEmail = supervisorEmail;
            this.aggregatorUid = aggregatorUid;
            this.assignedAt = assignedAt;
        }
        
        /**
         * Create from Firestore document.
         */
        public static Assignment fromDocument(String sensorId, DocumentSnapshot doc) {
            if (doc == null || !doc.exists()) {
                return null;
            }
            return new Assignment(
                sensorId,
                doc.getString("supervisorUid"),
                doc.getString("supervisorEmail"),
                doc.getString("aggregatorUid"),
                doc.getLong("assignedAt") != null ? doc.getLong("assignedAt") : 0
            );
        }
    }
    
    /**
     * Represents a supervisor user info.
     */
    public static class SupervisorInfo {
        public final String uid;
        public final String email;
        public final String displayName;
        
        public SupervisorInfo(String uid, String email, String displayName) {
            this.uid = uid;
            this.email = email;
            this.displayName = displayName;
        }
    }
    
    /**
     * Callback for assignment operations.
     */
    public interface AssignmentCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Callback for lookup operations.
     */
    public interface LookupCallback {
        void onResult(@Nullable Assignment assignment);
        void onError(String error);
    }
    
    /**
     * Callback for supervisor search.
     */
    public interface SearchCallback {
        void onResult(List<SupervisorInfo> supervisors);
        void onError(String error);
    }
    
    /**
     * Callback for finding a single supervisor by email.
     */
    public interface FindSupervisorCallback {
        void onFound(SupervisorInfo supervisor);
        void onNotFound();
        void onError(String error);
    }
    
    public static synchronized SensorAssignmentService getInstance(Context context) {
        if (instance == null) {
            instance = new SensorAssignmentService(context.getApplicationContext());
        }
        return instance;
    }
    
    private SensorAssignmentService(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    /**
     * Assign a supervisor to a sensor.
     * 
     * Steps:
     * 1. Find supervisor by email
     * 2. Create/update sensor_assignments/{sensorId} document
     * 3. Add sensorId to supervisor's supervisedSensorIds array
     * 
     * @param sensorId The sensor ID to assign
     * @param supervisorEmail The supervisor's email
     * @param callback Result callback
     */
    public void assignSupervisor(@NonNull String sensorId, @NonNull String supervisorEmail,
                                  @NonNull AssignmentCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Not authenticated");
            return;
        }
        
        String aggregatorUid = currentUser.getUid();
        Log.d(TAG, "Assigning supervisor " + supervisorEmail + " to sensor " + sensorId);
        
        // First, find the supervisor by email
        findSupervisorByEmail(supervisorEmail, new FindSupervisorCallback() {
            @Override
            public void onFound(SupervisorInfo supervisor) {
                // Check if there's an existing assignment and unassign if different supervisor
                getSupervisorForSensor(sensorId, new LookupCallback() {
                    @Override
                    public void onResult(@Nullable Assignment existing) {
                        if (existing != null && !existing.supervisorUid.equals(supervisor.uid)) {
                            // Need to remove sensorId from old supervisor first
                            removeFromSupervisorArray(existing.supervisorUid, sensorId, () -> {
                                performAssignment(sensorId, supervisor, aggregatorUid, callback);
                            });
                        } else {
                            performAssignment(sensorId, supervisor, aggregatorUid, callback);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        // Proceed with assignment anyway
                        Log.w(TAG, "Error checking existing assignment: " + error);
                        performAssignment(sensorId, supervisor, aggregatorUid, callback);
                    }
                });
            }
            
            @Override
            public void onNotFound() {
                callback.onError("Supervisor not found with email: " + supervisorEmail);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Perform the actual assignment (internal helper).
     */
    private void performAssignment(String sensorId, SupervisorInfo supervisor,
                                   String aggregatorUid, AssignmentCallback callback) {
        Map<String, Object> assignmentData = new HashMap<>();
        assignmentData.put("supervisorUid", supervisor.uid);
        assignmentData.put("supervisorEmail", supervisor.email);
        assignmentData.put("aggregatorUid", aggregatorUid);
        assignmentData.put("assignedAt", System.currentTimeMillis());
        
        // Create/update the assignment document
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .document(sensorId)
                .set(assignmentData)
                .addOnSuccessListener(aVoid -> {
                    // Now add sensorId to supervisor's array
                    firestore.collection(COLLECTION_USERS)
                            .document(supervisor.uid)
                            .update("supervisedSensorIds", FieldValue.arrayUnion(sensorId))
                            .addOnSuccessListener(aVoid2 -> {
                                Log.i(TAG, "Successfully assigned " + supervisor.email + " to " + sensorId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update supervisor array", e);
                                callback.onError("Failed to update supervisor: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create assignment", e);
                    callback.onError("Failed to create assignment: " + e.getMessage());
                });
    }
    
    /**
     * Remove a sensorId from a supervisor's array (internal helper).
     */
    private void removeFromSupervisorArray(String supervisorUid, String sensorId, Runnable onComplete) {
        firestore.collection(COLLECTION_USERS)
                .document(supervisorUid)
                .update("supervisedSensorIds", FieldValue.arrayRemove(sensorId))
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Failed to remove sensorId from old supervisor", task.getException());
                    }
                    onComplete.run();
                });
    }
    
    /**
     * Unassign a supervisor from a sensor.
     * 
     * Steps:
     * 1. Get current assignment to find supervisor UID
     * 2. Delete sensor_assignments/{sensorId} document
     * 3. Remove sensorId from supervisor's supervisedSensorIds array
     * 
     * @param sensorId The sensor ID to unassign
     * @param callback Result callback
     */
    public void unassignSupervisor(@NonNull String sensorId, @NonNull AssignmentCallback callback) {
        Log.d(TAG, "Unassigning supervisor from sensor " + sensorId);
        
        // First get the current assignment to know which supervisor to update
        getSupervisorForSensor(sensorId, new LookupCallback() {
            @Override
            public void onResult(@Nullable Assignment assignment) {
                if (assignment == null) {
                    Log.d(TAG, "No assignment found for sensor " + sensorId);
                    callback.onSuccess(); // Nothing to unassign
                    return;
                }
                
                // Delete the assignment document
                firestore.collection(COLLECTION_ASSIGNMENTS)
                        .document(sensorId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            // Remove from supervisor's array
                            firestore.collection(COLLECTION_USERS)
                                    .document(assignment.supervisorUid)
                                    .update("supervisedSensorIds", FieldValue.arrayRemove(sensorId))
                                    .addOnSuccessListener(aVoid2 -> {
                                        Log.i(TAG, "Successfully unassigned supervisor from " + sensorId);
                                        callback.onSuccess();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Assignment deleted but failed to update supervisor array", e);
                                        callback.onSuccess(); // Consider partial success
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete assignment", e);
                            callback.onError("Failed to unassign: " + e.getMessage());
                        });
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Get the supervisor assignment for a sensor.
     * 
     * @param sensorId The sensor ID to look up
     * @param callback Result callback (assignment may be null if not assigned)
     */
    public void getSupervisorForSensor(@NonNull String sensorId, @NonNull LookupCallback callback) {
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .document(sensorId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Assignment assignment = Assignment.fromDocument(sensorId, doc);
                        callback.onResult(assignment);
                    } else {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get assignment for " + sensorId, e);
                    callback.onError("Failed to lookup assignment: " + e.getMessage());
                });
    }
    
    /**
     * Search for supervisors by partial email match.
     * 
     * @param query The search query (partial email)
     * @param callback Result callback with matching supervisors
     */
    public void searchSupervisorsByEmail(@NonNull String query, @NonNull SearchCallback callback) {
        if (query.trim().length() < 2) {
            callback.onResult(new ArrayList<>());
            return;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Query all supervisors and filter client-side
        // (Firestore doesn't support contains/like queries natively)
        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("role", Constants.ROLE_SUPERVISOR)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<SupervisorInfo> matches = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String email = doc.getString("email");
                        if (email != null && email.toLowerCase().contains(lowerQuery)) {
                            String displayName = doc.getString("displayName");
                            matches.add(new SupervisorInfo(doc.getId(), email, displayName));
                        }
                    }
                    Log.d(TAG, "Found " + matches.size() + " supervisors matching '" + query + "'");
                    callback.onResult(matches);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to search supervisors", e);
                    callback.onError("Failed to search: " + e.getMessage());
                });
    }
    
    /**
     * Find a supervisor by exact email match.
     * 
     * @param email The exact email to find
     * @param callback Result callback
     */
    public void findSupervisorByEmail(@NonNull String email, @NonNull FindSupervisorCallback callback) {
        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("role", Constants.ROLE_SUPERVISOR)
                .whereEqualTo("email", email.trim().toLowerCase())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String displayName = doc.getString("displayName");
                        SupervisorInfo info = new SupervisorInfo(doc.getId(), email, displayName);
                        callback.onFound(info);
                    } else {
                        // Try without lowercasing in case emails are stored differently
                        firestore.collection(COLLECTION_USERS)
                                .whereEqualTo("role", Constants.ROLE_SUPERVISOR)
                                .whereEqualTo("email", email.trim())
                                .limit(1)
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    if (!qs2.isEmpty()) {
                                        DocumentSnapshot doc = qs2.getDocuments().get(0);
                                        String displayName = doc.getString("displayName");
                                        SupervisorInfo info = new SupervisorInfo(doc.getId(), email, displayName);
                                        callback.onFound(info);
                                    } else {
                                        callback.onNotFound();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to find supervisor", e);
                                    callback.onError("Failed to find supervisor: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to find supervisor", e);
                    callback.onError("Failed to find supervisor: " + e.getMessage());
                });
    }
    
    /**
     * Get all sensor assignments for the current aggregator.
     * Useful for displaying which sensors have supervisors assigned.
     * 
     * @param callback Result callback with map of sensorId -> supervisorEmail
     */
    public void getAllAssignmentsForAggregator(@NonNull SearchCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Not authenticated");
            return;
        }
        
        String aggregatorUid = currentUser.getUid();
        
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .whereEqualTo("aggregatorUid", aggregatorUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<SupervisorInfo> assignments = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String supervisorEmail = doc.getString("supervisorEmail");
                        String supervisorUid = doc.getString("supervisorUid");
                        // Use sensorId as the "displayName" to convey which sensor
                        assignments.add(new SupervisorInfo(doc.getId(), supervisorEmail, supervisorUid));
                    }
                    callback.onResult(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get assignments", e);
                    callback.onError("Failed to load assignments: " + e.getMessage());
                });
    }
    
    /**
     * Get a map of sensorId -> supervisorEmail for the current aggregator.
     * More convenient for UI binding.
     */
    public interface AssignmentMapCallback {
        void onResult(Map<String, String> sensorToSupervisorEmail);
        void onError(String error);
    }
    
    public void getAssignmentMap(@NonNull AssignmentMapCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Not authenticated");
            return;
        }
        
        String aggregatorUid = currentUser.getUid();
        
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .whereEqualTo("aggregatorUid", aggregatorUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, String> map = new HashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String sensorId = doc.getId();
                        String supervisorEmail = doc.getString("supervisorEmail");
                        if (supervisorEmail != null) {
                            map.put(sensorId, supervisorEmail);
                        }
                    }
                    callback.onResult(map);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get assignment map", e);
                    callback.onError("Failed to load assignments: " + e.getMessage());
                });
    }
}
