package com.melisa.innovamotionapp.bluetooth;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.sync.ChildRegistryManager;
import com.melisa.innovamotionapp.sync.FirestoreSyncService;
import com.melisa.innovamotionapp.sync.UserSession;
import com.melisa.innovamotionapp.utils.AlertNotifications;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.MessageParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Processes complete Bluetooth packets containing multiple child messages.
 * 
 * Responsibilities:
 * - Parse packet lines using MessageParser
 * - Filter invalid lines (log warnings, continue processing)
 * - Create entities with child ownership mapping
 * - Bulk insert to Room database
 * - Trigger batch Firestore upload
 * - Update packet statistics
 * 
 * Threading: All operations executed on background ExecutorService
 */
public class PacketProcessor {
    private static final String TAG = "PacketProcessor";
    
    private final Context context;
    private final InnovaDatabase database;
    private final FirestoreSyncService firestoreSyncService;
    private final UserSession userSession;
    private final ExecutorService executorService;
    private final String deviceAddress;
    
    /**
     * Create a packet processor instance.
     * 
     * @param context Application context
     * @param deviceAddress Bluetooth device MAC address
     * @param executorService Background thread pool for processing
     */
    public PacketProcessor(Context context, String deviceAddress, ExecutorService executorService) {
        this.context = context;
        this.database = InnovaDatabase.getInstance(context);
        this.firestoreSyncService = FirestoreSyncService.getInstance(context);
        this.userSession = UserSession.getInstance(context);
        this.executorService = executorService;
        this.deviceAddress = deviceAddress;
    }
    
    /**
     * Process a complete packet of message lines.
     * This method should be called from the Bluetooth thread when END_PACKET is received.
     * Processing will be executed on the background ExecutorService.
     * 
     * @param packetLines List of raw message lines (excluding END_PACKET delimiter)
     */
    public void processPacket(List<String> packetLines) {
        if (packetLines == null || packetLines.isEmpty()) {
            Log.w(TAG, "Received empty packet, skipping processing");
            return;
        }
        
        Log.i(TAG, "Processing packet with " + packetLines.size() + " lines");
        
        // Execute processing on background thread
        executorService.execute(() -> processPacketInBackground(packetLines));
    }
    
    /**
     * Process packet on background thread.
     * Thread: Background (ExecutorService)
     */
    private void processPacketInBackground(List<String> packetLines) {
        final long packetTimestamp = System.currentTimeMillis();
        
        // Get aggregator ID (authenticated user)
        String aggregatorId = getAggregatorId();
        if (aggregatorId == null) {
            Log.e(TAG, "Cannot process packet: aggregator not authenticated");
            return;
        }
        
        // Parse all lines and create entities
        List<ReceivedBtDataEntity> entities = new ArrayList<>();
        Map<String, Integer> messageCountByChild = new HashMap<>();
        Set<String> seenChildIds = new HashSet<>();
        int validMessages = 0;
        int invalidMessages = 0;
        
        for (String line : packetLines) {
            MessageParser.ParsedMessage parsed = MessageParser.parse(line);
            
            if (!parsed.hasChildId()) {
                Log.w(TAG, "Skipping message without childId: " + line);
                invalidMessages++;
                continue;
            }
            
            if (!parsed.isValid()) {
                Log.w(TAG, "Skipping invalid message: " + line);
                invalidMessages++;
                continue;
            }
            
            // Track unique child IDs in this packet
            seenChildIds.add(parsed.childId);
            
            // Create entity with child as owner
            ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                deviceAddress,
                packetTimestamp,
                parsed.hex,
                parsed.childId  // CRITICAL: Child ID is the owner
            );
            
            entities.add(entity);
            validMessages++;
            
            // Track message count per child
            messageCountByChild.put(
                parsed.childId,
                messageCountByChild.getOrDefault(parsed.childId, 0) + 1
            );
            
            // Check for falls (for local notification on aggregator phone)
            checkForFall(parsed.childId, parsed.hex, packetTimestamp);
        }
        
        // Auto-register any new children seen in this packet
        autoRegisterNewChildren(aggregatorId, seenChildIds);
        
        Log.i(TAG, "Packet parsed: " + validMessages + " valid, " + invalidMessages + " invalid messages");
        
        if (entities.isEmpty()) {
            Log.w(TAG, "No valid entities to save from packet");
            return;
        }
        
        // Bulk insert to Room database (single transaction)
        try {
            database.receivedBtDataDao().insertAll(entities);
            Log.i(TAG, "Saved " + entities.size() + " messages to Room database");
            
            // Update packet statistics in GlobalData (UI observes this)
            updatePacketStatistics(packetTimestamp, entities.size(), messageCountByChild);
            
            // Trigger batch Firestore upload
            uploadPacketToFirestore(entities);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving packet to database", e);
        }
    }
    
    /**
     * Get the authenticated aggregator's user ID.
     * @return Aggregator email/UID, or null if not authenticated
     */
    private String getAggregatorId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        // Use email as aggregator ID (matches Firestore document structure)
        return user.getEmail() != null ? user.getEmail() : user.getUid();
    }
    
    /**
     * Auto-register new children seen in this packet.
     * Thread: Background
     */
    private void autoRegisterNewChildren(String aggregatorId, Set<String> childIds) {
        if (childIds.isEmpty()) {
            return;
        }
        
        ChildRegistryManager registry = userSession.getChildRegistry();
        if (registry == null) {
            Log.w(TAG, "Child registry not available, skipping auto-registration");
            return;
        }
        
        for (String childId : childIds) {
            if (!registry.isChildRegistered(childId)) {
                Log.i(TAG, "Auto-registering new child: " + childId);
                
                registry.autoRegisterChild(aggregatorId, childId, new ChildRegistryManager.UpdateCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Auto-registration success: " + message);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Auto-registration failed for " + childId + ": " + error);
                    }
                });
            } else {
                // Update last seen timestamp for known children
                registry.updateLastSeen(aggregatorId, childId);
            }
        }
    }
    
    /**
     * Check if a message indicates a fall and trigger local notification.
     * Thread: Background
     */
    private void checkForFall(String childId, String hex, long timestamp) {
        try {
            Posture posture = PostureFactory.createPosture(hex);
            
            if (posture instanceof FallingPosture) {
                // Check if fall is recent (within 24 hours)
                long now = System.currentTimeMillis();
                long twentyFourHours = 24 * 60 * 60 * 1000;
                
                if (now - timestamp <= twentyFourHours) {
                    Log.w(TAG, "Fall detected for child: " + childId);
                    
                    // Trigger fall notification on aggregator phone
                    AlertNotifications.notifyFall(
                        context,
                        "Child " + childId,
                        "Fall detected for monitored person: " + childId
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for fall", e);
        }
    }
    
    /**
     * Update packet statistics in GlobalData for UI display.
     * Thread: Background
     */
    private void updatePacketStatistics(long timestamp, int messageCount, Map<String, Integer> countByChild) {
        GlobalData globalData = GlobalData.getInstance();
        
        // Update packet count (increment by 1 for each packet)
        Integer currentCount = globalData.getPacketCount().getValue();
        int newCount = (currentCount != null ? currentCount : 0) + 1;
        globalData.getPacketCount().postValue(newCount);
        
        // Update last packet timestamp
        globalData.getLastPacketTimestamp().postValue(timestamp);
        
        // Log packet summary
        StringBuilder summary = new StringBuilder("Packet #" + newCount + ": " + messageCount + " messages [");
        for (Map.Entry<String, Integer> entry : countByChild.entrySet()) {
            summary.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
        }
        summary.append("]");
        globalData.getLastRawMessage().postValue(summary.toString());
        
        Log.i(TAG, summary.toString());
    }
    
    /**
     * Upload packet entities to Firestore in batch.
     * Thread: Background
     */
    private void uploadPacketToFirestore(List<ReceivedBtDataEntity> entities) {
        // Get aggregator ID
        String aggregatorId = getAggregatorId();
        if (aggregatorId == null) {
            Log.e(TAG, "Cannot upload to Firestore: aggregator not authenticated");
            return;
        }
        
        // Use batch sync method for efficiency
        firestoreSyncService.batchSyncMessages(entities, aggregatorId, new FirestoreSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Packet synced to Firestore: " + message);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Packet sync to Firestore failed (will retry): " + error);
            }

            @Override
            public void onProgress(int current, int total) {
                Log.d(TAG, "Firestore sync progress: " + current + "/" + total);
            }
        });
    }
}

