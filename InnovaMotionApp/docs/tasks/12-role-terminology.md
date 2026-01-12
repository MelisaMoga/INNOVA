# TASK 12: Role Terminology Update (Supervised → Aggregator)

**Assigned To:** Any Developer (cleanup task)  
**Estimated Effort:** 1 day  
**Dependencies:** None (can be done last)  
**Status:** Pending

---

## Context

The specification renames "Supervised" role to "Aggregator" for clarity. The old name was confusing because supervised users aren't being supervised - they aggregate data from multiple monitored persons.

---

## Deliverables

### 1. Update `RoleProvider.java`

```java
public enum Role { 
    AGGREGATOR,   // NEW: Data collection role (formerly SUPERVISED)
    SUPERVISOR, 
    UNKNOWN,
    
    @Deprecated
    SUPERVISED    // Keep for backward compatibility, maps to AGGREGATOR
}

public static Role getCurrentRole() {
    String role = GlobalData.getInstance().currentUserRole;
    if ("supervised".equalsIgnoreCase(role) || "aggregator".equalsIgnoreCase(role)) {
        return Role.AGGREGATOR;
    }
    if ("supervisor".equalsIgnoreCase(role)) {
        return Role.SUPERVISOR;
    }
    return Role.UNKNOWN;
}

public static boolean isAggregator() {
    return getCurrentRole() == Role.AGGREGATOR;
}

@Deprecated
public static boolean isSupervised() {
    return isAggregator(); // Alias for backward compatibility
}
```

### 2. Update Firestore user documents

**Decision needed:** Use new role name or keep backward compatible?

**Option A: Backward compatible (recommended)**
- Accept both "supervised" and "aggregator" in code
- New signups use "aggregator"
- Existing users continue to work

**Option B: Migration**
- Run one-time Firestore migration script
- Update all `role: "supervised"` to `role: "aggregator"`

### 3. Update UI strings

In `strings.xml`:

```xml
<!-- Old -->
<string name="role_supervised">Supervised</string>

<!-- New -->
<string name="role_aggregator">Aggregator</string>
<string name="aggregator_description">Collects posture data from monitored persons</string>
```

Update any user-facing text that mentions "supervised user" to "aggregator".

### 4. Update code comments and documentation

Search and replace in codebase:
- "supervised user" → "aggregator"
- "Supervised" → "Aggregator" (in comments)

Keep functional code backward compatible.

---

## Files to Modify

- [`RoleProvider.java`](../../app/src/main/java/com/melisa/innovamotionapp/utils/RoleProvider.java)
- [`UserSession.java`](../../app/src/main/java/com/melisa/innovamotionapp/sync/UserSession.java)
- `app/src/main/res/values/strings.xml`
- Various code comments throughout the codebase

---

## Acceptance Criteria

- [ ] `Role.AGGREGATOR` enum added
- [ ] `isAggregator()` method works
- [ ] Old "supervised" role value still accepted (backward compatible)
- [ ] UI shows "Aggregator" terminology
- [ ] Code comments updated where appropriate
- [ ] No breaking changes to existing functionality
