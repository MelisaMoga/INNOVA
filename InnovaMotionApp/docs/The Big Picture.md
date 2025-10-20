## 🎯 Final Summary: The Big Picture

1. **Login → Main** is a **multi-step async flow**:
   - Google Auth → Firestore profile check → Role selection → Firestore save → SessionGate init → UserSession load → Role-based bootstrap (backfill + mirrors) → MainActivity
   - **Supervised** users end up in a state where they can connect a BT device and start uploading data.
   - **Supervisor** users end up with local Room database pre-filled with supervised users' data + real-time mirrors active.

2. **Bluetooth Pipeline** (supervised only) is a **three-stage pipeline**:
   - Stage 1: **BT thread** receives raw messages → adds to in-memory queue
   - Stage 2: **Batch thread** (every 500ms) drains queue → inserts into Room → triggers Firestore upload
   - Stage 3: **Sync service** (background) uploads to Firestore if online, otherwise retries when network returns
   - **Key file**: `DeviceCommunicationService.java` (batchSaving "DeviceCommunicationService.java")

3. **Supervisor Mirror Pipeline** (supervisor only) is a **real-time sync system**:
   - Stage 1: **Snapshot listeners** (mirrors) attach to supervised users' Firestore data after login
   - Stage 2: **Mirror callback** fires on new/updated messages → `handleSupervisorDocumentChanges()` processes updates
   - Stage 3: **Deduplication check** → Create entity with `owner_user_id` mapping → **Fall detection** → Insert to Room
   - **Fall detection**: If message is recent (24h) AND posture is FallingPosture → fetch supervised user email → show notification
   - **Key file**: `FirestoreSyncService.java` (method: `handleSupervisorDocumentChanges()`)
   - **UI updates**: Room LiveData observers trigger automatic UI refresh in `BtConnectedActivity`

4. **Threading discipline**:
   - All network/DB operations: **Background threads** (executorService, Firestore callbacks, Room DAOs, BT socket)
   - All UI updates: **UI thread** (via `runOnUiThread()`)
   - No blocking operations on UI thread (good practice observed throughout)

5. **Offline resilience**:
   - Supervised users: Messages always saved to Room first, Firestore upload is best-effort (retries on reconnect)
   - Supervisor users: Backfill downloads missing data on reconnect, mirrors re-attach if needed