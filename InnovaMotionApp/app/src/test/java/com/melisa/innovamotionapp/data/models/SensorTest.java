package com.melisa.innovamotionapp.data.models;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for Sensor POJO.
 * 
 * Tests cover:
 * - Constructor field assignment
 * - Serialization to Firestore document
 * - Deserialization from Firestore document
 * - Round-trip: serialize then deserialize
 * - Edge cases (null values)
 */
public class SensorTest {

    private static final String TEST_SENSOR_ID = "sensor001";
    private static final String TEST_DEVICE_ADDRESS = "AA:BB:CC:DD:EE:FF";
    private static final String TEST_OWNER_UID = "owner_uid_123";
    private static final String TEST_DISPLAY_NAME = "Living Room Sensor";

    // ========== Constructor Tests ==========

    @Test
    public void testConstructorSetsAllFields() {
        Sensor sensor = new Sensor(TEST_SENSOR_ID, TEST_DEVICE_ADDRESS, TEST_OWNER_UID, TEST_DISPLAY_NAME);
        
        assertEquals(TEST_SENSOR_ID, sensor.getSensorId());
        assertEquals(TEST_DEVICE_ADDRESS, sensor.getDeviceAddress());
        assertEquals(TEST_OWNER_UID, sensor.getOwnerUid());
        assertEquals(TEST_DISPLAY_NAME, sensor.getDisplayName());
    }

    @Test
    public void testDefaultConstructor() {
        Sensor sensor = new Sensor();
        
        assertNull(sensor.getSensorId());
        assertNull(sensor.getDeviceAddress());
        assertNull(sensor.getOwnerUid());
        assertNull(sensor.getDisplayName());
    }

    @Test
    public void testConstructorWithNullValues() {
        Sensor sensor = new Sensor(TEST_SENSOR_ID, null, TEST_OWNER_UID, null);
        
        assertEquals(TEST_SENSOR_ID, sensor.getSensorId());
        assertNull(sensor.getDeviceAddress());
        assertEquals(TEST_OWNER_UID, sensor.getOwnerUid());
        assertNull(sensor.getDisplayName());
    }

    // ========== Serialization Tests ==========

    @Test
    public void testToFirestoreDocumentIncludesAllFields() {
        Sensor sensor = new Sensor(TEST_SENSOR_ID, TEST_DEVICE_ADDRESS, TEST_OWNER_UID, TEST_DISPLAY_NAME);
        
        Map<String, Object> doc = sensor.toFirestoreDocument();
        
        assertEquals(TEST_DEVICE_ADDRESS, doc.get("deviceAddress"));
        assertEquals(TEST_OWNER_UID, doc.get("ownerUid"));
        assertEquals(TEST_DISPLAY_NAME, doc.get("displayName"));
    }

    @Test
    public void testToFirestoreDocumentFieldCount() {
        Sensor sensor = new Sensor(TEST_SENSOR_ID, TEST_DEVICE_ADDRESS, TEST_OWNER_UID, TEST_DISPLAY_NAME);
        
        Map<String, Object> doc = sensor.toFirestoreDocument();
        
        // Should have 3 fields: deviceAddress, ownerUid, displayName
        // sensorId is the document ID, not stored as a field
        assertEquals(3, doc.size());
    }

    @Test
    public void testToFirestoreDocumentDoesNotIncludeSensorId() {
        Sensor sensor = new Sensor(TEST_SENSOR_ID, TEST_DEVICE_ADDRESS, TEST_OWNER_UID, TEST_DISPLAY_NAME);
        
        Map<String, Object> doc = sensor.toFirestoreDocument();
        
        // sensorId should be the document ID, not in the document body
        assertFalse(doc.containsKey("sensorId"));
    }

    // ========== Deserialization Tests ==========

    @Test
    public void testFromDocumentReadsAllFields() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("deviceAddress", TEST_DEVICE_ADDRESS);
        doc.put("ownerUid", TEST_OWNER_UID);
        doc.put("displayName", TEST_DISPLAY_NAME);
        
        // Create mock DocumentSnapshot behavior
        Sensor sensor = createSensorFromMap(TEST_SENSOR_ID, doc);
        
        assertEquals(TEST_SENSOR_ID, sensor.getSensorId());
        assertEquals(TEST_DEVICE_ADDRESS, sensor.getDeviceAddress());
        assertEquals(TEST_OWNER_UID, sensor.getOwnerUid());
        assertEquals(TEST_DISPLAY_NAME, sensor.getDisplayName());
    }

    @Test
    public void testFromDocumentHandlesMissingFields() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("ownerUid", TEST_OWNER_UID);
        // Missing: deviceAddress, displayName
        
        Sensor sensor = createSensorFromMap(TEST_SENSOR_ID, doc);
        
        assertEquals(TEST_SENSOR_ID, sensor.getSensorId());
        assertNull(sensor.getDeviceAddress());
        assertEquals(TEST_OWNER_UID, sensor.getOwnerUid());
        assertNull(sensor.getDisplayName());
    }

    // ========== Round-Trip Tests ==========

    @Test
    public void testSerializeDeserializeRoundTrip() {
        Sensor original = new Sensor(TEST_SENSOR_ID, TEST_DEVICE_ADDRESS, TEST_OWNER_UID, TEST_DISPLAY_NAME);
        
        // Serialize
        Map<String, Object> doc = original.toFirestoreDocument();
        
        // Deserialize
        Sensor restored = createSensorFromMap(TEST_SENSOR_ID, doc);
        
        assertEquals(original.getSensorId(), restored.getSensorId());
        assertEquals(original.getDeviceAddress(), restored.getDeviceAddress());
        assertEquals(original.getOwnerUid(), restored.getOwnerUid());
        assertEquals(original.getDisplayName(), restored.getDisplayName());
    }

    @Test
    public void testRoundTripWithNullDeviceAddress() {
        Sensor original = new Sensor(TEST_SENSOR_ID, null, TEST_OWNER_UID, TEST_DISPLAY_NAME);
        
        Map<String, Object> doc = original.toFirestoreDocument();
        Sensor restored = createSensorFromMap(TEST_SENSOR_ID, doc);
        
        assertNull(restored.getDeviceAddress());
        assertEquals(TEST_DISPLAY_NAME, restored.getDisplayName());
    }

    // ========== Setter Tests ==========

    @Test
    public void testSetters() {
        Sensor sensor = new Sensor();
        
        sensor.setSensorId("new_id");
        sensor.setDeviceAddress("11:22:33:44:55:66");
        sensor.setOwnerUid("new_owner");
        sensor.setDisplayName("New Name");
        
        assertEquals("new_id", sensor.getSensorId());
        assertEquals("11:22:33:44:55:66", sensor.getDeviceAddress());
        assertEquals("new_owner", sensor.getOwnerUid());
        assertEquals("New Name", sensor.getDisplayName());
    }

    // ========== Edge Cases ==========

    @Test
    public void testEmptyDisplayName() {
        Sensor sensor = new Sensor(TEST_SENSOR_ID, TEST_DEVICE_ADDRESS, TEST_OWNER_UID, "");
        
        assertEquals("", sensor.getDisplayName());
    }

    @Test
    public void testSensorIdUsedAsDocumentId() {
        Sensor sensor = new Sensor(TEST_SENSOR_ID, TEST_DEVICE_ADDRESS, TEST_OWNER_UID, TEST_DISPLAY_NAME);
        
        // In Firestore, the document ID should be the sensorId
        // The toFirestoreDocument method excludes sensorId because it's the doc ID
        Map<String, Object> doc = sensor.toFirestoreDocument();
        assertFalse(doc.containsKey("sensorId"));
        
        // But getSensorId should still return it
        assertEquals(TEST_SENSOR_ID, sensor.getSensorId());
    }

    // ========== Helper Methods ==========

    /**
     * Simulates creating a Sensor from a Firestore document map.
     * This mimics what Sensor.fromDocument(DocumentSnapshot) does.
     */
    private Sensor createSensorFromMap(String documentId, Map<String, Object> doc) {
        Sensor sensor = new Sensor();
        sensor.setSensorId(documentId);
        sensor.setDeviceAddress((String) doc.get("deviceAddress"));
        sensor.setOwnerUid((String) doc.get("ownerUid"));
        sensor.setDisplayName((String) doc.get("displayName"));
        return sensor;
    }
}
