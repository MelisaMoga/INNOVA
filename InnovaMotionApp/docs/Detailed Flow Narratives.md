
## 📖 Detailed Flow Narratives

### 1. Entry Flow: LoginActivity → MainActivity

#### Starting Point: `LoginActivity.java`

**User Journey**:
1. **App launches** → `LoginActivity.onCreate()` initializes Firebase Auth, CredentialManager, and UI components.
2. **User clicks "Sign in with Google"** → `signInWithGoogle()` is called.

**What Happens** (step-by-step):

##### Step 1: Google Sign-In
- **Thread**: Background (async callback via CredentialManager)
- **Actions**:
  - `CredentialManager` requests Google ID token
  - If successful, `handleSignInResult()` extracts the token → calls `authenticateWithFirebase(idToken)`
  - Firebase Auth signs in with the Google credential
- **Outcomes**:
  - **Success**: Firebase Auth completes, `mAuth.getCurrentUser()` is now non-null → proceed to Step 2
  - **Failure**: Show error toast (e.g., "Sign-in failed"), stay on `LoginActivity`

##### Step 2: Check User in Firestore
- **Thread**: Background (Firestore async)
- **Actions**:
  - `checkUserInFirestore(user)` queries `db.collection("users").document(user.getUid())`
- **Outcomes**:
  - **User document exists**: Continue to Step 3 (show role selection UI)
  - **User document does NOT exist**: Call `createUserProfile(user)` → save basic profile (UID, email, displayName) to Firestore → then proceed to Step 3
  - **Firestore error** (e.g., permission denied): Show error toast, possibly sign out user

##### Step 3: Show Role Selection UI
- **Thread**: UI thread
- **Actions**:
  - `showRoleSelectionUI(user)` hides sign-in button, shows "Welcome [name]" + role radio buttons
  - **Pre-fill logic** (`fetchAndPreFillUserPreferences`):
    - **Thread**: Background (Firestore get)
    - If user doc has `role = "supervisor"` + `supervisedEmail`: Pre-fill email field, check "Supervisor" radio
    - If user doc has `role = "supervised"`: Check "Supervised" radio, hide email field
    - If neither: Default to "Supervised" checked
- **User Interaction**:
  - User can **change** the pre-filled selection
  - If "Supervisor" selected: Email field becomes visible, shows autocomplete suggestions (queries Firestore for users with `role = "supervised"`)
  - Autocomplete search: **Background thread** (Firestore query), debounced by 300ms

##### Step 4: User Clicks "Continue" (`onProceed()`)

**Validation** (UI thread):
- **Common**: Role must be selected (one radio button checked)
- **Supervisor-specific**:
  - Email field must not be empty
  - Email must match Gmail format (`^[A-Za-z0-9+_.-]+@gmail\\.com$`)
  - If validation fails: Show inline error, stay on screen

**If Validation Passes**:
- **Thread**: Background (Firestore update)
- **Actions**:
  - `saveUserRole(user, role, supervisedEmail)` updates Firestore:
    - `role`: "supervised" or "supervisor"
    - `lastSelectedRole`: Same as `role` (for future pre-filling)
    - `supervisedEmail`: Set if supervisor, null otherwise
    - `lastSignIn`: Current timestamp
- **Outcomes**:
  - **Success**: Proceed to Step 5
  - **Failure**: Show error toast, re-enable buttons, stay on screen

##### Step 5: Initialize SessionGate
- **Thread**: UI thread (singleton init), then background (session loading)
- **Actions**:
  - `SessionGate.getInstance(context)` creates the singleton (if not exists)
  - SessionGate's constructor sets up a Firebase Auth state listener (background callback)
  - Listener detects authenticated user → calls `handleUserAuthenticated(user)`

##### Step 6: Load User Session
- **Thread**: Background (Firestore get + query)
- **Actions** (`UserSession.loadUserSession()`):
  1. Fetch user doc from `users` collection
  2. Extract `role` field
  3. **If supervisor**:
     - Try to get `supervisedUserIds` array (list of UIDs)
     - If array is empty/null, try `supervisedEmail` field → query `users` collection for matching email → resolve to UID
     - Cache `supervisedUserIds = ["childUid1", "childUid2", ...]`
  4. **If supervised**:
     - Cache `supervisedUserIds = []` (empty list)
- **Outcomes**:
  - **Success**: `onSessionLoaded(userId, role, supervisedUserIds)` callback → proceed to Step 7
  - **Failure**: `onSessionLoadError(error)` → session not ready, but app continues (user may see degraded experience)

##### Step 7: Run Post-Auth Bootstrap
- **Thread**: Background (executorService)
- **Actions** (`SessionGate.runPostAuthBootstrap()`):

**Path A: Supervised User** (`runSupervisedPipeline()`):
1. Call `FirestoreSyncService.backfillLocalFromCloudForCurrentUser()`
   - **Thread**: Background
   - **Actions**:
     - Query Firestore `bluetooth_messages` collection: `where userId = currentUserId AND timestamp > localMaxTimestamp`
     - Convert Firestore docs to `ReceivedBtDataEntity`
     - Insert into Room database (local SQLite)
   - **Why**: Restore historical data if user reinstalled app or cleared data
2. Backfill completes → log success/failure
3. Proceed to Step 8

**Path B: Supervisor User** (`runSupervisorPipeline()`):
1. Call `FirestoreSyncService.purgeAndBackfillForSupervisor(supervisedUserIds)`
   - **Thread**: Background
   - **Actions**:
     a. **Purge old data**: Delete all Room rows where `owner_user_id NOT IN supervisedUserIds`
        - **Why**: Clean up data from previously supervised users who are no longer monitored
     b. **Backfill for each supervised UID**:
        - For each `childUid` in `supervisedUserIds`:
          - Query Firestore `bluetooth_messages`: `where userId = childUid AND timestamp > localMaxTimestamp`
          - Insert into Room with `owner_user_id = childUid`
        - **Why**: Download supervised users' data to local Room so supervisor can view offline
     c. **Start real-time mirrors**:
        - For each `childUid`, attach a Firestore snapshot listener: `collection("bluetooth_messages").whereEqualTo("userId", childUid).orderBy("timestamp")`
        - **Why**: Automatically download new messages as supervised users upload them
2. Backfill completes → mirrors active → log success
3. Proceed to Step 8

##### Step 8: Navigate to MainActivity
- **Thread**: UI thread
- **Actions**:
  - `navigateToMainActivity()` creates an `Intent` with flags `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`
  - **Why flags**: Clear entire back stack so user can't navigate back to LoginActivity
  - `startActivity(intent)` → `finish()` (LoginActivity destroyed)
- **Outcome**: User now sees `MainActivity.java`

---

### 2. MainActivity → Launch Monitoring

**User Action**: User clicks "Launch Monitoring" button in `MainActivity`

**What Happens**:

##### Step 1: Check Session Readiness
- **Thread**: UI thread (SessionGate check)
- **Actions**:
  - `SessionGate.getInstance(this).waitForSessionReady(callback)`
  - **If session is ready**: Callback `onSessionReady(userId, role, supervisedUserIds)` fires → proceed to Step 2
  - **If session is NOT ready**: Callback `onSessionError(error)` fires → fallback to supervised flow (route to `BtSettingsActivity`)

##### Step 2: Route Based on Role

**Path A: Supervised User**:
- **Target**: `BtSettingsActivity.java`
- **Why**: Supervised user needs to scan for and connect to a Bluetooth device
- **Navigation**: `Intent` to `BtSettingsActivity`

**Path B: Supervisor User**:
- **Target**: `BtConnectedActivity.java` (skip Bluetooth setup entirely)
- **Why**: Supervisor views data from Room database (no physical device needed)
- **Navigation**: `Intent` to `BtConnectedActivity` with flags `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP`

---

### 3. Bluetooth Message Pipeline (Supervised User)

**Assumption**: User has successfully connected a Bluetooth device via `BtSettingsActivity` → `DeviceCommunicationService` is running as a foreground service.

#### Stage 1: Bluetooth Connection (`DeviceCommunicationThread.java`)

**Thread**: Background (dedicated Bluetooth thread)

**Lifecycle**:
1. **Connect**: `socket.connect()` → `onConnectionEstablished(device)` callback
   - Update foreground notification: "Connected to [Device Name]"
   - Set global flag `isConnectedDevice = true`
2. **Receive Data**: `BufferedReader.readLine()` in infinite loop
   - Each line (terminated by `\n` or `\r\n`) triggers `onDataReceived(device, receivedData)` callback
   - **Threading**: Still on Bluetooth thread
3. **Disconnect**: On error or stream close → `onConnectionDisconnected()` callback
   - Retry connection up to 2 times
   - If max retries exceeded: Stop service

#### Stage 2: Message Received → Add to Batch Queue

**File**: `DeviceCommunicationService.java`, method: `onDataReceived()`

**Thread**: Background (Bluetooth callback thread)

**Actions**:
1. **Parse message**: Extract raw hex string (e.g., `"0xAB3311"`)
2. **Create entity**:
   ```
   ReceivedBtDataEntity(
     deviceAddress = device.getAddress(),
     timestamp = System.currentTimeMillis(),
     receivedMsg = receivedData,
     owner_user_id = currentUserId // if supervised
   )
   ```
3. **Add to batch queue**:
   - `synchronized (lock) { temporaryReceivedBtDataListToSave.add(entity); }`
   - **Why synchronized**: Batch-saving thread accesses the same list concurrently
4. **Side effects**:
   - Parse posture type (via `PostureFactory.createPosture(receivedData)`)
   - Update `GlobalData.setReceivedPosture(posture)` → triggers LiveData observers in UI
   - If posture is "falling", show local notification

**Open Question**: What happens if the batch queue grows faster than 500ms can drain it? (e.g., device sends 100 messages in 100ms)  
**Assumption**: Queue keeps growing until next batch cycle; no explicit size limit observed in code.

#### Stage 3: Batch-Saving Thread (Every 500ms)

**File**: `DeviceCommunicationService.java`, background thread started in `onCreate()`

**Thread**: Background (dedicated thread with `while` loop)

**Timing**: `Constants.COUNTDOWN_TIMER_IN_MILLISECONDS_FOR_MESSAGE_SAVE = 500` ms

**Actions** (per cycle):
1. **Sleep**: `Thread.sleep(500)`
2. **Copy and clear queue**:
   ```
   synchronized (lock) {
     currentBatch = new ArrayList<>(temporaryReceivedBtDataListToSave);
     temporaryReceivedBtDataListToSave.clear();
   }
   ```
3. **If batch is not empty**:
   - **Save to Room**: `database.receivedBtDataDao().insertAll(currentBatch)`
     - **Threading**: Synchronous Room insert (blocks this thread, but it's already background)
     - **Strategy**: `@Insert(onConflict = OnConflictStrategy.IGNORE)` → duplicates are silently skipped
   - **Sync each message to Firestore**:
     - Loop over `currentBatch`
     - For each entity: `firestoreSyncService.syncNewMessage(entity, callback)`
       - **Threading**: Spawns new background task on `executorService`
       - **Callback**: Logs success/error (non-blocking)
4. **If batch is empty**: No-op, wait for next cycle

**Outcome**: All messages are **guaranteed to be saved to Room** (local persistence). Firebase upload is **best-effort** (may fail if offline).

#### Stage 4: Sync to Firebase (`FirestoreSyncService.java`, method: `syncNewMessage()`)

**Thread**: Background (executorService)

**Preconditions** (checked in order):
1. User is authenticated? **No** → callback `onError("User not authenticated")`
2. UserSession is loaded? **No** → callback `onError("User session not loaded")`
3. User is supervised? **No** → callback `onSuccess("User is not supervised, no sync needed")` (supervisors don't upload)
4. Network is connected? **No** → callback `onSuccess("Message saved locally, will sync when online")`

**If all preconditions pass**:
- **Actions**:
  1. Convert `ReceivedBtDataEntity` to `FirestoreDataModel`
  2. Generate document ID: `"<userId>_<timestamp>_<deviceAddress>"`
  3. Call `firestore.collection("bluetooth_messages").document(documentId).set(firestoreModel)`
     - **Threading**: Firestore async operation (callback on background thread)
- **Outcomes**:
  - **Success**: Callback `onSuccess("Message synced successfully")`
  - **Failure**: Callback `onError("Failed to sync to Firestore: [error]")` → message stays in Room for later retry

#### Stage 5: Offline → Online Transition (Retry Logic)

**Trigger**: `NetworkConnectivityMonitor` detects connectivity restored

**Actions**:
- `FirestoreSyncService.handleConnectivityRestored()` (background thread)
- **If user is supervised**:
  - Call `syncLocalDataToFirestore()`
    - Query Room for all local messages
    - Check which docs already exist in Firestore (batch query)
    - Upload only missing messages (batch write, max 500 per batch)
- **If user is supervisor**:
  - Call `startSupervisorMirrors()` (re-attach listeners if they were stopped)

**Outcome**: Local Room database eventually syncs with Firestore when network is stable.

---

### 4. Supervisor Real-Time Mirror Pipeline

**Context**: After supervisor completes login and backfill, real-time mirrors are active for each supervised user.

#### Stage 1: Mirror Listener Attached

**File**: `FirestoreSyncService.java`, method: `startSupervisorMirror()`

**Thread**: Background (Firestore snapshot listener callback)

**Actions**:
1. Attach Firestore snapshot listener for each supervised UID:
   - Query: `collection("bluetooth_messages").whereEqualTo("userId", supervisedUserId).orderBy("timestamp")`
   - Listener fires on **every change** (new message added, existing message modified)
2. Store listener registration in `mirrorByUid` map for cleanup later
3. **Deduplication**: Check if mirror already active for this UID (skip if already listening)

**Trigger**: New message uploaded by supervised user → Firestore sends snapshot update → listener callback fires

#### Stage 2: Process Document Changes (`handleSupervisorDocumentChanges()`)

**File**: `FirestoreSyncService.java`, method: `handleSupervisorDocumentChanges()`

**Thread**: Background (executorService)

**This is the critical function that processes real-time updates from supervised users.**

**Actions** (for each document change):

##### Step 1: Filter Document Changes
- Loop through all `documentChanges` in the snapshot
- Process only `ADDED` or `MODIFIED` types (ignore `REMOVED`)
- **Why**: We want new messages and updates, but deletions are rare/unsupported

##### Step 2: Convert Firestore Doc to Entity
- Parse Firestore document into `FirestoreDataModel`
- Extract: `deviceAddress`, `timestamp`, `receivedMsg`, `userId`

##### Step 3: Deduplication Check
- **Critical**: Query Room database to check if message already exists locally
- Call: `dao.messageExistsOwned(supervisedUserId, deviceAddress, timestamp, receivedMsg)`
- **Returns**: `0` if not exists, `1` if exists
- **Why**: Firestore listeners can fire multiple times for the same doc (e.g., on reconnect, cache vs server updates)

##### Step 4: Create Room Entity with Owner Mapping
- **Only if message doesn't exist** (dedup check passed):
  ```
  ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
    deviceAddress,
    timestamp,
    receivedMsg,
    supervisedUserId  // <- CRITICAL: owner_user_id set to supervised user's UID
  )
  ```
- Add entity to `entitiesToInsert` list
- **Why owner mapping matters**: Supervisor's Room database contains messages from **multiple** supervised users. Each row must be tagged with `owner_user_id` so queries can filter by supervised user.

##### Step 5: Fall Detection and Notification (if applicable)

**Condition**: Message is recent (within 24 hours) AND looks like a fall

**Actions**:
1. **Parse posture type**: `PostureFactory.createPosture(receivedMsg)`
2. **Check if fall**: `if (posture instanceof FallingPosture)`
3. **If fall detected**:
   - **Fetch supervised user's email** (async Firestore query to `users` collection)
     - **Thread**: Background (Firestore get callback)
     - **Success**: Use email in notification text (e.g., "child@gmail.com has fallen")
     - **Failure**: Fallback to UID in notification text
   - **Show notification** on supervisor's device:
     - `AlertNotifications.notifyFall(context, who, body)`
     - **Threading**: Notification shown on UI thread via system notification manager
     - **Why**: Alert supervisor immediately so they can check on supervised user

**Fall Detection Logic**:
- Recent threshold: `RECENT_MS = 60_000 * 1 * 60 * 24` (24 hours)
  - **Why 24 hours?**: Supervisor may be offline when fall occurs; notification still shown when they come back online if within 24h
- Only notify if `(now - timestamp) <= RECENT_MS` AND message is a fall posture
- **Threading note**: Firestore email lookup and notification happen **asynchronously** (don't block the mirror pipeline)

##### Step 6: Batch Insert into Room
- After processing all document changes, insert all new entities:
  - `dao.insertAll(entitiesToInsert)`
  - **Strategy**: `@Insert(onConflict = OnConflictStrategy.IGNORE)` (duplicates silently skipped)
- **Verification logging**: Query Room to confirm inserts: `dao.dbgCountForOwner(supervisedUserId)`

**Outcome**: 
- Supervisor's Room database now contains the new messages (tagged with `owner_user_id`)
- If any falls detected, supervisor receives local notification
- `BtConnectedActivity` (if open) observes Room LiveData → UI updates automatically to show latest posture

#### Stage 3: UI Update (if BtConnectedActivity is open)

**File**: `BtConnectedActivity.java`, observes `SupervisorFeedViewModel.getLatestPosture()`

**Thread**: UI thread (LiveData observer)

**Actions**:
1. `SupervisorFeedViewModel` queries Room for latest message (filtered by target supervised user)
2. LiveData emits new `Posture` object
3. `BtConnectedActivity.displayPostureData()` updates UI:
   - Shows posture description (e.g., "Falling detected")
   - Updates risk level text
   - Plays/stops video animation

**Why this matters**: Supervisor sees **real-time updates** without refreshing the screen. As supervised user's device uploads messages, supervisor's device automatically downloads and displays them.

---

### 5. Supervisor Mirror Lifecycle Management

#### Mirror Start Conditions
- **Trigger 1**: After backfill completes during login (via `purgeAndBackfillForSupervisor()`)
- **Trigger 2**: When network connectivity is restored (via `handleConnectivityRestored()`)

#### Mirror Stop Conditions
- **Trigger 1**: User signs out (`SessionGate.handleUserSignedOut()` → `stopAllMirrors()`)
- **Trigger 2**: Network connectivity lost (listener automatically pauses, stopped explicitly by `stopAllMirrors()`)
- **Trigger 3**: App is destroyed/closed

#### Cleanup
**File**: `FirestoreSyncService.java`, method: `stopAllMirrors()`

**Actions**:
1. Loop through all active listeners in `mirrorByUid` map
2. Call `listener.remove()` for each (detaches Firestore listener)
3. Clear `mirrorByUid` map and `startingUids` set
4. Set `isSupervisorSyncActive = false`

**Why proper cleanup matters**: Prevents memory leaks and ensures old listeners don't fire after role change or sign-out.

---

## 🔍 Decision Points & Branches Summary

### Key Decision Point 1: User Role Selection
**Location**: `LoginActivity.onProceed()`

| Condition | Path | Actions | Next State |
|-----------|------|---------|------------|
| Role = supervised | Supervised flow | Save role → Load session → Backfill own data → Navigate to MainActivity | User can connect Bluetooth device |
| Role = supervisor + valid email | Supervisor flow | Save role + email → Load session → Resolve email to UID → Purge old data → Backfill supervised users' data → Start mirrors → Navigate to MainActivity | User can monitor supervised users |
| Role = supervisor + invalid email | Error | Show inline error | Stay on LoginActivity |

### Key Decision Point 2: Launch Monitoring from MainActivity
**Location**: `MainActivity.LaunchMonitoring()`

| User Role | SessionGate Ready? | Destination | Reason |
|-----------|-------------------|-------------|--------|
| Supervised | Yes | BtSettingsActivity | Scan for Bluetooth device |
| Supervised | No | BtSettingsActivity | Fallback (session error) |
| Supervisor | Yes | BtConnectedActivity | View Room data (no BT needed) |
| Supervisor | No | BtSettingsActivity | Fallback (session error) |

### Key Decision Point 3: Sync Message to Firestore
**Location**: `FirestoreSyncService.syncNewMessage()`

| Condition | Action | Outcome |
|-----------|--------|---------|
| User authenticated + supervised + online | Upload to Firestore | Message in cloud + local |
| User supervised + offline | Skip upload | Message in local Room only (retry later) |
| User is supervisor | Skip upload | Supervisor doesn't upload their own messages |
| User not authenticated | Skip upload | Message in local Room only |

### Key Decision Point 4: Supervisor Mirror Document Processing
**Location**: `FirestoreSyncService.handleSupervisorDocumentChanges()`

| Stage | Condition | True Outcome | False Outcome | Side Effects | Threading |
|-------|-----------|--------------|---------------|--------------|-----------|
| Filter Changes | Change type is ADDED or MODIFIED? | Process document | Skip document | None | Background (executorService) |
| Deduplication | Message already exists in Room? | Skip insert | Continue to create entity | None | Background (Room query) |
| Fall Detection | Message is recent AND posture is FallingPosture? | Fetch supervised user email → Show notification | No notification | Local notification shown to supervisor | Background (Firestore get) + UI thread (notification) |
| Email Fetch (if fall) | Supervised user doc has email field? | Use email in notification text | Use UID in notification text | None | Background (Firestore get callback) |
| Insert to Room | Batch insert succeeds? | New messages available to supervisor | Log error | Room database updated, LiveData observers notified | Background (Room insert) |

---

## ⚙️ Threading Summary

| Component | Thread Type | Why |
|-----------|-------------|-----|
| `LoginActivity.signInWithGoogle()` | Background (CredentialManager callback) | Network call to Google Auth |
| `LoginActivity.checkUserInFirestore()` | Background (Firestore async) | Network call to Firebase |
| `LoginActivity.fetchAndPreFillUserPreferences()` | Background (Firestore async) | Network call to Firebase |
| `SessionGate.handleUserAuthenticated()` | Background (executorService) | Loads session + starts backfill |
| `UserSession.loadUserSession()` | Background (executorService) | Queries Firestore for user profile |
| `FirestoreSyncService.backfillLocalFromCloudForCurrentUser()` | Background (executorService) | Downloads historical data |
| `FirestoreSyncService.purgeAndBackfillForSupervisor()` | Background (executorService) | Purges Room + downloads supervised users' data |
| `FirestoreSyncService.startSupervisorMirror()` | Background (Firestore snapshot listener) | Real-time sync |
| `DeviceCommunicationThread` | Background (dedicated thread) | Bluetooth I/O (blocking socket operations) |
| `DeviceCommunicationService` batch-saving thread | Background (dedicated thread with sleep loop) | Periodic Room inserts + Firestore uploads |
| `FirestoreSyncService.syncNewMessage()` | Background (executorService) | Non-blocking Firestore upload |
| `FirestoreSyncService.handleSupervisorDocumentChanges()` | Background (executorService) + async callbacks | Processes Firestore snapshot changes, checks duplicates, detects falls, inserts to Room |

**Rule of thumb**: All Firestore, Room, and Bluetooth operations happen on **background threads**. Only UI updates (show/hide buttons, navigation) happen on **UI thread** (via `runOnUiThread()`).

---

## 🚨 Open Questions & Assumptions

### Open Questions

1. **Batch queue overflow**: If Bluetooth device sends > 500 messages in 500ms, does the queue grow unbounded? Is there a max size or backpressure mechanism?
   - **Code location**: `DeviceCommunicationService.temporaryReceivedBtDataListToSave` (no size check observed)

2. **Supervisor mirror lifecycle**: If a supervisor signs out and signs back in with a different supervised email, are old mirrors properly cleaned up?
   - **Observed**: `SessionGate.handleUserSignedOut()` calls `stopAllMirrors()` and `clearLocalData()` (seems safe)

3. **Network retry strategy**: When offline messages are retried via `syncLocalDataToFirestore()`, is there exponential backoff or rate limiting?
   - **Code location**: `FirestoreSyncService.syncLocalDataToFirestore()` (no backoff observed, appears to be one-shot batch upload)

4. **Room database migration**: What happens if the Room schema changes between app versions?
   - **Not covered in current files** (would need to check `InnovaDatabase.java` migrations)

5. **Fall notification timing**: If supervisor is offline and multiple falls occur within 24 hours, will they receive multiple notifications when they come back online?
   - **Code location**: `FirestoreSyncService.handleSupervisorDocumentChanges()` line 1127
   - **Observed behavior**: Each fall message within 24h window triggers notification check (potential for notification spam)
   - **Suggestion**: Consider tracking "already notified" falls to prevent duplicate alerts

### Assumptions

- **Assumption 1**: Firebase Auth tokens are cached offline, so `FirebaseAuth.getCurrentUser()` returns non-null even without network (after initial sign-in).
  - **Evidence**: Code comment in `DeviceCommunicationService` line 188: "Firebase caches UID offline"

- **Assumption 2**: The 500ms batch timer starts when `DeviceCommunicationService.onCreate()` is called (when service is bound/started).
  - **Evidence**: Thread creation in `onCreate()` (line 57)

- **Assumption 3**: Supervised users' messages are uploaded to Firestore with `userId = currentUserId`, allowing supervisors to query by `whereEqualTo("userId", supervisedUserId)`.
  - **Evidence**: `FirestoreDataModel` constructor (line 204-209) and mirror query (line 1029)

- **Assumption 4**: The supervised email autocomplete popup shows **all** users with `role = "supervised"`, not just users the current supervisor is authorized to monitor.
  - **Evidence**: Query in `searchSupervisedUsers()` (line 676-678) has no additional filters

---

