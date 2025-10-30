package com.melisa.innovamotionapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.melisa.innovamotionapp.data.models.ChildProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the registry of children (monitored persons) for an aggregator account.
 * 
 * Responsibilities:
 * - Load child registry from Firestore
 * - Cache child profiles in memory for fast lookup
 * - Auto-register new children when first seen in packets
 * - Update child metadata (name, location, notes, lastSeen)
 * - Provide lookup methods for UI display
 * 
 * Firestore structure:
 * aggregators/{aggregatorId}/children/{childId} → ChildProfile document
 */
public class ChildRegistryManager {
    private static final String TAG = "ChildRegistryManager";
    private static final String COLLECTION_AGGREGATORS = "aggregators";
    private static final String SUBCOLLECTION_CHILDREN = "children";
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;
    
    // In-memory cache of child profiles
    private final Map<String, ChildProfile> childCache = new HashMap<>();
    
    // Firestore listener for real-time updates
    private ListenerRegistration registryListener;
    
    // Callback interfaces
    public interface LoadCallback {
        void onLoaded(List<ChildProfile> children);
        void onError(String error);
    }
    
    public interface UpdateCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public ChildRegistryManager(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Load the child registry from Firestore for a specific aggregator
     * Sets up a real-time listener for updates
     */
    public void loadRegistry(@NonNull String aggregatorId, @NonNull LoadCallback callback) {
        Log.i(TAG, "Loading child registry for aggregator: " + aggregatorId);
        
        // Stop existing listener if any
        if (registryListener != null) {
            registryListener.remove();
            registryListener = null;
        }
        
        // Clear cache
        childCache.clear();
        
        // Set up real-time listener
        registryListener = firestore.collection(COLLECTION_AGGREGATORS)
                .document(aggregatorId)
                .collection(SUBCOLLECTION_CHILDREN)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to child registry", e);
                        callback.onError("Failed to load child registry: " + e.getMessage());
                        return;
                    }
                    
                    if (queryDocumentSnapshots == null) {
                        Log.w(TAG, "Null snapshot for child registry");
                        return;
                    }
                    
                    List<ChildProfile> children = new ArrayList<>();
                    
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        try {
                            ChildProfile child = document.toObject(ChildProfile.class);
                            if (child != null) {
                                children.add(child);
                                childCache.put(child.getChildId(), child);
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing child profile: " + document.getId(), ex);
                        }
                    }
                    
                    Log.i(TAG, "Child registry loaded: " + children.size() + " children");
                    callback.onLoaded(children);
                });
    }
    
    /**
     * Update a child profile in Firestore
     */
    public void updateChild(@NonNull String aggregatorId, @NonNull ChildProfile profile, @NonNull UpdateCallback callback) {
        Log.i(TAG, "Updating child profile: " + profile.getChildId());
        
        executorService.execute(() -> {
            firestore.collection(COLLECTION_AGGREGATORS)
                    .document(aggregatorId)
                    .collection(SUBCOLLECTION_CHILDREN)
                    .document(profile.getChildId())
                    .set(profile)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Child profile updated successfully: " + profile.getChildId());
                        childCache.put(profile.getChildId(), profile);
                        callback.onSuccess("Child profile updated");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update child profile: " + profile.getChildId(), e);
                        callback.onError("Failed to update child: " + e.getMessage());
                    });
        });
    }
    
    /**
     * Auto-register a new child when first seen in a packet
     * Creates a minimal profile with just the childId
     */
    public void autoRegisterChild(@NonNull String aggregatorId, @NonNull String childId, @NonNull UpdateCallback callback) {
        // Check if already in cache
        if (childCache.containsKey(childId)) {
            Log.d(TAG, "Child already registered: " + childId);
            callback.onSuccess("Child already registered");
            return;
        }
        
        Log.i(TAG, "Auto-registering new child: " + childId);
        
        ChildProfile newChild = new ChildProfile(childId);
        
        executorService.execute(() -> {
            firestore.collection(COLLECTION_AGGREGATORS)
                    .document(aggregatorId)
                    .collection(SUBCOLLECTION_CHILDREN)
                    .document(childId)
                    .set(newChild)
                    .addOnSuccessListener(aVoid -> {
                        Log.i(TAG, "Child auto-registered successfully: " + childId);
                        childCache.put(childId, newChild);
                        callback.onSuccess("New child registered: " + childId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to auto-register child: " + childId, e);
                        callback.onError("Failed to register child: " + e.getMessage());
                    });
        });
    }
    
    /**
     * Update last seen timestamp for a child
     */
    public void updateLastSeen(@NonNull String aggregatorId, @NonNull String childId) {
        ChildProfile child = childCache.get(childId);
        if (child != null) {
            child.updateLastSeen();
            
            // Update in Firestore asynchronously (fire and forget)
            executorService.execute(() -> {
                firestore.collection(COLLECTION_AGGREGATORS)
                        .document(aggregatorId)
                        .collection(SUBCOLLECTION_CHILDREN)
                        .document(childId)
                        .update("lastSeen", child.getLastSeen())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Last seen updated for: " + childId))
                        .addOnFailureListener(e -> Log.w(TAG, "Failed to update last seen for: " + childId, e));
            });
        }
    }
    
    /**
     * Get a child profile by ID
     */
    @Nullable
    public ChildProfile getChild(@NonNull String childId) {
        return childCache.get(childId);
    }
    
    /**
     * Get child display name - returns friendly name if set, otherwise childId
     */
    @NonNull
    public String getChildName(@NonNull String childId) {
        ChildProfile child = childCache.get(childId);
        if (child != null) {
            return child.getDisplayName();
        }
        return childId; // Fallback to childId if not in cache
    }
    
    /**
     * Get all children from cache
     */
    @NonNull
    public List<ChildProfile> getAllChildren() {
        return new ArrayList<>(childCache.values());
    }
    
    /**
     * Get all child IDs from cache
     */
    @NonNull
    public List<String> getAllChildIds() {
        return new ArrayList<>(childCache.keySet());
    }
    
    /**
     * Check if a child is registered
     */
    public boolean isChildRegistered(@NonNull String childId) {
        return childCache.containsKey(childId);
    }
    
    /**
     * Get number of registered children
     */
    public int getChildCount() {
        return childCache.size();
    }
    
    /**
     * Clear cache and stop listener
     */
    public void cleanup() {
        Log.i(TAG, "Cleaning up ChildRegistryManager");
        
        if (registryListener != null) {
            registryListener.remove();
            registryListener = null;
        }
        
        childCache.clear();
        executorService.shutdown();
    }
}

