package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.melisa.innovamotionapp.data.models.Assignment;
import com.melisa.innovamotionapp.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing sensor-supervisor assignments.
 * 
 * Collection: assignments
 * Document ID: {supervisorUid}_{sensorId}
 * 
 * This is the SINGLE source of truth for permissions.
 * We no longer update arrays on user profiles - just query this collection.
 */
public class SensorAssignmentService {
    private static final String TAG = "SensorAssignmentService";
    private static final String COLLECTION_ASSIGNMENTS = Constants.FIRESTORE_COLLECTION_ASSIGNMENTS;
    private static final String COLLECTION_USERS = Constants.FIRESTORE_COLLECTION_USERS;
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    
    private static SensorAssignmentService instance;
    
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
     * Callback for fetching multiple assignments.
     */
    public interface AssignmentListCallback {
        void onResult(List<Assignment> assignments);
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
     * Creates a document in the 'assignments' collection.
     * Document ID: {supervisorUid}_{sensorId}
     * 
     * @param sensorId        The sensor ID to assign
     * @param supervisorEmail The supervisor's email
     * @param callback        Result callback
     */
    public void assignSupervisor(@NonNull String sensorId, @NonNull String supervisorEmail,
                                  @NonNull AssignmentCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Not authenticated");
            return;
        }
        
        String assignedBy = currentUser.getUid();
        Log.d(TAG, "Assigning supervisor " + supervisorEmail + " to sensor " + sensorId);
        
        // First, find the supervisor by email
        findSupervisorByEmail(supervisorEmail, new FindSupervisorCallback() {
            @Override
            public void onFound(SupervisorInfo supervisor) {
                performAssignment(sensorId, supervisor.uid, assignedBy, callback);
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
     * Assign a supervisor to a sensor by UID.
     * 
     * @param sensorId      The sensor ID to assign
     * @param supervisorUid The supervisor's UID
     * @param callback      Result callback
     */
    public void assignSupervisorByUid(@NonNull String sensorId, @NonNull String supervisorUid,
                                       @NonNull AssignmentCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Not authenticated");
            return;
        }
        
        String assignedBy = currentUser.getUid();
        performAssignment(sensorId, supervisorUid, assignedBy, callback);
    }
    
    /**
     * Perform the actual assignment (internal helper).
     */
    private void performAssignment(String sensorId, String supervisorUid,
                                   String assignedBy, AssignmentCallback callback) {
        Assignment assignment = new Assignment(supervisorUid, sensorId, assignedBy);
        String docId = assignment.getDocumentId();
        
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .document(docId)
                .set(assignment.toFirestoreDocument())
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Successfully assigned supervisor " + supervisorUid + " to " + sensorId);
                                callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create assignment", e);
                    callback.onError("Failed to create assignment: " + e.getMessage());
                });
    }
    
    /**
     * Unassign a supervisor from a sensor.
     * 
     * @param sensorId      The sensor ID
     * @param supervisorUid The supervisor's UID
     * @param callback      Result callback
     */
    public void unassignSupervisor(@NonNull String sensorId, @NonNull String supervisorUid,
                                    @NonNull AssignmentCallback callback) {
        String docId = Assignment.generateDocumentId(supervisorUid, sensorId);
        Log.d(TAG, "Unassigning supervisor " + supervisorUid + " from sensor " + sensorId);
        
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Successfully unassigned supervisor from " + sensorId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete assignment", e);
                    callback.onError("Failed to unassign: " + e.getMessage());
                });
    }
    
    /**
     * Unassign all supervisors from a sensor.
     * 
     * @param sensorId The sensor ID
     * @param callback Result callback
     */
    public void unassignAllFromSensor(@NonNull String sensorId, @NonNull AssignmentCallback callback) {
        Log.d(TAG, "Unassigning all supervisors from sensor " + sensorId);
        
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .whereEqualTo("sensorId", sensorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess();
                    return;
                }
                
                    // Delete all assignments for this sensor
                    com.google.firebase.firestore.WriteBatch batch = firestore.batch();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                    }
                    
                    batch.commit()
                        .addOnSuccessListener(aVoid -> {
                                Log.i(TAG, "Unassigned all supervisors from " + sensorId);
                                        callback.onSuccess();
                                    })
                                    .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to unassign supervisors", e);
                                callback.onError("Failed to unassign: " + e.getMessage());
                                    });
                        })
                        .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query assignments", e);
                            callback.onError("Failed to unassign: " + e.getMessage());
                        });
            }
            
    /**
     * Get all supervisors assigned to a sensor.
     * 
     * @param sensorId The sensor ID to look up
     * @param callback Result callback with list of assignments
     */
    public void getSupervisorsForSensor(@NonNull String sensorId, @NonNull AssignmentListCallback callback) {
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .whereEqualTo("sensorId", sensorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Assignment assignment = Assignment.fromDocument(doc);
                        if (assignment != null) {
                            assignments.add(assignment);
                        }
                    }
                    Log.d(TAG, "Found " + assignments.size() + " supervisors for sensor " + sensorId);
                    callback.onResult(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get supervisors for " + sensorId, e);
                    callback.onError("Failed to lookup assignments: " + e.getMessage());
                });
    }
    
    /**
     * Get all sensors assigned to a supervisor.
     * 
     * @param supervisorUid The supervisor's UID
     * @param callback      Result callback with list of assignments
     */
    public void getSensorsForSupervisor(@NonNull String supervisorUid, @NonNull AssignmentListCallback callback) {
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .whereEqualTo("supervisorUid", supervisorUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Assignment assignment = Assignment.fromDocument(doc);
                        if (assignment != null) {
                            assignments.add(assignment);
            }
                    }
                    Log.d(TAG, "Found " + assignments.size() + " sensors for supervisor " + supervisorUid);
                    callback.onResult(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get sensors for supervisor " + supervisorUid, e);
                    callback.onError("Failed to lookup assignments: " + e.getMessage());
        });
    }
    
    /**
     * Get all assignments created by the current aggregator.
     * 
     * @param callback Result callback with list of assignments
     */
    public void getAssignmentsByAggregator(@NonNull AssignmentListCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Not authenticated");
            return;
        }
        
        String aggregatorUid = currentUser.getUid();
        
        firestore.collection(COLLECTION_ASSIGNMENTS)
                .whereEqualTo("assignedBy", aggregatorUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Assignment assignment = Assignment.fromDocument(doc);
                        if (assignment != null) {
                            assignments.add(assignment);
                        }
                    }
                    callback.onResult(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get assignments", e);
                    callback.onError("Failed to load assignments: " + e.getMessage());
                });
    }
    
    /**
     * Search for supervisors by partial email match.
     * 
     * @param query    The search query (partial email)
     * @param callback Result callback with matching supervisors
     */
    public void searchSupervisorsByEmail(@NonNull String query, @NonNull SearchCallback callback) {
        if (query.trim().length() < 2) {
            callback.onResult(new ArrayList<>());
            return;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Query all users with supervisor role and filter client-side
        // Note: Now we check 'roles' array instead of 'role' string
        firestore.collection(COLLECTION_USERS)
                .whereArrayContains("roles", Constants.ROLE_SUPERVISOR)
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
                    // Fallback: Try legacy 'role' field
                    searchSupervisorsByEmailLegacy(query, callback);
                });
    }
    
    /**
     * Legacy search for supervisors using old 'role' field.
     */
    private void searchSupervisorsByEmailLegacy(String query, SearchCallback callback) {
        String lowerQuery = query.toLowerCase().trim();
        
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
     * @param email    The exact email to find
     * @param callback Result callback
     */
    public void findSupervisorByEmail(@NonNull String email, @NonNull FindSupervisorCallback callback) {
        // First try with new 'roles' array
        firestore.collection(COLLECTION_USERS)
                .whereArrayContains("roles", Constants.ROLE_SUPERVISOR)
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
                        // Try legacy 'role' field
                        findSupervisorByEmailLegacy(email, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to legacy
                    findSupervisorByEmailLegacy(email, callback);
                });
    }
    
    /**
     * Legacy find supervisor using old 'role' field.
     */
    private void findSupervisorByEmailLegacy(String email, FindSupervisorCallback callback) {
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
                        // Try without lowercasing
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
     * Get a map of sensorId -> list of supervisor emails for the current aggregator.
     * Useful for UI display.
     */
    public interface AssignmentMapCallback {
        void onResult(Map<String, List<String>> sensorToSupervisorEmails);
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
                .whereEqualTo("assignedBy", aggregatorUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, List<String>> map = new HashMap<>();
                    
                    // Collect supervisor UIDs to resolve emails
                    List<String> supervisorUids = new ArrayList<>();
                    Map<String, String> sensorToSupervisorUid = new HashMap<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String sensorId = doc.getString("sensorId");
                        String supervisorUid = doc.getString("supervisorUid");
                        if (sensorId != null && supervisorUid != null) {
                            if (!map.containsKey(sensorId)) {
                                map.put(sensorId, new ArrayList<>());
                            }
                            // Temporarily store UID, will resolve to email
                            map.get(sensorId).add(supervisorUid);
                            if (!supervisorUids.contains(supervisorUid)) {
                                supervisorUids.add(supervisorUid);
                        }
                    }
                    }
                    
                    // For now, return UIDs. A more complete implementation would resolve emails.
                    callback.onResult(map);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get assignment map", e);
                    callback.onError("Failed to load assignments: " + e.getMessage());
                });
    }
}
