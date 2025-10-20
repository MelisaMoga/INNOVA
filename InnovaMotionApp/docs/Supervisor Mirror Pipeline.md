# Supervisor Mirror Pipeline – Detailed Guide

## Overview

When a **supervisor** signs in, the app downloads real-time updates from their **supervised users** via Firestore snapshot listeners (called "mirrors"). This document explains the complete mirror lifecycle.

---

## Phase 1: Mirror Setup (After Login)

### Trigger
- User completes login with role = "supervisor"
- `SessionGate.runSupervisorPipeline()` executes
- After backfill completes, `startSupervisorMirrors(supervisedUserIds)` is called

### What Happens
For each supervised UID:
1. Attach Firestore listener: `collection("bluetooth_messages").whereEqualTo("userId", supervisedUid)`
2. Store listener registration in `mirrorByUid` map
3. Listener fires immediately with **all existing documents** (initial snapshot)
4. Listener continues firing on **every new/updated document**

### Files Involved
- `FirestoreSyncService.java`: `startSupervisorMirror(supervisedUserId)`
- `SessionGate.java`: `runSupervisorPipeline()`

---

## Phase 2: Real-Time Updates (Mirror Active)

### Trigger
- Supervised user's device uploads new message to Firestore
- Firestore sends snapshot update to supervisor's device
- Listener callback fires: `queryDocumentSnapshots` parameter contains document changes

### Processing Pipeline

#### Step 1: Receive Snapshot
**Thread**: Background (Firestore listener callback)
- Firestore delivers `QuerySnapshot` with list of `DocumentChange` objects
- Metadata indicates if data is from cache or server: `fromCache` boolean

#### Step 2: Handle Document Changes
**Thread**: Background (executorService)
**Method**: `handleSupervisorDocumentChanges(documentChanges, supervisedUserId)`

**Actions per document:**
1. **Filter**: Only process `ADDED` or `MODIFIED` changes
2. **Parse**: Convert Firestore doc → `FirestoreDataModel` → extract fields
3. **Dedup Check**: Query Room: `dao.messageExistsOwned(supervisedUserId, deviceAddress, timestamp, receivedMsg)`
   - If exists (returns `1`): Skip this message
   - If not exists (returns `0`): Continue
4. **Create Entity**:
   ```java
   ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
     deviceAddress,
     timestamp,
     receivedMsg,
     supervisedUserId  // owner_user_id = supervised user's UID
   )
   ```
5. **Fall Detection** (if message is recent):
   - Parse posture: `PostureFactory.createPosture(receivedMsg)`
   - If `FallingPosture` detected:
     - Fetch supervised user's email from Firestore (async)
     - Show local notification on supervisor's device
     - Notification text: "Supervised user ([email]) has fallen"
6. **Batch Insert**: After processing all changes:
   - `dao.insertAll(entitiesToInsert)`
   - Log verification: `dao.dbgCountForOwner(supervisedUserId)`

#### Step 3: UI Update (if BtConnectedActivity is visible)
**Thread**: UI thread (LiveData observer)
- `SupervisorFeedViewModel` observes Room database (filtered by target supervised user)
- New message inserted → LiveData emits new `Posture`
- `BtConnectedActivity.displayPostureData()` updates UI immediately

---

## Phase 3: Fall Detection & Notification

### When It Happens
- During mirror document processing
- **Only if**:
  1. Message timestamp is within **24 hours** of current time
  2. Message parses as `FallingPosture` type

### Why 24 Hours?
- Supervisor may be offline when fall occurs
- On reconnect, mirror fires with all missed messages
- Still notify supervisor if fall happened recently (within 24h)
- **Tradeoff**: Risk of notification spam if multiple falls in 24h window

### Notification Flow
1. Detect fall in `handleSupervisorDocumentChanges()`
2. **Async fetch supervised user's email**:
   - Query: `firestore.collection("users").document(supervisedUserId).get()`
   - Success: Use `email` field for notification text
   - Failure: Fallback to `supervisedUserId` (UID) for notification text
3. **Show notification**:
   - Method: `AlertNotifications.notifyFall(context, who, body)`
   - Notification appears in system notification tray
   - Tapping notification opens app (configured in `AlertNotifications`)

### Threading Notes
- Fall detection: Background thread (part of mirror processing)
- Email fetch: Background thread (Firestore async get)
- Notification shown: UI thread (via system NotificationManager)

---

## Phase 4: Mirror Lifecycle Events

### Event: Network Disconnected
**Trigger**: `NetworkConnectivityMonitor` detects offline
**Action**: `stopAllMirrors()` called
**Effect**: 
- All listeners detached (call `listener.remove()`)
- `mirrorByUid` map cleared
- No more real-time updates until reconnect

### Event: Network Reconnected
**Trigger**: `NetworkConnectivityMonitor` detects online
**Action**: `handleConnectivityRestored()` → `startSupervisorMirrors()` called
**Effect**:
- Mirrors re-attached for each supervised UID
- Listeners fire immediately with all documents (including any missed during offline)
- Fall notifications may be shown for recent missed falls

### Event: User Signs Out
**Trigger**: User clicks sign out button
**Action**: `SessionGate.handleUserSignedOut()` → `stopAllMirrors()` + `clearLocalData()`
**Effect**:
- All mirrors stopped
- Room database cleared (all messages deleted)
- Next sign-in starts fresh

### Event: User Changes Role
**Trigger**: User signs out, signs back in with different role (e.g., supervisor → supervised)
**Action**: `stopAllMirrors()` during sign-out, then `runSupervisedPipeline()` on new sign-in
**Effect**:
- Old mirrors properly cleaned up
- No mirrors started for supervised role
- Room database purged of old supervised users' data

---

## Key Implementation Details

### Owner Mapping (Critical)
**Problem**: Supervisor's Room database contains messages from **multiple** supervised users. How do we keep them separate?

**Solution**: Every `ReceivedBtDataEntity` has `owner_user_id` field:
- For **supervised users**: `owner_user_id = currentUserId` (their own UID)
- For **supervisor mirrors**: `owner_user_id = supervisedUserId` (the supervised user's UID)

**Queries**: All Room queries for supervisors use `WHERE owner_user_id = ?` to filter by supervised user.

### Deduplication Strategy
**Problem**: Firestore listeners can fire multiple times for the same document (cache vs server updates, reconnect events)

**Solution**: Before inserting, check if message already exists:
```java
int exists = dao.messageExistsOwned(supervisedUserId, deviceAddress, timestamp, receivedMsg);
if (exists == 0) {
  // Insert new message
}
```

**Why this works**: Unique constraint on `(owner_user_id, device_address, timestamp, received_msg)` ensures no duplicates.

### Mirror Concurrency Control
**Problem**: Multiple threads might try to start the same mirror simultaneously

**Solution**: Use two data structures:
1. `mirrorByUid` (Map): Stores active listeners
2. `startingUids` (Set): Tracks mirrors currently being started

**Logic**:
```java
if (mirrorByUid.containsKey(supervisedUserId)) {
  return; // Already active
}
if (startingUids.contains(supervisedUserId)) {
  return; // Start in progress
}
startingUids.add(supervisedUserId);
// ... attach listener ...
mirrorByUid.put(supervisedUserId, listener);
startingUids.remove(supervisedUserId);
```

---

## Files Reference

| File | Responsibility |
|------|---------------|
| `FirestoreSyncService.java` | Mirror setup, document processing, fall detection, Room inserts |
| `SessionGate.java` | Orchestrates mirror start after login, cleanup on sign-out |
| `NetworkConnectivityMonitor.java` | Detects online/offline, triggers mirror restart |
| `SupervisorFeedViewModel.java` | Queries Room for latest messages (filtered by supervised user) |
| `BtConnectedActivity.java` | Observes ViewModel, updates UI with latest posture |
| `AlertNotifications.java` | Shows fall notifications |
| `ReceivedBtDataDao.java` | Room database queries (dedup check, insert, owner filtering) |

---

## Common Issues & Debugging

### Issue: Supervisor not seeing real-time updates
**Check**:
1. Mirror active? Check logs for `"SYNC/Mirror: Attach listener for childUid=..."`
2. Network online? Check `NetworkConnectivityMonitor.isConnected()`
3. Messages in Firestore? Query `bluetooth_messages` collection for supervised UID
4. Owner mapping correct? Check `dao.dbgCountForOwner(supervisedUserId)` → should be > 0

### Issue: Duplicate notifications for same fall
**Root cause**: Fall detection checks every message in 24h window, no "already notified" tracking
**Workaround**: Reduce `RECENT_MS` constant (currently 24h) or add "notified" flag to Room database

### Issue: Mirror not restarting after network reconnect
**Check**:
1. `handleConnectivityRestored()` called? Check logs for `"Network connectivity restored"`
2. User session loaded? Check `userSession.isLoaded()` and `userSession.isSupervisor()`
3. Supervised user IDs present? Check `userSession.getSupervisedUserIds()` → should not be empty

---

