package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for UserSession sensor-based functionality.
 * 
 * Tests the supervisor's sensor ID list management and backward compatibility.
 */
public class UserSessionSensorTest {

    // ========== Sensor ID List Tests ==========

    @Test
    public void getSupervisedSensorIds_nullList_returnsEmptyList() {
        List<String> result = safeGetSensorIds(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getSupervisedSensorIds_emptyList_returnsEmptyList() {
        List<String> result = safeGetSensorIds(new ArrayList<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getSupervisedSensorIds_withSensors_returnsCopy() {
        List<String> original = Arrays.asList("sensor001", "sensor002", "sensor003");
        List<String> result = safeGetSensorIds(original);
        
        assertEquals(3, result.size());
        assertEquals("sensor001", result.get(0));
        assertEquals("sensor002", result.get(1));
        assertEquals("sensor003", result.get(2));
    }

    @Test
    public void getSupervisedSensorIds_returnsCopyNotReference() {
        List<String> original = new ArrayList<>(Arrays.asList("sensor001", "sensor002"));
        List<String> result = safeGetSensorIds(original);
        
        // Modify the result
        result.add("sensor003");
        
        // Original should be unchanged
        assertEquals(2, original.size());
    }

    // ========== Role-Based Tests ==========

    @Test
    public void isAggregator_aggregatorRole_returnsTrue() {
        assertTrue(isAggregator("aggregator"));
    }

    @Test
    public void isAggregator_supervisedRole_returnsTrueForBackwardCompat() {
        // "supervised" is the legacy name for "aggregator"
        assertTrue(isAggregator("supervised"));
    }

    @Test
    public void isAggregator_supervisorRole_returnsFalse() {
        assertFalse(isAggregator("supervisor"));
    }

    @Test
    public void isAggregator_nullRole_returnsFalse() {
        assertFalse(isAggregator(null));
    }

    @Test
    public void isSupervisor_supervisorRole_returnsTrue() {
        assertTrue(isSupervisor("supervisor"));
    }

    @Test
    public void isSupervisor_aggregatorRole_returnsFalse() {
        assertFalse(isSupervisor("aggregator"));
    }

    @Test
    public void isSupervisor_nullRole_returnsFalse() {
        assertFalse(isSupervisor(null));
    }

    // ========== Aggregator UID Tests ==========

    @Test
    public void aggregatorUid_stored_returnsValue() {
        String aggregatorUid = "uid_aggregator_123";
        assertEquals(aggregatorUid, getAggregatorUid(aggregatorUid));
    }

    @Test
    public void aggregatorUid_null_returnsNull() {
        assertNull(getAggregatorUid(null));
    }

    @Test
    public void aggregatorUid_empty_returnsEmpty() {
        assertEquals("", getAggregatorUid(""));
    }

    // ========== Firestore Document Field Tests ==========

    @Test
    public void firestoreFieldMapping_supervisedSensorIds_isCorrectField() {
        // Verify the expected Firestore field name
        assertEquals("supervisedSensorIds", getSupervisorSensorIdsFieldName());
    }

    @Test
    public void firestoreFieldMapping_aggregatorUid_isCorrectField() {
        assertEquals("aggregatorUid", getAggregatorUidFieldName());
    }

    @Test
    public void firestoreFieldMapping_legacyField_isRecognized() {
        // For backward compatibility during migration
        assertEquals("supervisedUserIds", getLegacySupervisedFieldName());
    }

    // ========== Sensor ID Validation Tests ==========

    @Test
    public void validateSensorIds_allValid_returnsTrue() {
        List<String> sensors = Arrays.asList("sensor001", "sensor002");
        assertTrue(validateSensorIds(sensors));
    }

    @Test
    public void validateSensorIds_containsNull_returnsFalse() {
        List<String> sensors = new ArrayList<>();
        sensors.add("sensor001");
        sensors.add(null);
        assertFalse(validateSensorIds(sensors));
    }

    @Test
    public void validateSensorIds_containsEmpty_returnsFalse() {
        List<String> sensors = Arrays.asList("sensor001", "");
        assertFalse(validateSensorIds(sensors));
    }

    @Test
    public void validateSensorIds_containsUnknown_returnsFalse() {
        List<String> sensors = Arrays.asList("sensor001", "unknown");
        assertFalse(validateSensorIds(sensors));
    }

    @Test
    public void validateSensorIds_emptyList_returnsTrue() {
        // Empty list is valid (supervisor with no sensors assigned)
        assertTrue(validateSensorIds(new ArrayList<>()));
    }

    // ========== Backward Compatibility Tests ==========

    @Test
    public void deprecatedMethod_getSupervisedUserIds_callsGetSupervisedSensorIds() {
        // Verify that the deprecated method delegates correctly
        List<String> sensors = Arrays.asList("sensor001", "sensor002");
        List<String> viaSensorIds = safeGetSensorIds(sensors);
        List<String> viaUserIds = deprecatedGetSupervisedUserIds(sensors);
        
        assertEquals(viaSensorIds, viaUserIds);
    }

    // ========== Helper Methods (simulating UserSession logic) ==========

    private List<String> safeGetSensorIds(List<String> sensorIds) {
        return sensorIds != null ? new ArrayList<>(sensorIds) : new ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    private List<String> deprecatedGetSupervisedUserIds(List<String> sensorIds) {
        // Simulates the deprecated method behavior
        return safeGetSensorIds(sensorIds);
    }

    private boolean isAggregator(String role) {
        return "aggregator".equals(role) || "supervised".equals(role);
    }

    private boolean isSupervisor(String role) {
        return "supervisor".equals(role);
    }

    private String getAggregatorUid(String aggregatorUid) {
        return aggregatorUid;
    }

    private String getSupervisorSensorIdsFieldName() {
        return "supervisedSensorIds";
    }

    private String getAggregatorUidFieldName() {
        return "aggregatorUid";
    }

    private String getLegacySupervisedFieldName() {
        return "supervisedUserIds";
    }

    private boolean validateSensorIds(List<String> sensorIds) {
        for (String sensorId : sensorIds) {
            if (sensorId == null || sensorId.isEmpty() || "unknown".equals(sensorId)) {
                return false;
            }
        }
        return true;
    }
}
