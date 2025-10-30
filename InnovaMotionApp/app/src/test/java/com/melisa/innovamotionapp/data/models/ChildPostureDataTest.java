package com.melisa.innovamotionapp.data.models;

import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.types.StandingPosture;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ChildPostureData
 * Tests combined data model for supervisor dashboard
 */
public class ChildPostureDataTest {
    
    private static final String TEST_CHILD_ID = "sensor001";
    private static final String TEST_HEX = "0xAB3311";
    
    @Test
    public void testConstructor() {
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID, "John Doe", "Room 201", "Notes");
        Posture posture = new StandingPosture();
        long timestamp = System.currentTimeMillis();
        
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, profile, posture, timestamp, TEST_HEX
        );
        
        assertNotNull(data);
        assertEquals(TEST_CHILD_ID, data.getChildId());
        assertEquals(profile, data.getProfile());
        assertEquals(posture, data.getLatestPosture());
        assertEquals(timestamp, data.getLastUpdateTimestamp());
        assertEquals(TEST_HEX, data.getHexCode());
    }
    
    @Test
    public void testGetDisplayName_WithProfile() {
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID, "John Doe", null, null);
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, profile, null, 0L, null
        );
        
        assertEquals("John Doe", data.getDisplayName());
    }
    
    @Test
    public void testGetDisplayName_WithoutProfile() {
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, null, null, 0L, null
        );
        
        assertEquals(TEST_CHILD_ID, data.getDisplayName());
    }
    
    @Test
    public void testGetLocation_WithProfile() {
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID, "John", "Room 201", null);
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, profile, null, 0L, null
        );
        
        assertEquals("Room 201", data.getLocation());
    }
    
    @Test
    public void testGetLocation_WithoutProfile() {
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, null, null, 0L, null
        );
        
        assertNull(data.getLocation());
    }
    
    @Test
    public void testHasRecentData_WithinFiveMinutes() {
        long nowMinus2Min = System.currentTimeMillis() - (2 * 60 * 1000);
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, null, null, nowMinus2Min, null
        );
        
        assertTrue(data.hasRecentData());
    }
    
    @Test
    public void testHasRecentData_OlderThanFiveMinutes() {
        long nowMinus10Min = System.currentTimeMillis() - (10 * 60 * 1000);
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, null, null, nowMinus10Min, null
        );
        
        assertFalse(data.hasRecentData());
    }
    
    @Test
    public void testHasRecentData_NoTimestamp() {
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, null, null, 0L, null
        );
        
        assertFalse(data.hasRecentData());
    }
    
    @Test
    public void testGetTimeSinceLastUpdate() {
        long timestamp = System.currentTimeMillis() - 60000; // 1 minute ago
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, null, null, timestamp, null
        );
        
        long timeSince = data.getTimeSinceLastUpdate();
        assertTrue(timeSince >= 60000 && timeSince < 70000); // Approximately 1 minute
    }
    
    @Test
    public void testGetTimeSinceLastUpdate_NoTimestamp() {
        ChildPostureData data = new ChildPostureData(
            TEST_CHILD_ID, null, null, 0L, null
        );
        
        assertEquals(Long.MAX_VALUE, data.getTimeSinceLastUpdate());
    }
}

