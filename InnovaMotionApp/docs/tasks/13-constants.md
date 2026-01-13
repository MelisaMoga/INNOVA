# TASK 13: Constants and Configuration Centralization

**Assigned To:** Any Developer  
**Estimated Effort:** 0.5 days  
**Dependencies:** All other tasks (cleanup at end)  
**Status:** Pending

---

## Context

Ensure all new constants are centralized and configurable. This enables easy tuning and consistent behavior across the app.

---

## Deliverables

### 1. Update `Constants.java`

Add new protocol and configuration constants:

```java
public final class Constants {
    // ... existing constants ...
    
    // ===== Multi-User Protocol =====
    
    /** Terminator line that marks end of a packet */
    public static final String PACKET_TERMINATOR = "END_PACKET";
    
    /** Delimiter between sensor ID and hex code in packet lines */
    public static final String SENSOR_ID_DELIMITER = ";";
    
    /** Maximum readings allowed per packet (safety limit) */
    public static final int MAX_READINGS_PER_PACKET = 1000;
    
    // ===== UI Configuration =====
    
    /** Maximum messages to display in message log */
    public static final int MESSAGE_LOG_MAX_ITEMS = 500;
    
    /** Threshold (ms) after which data is considered stale in dashboard */
    public static final long STALE_DATA_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes
    
    /** Dashboard grid columns (phones) */
    public static final int DASHBOARD_SPAN_COUNT_PHONE = 2;
    
    /** Dashboard grid columns (tablets) */
    public static final int DASHBOARD_SPAN_COUNT_TABLET = 3;
    
    // ===== Sync Configuration =====
    
    /** Firestore whereIn query limit */
    public static final int FIRESTORE_WHERE_IN_LIMIT = 10;
    
    /** WriteBatch document limit */
    public static final int FIRESTORE_BATCH_LIMIT = 500;
    
    private Constants() {} // Prevent instantiation
}
```

### 2. Create `FeatureFlags.java` (optional)

For gradual rollout and A/B testing:

```java
public final class FeatureFlags {
    
    /** Enable multi-user packet protocol */
    public static final boolean MULTI_USER_PROTOCOL_ENABLED = true;
    
    /** Enable new aggregator dashboard UI */
    public static final boolean AGGREGATOR_DASHBOARD_ENABLED = true;
    
    /** Enable new supervisor multi-person dashboard */
    public static final boolean SUPERVISOR_DASHBOARD_ENABLED = true;
    
    /** Use compound Firestore queries for supervisor */
    public static final boolean COMPOUND_QUERIES_ENABLED = true;
    
    private FeatureFlags() {}
}
```

Usage:
```java
if (FeatureFlags.MULTI_USER_PROTOCOL_ENABLED) {
    // Use new packet parser
} else {
    // Fall back to legacy single-line handling
}
```

### 3. Move any scattered magic numbers

Search codebase for:
- Hardcoded timeouts
- Buffer sizes
- Retry counts
- UI dimensions

Move to `Constants.java` with descriptive names.

---

## Files to Modify

- [`Constants.java`](../../app/src/main/java/com/melisa/innovamotionapp/utils/Constants.java)

## Files to Create (optional)

- `app/src/main/java/com/melisa/innovamotionapp/utils/FeatureFlags.java`

---

## Acceptance Criteria

- [ ] All protocol constants in Constants.java
- [ ] All UI configuration values centralized
- [ ] All sync configuration values centralized
- [ ] No magic numbers scattered in code
- [ ] FeatureFlags enables gradual rollout (optional)
- [ ] Constants have descriptive Javadoc comments
