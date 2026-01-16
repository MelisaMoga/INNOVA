package com.melisa.innovamotionapp.utils;

/**
 * Centralized constants for the InnovaMotion application.
 * 
 * All configuration values, magic numbers, and protocol constants should be defined here
 * to enable easy tuning and consistent behavior across the app.
 * 
 * Sections:
 * - File Storage
 * - Multi-User Protocol
 * - Firestore Sync Configuration
 * - UI Configuration
 * - Timing Configuration
 * - Dashboard Configuration
 */
public final class Constants {
    
    // ========== FILE STORAGE ==========
    
    /** File name pattern for posture data storage */
    public static final String POSTURE_DATA_SAVE_FILE_NAME = "%s_data.txt";
    
    /** Countdown timer in milliseconds before saving messages (debounce) */
    public static final int COUNTDOWN_TIMER_IN_MILLISECONDS_FOR_MESSAGE_SAVE = 500;
    
    // ========== MULTI-USER PROTOCOL ==========
    
    /** Terminator line that marks the end of a packet */
    public static final String PACKET_TERMINATOR = "END_PACKET";
    
    /** Delimiter between sensor ID and hex code in packet lines (e.g., "sensor001;0xAB3311") */
    public static final String SENSOR_ID_DELIMITER = ";";
    
    /** Maximum readings allowed per packet to prevent memory exhaustion from missing END_PACKET */
    public static final int MAX_READINGS_PER_PACKET = 1000;
    
    // ========== FIRESTORE SYNC CONFIGURATION ==========
    
    /** Firestore whereIn query limit (Firestore limitation: max 10 values) */
    public static final int FIRESTORE_WHERE_IN_LIMIT = 10;
    
    /** Firestore WriteBatch document limit (Firestore limitation: max 500 operations) */
    public static final int FIRESTORE_BATCH_LIMIT = 500;
    
    /** Page size for paginated Firestore queries */
    public static final int FIRESTORE_PAGE_SIZE = 500;
    
    /** Collection name for Bluetooth data in Firestore */
    public static final String FIRESTORE_COLLECTION_BT_DATA = "bluetooth_messages";
    
    /** Collection name for user profiles in Firestore */
    public static final String FIRESTORE_COLLECTION_USERS = "users";
    
    /** Collection name for sensor inventory in Firestore */
    public static final String FIRESTORE_COLLECTION_SENSORS = "sensors";
    
    /** Collection name for supervisor-sensor assignments in Firestore */
    public static final String FIRESTORE_COLLECTION_ASSIGNMENTS = "assignments";
    
    /** @deprecated Use FIRESTORE_COLLECTION_SENSORS instead */
    @Deprecated
    public static final String FIRESTORE_COLLECTION_PERSON_NAMES = "person_names";
    
    /** @deprecated Use FIRESTORE_COLLECTION_ASSIGNMENTS instead */
    @Deprecated
    public static final String FIRESTORE_COLLECTION_SENSOR_ASSIGNMENTS = "sensor_assignments";
    
    // ========== UI CONFIGURATION ==========
    
    /** Maximum messages to display in the message log */
    public static final int MESSAGE_LOG_MAX_ITEMS = 500;
    
    /** Maximum length for log string truncation */
    public static final int LOG_TRUNCATE_LENGTH = 50;
    
    /** Threshold (ms) after which data is considered stale in dashboard (5 minutes) */
    public static final long STALE_DATA_THRESHOLD_MS = 5 * 60 * 1000;
    
    // ========== DASHBOARD CONFIGURATION ==========
    
    /** Dashboard grid columns for phones (portrait) */
    public static final int DASHBOARD_SPAN_COUNT_PHONE = 2;
    
    /** Dashboard grid columns for tablets */
    public static final int DASHBOARD_SPAN_COUNT_TABLET = 3;
    
    /** Minimum screen width (dp) to be considered a tablet */
    public static final int TABLET_MIN_WIDTH_DP = 600;
    
    // ========== TIMING CONFIGURATION ==========
    
    /** Debounce delay for search input (milliseconds) */
    public static final int SEARCH_DEBOUNCE_DELAY_MS = 300;
    
    /** Time window for fall alerts to be considered recent (24 hours) */
    public static final long FALL_ALERT_RECENT_WINDOW_MS = 24 * 60 * 60 * 1000;
    
    /** Offline queue retry interval (milliseconds) */
    public static final long OFFLINE_QUEUE_RETRY_INTERVAL_MS = 30 * 1000;
    
    /** Maximum retry attempts for offline queue items */
    public static final int OFFLINE_QUEUE_MAX_RETRIES = 5;
    
    // ========== ROLE CONSTANTS ==========
    
    /** Role string for aggregator (data collector) users */
    public static final String ROLE_AGGREGATOR = "aggregator";
    
    /** Role string for supervisor (monitoring) users */
    public static final String ROLE_SUPERVISOR = "supervisor";
    
    /** Default sensor ID for unknown sensors */
    public static final String DEFAULT_SENSOR_ID = "unknown";
    
    // Prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
