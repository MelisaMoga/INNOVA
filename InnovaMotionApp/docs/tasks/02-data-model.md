# TASK 2: Data Model Extension for Multi-User

**Assigned To:** Backend/Database Developer  
**Estimated Effort:** 2-3 days  
**Dependencies:** None (can run parallel with Task 1)  
**Status:** Pending

---

## Context

Current `ReceivedBtDataEntity` stores messages per `deviceAddress` (Bluetooth MAC). The new model needs to track per `sensorId` (monitored person ID from hardware).

---

## Deliverables

### 1. Update `ReceivedBtDataEntity.java`

Add new field for sensor ID:

```java
@Nullable
@ColumnInfo(name = "sensor_id")
private String sensorId;
```

Add constructor overload accepting sensorId:

```java
// New multi-user constructor
public ReceivedBtDataEntity(
    @NonNull String deviceAddress, 
    long timestamp, 
    @NonNull String receivedMsg,
    @NonNull String ownerUserId,
    @NonNull String sensorId
) {
    this.deviceAddress = deviceAddress;
    this.timestamp = timestamp;
    this.receivedMsg = receivedMsg;
    this.ownerUserId = ownerUserId;
    this.sensorId = sensorId;
}
```

Update existing constructors for backward compatibility (sensorId = null for legacy data).

Add getter/setter for sensorId.

### 2. Update `ReceivedBtDataDao.java`

Add queries filtered by `sensor_id`:

```java
// Get latest reading for a specific sensor
@Query("SELECT * FROM received_bt_data WHERE sensor_id = :sensorId ORDER BY timestamp DESC LIMIT 1")
LiveData<ReceivedBtDataEntity> getLatestForSensor(String sensorId);

// Get all readings for a specific sensor
@Query("SELECT * FROM received_bt_data WHERE sensor_id = :sensorId ORDER BY timestamp ASC")
LiveData<List<ReceivedBtDataEntity>> getAllForSensor(String sensorId);

// Get all distinct sensor IDs (for dropdown/list population)
@Query("SELECT DISTINCT sensor_id FROM received_bt_data WHERE sensor_id IS NOT NULL")
LiveData<List<String>> getDistinctSensorIds();

// Get latest reading per sensor (for dashboard - one row per person)
@Query("SELECT * FROM received_bt_data WHERE sensor_id IS NOT NULL GROUP BY sensor_id HAVING timestamp = MAX(timestamp)")
LiveData<List<ReceivedBtDataEntity>> getLatestForEachSensor();

// Combined owner + sensor filtering
@Query("SELECT * FROM received_bt_data WHERE owner_user_id = :ownerUid AND sensor_id = :sensorId ORDER BY timestamp DESC LIMIT 1")
LiveData<ReceivedBtDataEntity> getLatestForOwnerAndSensor(String ownerUid, String sensorId);

// Get distinct sensors for an owner
@Query("SELECT DISTINCT sensor_id FROM received_bt_data WHERE owner_user_id = :ownerUid AND sensor_id IS NOT NULL")
LiveData<List<String>> getDistinctSensorIdsForOwner(String ownerUid);
```

### 3. Update `FirestoreDataModel.java`

Add sensorId field:

```java
private String sensorId;

// Update constructor
public FirestoreDataModel(String deviceAddress, long timestamp, String receivedMsg, String userId, String sensorId) {
    // ... existing code ...
    this.sensorId = sensorId;
    this.documentId = generateDocumentId(userId, deviceAddress, timestamp, sensorId);
}

// Update generateDocumentId to include sensorId
public static String generateDocumentId(String userId, String deviceAddress, long timestamp, String sensorId) {
    String sensorPart = (sensorId != null) ? sensorId : "legacy";
    return userId + "_" + deviceAddress.replace(":", "") + "_" + sensorPart + "_" + timestamp;
}

// Update toFirestoreDocument
public Map<String, Object> toFirestoreDocument() {
    Map<String, Object> doc = new HashMap<>();
    // ... existing fields ...
    doc.put("sensorId", sensorId);
    return doc;
}

// Update fromFirestoreDocument
public static FirestoreDataModel fromFirestoreDocument(Map<String, Object> doc) {
    // ... existing code ...
    model.sensorId = (String) doc.get("sensorId");
    return model;
}
```

### 4. Create Room migration

In `InnovaDatabase.java`:

```java
static final Migration MIGRATION_X_Y = new Migration(X, Y) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE received_bt_data ADD COLUMN sensor_id TEXT");
        // Optionally create index for faster queries
        database.execSQL("CREATE INDEX IF NOT EXISTS index_received_bt_data_sensor_id ON received_bt_data(sensor_id)");
    }
};
```

Update database version and add migration to builder.

---

## Files to Modify

- [`ReceivedBtDataEntity.java`](../../app/src/main/java/com/melisa/innovamotionapp/data/database/ReceivedBtDataEntity.java)
- [`ReceivedBtDataDao.java`](../../app/src/main/java/com/melisa/innovamotionapp/data/database/ReceivedBtDataDao.java)
- [`InnovaDatabase.java`](../../app/src/main/java/com/melisa/innovamotionapp/data/database/InnovaDatabase.java)
- [`FirestoreDataModel.java`](../../app/src/main/java/com/melisa/innovamotionapp/sync/FirestoreDataModel.java)

---

## Acceptance Criteria

- [ ] `sensor_id` column added to database with proper migration
- [ ] All new DAO queries compile and work correctly
- [ ] Existing data continues to work (backward compatibility)
- [ ] FirestoreDataModel includes sensorId in serialization/deserialization
- [ ] Document IDs are unique even with same timestamp but different sensors
- [ ] Index created for sensor_id column for query performance
