package com.melisa.innovamotionapp.bluetooth;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for ParsedReading data class.
 */
public class ParsedReadingTest {
    
    @Test
    public void testConstructorWithTimestamp() {
        long timestamp = 1234567890L;
        ParsedReading reading = new ParsedReading("sensor001", "0xAB3311", timestamp);
        
        assertEquals("sensor001", reading.getSensorId());
        assertEquals("0xAB3311", reading.getHexCode());
        assertEquals(timestamp, reading.getReceivedTimestamp());
    }
    
    @Test
    public void testConstructorWithoutTimestamp() {
        long before = System.currentTimeMillis();
        ParsedReading reading = new ParsedReading("sensor001", "0xAB3311");
        long after = System.currentTimeMillis();
        
        assertEquals("sensor001", reading.getSensorId());
        assertEquals("0xAB3311", reading.getHexCode());
        assertTrue(reading.getReceivedTimestamp() >= before);
        assertTrue(reading.getReceivedTimestamp() <= after);
    }
    
    @Test
    public void testTrimming() {
        ParsedReading reading = new ParsedReading("  sensor001  ", "  0xAB3311  ", 0);
        
        assertEquals("sensor001", reading.getSensorId());
        assertEquals("0xAB3311", reading.getHexCode());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullSensorId() {
        new ParsedReading(null, "0xAB3311", 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEmptySensorId() {
        new ParsedReading("", "0xAB3311", 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testWhitespaceSensorId() {
        new ParsedReading("   ", "0xAB3311", 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullHexCode() {
        new ParsedReading("sensor001", null, 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyHexCode() {
        new ParsedReading("sensor001", "", 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testWhitespaceHexCode() {
        new ParsedReading("sensor001", "   ", 0);
    }
    
    @Test
    public void testEquals() {
        ParsedReading reading1 = new ParsedReading("sensor001", "0xAB3311", 1000L);
        ParsedReading reading2 = new ParsedReading("sensor001", "0xAB3311", 1000L);
        ParsedReading reading3 = new ParsedReading("sensor002", "0xAB3311", 1000L);
        ParsedReading reading4 = new ParsedReading("sensor001", "0xEF0112", 1000L);
        ParsedReading reading5 = new ParsedReading("sensor001", "0xAB3311", 2000L);
        
        assertEquals(reading1, reading2);
        assertNotEquals(reading1, reading3); // Different sensorId
        assertNotEquals(reading1, reading4); // Different hexCode
        assertNotEquals(reading1, reading5); // Different timestamp
        assertNotEquals(reading1, null);
        assertNotEquals(reading1, "string");
    }
    
    @Test
    public void testHashCode() {
        ParsedReading reading1 = new ParsedReading("sensor001", "0xAB3311", 1000L);
        ParsedReading reading2 = new ParsedReading("sensor001", "0xAB3311", 1000L);
        
        assertEquals(reading1.hashCode(), reading2.hashCode());
    }
    
    @Test
    public void testToString() {
        ParsedReading reading = new ParsedReading("sensor001", "0xAB3311", 1000L);
        String str = reading.toString();
        
        assertTrue(str.contains("sensor001"));
        assertTrue(str.contains("0xAB3311"));
        assertTrue(str.contains("1000"));
    }
    
    @Test
    public void testUuidSensorId() {
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        ParsedReading reading = new ParsedReading(uuid, "0xAB3311", 0);
        
        assertEquals(uuid, reading.getSensorId());
    }
    
    @Test
    public void testVariousHexCodeFormats() {
        // Different valid hex code formats
        ParsedReading r1 = new ParsedReading("s1", "0xAB3311", 0);
        ParsedReading r2 = new ParsedReading("s2", "0XAB3311", 0);
        ParsedReading r3 = new ParsedReading("s3", "ab3311", 0);
        ParsedReading r4 = new ParsedReading("s4", "AB3311", 0);
        
        assertEquals("0xAB3311", r1.getHexCode());
        assertEquals("0XAB3311", r2.getHexCode());
        assertEquals("ab3311", r3.getHexCode());
        assertEquals("AB3311", r4.getHexCode());
    }
}
