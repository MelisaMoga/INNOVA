# TASK 3: Monitored Person Entity and Name Management

**Assigned To:** Backend Developer  
**Estimated Effort:** 2 days  
**Dependencies:** Task 2 (data model)  
**Status:** Pending

---

## Context

Sensor IDs like `sensor001` or UUIDs are not human-readable. The system needs to map these to friendly names like "Ion Popescu" that sync to supervisor devices.

---

## Deliverables

### 1. Create `MonitoredPerson.java` entity in `data/database/`

```java
@Entity(
    tableName = "monitored_persons",
    indices = {
        @Index(value = {"sensor_id"}, unique = true)
    }
)
public class MonitoredPerson {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "sensor_id")
    private String sensorId;

    @NonNull
    @ColumnInfo(name = "display_name")
    private String displayName;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    // Constructors, getters, setters
}
```

### 2. Create `MonitoredPersonDao.java`

```java
@Dao
public interface MonitoredPersonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MonitoredPerson person);

    @Update
    void update(MonitoredPerson person);

    @Delete
    void delete(MonitoredPerson person);

    // Get display name for a sensor, returns null if not found
    @Query("SELECT display_name FROM monitored_persons WHERE sensor_id = :sensorId LIMIT 1")
    String getDisplayNameForSensor(String sensorId);

    // Get all monitored persons (LiveData for UI)
    @Query("SELECT * FROM monitored_persons ORDER BY display_name ASC")
    LiveData<List<MonitoredPerson>> getAllMonitoredPersons();

    // Synchronous version for background operations
    @Query("SELECT * FROM monitored_persons ORDER BY display_name ASC")
    List<MonitoredPerson> getAllMonitoredPersonsSync();

    // Upsert by sensor ID
    @Query("INSERT OR REPLACE INTO monitored_persons (sensor_id, display_name, created_at, updated_at) " +
           "VALUES (:sensorId, :displayName, :now, :now)")
    void upsertByName(String sensorId, String displayName, long now);

    // Check if sensor exists
    @Query("SELECT COUNT(*) FROM monitored_persons WHERE sensor_id = :sensorId")
    int sensorExists(String sensorId);

    // Get person by sensor ID
    @Query("SELECT * FROM monitored_persons WHERE sensor_id = :sensorId LIMIT 1")
    MonitoredPerson getPersonBySensorId(String sensorId);
}
```

### 3. Create `PersonNameManager.java` in `utils/` or new `managers/` package

```java
public class PersonNameManager {
    private static PersonNameManager instance;
    private final MonitoredPersonDao dao;
    private final ExecutorService executor;

    private PersonNameManager(Context context) {
        this.dao = InnovaDatabase.getInstance(context).monitoredPersonDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized PersonNameManager getInstance(Context context) {
        if (instance == null) {
            instance = new PersonNameManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Get display name for a sensor ID.
     * Returns the sensorId itself if no friendly name is set.
     * Must be called off main thread.
     */
    public String getDisplayName(String sensorId) {
        String name = dao.getDisplayNameForSensor(sensorId);
        return (name != null && !name.isEmpty()) ? name : sensorId;
    }

    /**
     * Set display name for a sensor ID.
     * Creates entry if it doesn't exist.
     */
    public void setDisplayName(String sensorId, String displayName) {
        executor.execute(() -> {
            dao.upsertByName(sensorId, displayName, System.currentTimeMillis());
        });
    }

    /**
     * Ensure a sensor ID exists in the database.
     * If not, creates an entry with sensorId as the default display name.
     * Call this when a new sensor is first seen.
     */
    public void ensureSensorExists(String sensorId) {
        executor.execute(() -> {
            if (dao.sensorExists(sensorId) == 0) {
                dao.upsertByName(sensorId, sensorId, System.currentTimeMillis());
            }
        });
    }

    /**
     * Get LiveData of all monitored persons for UI.
     */
    public LiveData<List<MonitoredPerson>> getAllPersonsLive() {
        return dao.getAllMonitoredPersons();
    }
}
```

### 4. Update `InnovaDatabase.java`

```java
@Database(
    entities = {
        ReceivedBtDataEntity.class,
        MonitoredPerson.class  // ADD THIS
    },
    version = X + 1,  // INCREMENT VERSION
    exportSchema = false
)
public abstract class InnovaDatabase extends RoomDatabase {
    // ... existing code ...
    
    public abstract MonitoredPersonDao monitoredPersonDao();  // ADD THIS
}

// Add migration
static final Migration MIGRATION_X_Y = new Migration(X, Y) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS monitored_persons (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "sensor_id TEXT NOT NULL, " +
            "display_name TEXT NOT NULL, " +
            "created_at INTEGER NOT NULL DEFAULT 0, " +
            "updated_at INTEGER NOT NULL DEFAULT 0)"
        );
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_monitored_persons_sensor_id ON monitored_persons(sensor_id)"
        );
    }
};
```

---

## Sub-task 3b: Cloud Sync for Person Names

### 5. Create `PersonNamesFirestoreSync.java` in `sync/`

```java
public class PersonNamesFirestoreSync {
    private static final String TAG = "PersonNamesSync";
    private static final String COLLECTION_MONITORED_PERSONS = "monitored_persons";
    
    private final FirebaseFirestore firestore;
    private final MonitoredPersonDao dao;
    private final String aggregatorUid;
    
    /**
     * Upload all person names to Firestore.
     * Collection path: users/{aggregatorUid}/monitored_persons/{sensorId}
     */
    public void uploadAllNames(SyncCallback callback) {
        List<MonitoredPerson> persons = dao.getAllMonitoredPersonsSync();
        
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
            .addOnSuccessListener(aVoid -> callback.onSuccess("Uploaded " + persons.size() + " names"))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    /**
     * Download person names from a specific aggregator (for supervisors).
     */
    public void downloadNamesFromAggregator(String aggregatorUid, SyncCallback callback) {
        firestore.collection("users")
            .document(aggregatorUid)
            .collection(COLLECTION_MONITORED_PERSONS)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (DocumentSnapshot doc : querySnapshot) {
                    String sensorId = doc.getString("sensorId");
                    String displayName = doc.getString("displayName");
                    if (sensorId != null && displayName != null) {
                        dao.upsertByName(sensorId, displayName, System.currentTimeMillis());
                    }
                }
                callback.onSuccess("Downloaded " + querySnapshot.size() + " names");
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
```

---

## Files to Create

- `app/src/main/java/com/melisa/innovamotionapp/data/database/MonitoredPerson.java`
- `app/src/main/java/com/melisa/innovamotionapp/data/database/MonitoredPersonDao.java`
- `app/src/main/java/com/melisa/innovamotionapp/utils/PersonNameManager.java` (or `managers/`)
- `app/src/main/java/com/melisa/innovamotionapp/sync/PersonNamesFirestoreSync.java`

## Files to Modify

- [`InnovaDatabase.java`](../../app/src/main/java/com/melisa/innovamotionapp/data/database/InnovaDatabase.java)

---

## Acceptance Criteria

- [ ] `MonitoredPerson` entity created with proper Room annotations
- [ ] DAO provides all necessary CRUD and query operations
- [ ] `PersonNameManager` provides simple API for name lookup with fallback
- [ ] New sensors are auto-registered when first seen
- [ ] Person names sync to Firestore for supervisor access
- [ ] Supervisors can download names from their linked aggregator
- [ ] Database migration works correctly
