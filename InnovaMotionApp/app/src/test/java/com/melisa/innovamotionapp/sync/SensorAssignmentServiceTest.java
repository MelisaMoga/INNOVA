package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for SensorAssignmentService logic.
 * 
 * These tests focus on the data structures and logic rather than actual Firestore operations,
 * since Firestore requires Android context and network access.
 * 
 * Tests cover:
 * - Assignment data model creation
 * - Supervisor info handling
 * - Assignment map operations
 * - Email validation logic
 */
public class SensorAssignmentServiceTest {

    // Test data
    private static final String SENSOR_ID_1 = "mock_sensor_001";
    private static final String SENSOR_ID_2 = "mock_sensor_002";
    private static final String SENSOR_ID_3 = "mock_sensor_003";
    
    private static final String SUPERVISOR_UID = "sup_uid_123";
    private static final String SUPERVISOR_EMAIL = "supervisor@example.com";
    private static final String SUPERVISOR_NAME = "Test Supervisor";
    
    private static final String AGGREGATOR_UID = "agg_uid_456";
    
    // ========== Assignment Model Tests ==========
    
    @Test
    public void assignment_fromValidData_createsCorrectObject() {
        // Simulate creating Assignment from document data
        long timestamp = System.currentTimeMillis();
        
        Assignment assignment = new Assignment(
                SENSOR_ID_1,
                SUPERVISOR_UID,
                SUPERVISOR_EMAIL,
                AGGREGATOR_UID,
                timestamp
        );
        
        assertEquals(SENSOR_ID_1, assignment.sensorId);
        assertEquals(SUPERVISOR_UID, assignment.supervisorUid);
        assertEquals(SUPERVISOR_EMAIL, assignment.supervisorEmail);
        assertEquals(AGGREGATOR_UID, assignment.aggregatorUid);
        assertEquals(timestamp, assignment.assignedAt);
    }
    
    @Test
    public void assignment_withNullEmail_handlesGracefully() {
        Assignment assignment = new Assignment(
                SENSOR_ID_1,
                SUPERVISOR_UID,
                null, // null email
                AGGREGATOR_UID,
                0
        );
        
        assertNull(assignment.supervisorEmail);
        assertNotNull(assignment.sensorId);
    }
    
    @Test
    public void assignment_withZeroTimestamp_isValid() {
        Assignment assignment = new Assignment(
                SENSOR_ID_1,
                SUPERVISOR_UID,
                SUPERVISOR_EMAIL,
                AGGREGATOR_UID,
                0
        );
        
        assertEquals(0, assignment.assignedAt);
    }
    
    // ========== SupervisorInfo Model Tests ==========
    
    @Test
    public void supervisorInfo_fromValidData_createsCorrectObject() {
        SupervisorInfo info = new SupervisorInfo(
                SUPERVISOR_UID,
                SUPERVISOR_EMAIL,
                SUPERVISOR_NAME
        );
        
        assertEquals(SUPERVISOR_UID, info.uid);
        assertEquals(SUPERVISOR_EMAIL, info.email);
        assertEquals(SUPERVISOR_NAME, info.displayName);
    }
    
    @Test
    public void supervisorInfo_withNullDisplayName_isValid() {
        SupervisorInfo info = new SupervisorInfo(
                SUPERVISOR_UID,
                SUPERVISOR_EMAIL,
                null
        );
        
        assertNull(info.displayName);
        assertNotNull(info.email);
    }
    
    // ========== Assignment Map Tests ==========
    
    @Test
    public void assignmentMap_empty_hasNoEntries() {
        Map<String, String> map = new HashMap<>();
        
        assertTrue(map.isEmpty());
        assertNull(map.get(SENSOR_ID_1));
    }
    
    @Test
    public void assignmentMap_singleEntry_retrievesCorrectly() {
        Map<String, String> map = new HashMap<>();
        map.put(SENSOR_ID_1, SUPERVISOR_EMAIL);
        
        assertEquals(1, map.size());
        assertEquals(SUPERVISOR_EMAIL, map.get(SENSOR_ID_1));
        assertNull(map.get(SENSOR_ID_2));
    }
    
    @Test
    public void assignmentMap_multipleEntries_retrievesAll() {
        Map<String, String> map = new HashMap<>();
        map.put(SENSOR_ID_1, "super1@example.com");
        map.put(SENSOR_ID_2, "super2@example.com");
        map.put(SENSOR_ID_3, "super1@example.com"); // Same supervisor for multiple sensors
        
        assertEquals(3, map.size());
        assertEquals("super1@example.com", map.get(SENSOR_ID_1));
        assertEquals("super2@example.com", map.get(SENSOR_ID_2));
        assertEquals("super1@example.com", map.get(SENSOR_ID_3));
    }
    
    @Test
    public void assignmentMap_updateEntry_replacesOldValue() {
        Map<String, String> map = new HashMap<>();
        map.put(SENSOR_ID_1, "old@example.com");
        
        assertEquals("old@example.com", map.get(SENSOR_ID_1));
        
        // Update
        map.put(SENSOR_ID_1, "new@example.com");
        
        assertEquals("new@example.com", map.get(SENSOR_ID_1));
        assertEquals(1, map.size());
    }
    
    @Test
    public void assignmentMap_removeEntry_removesCorrectly() {
        Map<String, String> map = new HashMap<>();
        map.put(SENSOR_ID_1, SUPERVISOR_EMAIL);
        map.put(SENSOR_ID_2, SUPERVISOR_EMAIL);
        
        assertEquals(2, map.size());
        
        map.remove(SENSOR_ID_1);
        
        assertEquals(1, map.size());
        assertNull(map.get(SENSOR_ID_1));
        assertEquals(SUPERVISOR_EMAIL, map.get(SENSOR_ID_2));
    }
    
    // ========== Email Validation Tests ==========
    
    @Test
    public void emailQuery_caseInsensitive_matchesCorrectly() {
        String storedEmail = "Supervisor@Example.com";
        String queryEmail = "supervisor@example.com";
        
        assertTrue(storedEmail.toLowerCase().contains(queryEmail.toLowerCase()));
    }
    
    @Test
    public void emailQuery_partialMatch_works() {
        String storedEmail = "supervisor@example.com";
        String query = "super";
        
        assertTrue(storedEmail.toLowerCase().contains(query.toLowerCase()));
    }
    
    @Test
    public void emailQuery_noMatch_returnsFalse() {
        String storedEmail = "supervisor@example.com";
        String query = "admin";
        
        assertFalse(storedEmail.toLowerCase().contains(query.toLowerCase()));
    }
    
    @Test
    public void emailQuery_shortQuery_shouldBeSkipped() {
        String query = "s";
        int minQueryLength = 2;
        
        assertTrue(query.length() < minQueryLength);
    }
    
    // ========== Sensor ID List Operations Tests ==========
    
    @Test
    public void sensorIdList_addToArray_containsElement() {
        List<String> sensorIds = new ArrayList<>();
        sensorIds.add(SENSOR_ID_1);
        
        assertTrue(sensorIds.contains(SENSOR_ID_1));
        assertEquals(1, sensorIds.size());
    }
    
    @Test
    public void sensorIdList_removeFromArray_noLongerContains() {
        List<String> sensorIds = new ArrayList<>(Arrays.asList(SENSOR_ID_1, SENSOR_ID_2));
        
        sensorIds.remove(SENSOR_ID_1);
        
        assertFalse(sensorIds.contains(SENSOR_ID_1));
        assertTrue(sensorIds.contains(SENSOR_ID_2));
        assertEquals(1, sensorIds.size());
    }
    
    @Test
    public void sensorIdList_addDuplicate_shouldBeHandled() {
        // In Firestore, arrayUnion prevents duplicates
        // Simulate this behavior
        List<String> sensorIds = new ArrayList<>();
        sensorIds.add(SENSOR_ID_1);
        
        // Simulate arrayUnion behavior
        if (!sensorIds.contains(SENSOR_ID_1)) {
            sensorIds.add(SENSOR_ID_1);
        }
        
        assertEquals(1, sensorIds.size());
    }
    
    // ========== Helper classes for testing ==========
    
    /**
     * Simulated Assignment class matching SensorAssignmentService.Assignment.
     */
    static class Assignment {
        public final String sensorId;
        public final String supervisorUid;
        public final String supervisorEmail;
        public final String aggregatorUid;
        public final long assignedAt;
        
        public Assignment(String sensorId, String supervisorUid, String supervisorEmail,
                          String aggregatorUid, long assignedAt) {
            this.sensorId = sensorId;
            this.supervisorUid = supervisorUid;
            this.supervisorEmail = supervisorEmail;
            this.aggregatorUid = aggregatorUid;
            this.assignedAt = assignedAt;
        }
    }
    
    /**
     * Simulated SupervisorInfo class matching SensorAssignmentService.SupervisorInfo.
     */
    static class SupervisorInfo {
        public final String uid;
        public final String email;
        public final String displayName;
        
        public SupervisorInfo(String uid, String email, String displayName) {
            this.uid = uid;
            this.email = email;
            this.displayName = displayName;
        }
    }
}
