# InnovaMotion App Documentation

**Audience**: Junior/"little tech" teammates  
**Style**: High-level, plain language explanations (no code snippets)  
**Last Updated**: October 2025

---

## 📑 Documentation Index

### Quick Start

1. **[Summary.md](Summary.md)** – Executive summary, glossary of key terms
2. **[The Big Picture.md](The%20Big%20Picture.md)** – 5-point overview of all major flows

### Detailed Guides

3. **[Detailed Flow Narratives.md](Detailed%20Flow%20Narratives.md)** – Complete step-by-step flows:
   - Entry flow: LoginActivity → MainActivity (both roles)
   - Bluetooth message pipeline (supervised users)
   - Supervisor real-time mirror pipeline (**NEW**)
   - Decision tables and threading details
   - Open questions and assumptions

4. **[Supervisor Mirror Pipeline.md](Supervisor%20Mirror%20Pipeline.md)** – Deep dive into supervisor monitoring:
   - Mirror setup and lifecycle
   - Real-time update processing via `handleSupervisorDocumentChanges()`
   - Fall detection and notification system
   - Owner mapping and deduplication strategy
   - Common issues and debugging tips

### Visual References

5. **[High-Level Flowcharts/](High-Level%20Flowcharts/)** – Mermaid flowcharts:
   - Login → Main (supervised user)
   - Login → Main (supervisor user)

6. **[Sequence Diagrams/](Sequence%20Diagrams/)** – Mermaid sequence diagrams:
   - `onProceed()` for supervised users
   - `onProceed()` for supervisor users
   - 500ms batch send cycle (supervised)

7. **[File Reference Summary.md](File%20Reference%20Summary.md)** – Quick lookup of all key files and their responsibilities

---

## 🎯 What's New (Latest Updates)

### Added Documentation for `handleSupervisorDocumentChanges()`

The critical function that was missing from the original documentation! This function is the heart of the supervisor mirror pipeline and handles:

- **Real-time message processing** from supervised users
- **Deduplication checks** (Firestore listeners can fire multiple times for the same doc)
- **Owner mapping** (tagging messages with `owner_user_id` for multi-user support)
- **Fall detection** (parsing posture, checking 24h window, fetching supervised user email)
- **Supervisor notifications** (alerting supervisor when supervised user falls)
- **Room database inserts** (with verification logging)

**Files Updated**:
- `Detailed Flow Narratives.md`: Added Section 4 (Supervisor Real-Time Mirror Pipeline) and Section 5 (Mirror Lifecycle Management)
- `Summary.md`: Added supervisor pipeline summary
- `The Big Picture.md`: Added point 3 (Supervisor Mirror Pipeline)
- `File Reference Summary.md`: Expanded `FirestoreSyncService.java` description
- **New**: `Supervisor Mirror Pipeline.md` (standalone deep-dive guide)

---

## 🔍 How to Use This Documentation

### If you're new to the project:
1. Start with **Summary.md** to understand terminology
2. Read **The Big Picture.md** for a 5-minute overview
3. Dive into **Detailed Flow Narratives.md** for complete step-by-step flows

### If you're debugging supervisor issues:
1. Go directly to **Supervisor Mirror Pipeline.md**
2. Check the "Common Issues & Debugging" section at the end
3. Review the decision table in **Detailed Flow Narratives.md** (Key Decision Point 4)

### If you need to understand threading:
1. Check the **Threading Summary** table in **Detailed Flow Narratives.md**
2. Look for **Thread** annotations throughout all narratives

### If you're looking for a specific file:
1. Use **File Reference Summary.md** for quick lookups

---

## 📐 Documentation Principles

1. **No code snippets** – Only file names, class names, and method names
2. **Explicit threading** – Every action notes whether it's UI thread or background
3. **Plain language** – Explain *what happens and why*, not *how code is written*
4. **Decision branches** – All conditions, outcomes, side effects clearly labeled
5. **Assumptions vs facts** – Inferred behavior is clearly marked as "Assumption"

---

## 🗂️ File Structure

```
docs/
├── README.md (this file)
├── Summary.md
├── The Big Picture.md
├── Detailed Flow Narratives.md
├── Supervisor Mirror Pipeline.md (NEW)
├── File Reference Summary.md
├── High-Level Flowcharts/
│   └── High-Level Flowcharts.md
└── Sequence Diagrams/
    ├── Sequence Diagrams.md
    ├── diagram1_on_proceed_supervised_user.png
    ├── diagram2_on_proceed_supervisor_user.png
    └── diagram3_supervised_bluetooth_message_pipeline500ms_batch_cycle.png
```

---

## 💡 Key Concepts to Understand

### Owner Mapping (Critical for Supervisors)
- Supervisor's Room database contains messages from **multiple** supervised users
- Each `ReceivedBtDataEntity` has `owner_user_id` field
- Supervised users: `owner_user_id = their own UID`
- Supervisor mirrors: `owner_user_id = supervised user's UID`
- All queries filter by `owner_user_id` to show correct data

### Deduplication Strategy
- Firestore listeners can fire multiple times for same document
- Before inserting, check: `dao.messageExistsOwned(supervisedUserId, deviceAddress, timestamp, receivedMsg)`
- Returns `0` if not exists, `1` if exists
- Prevents duplicate data in Room database

### Fall Detection (24-Hour Window)
- Supervisor receives notifications for falls within 24 hours
- Why? Supervisor may be offline when fall occurs
- On reconnect, mirror fires with all missed messages
- Still notify if fall happened recently
- Tradeoff: Risk of notification spam if multiple falls in 24h

### Threading Discipline
- **Background threads**: All Firestore, Room, Bluetooth operations
- **UI thread**: Only UI updates (buttons, navigation, LiveData observers)
- No blocking operations on UI thread (good practice throughout)

---

## 📞 Questions or Feedback?

If something is unclear or missing, please update this documentation! Follow the same style:
- Plain language (no code)
- Explicit threading
- Decision branches clearly labeled
- Assumptions marked as such

Happy reading! 📖

