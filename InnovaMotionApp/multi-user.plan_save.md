# Multi-User Packet-Based Posture Collection Architecture

## Overview

Transform the application from 1:1 supervised user/device mapping to 1:N aggregator/children model where one phone collects data for multiple monitored persons via packet-based Bluetooth messages.

## Key Changes

- Role rename: "supervised" → "aggregator"
- Message format: `<hex>` → `<childId;hex>`
- Packet protocol: Multiple messages terminated by `END_PACKET` delimiter
- Storage: Remove 500ms batch thread, use packet-triggered insertion
- Firestore: New collection "posture_messages" with `aggregatorId` field
- UI: Aggregator gets tabbed view (Raw Log + Live Posture Debug)

---

## Phase 1: Core Infrastructure

### 1.1 Message Parser Utility

**File:** `app/src/main/java/com/melisa/innovamotionapp/utils/MessageParser.java` (NEW)

Create parser for new message format:

- Parse `childId;hex` format (e.g., "sensor001;0xAB3311")
- Support legacy `hex` format during transition
- Return `ParsedMessage` with nullable `childId` and `hex` fields
- Validation methods for format checking

### 1.2 Update Constants

**File:** `app/src/main/java/com/melisa/innovamotionapp/utils/Constants.java`

Add new constants:

- `PACKET_END_DELIMITER = "END_PACKET"`
- `MESSAGE_DELIMITER = ";"`
- Remove or deprecate `COUNTDOWN_TIMER_IN_MILLISECONDS_FOR_MESSAGE_SAVE` (no longer used)

### 1.3 Role Provider Updates

**File:** `app/src/main/java/com/melisa/innovamotionapp/utils/RoleProvider.java`

Update role enum and methods:

- Rename `SUPERVISED` → `AGGREGATOR`
- Update all `isSupervised()` → `isAggregator()`
- Update string constants in Firestore ("supervised" → "aggregator")

---

## Phase 2: Bluetooth Layer Transformation

### 2.1 Packet Processor (NEW Component)

**File:** `app/src/main/java/com/melisa/innovamotionapp/bluetooth/PacketProcessor.java` (NEW)

Responsibilities:

- Receive list of raw message lines from BT thread
- Parse each line with `MessageParser`
- Filter invalid lines (log warnings, continue processing)
- Create `ReceivedBtDataEntity` with `owner_user_id = childId`
- Bulk insert to Room (`dao.insertAll()`)
- Trigger batch Firestore upload
- Update packet statistics in GlobalData

Threading: Execute on background ExecutorService

### 2.2 Update DeviceCommunicationThread

**File:** `app/src/main/java/com/melisa/innovamotionapp/bluetooth/DeviceCommunicationThread.java`

Modify `startReceiving()` method:

- Add packet accumulator: `List<String> packetLines = new ArrayList<>()`
- In `readLine()` loop: check if line equals `END_PACKET`
  - If yes: trigger `callback.onPacketReceived(packetLines)`, clear list
  - If no: add line to `packetLines`
- Add new callback method: `void onPacketReceived(List<String> lines)`

### 2.3 Update DeviceCommunicationService

**File:** `app/src/main/java/com/melisa/innovamotionapp/bluetooth/DeviceCommunicationService.java`

Major changes:

- REMOVE 500ms batch-saving thread (lines 57-100)
- Remove `temporaryReceivedBtDataListToSave` list and `lock` object
- Implement `onPacketReceived()` callback:
  - Instantiate `PacketProcessor`
  - Pass packet lines + ExecutorService
  - PacketProcessor handles parsing, Room insert, Firestore sync
- Update `onDataReceived()`: deprecated, or use for logging only
- Keep connection lifecycle management unchanged

---

## Phase 3: Data Layer Updates

### 3.1 ReceivedBtDataEntity (No changes needed)

**File:** `app/src/main/java/com/melisa/innovamotionapp/data/database/ReceivedBtDataEntity.java`

Existing schema already supports:

- `owner_user_id` field (will store childId)
- Unique index on (owner_user_id, device_address, timestamp, received_msg)
- Constructor with owner parameter

### 3.2 Update DAO Queries

**File:** `app/src/main/java/com/melisa/innovamotionapp/data/database/ReceivedBtDataDao.java`

Add new queries:

```java
// Get recent messages (all children, for aggregator raw log)
@Query("SELECT * FROM received_bt_data ORDER BY timestamp DESC LIMIT :limit")
LiveData<List<ReceivedBtDataEntity>> getRecentMessages(int limit);

// Get message count per child
@Query("SELECT owner_user_id, COUNT(*) as count FROM received_bt_data WHERE owner_user_id IN (:childIds) GROUP BY owner_user_id")
List<ChildMessageCount> getMessageCountPerChild(List<String> childIds);

// Helper class for count query
class ChildMessageCount {
    @ColumnInfo(name = "owner_user_id") public String childId;
    @ColumnInfo(name = "count") public int count;
}
```

### 3.3 Database Migration

**File:** `app/src/main/java/com/melisa/innovamotionapp/data/database/InnovaDatabase.java`

Update version and add migration:

- Increment database version number
- Add migration: `MIGRATION_X_Y` that clears all tables (fresh start)
- Or use `fallbackToDestructiveMigration()` for development

---

## Phase 4: Firestore Sync Transformation

### 4.1 New Firestore Data Model

**File:** `app/src/main/java/com/melisa/innovamotionapp/sync/FirestoreDataModel.java`

Update structure:

```java
private String childId;           // The monitored person
private String aggregatorId;      // The aggregator account uploading
private String deviceAddress;
private long timestamp;
private String receivedMsg;       // Hex code only
private long syncTimestamp;

// Document ID format: {aggregatorId}_{childId}_{timestamp}
public static String generateDocumentId(String aggregatorId, String childId, long timestamp)
```

### 4.2 Batch Upload to Firestore

**File:** `app/src/main/java/com/melisa/innovamotionapp/sync/FirestoreSyncService.java`

Add new method:

```java
public void batchSyncMessages(List<ReceivedBtDataEntity> entities, SyncCallback callback)
```

Implementation:

- Get authenticated aggregator ID from FirebaseAuth
- Split entities into batches of 500 (Firestore WriteBatch limit)
- For each batch:
  - Create WriteBatch
  - For each entity: `batch.set(docRef, firestoreModel.toFirestoreDocument())`
  - Commit batch
- Handle success/failure callbacks
- Execute on background thread

Update `syncNewMessage()` to be deprecated (use batch method instead)

### 4.3 Update Supervisor Mirror Pipeline

**File:** `app/src/main/java/com/melisa/innovamotionapp/sync/FirestoreSyncService.java`

Change query strategy:

- Replace per-child listeners with SINGLE listener per aggregator
- Query: `firestore.collection("posture_messages").whereEqualTo("aggregatorId", linkedAggregatorId)`
- Remove `startSupervisorMirrors()` plural method
- Add `startAggregatorMirror(String aggregatorId)` singular method
- Update `handleSupervisorDocumentChanges()` to handle all children in one listener

### 4.4 Update Collection Name

**File:** `app/src/main/java/com/melisa/innovamotionapp/sync/FirestoreSyncService.java`

Change constant:

```java
private static final String COLLECTION_BT_DATA = "posture_messages"; // was "bluetooth_messages"
```

---

## Phase 5: Child Registry System

### 5.1 Child Registry Model (NEW)

**File:** `app/src/main/java/com/melisa/innovamotionapp/data/models/ChildProfile.java` (NEW)

```java
public class ChildProfile {
    private String childId;
    private String name;
    private String location;
    private String notes;
    private long addedAt;
    private long lastSeen;
    // getters, setters, constructors
}
```

### 5.2 Child Registry Manager (NEW)

**File:** `app/src/main/java/com/melisa/innovamotionapp/sync/ChildRegistryManager.java` (NEW)

Responsibilities:

- Fetch child registry from Firestore (`aggregators/{aggregatorId}/children`)
- Cache in memory (Map<String, ChildProfile>)
- Auto-add new children when first seen in packets
- Update child metadata (name, location, notes)
- Provide `getChildName(childId)` lookup method

Methods:

```java
public void loadRegistry(String aggregatorId, Callback callback)
public void updateChild(String childId, ChildProfile profile, Callback callback)
public ChildProfile getChild(String childId)
public String getChildName(String childId) // Returns name or childId if not found
public void autoRegisterChild(String childId)
```

### 5.3 Update UserSession

**File:** `app/src/main/java/com/melisa/innovamotionapp/sync/UserSession.java`

Add fields:

- `private ChildRegistryManager childRegistry`
- `private String linkedAggregatorId` (for supervisors)

Add methods:

- `public ChildRegistryManager getChildRegistry()`
- `public String getLinkedAggregatorId()`

---

## Phase 6: Aggregator UI (Data Collection Phone)

### 6.1 DataAggregatorActivity (NEW)

**File:** `app/src/main/java/com/melisa/innovamotionapp/activities/DataAggregatorActivity.java` (NEW)

TabLayout with 2 tabs:

1. **Raw Log Tab**: RecyclerView showing recent 100 messages from all children
2. **Live Posture Tab**: Child selector dropdown + posture visualization (reuse existing posture display logic)

Observe:

- `dao.getRecentMessages(100)` for raw log
- Connection status from GlobalData
- Packet statistics (count, last timestamp)

Display:

- BT device address
- Connection status indicator
- Last packet timestamp
- Message breakdown by child (table: childId → count)

### 6.2 Raw Log Fragment

**File:** `app/src/main/java/com/melisa/innovamotionapp/ui/fragments/RawLogFragment.java` (NEW)

RecyclerView with adapter showing:

- Timestamp
- Child ID (with friendly name if available)
- Hex code
- Device address
- Color-coded by posture type

### 6.3 Live Posture Fragment

**File:** `app/src/main/java/com/melisa/innovamotionapp/ui/fragments/LivePostureFragment.java` (NEW)

UI components:

- Spinner/Dropdown to select child
- VideoView for posture animation (reuse from BtConnectedActivity)
- Posture description text
- Risk indicator
- Timestamp of last update

Observe:

- `dao.getLatestForOwner(selectedChildId)` 
- Parse with PostureFactory
- Display posture video/animation

### 6.4 RawMessageAdapter

**File:** `app/src/main/java/com/melisa/innovamotionapp/ui/adapters/RawMessageAdapter.java` (NEW)

RecyclerView adapter for message list:

- ViewHolder with: timestamp, childId, friendlyName, hexCode, deviceAddress
- Format timestamp as "HH:mm:ss.SSS"
- Lookup child name from ChildRegistryManager
- Color-code background by posture type (red for falls, green for normal)

### 6.5 Layout Files

**Files:** (NEW)

- `app/src/main/res/layout/activity_data_aggregator.xml` - TabLayout container
- `app/src/main/res/layout/fragment_raw_log.xml` - RecyclerView
- `app/src/main/res/layout/fragment_live_posture.xml` - Child selector + VideoView
- `app/src/main/res/layout/item_raw_message.xml` - Message list item

### 6.6 Update GlobalData

**File:** `app/src/main/java/com/melisa/innovamotionapp/utils/GlobalData.java`

Add observables:

```java
private MutableLiveData<Integer> packetCount = new MutableLiveData<>(0);
private MutableLiveData<Long> lastPacketTimestamp = new MutableLiveData<>(0L);
private MutableLiveData<String> lastRawMessage = new MutableLiveData<>("");

// Getters and setters
```

---

## Phase 7: Supervisor UI Updates

### 7.1 SupervisorDashboardActivity (UPDATE)

**File:** `app/src/main/java/com/melisa/innovamotionapp/activities/SupervisorDashboardActivity.java`

Update to use aggregator-based queries:

- Load linked aggregator ID from UserSession
- Fetch child registry to get friendly names
- Query all children's latest postures
- Display in grid (2 columns)
- Show friendly names instead of IDs

### 7.2 SupervisorDashboardViewModel (UPDATE)

**File:** `app/src/main/java/com/melisa/innovamotionapp/ui/viewmodels/SupervisorDashboardViewModel.java`

Update `getLatestDataForUsers()`:

- Accept aggregator ID instead of child list
- Query `dao.getLatestForOwners(childIds)` where childIds from registry
- Map childId to friendly name in transformation
- Return `List<ChildPostureData>` with name field

### 7.3 Child Detail Activity (NEW or UPDATE)

**File:** `app/src/main/java/com/melisa/innovamotionapp/activities/ChildDetailActivity.java` (NEW)

Individual child monitoring view:

- Show child's friendly name, location, notes
- Live posture display (same as BtConnectedActivity)
- Posture history (timeline/chart)
- Statistics (time in each posture)

Launch from supervisor dashboard card click

---

## Phase 8: MainActivity Routing

### 8.1 Update MainActivity Navigation

**File:** `app/src/main/java/com/melisa/innovamotionapp/activities/MainActivity.java`

Update `LaunchMonitoring()`:

```java
if ("supervisor".equals(role)) {
    // Route to SupervisorDashboardActivity
    Intent intent = new Intent(this, SupervisorDashboardActivity.class);
    startActivity(intent);
} else if ("aggregator".equals(role)) {
    // Route to BtSettingsActivity (scan/connect) 
    // Then to DataAggregatorActivity after connection
    Intent intent = new Intent(this, BtSettingsActivity.class);
    startActivity(intent);
}
```

### 8.2 Update BtConnectedActivity Routing

**File:** `app/src/main/java/com/melisa/innovamotionapp/activities/BtSettingsActivity.java`

After successful connection:

```java
if (RoleProvider.getCurrentRole() == RoleProvider.Role.AGGREGATOR) {
    Intent intent = new Intent(this, DataAggregatorActivity.class);
    startActivity(intent);
    finish();
} else {
    // Supervisors don't connect BT, should not reach here
}
```

---

## Phase 9: SessionGate Updates

### 9.1 Update Session Initialization

**File:** `app/src/main/java/com/melisa/innovamotionapp/sync/SessionGate.java`

Update `initializeUserSession()`:

- For aggregators: Load child registry, no backfill
- For supervisors: 
  - Fetch linked aggregator ID from supervisor profile
  - Fetch child registry from aggregator profile
  - Backfill historical data (query by aggregatorId)
  - Start single aggregator mirror

---

## Phase 10: Firestore Security Rules

### 10.1 Update Security Rules

**File:** `firestore.rules` (if managed in codebase, otherwise manual update)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Posture messages
    match /posture_messages/{messageId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                      request.resource.data.aggregatorId == request.auth.token.email;
    }
    
    // Aggregator profiles
    match /aggregators/{aggregatorId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && aggregatorId == request.auth.token.email;
    }
    
    // Supervisor profiles
    match /supervisors/{supervisorId} {
      allow read, write: if request.auth != null && supervisorId == request.auth.token.email;
    }
  }
}
```

---

## Phase 11: Testing & Validation

### 11.1 Unit Tests

Create tests for:

- MessageParser (various formats, edge cases)
- PacketProcessor (packet parsing, validation)
- ChildRegistryManager (CRUD operations)

### 11.2 Integration Tests

Test flows:

- Packet reception → Room insert → Firestore upload
- Supervisor mirror → download → Room insert
- Child registry auto-add and update

### 11.3 Manual Testing Checklist

- [ ] Connect BT device as aggregator
- [ ] Send test packet with multiple children
- [ ] Verify Room database contains correct owner_user_id
- [ ] Verify Firestore upload with aggregatorId
- [ ] Edit child names in registry
- [ ] Login as supervisor, verify mirror download
- [ ] Check supervisor dashboard shows all children
- [ ] Test fall detection for specific child

---

## Phase 12: Cleanup & Documentation

### 12.1 Remove Deprecated Code

Files/methods to remove or deprecate:

- 500ms batch thread in DeviceCommunicationService
- Old single-message sync methods
- Legacy "supervised" role references
- Old BtConnectedActivity routing for aggregators

### 12.2 Update String Resources

**File:** `app/src/main/res/values/strings.xml`

Update labels:

- "Supervised" → "Aggregator"
- Add new strings for child registry UI
- Update notification text for multi-child context

### 12.3 Update Documentation

**Files:** `docs/*.md`

Update architectural docs to reflect:

- New packet-based protocol
- Aggregator/supervisor/child hierarchy
- Firestore collection structure
- UI flow diagrams

---

## Implementation Order

1. Core Infrastructure (MessageParser, Constants, RoleProvider)
2. Bluetooth Layer (PacketProcessor, DeviceCommunicationThread updates)
3. Data Layer (DAO queries, Database migration)
4. Firestore Sync (Batch upload, new collection, mirror updates)
5. Child Registry System (Models, Manager, UserSession integration)
6. Aggregator UI (DataAggregatorActivity, fragments, adapters)
7. Supervisor UI (Dashboard updates, detail views)
8. Navigation & Routing (MainActivity, SessionGate)
9. Testing & Validation
10. Cleanup & Documentation

---

## Critical Success Factors

- Packet delimiter detection must be reliable (END_PACKET)
- Batch Firestore writes stay under 500 document limit
- Child registry caching for fast UI lookups
- Proper threading (no Room/Firestore on UI thread)
- Deduplication working correctly with new schema
- Fall detection includes child's friendly name
- Supervisor sees real-time updates for all children