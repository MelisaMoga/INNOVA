# TASK 10: Firestore Batch Upload Optimization

**Assigned To:** Backend Developer  
**Estimated Effort:** 2-3 days  
**Dependencies:** Task 2 (Data Model), Task 4 (Service Integration)  
**Status:** Pending

---

## Context

Current system syncs messages one-by-one. New system should upload entire packets as batches for better performance.

---

## Deliverables

### 1. Modify `FirestoreSyncService.java`

Add batch sync method:

```java
/**
 * Sync an entire packet as a batch write.
 * More efficient than individual writes.
 */
public void syncPacketBatch(List<ReceivedBtDataEntity> packet, SyncCallback callback) {
    if (!isUserAuthenticated() || !userSession.isSupervised()) {
        callback.onSuccess("Not supervised user, skipping sync");
        return;
    }
    
    if (!connectivityMonitor.isConnected()) {
        // Queue for later
        queuePacketForLaterSync(packet);
        callback.onSuccess("Offline, queued for later sync");
        return;
    }
    
    executorService.execute(() -> {
        FirebaseUser user = auth.getCurrentUser();
        WriteBatch batch = firestore.batch();
        
        for (ReceivedBtDataEntity entity : packet) {
            FirestoreDataModel model = new FirestoreDataModel(
                entity.getDeviceAddress(),
                entity.getTimestamp(),
                entity.getReceivedMsg(),
                user.getUid(),
                entity.getSensorId()
            );
            
            DocumentReference docRef = firestore
                .collection(COLLECTION_BT_DATA)
                .document(model.getDocumentId());
            
            batch.set(docRef, model.toFirestoreDocument());
        }
        
        batch.commit()
            .addOnSuccessListener(v -> callback.onSuccess("Batch synced: " + packet.size()))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    });
}
```

### 2. Add offline queue management

```java
private final Queue<List<ReceivedBtDataEntity>> offlineQueue = new ConcurrentLinkedQueue<>();

private void queuePacketForLaterSync(List<ReceivedBtDataEntity> packet) {
    offlineQueue.add(new ArrayList<>(packet));
    Log.d(TAG, "Queued packet for later sync. Queue size: " + offlineQueue.size());
}

/**
 * Process queued packets when connectivity is restored.
 * Called from handleConnectivityRestored()
 */
private void processOfflineQueue() {
    if (offlineQueue.isEmpty()) return;
    
    Log.i(TAG, "Processing offline queue: " + offlineQueue.size() + " packets");
    
    while (!offlineQueue.isEmpty()) {
        List<ReceivedBtDataEntity> packet = offlineQueue.poll();
        if (packet != null) {
            syncPacketBatch(packet, new SyncCallback() {
                @Override public void onSuccess(String msg) { 
                    Log.d(TAG, "Queued packet synced: " + msg); 
                }
                @Override public void onError(String err) { 
                    Log.w(TAG, "Failed to sync queued packet: " + err);
                    // Re-queue on failure? Consider retry limits.
                }
                @Override public void onProgress(int c, int t) {}
            });
        }
    }
}
```

### 3. Update `DeviceCommunicationService.java`

Trigger batch sync after packet processing:

```java
private void processPacket(BluetoothDevice device, List<ParsedReading> readings) {
    // ... create entities list ...
    
    // Batch insert to Room
    synchronized (lock) {
        temporaryReceivedBtDataListToSave.addAll(entities);
    }
    
    // Trigger batch sync to Firestore
    firestoreSyncService.syncPacketBatch(entities, new SyncCallback() {
        @Override public void onSuccess(String msg) { 
            Log.d(TAG, "Packet synced: " + msg); 
        }
        @Override public void onError(String err) { 
            Log.w(TAG, "Packet sync error: " + err); 
        }
        @Override public void onProgress(int c, int t) {}
    });
}
```

---

## Files to Modify

- [`FirestoreSyncService.java`](../../app/src/main/java/com/melisa/innovamotionapp/sync/FirestoreSyncService.java)
- [`DeviceCommunicationService.java`](../../app/src/main/java/com/melisa/innovamotionapp/bluetooth/DeviceCommunicationService.java)

---

## Acceptance Criteria

- [ ] Packets are uploaded as single Firestore batch write
- [ ] Offline packets are queued and processed when online
- [ ] Single network round-trip per packet (not per message)
- [ ] WriteBatch limit (500 docs) is respected for large packets
- [ ] Error handling and retry logic for failed batches
