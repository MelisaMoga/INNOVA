package com.melisa.innovamotionapp.data.models;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ChildProfile model
 * Tests child profile creation, display name logic, and timestamp updates
 */
public class ChildProfileTest {
    
    private static final String TEST_CHILD_ID = "sensor001";
    private static final String TEST_NAME = "John Doe";
    private static final String TEST_LOCATION = "Room 201";
    private static final String TEST_NOTES = "Test notes";
    
    @Test
    public void testDefaultConstructor() {
        // Test: default constructor initializes correctly
        ChildProfile profile = new ChildProfile();
        
        assertNotNull(profile);
        assertEquals("", profile.getChildId());
        assertNull(profile.getName());
        assertNull(profile.getLocation());
        assertNull(profile.getNotes());
        assertTrue(profile.getAddedAt() > 0);
        assertTrue(profile.getLastSeen() > 0);
    }
    
    @Test
    public void testConstructorWithChildId() {
        // Test: constructor with childId only
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID);
        
        assertNotNull(profile);
        assertEquals(TEST_CHILD_ID, profile.getChildId());
        assertNull(profile.getName());
        assertNull(profile.getLocation());
        assertNull(profile.getNotes());
        assertTrue(profile.getAddedAt() > 0);
        assertTrue(profile.getLastSeen() > 0);
    }
    
    @Test
    public void testConstructorWithAllFields() {
        // Test: constructor with all fields
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID, TEST_NAME, TEST_LOCATION, TEST_NOTES);
        
        assertNotNull(profile);
        assertEquals(TEST_CHILD_ID, profile.getChildId());
        assertEquals(TEST_NAME, profile.getName());
        assertEquals(TEST_LOCATION, profile.getLocation());
        assertEquals(TEST_NOTES, profile.getNotes());
        assertTrue(profile.getAddedAt() > 0);
        assertTrue(profile.getLastSeen() > 0);
    }
    
    @Test
    public void testGetDisplayName_WithName() {
        // Test: getDisplayName() returns name when set
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID, TEST_NAME, TEST_LOCATION, TEST_NOTES);
        
        assertEquals(TEST_NAME, profile.getDisplayName());
    }
    
    @Test
    public void testGetDisplayName_WithoutName() {
        // Test: getDisplayName() returns childId when name is null
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID);
        
        assertEquals(TEST_CHILD_ID, profile.getDisplayName());
    }
    
    @Test
    public void testGetDisplayName_EmptyName() {
        // Test: getDisplayName() returns childId when name is empty
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID);
        profile.setName("");
        
        assertEquals(TEST_CHILD_ID, profile.getDisplayName());
    }
    
    @Test
    public void testGetDisplayName_WhitespaceName() {
        // Test: getDisplayName() returns childId when name is whitespace only
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID);
        profile.setName("   ");
        
        assertEquals(TEST_CHILD_ID, profile.getDisplayName());
    }
    
    @Test
    public void testUpdateLastSeen() {
        // Test: updateLastSeen() updates timestamp
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID);
        long initialLastSeen = profile.getLastSeen();
        
        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        profile.updateLastSeen();
        long updatedLastSeen = profile.getLastSeen();
        
        assertTrue("LastSeen should be updated", updatedLastSeen > initialLastSeen);
    }
    
    @Test
    public void testGettersAndSetters() {
        // Test: all getters and setters work correctly
        ChildProfile profile = new ChildProfile();
        
        profile.setChildId(TEST_CHILD_ID);
        assertEquals(TEST_CHILD_ID, profile.getChildId());
        
        profile.setName(TEST_NAME);
        assertEquals(TEST_NAME, profile.getName());
        
        profile.setLocation(TEST_LOCATION);
        assertEquals(TEST_LOCATION, profile.getLocation());
        
        profile.setNotes(TEST_NOTES);
        assertEquals(TEST_NOTES, profile.getNotes());
        
        long timestamp = System.currentTimeMillis();
        profile.setAddedAt(timestamp);
        assertEquals(timestamp, profile.getAddedAt());
        
        profile.setLastSeen(timestamp);
        assertEquals(timestamp, profile.getLastSeen());
    }
    
    @Test
    public void testToString() {
        // Test: toString() includes all fields
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID, TEST_NAME, TEST_LOCATION, TEST_NOTES);
        String toString = profile.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains(TEST_CHILD_ID));
        assertTrue(toString.contains(TEST_NAME));
        assertTrue(toString.contains(TEST_LOCATION));
        assertTrue(toString.contains(TEST_NOTES));
    }
    
    @Test
    public void testTimestampsInitialized() {
        // Test: timestamps are initialized to reasonable values
        long beforeCreation = System.currentTimeMillis();
        ChildProfile profile = new ChildProfile(TEST_CHILD_ID);
        long afterCreation = System.currentTimeMillis();
        
        assertTrue("AddedAt should be >= creation time", profile.getAddedAt() >= beforeCreation);
        assertTrue("AddedAt should be <= after creation", profile.getAddedAt() <= afterCreation);
        assertTrue("LastSeen should be >= creation time", profile.getLastSeen() >= beforeCreation);
        assertTrue("LastSeen should be <= after creation", profile.getLastSeen() <= afterCreation);
    }
}

