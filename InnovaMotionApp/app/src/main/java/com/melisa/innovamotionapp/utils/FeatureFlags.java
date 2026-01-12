package com.melisa.innovamotionapp.utils;

/**
 * Feature flags for gradual rollout and A/B testing.
 * 
 * These flags enable controlled feature deployment:
 * - Enable/disable features without code changes
 * - Gradual rollout to users
 * - Quick rollback if issues arise
 * - A/B testing capabilities
 * 
 * Usage:
 * <pre>
 * if (FeatureFlags.MULTI_USER_PROTOCOL_ENABLED) {
 *     // Use new packet parser
 * } else {
 *     // Fall back to legacy single-line handling
 * }
 * </pre>
 * 
 * For production, consider migrating to a remote config system (Firebase Remote Config)
 * to enable dynamic flag updates without app releases.
 */
public final class FeatureFlags {
    
    // ========== PROTOCOL FLAGS ==========
    
    /**
     * Enable multi-user packet protocol.
     * 
     * When true: Parse packets with format "sensorId;hexCode\n...END_PACKET\n"
     * When false: Fall back to legacy single-line format "hexCode\n"
     */
    public static final boolean MULTI_USER_PROTOCOL_ENABLED = true;
    
    // ========== UI FLAGS ==========
    
    /**
     * Enable new aggregator dashboard UI.
     * 
     * When true: Show tabbed interface with Message Log and Live Posture tabs
     * When false: Use legacy single-view interface
     */
    public static final boolean AGGREGATOR_DASHBOARD_ENABLED = true;
    
    /**
     * Enable new supervisor multi-person dashboard.
     * 
     * When true: Show grid of all monitored persons with real-time updates
     * When false: Use legacy single-person view
     */
    public static final boolean SUPERVISOR_DASHBOARD_ENABLED = true;
    
    /**
     * Enable person name management UI.
     * 
     * When true: Allow assigning friendly names to sensor IDs
     * When false: Display raw sensor IDs
     */
    public static final boolean PERSON_NAMES_ENABLED = true;
    
    // ========== SYNC FLAGS ==========
    
    /**
     * Use compound Firestore queries for supervisor sync.
     * 
     * When true: Use whereIn() for batched sensor queries (more efficient)
     * When false: Use individual queries per sensor
     */
    public static final boolean COMPOUND_QUERIES_ENABLED = true;
    
    /**
     * Enable batch upload for aggregator data.
     * 
     * When true: Batch multiple readings into single Firestore writes
     * When false: Write each reading individually
     */
    public static final boolean BATCH_UPLOAD_ENABLED = true;
    
    /**
     * Enable offline queue for aggregator uploads.
     * 
     * When true: Queue uploads when offline, retry when connected
     * When false: Discard data when offline
     */
    public static final boolean OFFLINE_QUEUE_ENABLED = true;
    
    // ========== NOTIFICATION FLAGS ==========
    
    /**
     * Enable fall detection notifications.
     * 
     * When true: Show notifications when fall posture is detected
     * When false: Suppress fall notifications
     */
    public static final boolean FALL_NOTIFICATIONS_ENABLED = true;
    
    /**
     * Enable supervisor fall alerts from aggregator data.
     * 
     * When true: Supervisors receive alerts for falls detected by aggregators
     * When false: Only aggregators receive local fall alerts
     */
    public static final boolean SUPERVISOR_FALL_ALERTS_ENABLED = true;
    
    // ========== DEBUG FLAGS ==========
    
    /**
     * Enable verbose sync logging.
     * 
     * When true: Log detailed sync operations for debugging
     * When false: Minimal logging for production
     */
    public static final boolean VERBOSE_SYNC_LOGGING = false;
    
    /**
     * Enable Bluetooth packet logging.
     * 
     * When true: Log all received Bluetooth packets for debugging
     * When false: Minimal logging for production
     */
    public static final boolean VERBOSE_BT_LOGGING = false;
    
    // Prevent instantiation
    private FeatureFlags() {
        throw new UnsupportedOperationException("FeatureFlags class cannot be instantiated");
    }
}
