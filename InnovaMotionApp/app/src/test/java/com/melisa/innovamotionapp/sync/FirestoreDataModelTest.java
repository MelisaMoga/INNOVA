package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for FirestoreDataModel.
 * 
 * Tests cover:
 * - Constructor field assignment
 * - Document ID generation (includes sensorId for uniqueness)
 * - Serialization to Firestore document format
 * - Deserialization from Firestore document
 * - Round-trip: serialize then deserialize preserves all fields
 * - Different sensorId formats
 */
public class FirestoreDataModelTest {

    // ========== Constructor Tests ==========

    @Test
    public void testFullConstructorSetsAllFields() {
        String deviceAddress = "AA:BB:CC:DD:EE:FF";
        long timestamp = 1234567890L;
        String receivedMsg = "0xAB3311";
        String userId = "user123";
        String sensorId = "sensor001";

        FirestoreDataModel model = new FirestoreDataModel(
                deviceAddress, timestamp, receivedMsg, userId, sensorId);

        assertEquals(deviceAddress, model.getDeviceAddress());
        assertEquals(timestamp, model.getTimestamp());
        assertEquals(receivedMsg, model.getReceivedMsg());
        assertEquals(userId, model.getUserId());
        assertEquals(sensorId, model.getSensorId());
        assertTrue(model.getSyncTimestamp() > 0);
        assertNotNull(model.getDocumentId());
    }

    @Test
    public void testDefaultConstructorForFirestore() {
        FirestoreDataModel model = new FirestoreDataModel();

        // All fields should be null/default
        assertNull(model.getDeviceAddress());
        assertEquals(0L, model.getTimestamp());
        assertNull(model.getReceivedMsg());
        assertNull(model.getUserId());
        assertNull(model.getSensorId());
    }

    // ========== Document ID Generation Tests ==========

    @Test
    public void testDocumentIdIncludesSensorId() {
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user1", "sensor001");

        String docId = model.getDocumentId();

        assertTrue("Document ID should contain sensorId", docId.contains("sensor001"));
        assertTrue("Document ID should contain userId", docId.contains("user1"));
        assertTrue("Document ID should contain timestamp", docId.contains("1000"));
    }

    @Test
    public void testDocumentIdUniquenessWithDifferentSensors() {
        FirestoreDataModel m1 = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user1", "sensor001");
        FirestoreDataModel m2 = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user1", "sensor002");

        // Same timestamp, same user, same device, but different sensors = different doc IDs
        assertNotEquals(m1.getDocumentId(), m2.getDocumentId());
    }

    @Test
    public void testDocumentIdUniquenessWithDifferentTimestamps() {
        FirestoreDataModel m1 = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user1", "sensor001");
        FirestoreDataModel m2 = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 2000L, "0xAB3311", "user1", "sensor001");

        // Same sensor, same user, same device, but different timestamps = different doc IDs
        assertNotEquals(m1.getDocumentId(), m2.getDocumentId());
    }

    @Test
    public void testDocumentIdRemovesColonsFromDeviceAddress() {
        String docId = FirestoreDataModel.generateDocumentId(
                "user1", "AA:BB:CC:DD:EE:FF", "sensor001", 1000L);

        assertFalse("Document ID should not contain colons", docId.contains(":"));
        assertTrue("Document ID should contain device address without colons", 
                docId.contains("AABBCCDDEEFF"));
    }

    @Test
    public void testGenerateDocumentIdStaticMethod() {
        String docId = FirestoreDataModel.generateDocumentId(
                "aggregator123", "11:22:33:44:55:66", "sensorABC", 9999L);

        assertEquals("aggregator123_112233445566_sensorABC_9999", docId);
    }

    @Test
    public void testGenerateDocumentIdWithNullValues() {
        // Null device address
        String docId1 = FirestoreDataModel.generateDocumentId(
                "user1", null, "sensor001", 1000L);
        assertTrue(docId1.contains("unknown"));

        // Null sensor ID
        String docId2 = FirestoreDataModel.generateDocumentId(
                "user1", "AA:BB:CC:DD:EE:FF", null, 1000L);
        assertTrue(docId2.contains("unknown"));
    }

    // ========== Serialization Tests ==========

    @Test
    public void testToFirestoreDocumentIncludesAllFields() {
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user1", "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        assertEquals("AA:BB:CC:DD:EE:FF", doc.get("deviceAddress"));
        assertEquals(1000L, doc.get("timestamp"));
        assertEquals("0xAB3311", doc.get("receivedMsg"));
        assertEquals("user1", doc.get("userId"));
        assertEquals("sensor001", doc.get("sensorId"));
        assertNotNull(doc.get("syncTimestamp"));
        assertNotNull(doc.get("documentId"));
    }

    @Test
    public void testToFirestoreDocumentFieldCount() {
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user1", "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        // Should have 7 fields: deviceAddress, timestamp, receivedMsg, userId, sensorId, syncTimestamp, documentId
        assertEquals(7, doc.size());
    }

    // ========== Deserialization Tests ==========

    @Test
    public void testFromFirestoreDocumentReadsAllFields() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("deviceAddress", "AA:BB:CC:DD:EE:FF");
        doc.put("timestamp", 1000L);
        doc.put("receivedMsg", "0xAB3311");
        doc.put("userId", "user1");
        doc.put("sensorId", "sensor001");
        doc.put("syncTimestamp", 2000L);
        doc.put("documentId", "user1_AABBCCDDEEFF_sensor001_1000");

        FirestoreDataModel model = FirestoreDataModel.fromFirestoreDocument(doc);

        assertEquals("AA:BB:CC:DD:EE:FF", model.getDeviceAddress());
        assertEquals(1000L, model.getTimestamp());
        assertEquals("0xAB3311", model.getReceivedMsg());
        assertEquals("user1", model.getUserId());
        assertEquals("sensor001", model.getSensorId());
        assertEquals(2000L, model.getSyncTimestamp());
        assertEquals("user1_AABBCCDDEEFF_sensor001_1000", model.getDocumentId());
    }

    @Test
    public void testFromFirestoreDocumentHandlesMissingFields() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("deviceAddress", "AA:BB:CC:DD:EE:FF");
        doc.put("timestamp", 1000L);
        // Missing: receivedMsg, userId, sensorId, syncTimestamp, documentId

        FirestoreDataModel model = FirestoreDataModel.fromFirestoreDocument(doc);

        assertEquals("AA:BB:CC:DD:EE:FF", model.getDeviceAddress());
        assertEquals(1000L, model.getTimestamp());
        assertNull(model.getReceivedMsg());
        assertNull(model.getUserId());
        assertNull(model.getSensorId());
        assertEquals(0L, model.getSyncTimestamp()); // Defaults to 0
        assertNull(model.getDocumentId());
    }

    @Test
    public void testFromFirestoreDocumentHandlesNullTimestamp() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("deviceAddress", "AA:BB:CC:DD:EE:FF");
        doc.put("timestamp", null);
        doc.put("syncTimestamp", null);

        FirestoreDataModel model = FirestoreDataModel.fromFirestoreDocument(doc);

        assertEquals(0L, model.getTimestamp());
        assertEquals(0L, model.getSyncTimestamp());
    }

    // ========== Round-Trip Tests ==========

    @Test
    public void testSerializeDeserializeRoundTrip() {
        FirestoreDataModel original = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1234567890L, "0xAB3311", "aggregator1", "sensor001");

        // Serialize
        Map<String, Object> doc = original.toFirestoreDocument();

        // Deserialize
        FirestoreDataModel restored = FirestoreDataModel.fromFirestoreDocument(doc);

        // Verify all fields match
        assertEquals(original.getDeviceAddress(), restored.getDeviceAddress());
        assertEquals(original.getTimestamp(), restored.getTimestamp());
        assertEquals(original.getReceivedMsg(), restored.getReceivedMsg());
        assertEquals(original.getUserId(), restored.getUserId());
        assertEquals(original.getSensorId(), restored.getSensorId());
        assertEquals(original.getSyncTimestamp(), restored.getSyncTimestamp());
        assertEquals(original.getDocumentId(), restored.getDocumentId());
    }

    @Test
    public void testMultipleRoundTrips() {
        String[] sensorIds = {"sensor001", "sensor002", "uuid-format-id", "12345"};
        
        for (String sensorId : sensorIds) {
            FirestoreDataModel original = new FirestoreDataModel(
                    "AA:BB:CC:DD:EE:FF", System.currentTimeMillis(), "0xEF0112", "user", sensorId);

            Map<String, Object> doc = original.toFirestoreDocument();
            FirestoreDataModel restored = FirestoreDataModel.fromFirestoreDocument(doc);

            assertEquals("Failed for sensorId: " + sensorId, 
                    original.getSensorId(), restored.getSensorId());
        }
    }

    // ========== Various SensorId Formats ==========

    @Test
    public void testUuidSensorId() {
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", uuid);

        assertEquals(uuid, model.getSensorId());
        assertTrue(model.getDocumentId().contains(uuid));
    }

    @Test
    public void testSimpleSensorId() {
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", "sensor001");

        assertEquals("sensor001", model.getSensorId());
    }

    @Test
    public void testNumericSensorId() {
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", "12345");

        assertEquals("12345", model.getSensorId());
    }

    // ========== Setter Tests ==========

    @Test
    public void testSetters() {
        FirestoreDataModel model = new FirestoreDataModel();

        model.setDeviceAddress("11:22:33:44:55:66");
        model.setTimestamp(5000L);
        model.setReceivedMsg("0xBA3311");
        model.setUserId("newUser");
        model.setSensorId("newSensor");
        model.setSyncTimestamp(6000L);
        model.setDocumentId("customDocId");

        assertEquals("11:22:33:44:55:66", model.getDeviceAddress());
        assertEquals(5000L, model.getTimestamp());
        assertEquals("0xBA3311", model.getReceivedMsg());
        assertEquals("newUser", model.getUserId());
        assertEquals("newSensor", model.getSensorId());
        assertEquals(6000L, model.getSyncTimestamp());
        assertEquals("customDocId", model.getDocumentId());
    }

    // ========== Edge Cases ==========

    @Test
    public void testEmptyDocument() {
        Map<String, Object> emptyDoc = new HashMap<>();

        FirestoreDataModel model = FirestoreDataModel.fromFirestoreDocument(emptyDoc);

        assertNull(model.getDeviceAddress());
        assertEquals(0L, model.getTimestamp());
        assertNull(model.getReceivedMsg());
        assertNull(model.getUserId());
        assertNull(model.getSensorId());
    }

    @Test
    public void testSyncTimestampIsAutoSet() {
        long before = System.currentTimeMillis();
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", "sensor");
        long after = System.currentTimeMillis();

        assertTrue("syncTimestamp should be >= before", model.getSyncTimestamp() >= before);
        assertTrue("syncTimestamp should be <= after", model.getSyncTimestamp() <= after);
    }
}
