# TASK 11: Supervisor Query Optimization

**Assigned To:** Backend Developer  
**Estimated Effort:** 1-2 days  
**Dependencies:** Task 2 (Data Model)  
**Status:** Pending

---

## Context

Current system queries Firestore separately for each supervised user. Optimize to single query where possible.

---

## Deliverables

### 1. Update `FirestoreSyncService.java`

Replace multiple queries with compound query:

```java
/**
 * Sync data from multiple supervised users in single query.
 * Note: Firestore whereIn limit is 10 values, so batch if more.
 */
private void syncFromMultipleUsers(List<String> userIds, SyncCallback callback) {
    if (userIds.isEmpty()) {
        callback.onSuccess("No users to sync");
        return;
    }
    
    // Firestore whereIn limit is 10
    final int BATCH_SIZE = 10;
    
    for (int i = 0; i < userIds.size(); i += BATCH_SIZE) {
        List<String> batch = userIds.subList(i, Math.min(i + BATCH_SIZE, userIds.size()));
        
        firestore.collection(COLLECTION_BT_DATA)
            .whereIn("userId", batch)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Limit per query
            .get()
            .addOnSuccessListener(snapshot -> {
                processQueryResults(snapshot, callback);
            })
            .addOnFailureListener(e -> {
                callback.onError("Query failed: " + e.getMessage());
            });
    }
}
```

### 2. Update supervisor mirror setup

Consider trade-offs:

**Option A: Single compound listener (recommended for <10 users)**
```java
public void startCompoundMirror(List<String> supervisedUserIds) {
    if (supervisedUserIds.size() > 10) {
        // Fall back to per-user listeners
        startSupervisorMirrors(supervisedUserIds);
        return;
    }
    
    ListenerRegistration listener = firestore.collection(COLLECTION_BT_DATA)
        .whereIn("userId", supervisedUserIds)
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener((snapshot, e) -> {
            if (e != null) return;
            handleCompoundDocumentChanges(snapshot.getDocumentChanges());
        });
    
    // Store single listener reference
    compoundMirrorListener = listener;
}
```

**Option B: Keep per-user listeners (current approach)**
- Better for >10 users
- More granular error handling per user
- Document decision in code comments

### 3. Update Room queries

Ensure efficient queries exist:

```java
// Already efficient - single query, not N queries
@Query("SELECT * FROM received_bt_data WHERE sensor_id IS NOT NULL " +
       "GROUP BY sensor_id HAVING timestamp = MAX(timestamp)")
LiveData<List<ReceivedBtDataEntity>> getLatestForEachSensor();

// Ensure proper indexes exist
@Query("CREATE INDEX IF NOT EXISTS idx_sensor_timestamp ON received_bt_data(sensor_id, timestamp)")
void createSensorTimestampIndex();
```

---

## Files to Modify

- [`FirestoreSyncService.java`](../../app/src/main/java/com/melisa/innovamotionapp/sync/FirestoreSyncService.java)
- [`ReceivedBtDataDao.java`](../../app/src/main/java/com/melisa/innovamotionapp/data/database/ReceivedBtDataDao.java) (add indexes if needed)

---

## Acceptance Criteria

- [ ] Compound queries used for <10 supervised users
- [ ] Batching implemented for >10 users (Firestore whereIn limit)
- [ ] Room queries are efficient (single query per operation)
- [ ] Trade-off between single vs. per-user listeners documented
- [ ] Performance improvement measurable in logs
