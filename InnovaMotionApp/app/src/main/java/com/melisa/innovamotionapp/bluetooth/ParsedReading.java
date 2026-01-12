package com.melisa.innovamotionapp.bluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Immutable data class representing a single parsed reading from the multi-user Bluetooth protocol.
 * 
 * Each reading contains:
 * - sensorId: The unique identifier for the monitored person/sensor (e.g., "sensor001", UUID)
 * - hexCode: The posture hex code (e.g., "0xAB3311")
 * - receivedTimestamp: When this reading was received by the app
 * 
 * Example protocol line: "sensor001;0xAB3311\n"
 */
public final class ParsedReading {
    
    @NonNull
    private final String sensorId;
    
    @NonNull
    private final String hexCode;
    
    private final long receivedTimestamp;
    
    /**
     * Create a new ParsedReading.
     * 
     * @param sensorId The sensor/person identifier from hardware (must not be null or empty)
     * @param hexCode The posture hex code (must not be null or empty)
     * @param receivedTimestamp When this reading was received (epoch milliseconds)
     * @throws IllegalArgumentException if sensorId or hexCode is null or empty
     */
    public ParsedReading(@NonNull String sensorId, @NonNull String hexCode, long receivedTimestamp) {
        if (sensorId == null || sensorId.trim().isEmpty()) {
            throw new IllegalArgumentException("sensorId cannot be null or empty");
        }
        if (hexCode == null || hexCode.trim().isEmpty()) {
            throw new IllegalArgumentException("hexCode cannot be null or empty");
        }
        
        this.sensorId = sensorId.trim();
        this.hexCode = hexCode.trim();
        this.receivedTimestamp = receivedTimestamp;
    }
    
    /**
     * Create a new ParsedReading with current timestamp.
     * 
     * @param sensorId The sensor/person identifier from hardware
     * @param hexCode The posture hex code
     */
    public ParsedReading(@NonNull String sensorId, @NonNull String hexCode) {
        this(sensorId, hexCode, System.currentTimeMillis());
    }
    
    /**
     * @return The sensor/person identifier (never null or empty)
     */
    @NonNull
    public String getSensorId() {
        return sensorId;
    }
    
    /**
     * @return The posture hex code (never null or empty)
     */
    @NonNull
    public String getHexCode() {
        return hexCode;
    }
    
    /**
     * @return The timestamp when this reading was received (epoch milliseconds)
     */
    public long getReceivedTimestamp() {
        return receivedTimestamp;
    }
    
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedReading that = (ParsedReading) o;
        return receivedTimestamp == that.receivedTimestamp &&
                sensorId.equals(that.sensorId) &&
                hexCode.equals(that.hexCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sensorId, hexCode, receivedTimestamp);
    }
    
    @NonNull
    @Override
    public String toString() {
        return "ParsedReading{" +
                "sensorId='" + sensorId + '\'' +
                ", hexCode='" + hexCode + '\'' +
                ", receivedTimestamp=" + receivedTimestamp +
                '}';
    }
}
