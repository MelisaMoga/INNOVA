package com.melisa.innovamotionapp.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Parses Bluetooth messages in the format: <childId;hex>
 * Examples:
 *   - "sensor001;0xAB3311" -> ParsedMessage(childId="sensor001", hex="0xAB3311")
 *   - "0xAB3311" (legacy) -> ParsedMessage(childId=null, hex="0xAB3311")
 * 
 * Thread-safe: All methods are stateless and can be called from any thread.
 */
public class MessageParser {
    private static final String TAG = "MessageParser";
    
    /**
     * Parsed message container with childId and hex code.
     */
    public static class ParsedMessage {
        @Nullable
        public final String childId;        // The child/person ID extracted from message
        @NonNull
        public final String hex;            // The posture hex code
        @NonNull
        public final String rawMessage;     // Original message for debugging
        
        public ParsedMessage(@Nullable String childId, @NonNull String hex, @NonNull String rawMessage) {
            this.childId = childId;
            this.hex = hex;
            this.rawMessage = rawMessage;
        }
        
        /**
         * @return true if this message contains a child ID (new format)
         */
        public boolean hasChildId() {
            return childId != null && !childId.isEmpty();
        }
        
        /**
         * @return true if this is a valid message with a hex code
         */
        public boolean isValid() {
            return hex != null && !hex.isEmpty();
        }
        
        @Override
        public String toString() {
            return "ParsedMessage{childId='" + childId + "', hex='" + hex + "'}";
        }
    }
    
    /**
     * Parse a Bluetooth message.
     * Supports both formats:
     *   - New: "childId;hex" (e.g., "sensor001;0xAB3311")
     *   - Legacy: "hex" (e.g., "0xAB3311")
     * 
     * @param rawMessage The raw message received via Bluetooth
     * @return ParsedMessage with extracted childId and hex code
     */
    @NonNull
    public static ParsedMessage parse(@Nullable String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            Log.w(TAG, "Received null or empty message");
            return new ParsedMessage(null, "", "");
        }
        
        String trimmed = rawMessage.trim();
        
        // Check if message contains delimiter (new format)
        if (trimmed.contains(Constants.MESSAGE_DELIMITER)) {
            String[] parts = trimmed.split(Constants.MESSAGE_DELIMITER, 2); // Limit to 2 parts
            
            if (parts.length == 2) {
                String childId = parts[0].trim();
                String hex = parts[1].trim();
                
                // Validate that we got both parts
                if (childId.isEmpty()) {
                    Log.w(TAG, "Message has delimiter but empty childId: " + trimmed);
                    return new ParsedMessage(null, hex, trimmed);
                }
                
                if (hex.isEmpty()) {
                    Log.w(TAG, "Message has delimiter but empty hex: " + trimmed);
                    return new ParsedMessage(childId, "", trimmed);
                }
                
                Log.d(TAG, "Parsed new format - ChildId: " + childId + ", Hex: " + hex);
                return new ParsedMessage(childId, hex, trimmed);
            } else {
                Log.w(TAG, "Message has delimiter but incorrect split result: " + trimmed);
            }
        }
        
        // Legacy format or malformed message - treat entire message as hex
        Log.d(TAG, "Parsed legacy format - Hex: " + trimmed);
        return new ParsedMessage(null, trimmed, trimmed);
    }
    
    /**
     * Validate if a message format is correct.
     * @param rawMessage The message to validate
     * @return true if message can be parsed successfully with a valid hex code
     */
    public static boolean isValidFormat(@Nullable String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return false;
        }
        
        ParsedMessage parsed = parse(rawMessage);
        return parsed.isValid();
    }
    
    /**
     * Check if a message is the packet end delimiter.
     * @param line The line to check
     * @return true if this line is the END_PACKET delimiter
     */
    public static boolean isPacketEnd(@Nullable String line) {
        if (line == null) {
            return false;
        }
        return Constants.PACKET_END_DELIMITER.equals(line.trim());
    }
}

