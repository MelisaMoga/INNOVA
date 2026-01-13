package com.melisa.innovamotionapp.bluetooth;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Comprehensive unit tests for PacketParser.
 * 
 * Tests cover:
 * - Normal packet parsing
 * - Empty packets
 * - Malformed lines (no semicolon, empty parts)
 * - Large packets
 * - Buffer overflow protection
 * - Multiple consecutive packets
 * - Edge cases (whitespace, multiple delimiters, etc.)
 */
public class PacketParserTest {
    
    private PacketParser parser;
    
    @Before
    public void setUp() {
        parser = new PacketParser();
    }
    
    // ========== Normal Packet Parsing ==========
    
    @Test
    public void testSingleReadingPacket() {
        // Feed a single reading then END_PACKET
        assertNull(parser.feedLine("sensor001;0xAB3311"));
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sensor001", result.get(0).getSensorId());
        assertEquals("0xAB3311", result.get(0).getHexCode());
    }
    
    @Test
    public void testMultipleReadingsPacket() {
        // Feed multiple readings then END_PACKET
        assertNull(parser.feedLine("sensor001;0xAB3311"));
        assertNull(parser.feedLine("sensor002;0xEF0112"));
        assertNull(parser.feedLine("sensor003;0xBA3311"));
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("sensor001", result.get(0).getSensorId());
        assertEquals("sensor002", result.get(1).getSensorId());
        assertEquals("sensor003", result.get(2).getSensorId());
        assertEquals("0xAB3311", result.get(0).getHexCode());
        assertEquals("0xEF0112", result.get(1).getHexCode());
        assertEquals("0xBA3311", result.get(2).getHexCode());
    }
    
    @Test
    public void testDuplicateSensorInPacket() {
        // Same sensor can appear multiple times per spec
        assertNull(parser.feedLine("sensor001;0xAB3311"));
        assertNull(parser.feedLine("sensor002;0xEF0112"));
        assertNull(parser.feedLine("sensor001;0xBA3311")); // sensor001 again
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("sensor001", result.get(0).getSensorId());
        assertEquals("sensor001", result.get(2).getSensorId());
        assertEquals("0xAB3311", result.get(0).getHexCode());
        assertEquals("0xBA3311", result.get(2).getHexCode());
    }
    
    @Test
    public void testUuidSensorId() {
        // UUID format sensor IDs should work
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        assertNull(parser.feedLine(uuid + ";0xAB3311"));
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(uuid, result.get(0).getSensorId());
    }
    
    // ========== Empty Packets ==========
    
    @Test
    public void testEmptyPacket() {
        // Just END_PACKET with no readings
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testEmptyPacketAfterEmptyLines() {
        // Empty lines followed by END_PACKET
        assertNull(parser.feedLine(""));
        assertNull(parser.feedLine("   "));
        assertNull(parser.feedLine("\t"));
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    // ========== Malformed Lines ==========
    
    @Test
    public void testMalformedLineNoDelimiter() {
        // Line without semicolon should be skipped
        assertNull(parser.feedLine("sensor001_0xAB3311")); // No semicolon
        assertNull(parser.feedLine("sensor002;0xEF0112")); // Valid
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size()); // Only the valid one
        assertEquals("sensor002", result.get(0).getSensorId());
    }
    
    @Test
    public void testMalformedLineEmptySensorId() {
        // Empty sensor ID should be skipped
        assertNull(parser.feedLine(";0xAB3311")); // Empty sensorId
        assertNull(parser.feedLine("sensor001;0xEF0112")); // Valid
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sensor001", result.get(0).getSensorId());
    }
    
    @Test
    public void testMalformedLineEmptyHexCode() {
        // Empty hex code should be skipped
        assertNull(parser.feedLine("sensor001;")); // Empty hexCode
        assertNull(parser.feedLine("sensor002;0xEF0112")); // Valid
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sensor002", result.get(0).getSensorId());
    }
    
    @Test
    public void testMalformedLineBothEmpty() {
        // Both parts empty should be skipped
        assertNull(parser.feedLine(";")); // Both empty
        assertNull(parser.feedLine("sensor001;0xEF0112")); // Valid
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    public void testLineWithMultipleDelimiters() {
        // Multiple delimiters - should use first two parts
        assertNull(parser.feedLine("sensor001;0xAB3311;extra_stuff"));
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sensor001", result.get(0).getSensorId());
        assertEquals("0xAB3311", result.get(0).getHexCode());
    }
    
    @Test
    public void testMixedValidInvalidLines() {
        // Mix of valid and invalid lines
        assertNull(parser.feedLine("sensor001;0xAB3311")); // Valid
        assertNull(parser.feedLine("invalid_no_semicolon")); // Invalid
        assertNull(parser.feedLine("sensor002;0xEF0112")); // Valid
        assertNull(parser.feedLine(";empty_sensor")); // Invalid
        assertNull(parser.feedLine("sensor003;0xBA3311")); // Valid
        assertNull(parser.feedLine("no_hex;")); // Invalid
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(3, result.size());
    }
    
    // ========== Whitespace Handling ==========
    
    @Test
    public void testWhitespaceAroundDelimiter() {
        // Whitespace around parts should be trimmed
        assertNull(parser.feedLine("  sensor001  ;  0xAB3311  "));
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sensor001", result.get(0).getSensorId());
        assertEquals("0xAB3311", result.get(0).getHexCode());
    }
    
    @Test
    public void testWhitespaceAroundEndPacket() {
        // Whitespace around END_PACKET should work
        assertNull(parser.feedLine("sensor001;0xAB3311"));
        
        List<ParsedReading> result = parser.feedLine("  END_PACKET  ");
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    // ========== Large Packets ==========
    
    @Test
    public void testLargePacket() {
        // Test with 100+ readings
        int numReadings = 150;
        for (int i = 0; i < numReadings; i++) {
            assertNull(parser.feedLine("sensor" + String.format("%03d", i) + ";0xAB3311"));
        }
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(numReadings, result.size());
    }
    
    // ========== Buffer Overflow Protection ==========
    
    @Test
    public void testBufferOverflowProtection() {
        // Create parser with small max buffer
        PacketParser smallParser = new PacketParser(5);
        
        // Feed more than max without END_PACKET
        for (int i = 0; i < 6; i++) {
            assertNull(smallParser.feedLine("sensor" + i + ";0xAB3311"));
        }
        
        // At this point, buffer should have been cleared and only last reading should be there
        // (First 5 readings, then on 6th, buffer cleared before adding)
        
        // The last reading (sensor5) should be in buffer
        List<ParsedReading> result = smallParser.feedLine("END_PACKET");
        
        assertNotNull(result);
        // After clearing, only the reading that triggered overflow clearing is added
        assertEquals(1, result.size());
        assertEquals("sensor5", result.get(0).getSensorId());
    }
    
    @Test
    public void testCustomMaxBufferSize() {
        PacketParser customParser = new PacketParser(10);
        assertEquals(10, customParser.getMaxBufferSize());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxBufferSize() {
        new PacketParser(0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMaxBufferSize() {
        new PacketParser(-5);
    }
    
    // ========== Multiple Consecutive Packets ==========
    
    @Test
    public void testMultipleConsecutivePackets() {
        // First packet
        assertNull(parser.feedLine("sensor001;0xAB3311"));
        assertNull(parser.feedLine("sensor002;0xEF0112"));
        List<ParsedReading> result1 = parser.feedLine("END_PACKET");
        
        assertNotNull(result1);
        assertEquals(2, result1.size());
        
        // Buffer should be empty now
        assertTrue(parser.isBufferEmpty());
        
        // Second packet
        assertNull(parser.feedLine("sensor003;0xBA3311"));
        List<ParsedReading> result2 = parser.feedLine("END_PACKET");
        
        assertNotNull(result2);
        assertEquals(1, result2.size());
        assertEquals("sensor003", result2.get(0).getSensorId());
        
        // Third packet (empty)
        List<ParsedReading> result3 = parser.feedLine("END_PACKET");
        
        assertNotNull(result3);
        assertTrue(result3.isEmpty());
    }
    
    // ========== Reset Functionality ==========
    
    @Test
    public void testReset() {
        assertNull(parser.feedLine("sensor001;0xAB3311"));
        assertNull(parser.feedLine("sensor002;0xEF0112"));
        assertEquals(2, parser.getBufferSize());
        
        parser.reset();
        
        assertEquals(0, parser.getBufferSize());
        assertTrue(parser.isBufferEmpty());
        
        // New packet after reset
        assertNull(parser.feedLine("sensor003;0xBA3311"));
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sensor003", result.get(0).getSensorId());
    }
    
    // ========== Null Handling ==========
    
    @Test
    public void testNullLine() {
        assertNull(parser.feedLine(null));
        assertEquals(0, parser.getBufferSize());
    }
    
    // ========== Buffer State Methods ==========
    
    @Test
    public void testGetBufferSize() {
        assertEquals(0, parser.getBufferSize());
        
        parser.feedLine("sensor001;0xAB3311");
        assertEquals(1, parser.getBufferSize());
        
        parser.feedLine("sensor002;0xEF0112");
        assertEquals(2, parser.getBufferSize());
        
        parser.feedLine("END_PACKET");
        assertEquals(0, parser.getBufferSize());
    }
    
    @Test
    public void testIsBufferEmpty() {
        assertTrue(parser.isBufferEmpty());
        
        parser.feedLine("sensor001;0xAB3311");
        assertFalse(parser.isBufferEmpty());
        
        parser.feedLine("END_PACKET");
        assertTrue(parser.isBufferEmpty());
    }
    
    @Test
    public void testGetBufferContents() {
        parser.feedLine("sensor001;0xAB3311");
        parser.feedLine("sensor002;0xEF0112");
        
        List<ParsedReading> contents = parser.getBufferContents();
        
        assertEquals(2, contents.size());
        assertEquals("sensor001", contents.get(0).getSensorId());
        assertEquals("sensor002", contents.get(1).getSensorId());
        
        // Verify it's a copy (modifications don't affect internal buffer)
        try {
            contents.clear();
        } catch (UnsupportedOperationException e) {
            // Expected - unmodifiable list
        }
        assertEquals(2, parser.getBufferSize());
    }
    
    // ========== Timestamp Testing ==========
    
    @Test
    public void testTimestampIsSet() {
        long before = System.currentTimeMillis();
        parser.feedLine("sensor001;0xAB3311");
        long after = System.currentTimeMillis();
        
        List<ParsedReading> result = parser.feedLine("END_PACKET");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        
        long timestamp = result.get(0).getReceivedTimestamp();
        assertTrue("Timestamp should be >= before", timestamp >= before);
        assertTrue("Timestamp should be <= after", timestamp <= after);
    }
}
