package com.melisa.innovamotionapp.data.database;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for MonitoredPerson entity.
 * 
 * Tests cover:
 * - Constructor and field assignment
 * - Getters and setters
 * - Factory methods
 * - Various sensor ID formats
 */
public class MonitoredPersonTest {

    // ========== Constructor Tests ==========

    @Test
    public void testConstructorSetsAllFields() {
        long createdAt = 1000L;
        long updatedAt = 2000L;
        
        MonitoredPerson person = new MonitoredPerson("sensor001", "Ion Popescu", createdAt, updatedAt);
        
        assertEquals("sensor001", person.getSensorId());
        assertEquals("Ion Popescu", person.getDisplayName());
        assertEquals(createdAt, person.getCreatedAt());
        assertEquals(updatedAt, person.getUpdatedAt());
    }

    @Test
    public void testIdDefaultsToZeroBeforePersistence() {
        MonitoredPerson person = new MonitoredPerson("sensor001", "Test", 0, 0);
        assertEquals(0, person.getId());
    }

    // ========== Getter Tests ==========

    @Test
    public void testGetSensorId() {
        MonitoredPerson person = new MonitoredPerson("sensor-abc-123", "Test", 0, 0);
        assertEquals("sensor-abc-123", person.getSensorId());
    }

    @Test
    public void testGetDisplayName() {
        MonitoredPerson person = new MonitoredPerson("sensor001", "Maria Ionescu", 0, 0);
        assertEquals("Maria Ionescu", person.getDisplayName());
    }

    @Test
    public void testGetCreatedAt() {
        long createdAt = System.currentTimeMillis();
        MonitoredPerson person = new MonitoredPerson("sensor001", "Test", createdAt, 0);
        assertEquals(createdAt, person.getCreatedAt());
    }

    @Test
    public void testGetUpdatedAt() {
        long updatedAt = System.currentTimeMillis();
        MonitoredPerson person = new MonitoredPerson("sensor001", "Test", 0, updatedAt);
        assertEquals(updatedAt, person.getUpdatedAt());
    }

    // ========== Setter Tests ==========

    @Test
    public void testSetId() {
        MonitoredPerson person = new MonitoredPerson("sensor001", "Test", 0, 0);
        person.setId(42L);
        assertEquals(42L, person.getId());
    }

    @Test
    public void testSetSensorId() {
        MonitoredPerson person = new MonitoredPerson("sensor001", "Test", 0, 0);
        person.setSensorId("sensor002");
        assertEquals("sensor002", person.getSensorId());
    }

    @Test
    public void testSetDisplayNameUpdatesTimestamp() {
        MonitoredPerson person = new MonitoredPerson("sensor001", "Old Name", 1000, 1000);
        
        long before = System.currentTimeMillis();
        person.setDisplayName("New Name");
        long after = System.currentTimeMillis();
        
        assertEquals("New Name", person.getDisplayName());
        assertTrue("updatedAt should be >= before", person.getUpdatedAt() >= before);
        assertTrue("updatedAt should be <= after", person.getUpdatedAt() <= after);
    }

    @Test
    public void testSetCreatedAt() {
        MonitoredPerson person = new MonitoredPerson("sensor001", "Test", 0, 0);
        person.setCreatedAt(5000L);
        assertEquals(5000L, person.getCreatedAt());
    }

    @Test
    public void testSetUpdatedAt() {
        MonitoredPerson person = new MonitoredPerson("sensor001", "Test", 0, 0);
        person.setUpdatedAt(6000L);
        assertEquals(6000L, person.getUpdatedAt());
    }

    // ========== Factory Method Tests ==========

    @Test
    public void testCreateNewWithSensorIdOnly() {
        long before = System.currentTimeMillis();
        MonitoredPerson person = MonitoredPerson.createNew("sensor001");
        long after = System.currentTimeMillis();
        
        assertEquals("sensor001", person.getSensorId());
        assertEquals("sensor001", person.getDisplayName()); // Default to sensorId
        assertTrue(person.getCreatedAt() >= before);
        assertTrue(person.getCreatedAt() <= after);
        assertEquals(person.getCreatedAt(), person.getUpdatedAt());
    }

    @Test
    public void testCreateNewWithDisplayName() {
        long before = System.currentTimeMillis();
        MonitoredPerson person = MonitoredPerson.createNew("sensor001", "Ion Popescu");
        long after = System.currentTimeMillis();
        
        assertEquals("sensor001", person.getSensorId());
        assertEquals("Ion Popescu", person.getDisplayName());
        assertTrue(person.getCreatedAt() >= before);
        assertTrue(person.getCreatedAt() <= after);
        assertEquals(person.getCreatedAt(), person.getUpdatedAt());
    }

    // ========== Various SensorId Formats ==========

    @Test
    public void testSimpleSensorId() {
        MonitoredPerson person = MonitoredPerson.createNew("sensor001");
        assertEquals("sensor001", person.getSensorId());
    }

    @Test
    public void testUuidSensorId() {
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        MonitoredPerson person = MonitoredPerson.createNew(uuid, "Copil 1");
        assertEquals(uuid, person.getSensorId());
    }

    @Test
    public void testNumericSensorId() {
        MonitoredPerson person = MonitoredPerson.createNew("12345", "Test User");
        assertEquals("12345", person.getSensorId());
    }

    @Test
    public void testHashSensorId() {
        String hash = "a1b2c3d4e5f6g7h8";
        MonitoredPerson person = MonitoredPerson.createNew(hash);
        assertEquals(hash, person.getSensorId());
    }

    // ========== Display Name Formats ==========

    @Test
    public void testDisplayNameWithSpaces() {
        MonitoredPerson person = MonitoredPerson.createNew("sensor001", "Ion Alexandru Popescu");
        assertEquals("Ion Alexandru Popescu", person.getDisplayName());
    }

    @Test
    public void testDisplayNameWithSpecialCharacters() {
        MonitoredPerson person = MonitoredPerson.createNew("sensor001", "Ion-Maria Popescu");
        assertEquals("Ion-Maria Popescu", person.getDisplayName());
    }

    @Test
    public void testDisplayNameWithUnicode() {
        MonitoredPerson person = MonitoredPerson.createNew("sensor001", "Ștefan Ț Popescu");
        assertEquals("Ștefan Ț Popescu", person.getDisplayName());
    }

    // ========== Multiple Persons ==========

    @Test
    public void testMultiplePersonsWithDifferentSensors() {
        MonitoredPerson p1 = MonitoredPerson.createNew("sensor001", "Ion");
        MonitoredPerson p2 = MonitoredPerson.createNew("sensor002", "Maria");
        MonitoredPerson p3 = MonitoredPerson.createNew("sensor003", "Gheorghe");
        
        assertEquals("sensor001", p1.getSensorId());
        assertEquals("sensor002", p2.getSensorId());
        assertEquals("sensor003", p3.getSensorId());
        
        assertEquals("Ion", p1.getDisplayName());
        assertEquals("Maria", p2.getDisplayName());
        assertEquals("Gheorghe", p3.getDisplayName());
    }
}
