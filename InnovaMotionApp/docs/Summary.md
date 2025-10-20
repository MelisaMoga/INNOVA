## 📘 Executive Summary

The InnovaMotion app supports two user types:
1. **Supervised users**: Connect a Bluetooth wearable device, receive posture data, and upload it to the cloud
2. **Supervisors**: Monitor data from one or more supervised users in real-time

After successful Google sign-in at `LoginActivity.java`, users select their role and the app:
- Loads their profile from Firebase (on a **background thread**)
- Backfills historical data from the cloud
- Routes them to `MainActivity.java`

For **supervised users**, incoming Bluetooth messages follow this pipeline:
1. Connection thread receives raw data → adds to in-memory batch queue
2. Batch-saving thread (every **500 ms**) saves batch to local **Room database** → attempts Firebase upload
3. Retry/offline handling ensures data reaches the cloud when connectivity returns

For **supervisors**, real-time monitoring works via Firestore mirrors:
1. After login, mirrors (snapshot listeners) attach to supervised users' data streams
2. New messages automatically download and insert into Room with owner mapping
3. Fall detection runs on incoming messages → supervisor receives instant notifications
4. UI updates in real-time as supervised users' devices upload posture data

---

## 📚 Glossary

| Term | Definition |
|------|------------|
| **Supervised** | A user who wears the Bluetooth device and generates posture data |
| **Supervisor** | A user who monitors one or more supervised users' data in real-time |
| **Room** | Android's local SQLite database wrapper (`InnovaDatabase`, table: `ReceivedBtDataEntity`) |
| **Firebase** | Cloud backend (Firestore for user profiles + Bluetooth messages; Firebase Auth for authentication) |
| **Backfill** | Downloading historical data from cloud to local Room database after sign-in/role change |
| **Mirror** | Real-time listener (supervisor only) that downloads new messages from supervised users as they appear in Firebase |
| **Batch** | In-memory list of messages accumulated over 500 ms before saving to Room |
| **SessionGate** | Singleton service that orchestrates post-login bootstrap (loads user role, starts backfill, launches mirrors) |
| **UserSession** | Caches user role and supervised user IDs from Firebase |
| **FirestoreSyncService** | Handles all cloud ↔ local sync operations (upload for supervised, download+mirror for supervisor) |
| **DeviceCommunicationService** | Foreground Android service managing Bluetooth connection and batch-saving thread |