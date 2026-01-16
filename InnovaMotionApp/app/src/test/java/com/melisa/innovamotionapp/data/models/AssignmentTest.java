package com.melisa.innovamotionapp.data.models;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for Assignment POJO.
 * 
 * Tests cover:
 * - Constructor field assignment
 * - Document ID generation (format: {supervisorUid}_{sensorId})
 * - Serialization to Firestore document
 * - Deserialization from Firestore document
 * - Round-trip: serialize then deserialize
 * - Timestamp auto-generation
 */
public class AssignmentTest {

    private static final String TEST_SUPERVISOR_UID = "supervisor_uid_123";
    private static final String TEST_SENSOR_ID = "sensor001";
    private static final String TEST_ASSIGNED_BY = "aggregator_uid_456";

    // ========== Constructor Tests ==========

    @Test
    public void testConstructorSetsAllFields() {
        Assignment assignment = new Assignment(TEST_SUPERVISOR_UID, TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        
        assertEquals(TEST_SUPERVISOR_UID, assignment.getSupervisorUid());
        assertEquals(TEST_SENSOR_ID, assignment.getSensorId());
        assertEquals(TEST_ASSIGNED_BY, assignment.getAssignedBy());
        assertTrue(assignment.getAssignedAt() > 0);
    }

    @Test
    public void testDefaultConstructor() {
        Assignment assignment = new Assignment();
        
        assertNull(assignment.getSupervisorUid());
        assertNull(assignment.getSensorId());
        assertNull(assignment.getAssignedBy());
        assertEquals(0L, assignment.getAssignedAt());
    }

    // ========== Document ID Generation Tests ==========

    @Test
    public void testGenerateDocumentIdFormat() {
        String docId = Assignment.generateDocumentId(TEST_SUPERVISOR_UID, TEST_SENSOR_ID);
        
        assertEquals("supervisor_uid_123_sensor001", docId);
    }

    @Test
    public void testGenerateDocumentIdWithDifferentValues() {
        String docId = Assignment.generateDocumentId("user_abc", "sensorXYZ");
        
        assertEquals("user_abc_sensorXYZ", docId);
    }

    @Test
    public void testGetDocumentIdInstanceMethod() {
        Assignment assignment = new Assignment(TEST_SUPERVISOR_UID, TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        
        String docId = assignment.getDocumentId();
        
        assertEquals("supervisor_uid_123_sensor001", docId);
    }

    @Test
    public void testDocumentIdUniquenessWithDifferentSupervisors() {
        String docId1 = Assignment.generateDocumentId("supervisor1", TEST_SENSOR_ID);
        String docId2 = Assignment.generateDocumentId("supervisor2", TEST_SENSOR_ID);
        
        assertNotEquals(docId1, docId2);
    }

    @Test
    public void testDocumentIdUniquenessWithDifferentSensors() {
        String docId1 = Assignment.generateDocumentId(TEST_SUPERVISOR_UID, "sensor001");
        String docId2 = Assignment.generateDocumentId(TEST_SUPERVISOR_UID, "sensor002");
        
        assertNotEquals(docId1, docId2);
    }

    // ========== Serialization Tests ==========

    @Test
    public void testToFirestoreDocumentIncludesAllFields() {
        Assignment assignment = new Assignment(TEST_SUPERVISOR_UID, TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        
        Map<String, Object> doc = assignment.toFirestoreDocument();
        
        assertEquals(TEST_SUPERVISOR_UID, doc.get("supervisorUid"));
        assertEquals(TEST_SENSOR_ID, doc.get("sensorId"));
        assertEquals(TEST_ASSIGNED_BY, doc.get("assignedBy"));
        assertNotNull(doc.get("assignedAt"));
    }

    @Test
    public void testToFirestoreDocumentFieldCount() {
        Assignment assignment = new Assignment(TEST_SUPERVISOR_UID, TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        
        Map<String, Object> doc = assignment.toFirestoreDocument();
        
        // Should have 4 fields: supervisorUid, sensorId, assignedBy, assignedAt
        assertEquals(4, doc.size());
    }

    // ========== Deserialization Tests ==========

    @Test
    public void testFromDocumentReadsAllFields() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("supervisorUid", TEST_SUPERVISOR_UID);
        doc.put("sensorId", TEST_SENSOR_ID);
        doc.put("assignedBy", TEST_ASSIGNED_BY);
        doc.put("assignedAt", 1234567890L);
        
        Assignment assignment = createAssignmentFromMap(doc);
        
        assertEquals(TEST_SUPERVISOR_UID, assignment.getSupervisorUid());
        assertEquals(TEST_SENSOR_ID, assignment.getSensorId());
        assertEquals(TEST_ASSIGNED_BY, assignment.getAssignedBy());
        assertEquals(1234567890L, assignment.getAssignedAt());
    }

    @Test
    public void testFromDocumentHandlesMissingFields() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("supervisorUid", TEST_SUPERVISOR_UID);
        doc.put("sensorId", TEST_SENSOR_ID);
        // Missing: assignedBy, assignedAt
        
        Assignment assignment = createAssignmentFromMap(doc);
        
        assertEquals(TEST_SUPERVISOR_UID, assignment.getSupervisorUid());
        assertEquals(TEST_SENSOR_ID, assignment.getSensorId());
        assertNull(assignment.getAssignedBy());
        assertEquals(0L, assignment.getAssignedAt());
    }

    @Test
    public void testFromDocumentHandlesNullAssignedAt() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("supervisorUid", TEST_SUPERVISOR_UID);
        doc.put("sensorId", TEST_SENSOR_ID);
        doc.put("assignedBy", TEST_ASSIGNED_BY);
        doc.put("assignedAt", null);
        
        Assignment assignment = createAssignmentFromMap(doc);
        
        assertEquals(0L, assignment.getAssignedAt());
    }

    // ========== Round-Trip Tests ==========

    @Test
    public void testSerializeDeserializeRoundTrip() {
        Assignment original = new Assignment(TEST_SUPERVISOR_UID, TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        
        // Serialize
        Map<String, Object> doc = original.toFirestoreDocument();
        
        // Deserialize
        Assignment restored = createAssignmentFromMap(doc);
        
        assertEquals(original.getSupervisorUid(), restored.getSupervisorUid());
        assertEquals(original.getSensorId(), restored.getSensorId());
        assertEquals(original.getAssignedBy(), restored.getAssignedBy());
        assertEquals(original.getAssignedAt(), restored.getAssignedAt());
    }

    @Test
    public void testRoundTripPreservesDocumentId() {
        Assignment original = new Assignment(TEST_SUPERVISOR_UID, TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        String originalDocId = original.getDocumentId();
        
        Map<String, Object> doc = original.toFirestoreDocument();
        Assignment restored = createAssignmentFromMap(doc);
        
        // Document ID should be reconstructible from the restored data
        assertEquals(originalDocId, restored.getDocumentId());
    }

    // ========== Timestamp Tests ==========

    @Test
    public void testAssignedAtIsAutoSet() {
        long before = System.currentTimeMillis();
        Assignment assignment = new Assignment(TEST_SUPERVISOR_UID, TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        long after = System.currentTimeMillis();
        
        assertTrue(assignment.getAssignedAt() >= before);
        assertTrue(assignment.getAssignedAt() <= after);
    }

    // ========== Setter Tests ==========

    @Test
    public void testSetters() {
        Assignment assignment = new Assignment();
        
        assignment.setSupervisorUid("new_supervisor");
        assignment.setSensorId("new_sensor");
        assignment.setAssignedBy("new_aggregator");
        assignment.setAssignedAt(9999L);
        
        assertEquals("new_supervisor", assignment.getSupervisorUid());
        assertEquals("new_sensor", assignment.getSensorId());
        assertEquals("new_aggregator", assignment.getAssignedBy());
        assertEquals(9999L, assignment.getAssignedAt());
    }

    // ========== Multiple Supervisors Per Sensor Tests ==========

    @Test
    public void testMultipleSupervisorsCanHaveSameSensor() {
        Assignment assignment1 = new Assignment("supervisor1", TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        Assignment assignment2 = new Assignment("supervisor2", TEST_SENSOR_ID, TEST_ASSIGNED_BY);
        
        // Both assignments are valid - same sensor, different supervisors
        assertEquals(TEST_SENSOR_ID, assignment1.getSensorId());
        assertEquals(TEST_SENSOR_ID, assignment2.getSensorId());
        
        // But they have different document IDs
        assertNotEquals(assignment1.getDocumentId(), assignment2.getDocumentId());
    }

    // ========== Helper Methods ==========

    /**
     * Simulates creating an Assignment from a Firestore document map.
     * This mimics what Assignment.fromDocument(DocumentSnapshot) does.
     */
    private Assignment createAssignmentFromMap(Map<String, Object> doc) {
        Assignment assignment = new Assignment();
        assignment.setSupervisorUid((String) doc.get("supervisorUid"));
        assignment.setSensorId((String) doc.get("sensorId"));
        assignment.setAssignedBy((String) doc.get("assignedBy"));
        
        Object assignedAtVal = doc.get("assignedAt");
        assignment.setAssignedAt(assignedAtVal instanceof Long ? (Long) assignedAtVal : 0L);
        
        return assignment;
    }
}
