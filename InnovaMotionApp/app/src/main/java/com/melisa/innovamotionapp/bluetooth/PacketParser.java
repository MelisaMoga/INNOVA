package com.melisa.innovamotionapp.bluetooth;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.melisa.innovamotionapp.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for the multi-user Bluetooth protocol.
 * 
 * Buffers incoming lines until END_PACKET is received, then returns all parsed readings as a batch.
 * 
 * Protocol format:
 * <pre>
 * sensor001;0xAB3311\n
 * sensor002;0xEF0112\n
 * sensor003;0xBA3311\n
 * END_PACKET\n
 * </pre>
 * 
 * Usage:
 * <pre>
 * PacketParser parser = new PacketParser();
 * for (String line : incomingLines) {
 *     List<ParsedReading> readings = parser.feedLine(line);
 *     if (readings != null) {
 *         // Complete packet received, process readings
 *         processPacket(readings);
 *     }
 * }
 * </pre>
 * 
 * Thread Safety: This class is NOT thread-safe. Use external synchronization if accessed from multiple threads.
 */
public class PacketParser {
    
    private static final String TAG = "PacketParser";
    
    private final List<ParsedReading> buffer;
    private final int maxBufferSize;
    
    /**
     * Create a new PacketParser with default max buffer size.
     */
    public PacketParser() {
        this(Constants.MAX_READINGS_PER_PACKET);
    }
    
    /**
     * Create a new PacketParser with custom max buffer size.
     * 
     * @param maxBufferSize Maximum number of readings to buffer before auto-clearing
     *                      to prevent memory exhaustion from missing END_PACKET
     */
    public PacketParser(int maxBufferSize) {
        if (maxBufferSize <= 0) {
            throw new IllegalArgumentException("maxBufferSize must be positive");
        }
        this.maxBufferSize = maxBufferSize;
        this.buffer = new ArrayList<>();
    }
    
    /**
     * Feed a line to the parser.
     * 
     * @param line The raw line received from Bluetooth (without trailing newline)
     * @return List of parsed readings if packet is complete (END_PACKET received),
     *         null if packet is still in progress or line was invalid.
     *         Returns empty list for empty packets (just END_PACKET).
     */
    @Nullable
    public List<ParsedReading> feedLine(@Nullable String line) {
        if (line == null) {
            return null;
        }
        
        String trimmedLine = line.trim();
        
        // Check for packet terminator
        if (Constants.PACKET_TERMINATOR.equals(trimmedLine)) {
            List<ParsedReading> result = new ArrayList<>(buffer);
            buffer.clear();
            Log.d(TAG, "Packet complete with " + result.size() + " readings");
            return result;
        }
        
        // Skip empty lines
        if (trimmedLine.isEmpty()) {
            Log.v(TAG, "Skipping empty line");
            return null;
        }
        
        // Check buffer overflow protection
        if (buffer.size() >= maxBufferSize) {
            Log.w(TAG, "Buffer overflow protection: clearing " + buffer.size() + 
                    " readings (max: " + maxBufferSize + "). Possible missing END_PACKET.");
            buffer.clear();
        }
        
        // Parse the line
        ParsedReading reading = parseLine(trimmedLine);
        if (reading != null) {
            buffer.add(reading);
            Log.v(TAG, "Buffered reading: " + reading.getSensorId() + " -> " + reading.getHexCode());
        }
        
        return null;
    }
    
    /**
     * Parse a single protocol line into a ParsedReading.
     * 
     * Expected format: "sensorId;hexCode"
     * Example: "sensor001;0xAB3311"
     * 
     * @param line The trimmed line to parse
     * @return ParsedReading if valid, null if malformed
     */
    @Nullable
    private ParsedReading parseLine(@NonNull String line) {
        // Find the delimiter
        int delimiterIndex = line.indexOf(Constants.SENSOR_ID_DELIMITER);
        
        if (delimiterIndex == -1) {
            // No delimiter found - might be legacy single-line format or malformed
            Log.w(TAG, "Malformed line (no delimiter '" + Constants.SENSOR_ID_DELIMITER + 
                    "'): \"" + truncateForLog(line) + "\"");
            return null;
        }
        
        // Extract parts
        String sensorId = line.substring(0, delimiterIndex).trim();
        String hexCode = line.substring(delimiterIndex + 1).trim();
        
        // Validate sensorId
        if (sensorId.isEmpty()) {
            Log.w(TAG, "Malformed line (empty sensorId): \"" + truncateForLog(line) + "\"");
            return null;
        }
        
        // Validate hexCode
        if (hexCode.isEmpty()) {
            Log.w(TAG, "Malformed line (empty hexCode): \"" + truncateForLog(line) + "\"");
            return null;
        }
        
        // Check for multiple delimiters (take only first two parts)
        if (hexCode.contains(Constants.SENSOR_ID_DELIMITER)) {
            Log.w(TAG, "Line contains multiple delimiters, using first segment only: \"" + 
                    truncateForLog(line) + "\"");
            hexCode = hexCode.substring(0, hexCode.indexOf(Constants.SENSOR_ID_DELIMITER)).trim();
            if (hexCode.isEmpty()) {
                Log.w(TAG, "Malformed line (empty hexCode after delimiter handling): \"" + 
                        truncateForLog(line) + "\"");
                return null;
            }
        }
        
        try {
            return new ParsedReading(sensorId, hexCode);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to create ParsedReading: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Truncate a string for safe logging (prevents log pollution from very long lines).
     */
    private String truncateForLog(String s) {
        final int MAX_LOG_LENGTH = 50;
        if (s.length() <= MAX_LOG_LENGTH) {
            return s;
        }
        return s.substring(0, MAX_LOG_LENGTH) + "... (" + s.length() + " chars)";
    }
    
    /**
     * Reset the parser, clearing any buffered readings.
     * Call this when reconnecting or to discard partial packets.
     */
    public void reset() {
        int clearedCount = buffer.size();
        buffer.clear();
        if (clearedCount > 0) {
            Log.d(TAG, "Parser reset, cleared " + clearedCount + " buffered readings");
        }
    }
    
    /**
     * @return The current number of readings buffered (waiting for END_PACKET)
     */
    public int getBufferSize() {
        return buffer.size();
    }
    
    /**
     * @return The maximum buffer size before auto-clear
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }
    
    /**
     * @return True if the buffer is empty (no partial packet in progress)
     */
    public boolean isBufferEmpty() {
        return buffer.isEmpty();
    }
    
    /**
     * Get an unmodifiable view of the current buffer contents.
     * Useful for debugging/diagnostics.
     * 
     * @return Unmodifiable list of currently buffered readings
     */
    @NonNull
    public List<ParsedReading> getBufferContents() {
        return Collections.unmodifiableList(new ArrayList<>(buffer));
    }
}
