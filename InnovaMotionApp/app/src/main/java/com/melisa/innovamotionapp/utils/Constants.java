package com.melisa.innovamotionapp.utils;

public class Constants {
    public static final String POSTURE_DATA_SAVE_FILE_NAME = "%s_data.txt";
    // 1000ms = 1s
    public static final int COUNTDOWN_TIMER_IN_MILLISECONDS_FOR_MESSAGE_SAVE = 500;
    
    // ===== Multi-User Protocol Constants =====
    
    /** Terminator line that marks the end of a packet */
    public static final String PACKET_TERMINATOR = "END_PACKET";
    
    /** Delimiter between sensor ID and hex code in packet lines (e.g., "sensor001;0xAB3311") */
    public static final String SENSOR_ID_DELIMITER = ";";
    
    /** Maximum readings allowed per packet to prevent memory exhaustion from missing END_PACKET */
    public static final int MAX_READINGS_PER_PACKET = 1000;
}
