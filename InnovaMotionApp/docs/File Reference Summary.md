## 📦 File Reference Summary (No Code)

### Core Flow Files

| File | Responsibility |
|------|---------------|
| `LoginActivity.java` | Entry point, Google Auth, role selection, pre-fill preferences |
| `MainActivity.java` | Home screen, launches monitoring based on role |
| `SessionGate.java` | Orchestrates post-login bootstrap (loads session, starts backfill/mirrors) |
| `UserSession.java` | Caches user role + supervised UIDs from Firestore |
| `FirestoreSyncService.java` | All cloud ↔ local sync (upload for supervised, download+mirror for supervisor). Key methods: `syncNewMessage()` (supervised upload), `startSupervisorMirror()` (attach real-time listener), `handleSupervisorDocumentChanges()` (process mirror updates, fall detection, Room inserts) |
| `DeviceCommunicationService.java` | Foreground service managing Bluetooth + batch-saving thread (batchSaving "DeviceCommunicationService.java") |
| `DeviceCommunicationThread.java` | Bluetooth socket connection + data reception loop |
| `BtSettingsActivity.java` | Bluetooth device scanning + pairing (supervised users only) |
| `BtConnectedActivity.java` | Live posture display (supervised: from Bluetooth, supervisor: from Room) |

### Data Layer Files

| File | Responsibility |
|------|---------------|
| `InnovaDatabase` (Room) | Local SQLite database, table: `ReceivedBtDataEntity` |
| `ReceivedBtDataEntity` | Room entity (device address, timestamp, message, owner UID) |
| `FirestoreDataModel` | Firestore document structure for `bluetooth_messages` collection |

### Utility Files

| File | Responsibility |
|------|---------------|
| `Constants.java` | Defines `COUNTDOWN_TIMER_IN_MILLISECONDS_FOR_MESSAGE_SAVE = 500` |
| `GlobalData.java` | Singleton holding LiveData for UI (current posture, connection status) |
| `NetworkConnectivityMonitor` | Listens for network changes, triggers retry logic |
| `RoleProvider.java` | Helper to check current user role (supervised vs supervisor) |
| `AlertNotifications.java` | Shows fall detection notifications (both supervised and supervisor devices) |
| `PostureFactory.java` | Parses raw Bluetooth messages into `Posture` objects (FallingPosture, etc.) |
