package com.melisa.innovamotionapp.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for MessageParser
 * Tests parsing of multi-user messages with childId;hex format
 */
public class MessageParserTest {
    
    @Test
    public void testParseNewFormat_ValidMessage() {
        // Test: childId;hex format
        MessageParser.ParsedMessage result = MessageParser.parse("sensor001;0xAB3311");
        
        assertNotNull(result);
        assertEquals("sensor001", result.childId);
        assertEquals("0xAB3311", result.hex);
        assertEquals("sensor001;0xAB3311", result.rawMessage);
        assertTrue(result.hasChildId());
        assertTrue(result.isValid());
    }
    
    @Test
    public void testParseLegacyFormat_HexOnly() {
        // Test: legacy format without childId
        MessageParser.ParsedMessage result = MessageParser.parse("0xAB3311");
        
        assertNotNull(result);
        assertNull(result.childId);
        assertEquals("0xAB3311", result.hex);
        assertEquals("0xAB3311", result.rawMessage);
        assertFalse(result.hasChildId());
        assertTrue(result.isValid());
    }
    
    @Test
    public void testParseWithEmptyChildId() {
        // Test: delimiter present but empty childId
        MessageParser.ParsedMessage result = MessageParser.parse(";0xAB3311");
        
        assertNotNull(result);
        assertNull(result.childId);
        assertEquals("0xAB3311", result.hex);
        assertFalse(result.hasChildId());
        assertTrue(result.isValid());
    }
    
    @Test
    public void testParseWithEmptyHex() {
        // Test: childId present but empty hex
        MessageParser.ParsedMessage result = MessageParser.parse("sensor001;");
        
        assertNotNull(result);
        assertEquals("sensor001", result.childId);
        assertEquals("", result.hex);
        assertTrue(result.hasChildId());
        assertFalse(result.isValid()); // Invalid because hex is empty
    }
    
    @Test
    public void testParseNullMessage() {
        // Test: null input
        MessageParser.ParsedMessage result = MessageParser.parse(null);
        
        assertNotNull(result);
        assertNull(result.childId);
        assertEquals("", result.hex);
        assertEquals("", result.rawMessage);
        assertFalse(result.hasChildId());
        assertFalse(result.isValid());
    }
    
    @Test
    public void testParseEmptyMessage() {
        // Test: empty string input
        MessageParser.ParsedMessage result = MessageParser.parse("");
        
        assertNotNull(result);
        assertNull(result.childId);
        assertEquals("", result.hex);
        assertFalse(result.hasChildId());
        assertFalse(result.isValid());
    }
    
    @Test
    public void testParseWhitespaceMessage() {
        // Test: whitespace only
        MessageParser.ParsedMessage result = MessageParser.parse("   ");
        
        assertNotNull(result);
        assertNull(result.childId);
        assertEquals("", result.hex);
        assertFalse(result.isValid());
    }
    
    @Test
    public void testParseWithWhitespace() {
        // Test: message with leading/trailing whitespace
        MessageParser.ParsedMessage result = MessageParser.parse("  sensor001;0xAB3311  ");
        
        assertNotNull(result);
        assertEquals("sensor001", result.childId);
        assertEquals("0xAB3311", result.hex);
        assertTrue(result.hasChildId());
        assertTrue(result.isValid());
    }
    
    @Test
    public void testParseWithMultipleDelimiters() {
        // Test: multiple semicolons (should split on first only)
        MessageParser.ParsedMessage result = MessageParser.parse("sensor001;0xAB;extra");
        
        assertNotNull(result);
        assertEquals("sensor001", result.childId);
        assertEquals("0xAB;extra", result.hex); // Takes everything after first delimiter
        assertTrue(result.hasChildId());
        assertTrue(result.isValid());
    }
    
    @Test
    public void testIsPacketEnd_ValidDelimiter() {
        // Test: END_PACKET delimiter detection
        assertTrue(MessageParser.isPacketEnd("END_PACKET"));
        assertTrue(MessageParser.isPacketEnd("  END_PACKET  ")); // With whitespace
        assertTrue(MessageParser.isPacketEnd("end_packet")); // Case insensitive
    }
    
    @Test
    public void testIsPacketEnd_InvalidDelimiter() {
        // Test: non-delimiter strings
        assertFalse(MessageParser.isPacketEnd("sensor001;0xAB3311"));
        assertFalse(MessageParser.isPacketEnd("END_PACKE"));
        assertFalse(MessageParser.isPacketEnd(""));
        assertFalse(MessageParser.isPacketEnd(null));
    }
    
    @Test
    public void testIsValidFormat_ValidMessages() {
        // Test: format validation for valid messages
        assertTrue(MessageParser.isValidFormat("sensor001;0xAB3311"));
        assertTrue(MessageParser.isValidFormat("0xAB3311"));
    }
    
    @Test
    public void testIsValidFormat_InvalidMessages() {
        // Test: format validation for invalid messages
        assertFalse(MessageParser.isValidFormat(null));
        assertFalse(MessageParser.isValidFormat(""));
        assertFalse(MessageParser.isValidFormat("   "));
        assertFalse(MessageParser.isValidFormat("sensor001;")); // Empty hex
    }
    
    @Test
    public void testToString() {
        // Test: toString method
        MessageParser.ParsedMessage result = MessageParser.parse("sensor001;0xAB3311");
        String toString = result.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("sensor001"));
        assertTrue(toString.contains("0xAB3311"));
    }
    
    @Test
    public void testRealWorldMessages() {
        // Test: realistic message formats
        MessageParser.ParsedMessage msg1 = MessageParser.parse("5d6d75ee-b6c8-42d4-a233-b13d137fea38;0xEF0112");
        assertEquals("5d6d75ee-b6c8-42d4-a233-b13d137fea38", msg1.childId);
        assertEquals("0xEF0112", msg1.hex);
        
        MessageParser.ParsedMessage msg2 = MessageParser.parse("child_123;0xBA3311");
        assertEquals("child_123", msg2.childId);
        assertEquals("0xBA3311", msg2.hex);
    }
}

